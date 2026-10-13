import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// 📦 IMPORTS DE ANGULAR MATERIAL
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

// 🔌 SERVICIOS Y MODELOS
import { TipoService } from '../../services/tipo.service';
import { PrestadorService } from '../../services/prestador.service';
import { DocumentoService } from '../../services/documento.service';
import { AlertService } from '../../services/alert.service';
import { LoginService } from '../../services/login.service'; // 👈 Inyectamos LoginService
import { Tipo } from '../../models/tipo';
import { Prestador } from '../../models/prestador';
import { Documento } from '../../models/documento';

@Component({
  selector: 'app-prestador',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule
  ],
  templateUrl: './prestador.component.html',
  styleUrls: ['./prestador.component.css']
})
export class PrestadorComponent implements OnInit {

  public Number = Number;

  public nitBusqueda: string = '';
  public prestadorActual: Prestador | null = null;
  public tiposSoporte: Tipo[] = [];
  public soportesCargados: Map<number, Documento> = new Map();
  public cargando: boolean = false;

  // 🚩 Flags de vista según el rol del usuario
  public esUsuarioPrestador: boolean = false;

  constructor(
    private tipoService: TipoService,
    private prestadorService: PrestadorService,
    private documentoService: DocumentoService,
    private alertService: AlertService,
    private loginService: LoginService // 👈 Inyección de LoginService
  ) { }

  ngOnInit(): void {
    this.cargarTiposSoporte();
    this.evaluarRolYAutocargar();
  }

  // 📊 Cálculos dinámicos de completitud de soportes empresariales obligatorios (1 a 4)
  get tiposObligatorios(): Tipo[] {
    return this.tiposSoporte.filter(t => t.id && Number(t.id) >= 1 && Number(t.id) <= 4);
  }

  get totalObligatorios(): number {
    return this.tiposObligatorios.length || 4;
  }

  get cargadosObligatorios(): number {
    return this.tiposObligatorios.filter(t => this.tieneSoporte(t.id)).length;
  }

  get porcentajeProgreso(): number {
    if (this.totalObligatorios === 0) return 0;
    return Math.round((this.cargadosObligatorios / this.totalObligatorios) * 100);
  }

  get estaHabilitadoParaRadicar(): boolean {
    return this.totalObligatorios > 0 && this.cargadosObligatorios >= this.totalObligatorios;
  }

  obtenerIconoTipo(tipo: Tipo): string {
    const desc = (tipo.descripcion || tipo.codigo || '').toUpperCase();
    if (desc.includes('RUT')) return 'assignment_ind';
    if (desc.includes('CAMARA') || desc.includes('COMERCIO')) return 'corporate_fare';
    if (desc.includes('BANCARI')) return 'account_balance';
    if (desc.includes('CONTRATO')) return 'history_edu';
    return 'description';
  }

  esTipoObligatorio(tipo: Tipo): boolean {
    return tipo.id !== undefined && Number(tipo.id) >= 1 && Number(tipo.id) <= 4;
  }

  /**
   * 🔑 Evalúa el rol del usuario autenticado desde LoginService
   */
  private evaluarRolYAutocargar(): void {
    const roles = this.loginService.getUserRoles();
    
    // Si tiene rol de prestador y NO es admin/gestor
    this.esUsuarioPrestador = this.loginService.isPrestador && !this.loginService.isAdmin && !this.loginService.isGAdmin;

    if (this.esUsuarioPrestador) {
      // 🎯 Tomamos directamente el username/NIT del usuario de la sesión Keycloak
      const nitSesion = this.loginService.getUserName();
      if (nitSesion && nitSesion !== 'GESTOR_SISTEMA') {
        this.nitBusqueda = nitSesion;
        this.buscarPrestadorConNit(nitSesion);
      }
    }
  }

  // 1. Cargar catálogo de tipos de soporte (excluye PDF/XML de factura)
  cargarTiposSoporte(): void {
    this.tipoService.listar().subscribe({
      next: (tipos: any[]) => {
        this.tiposSoporte = tipos.filter(t => {
          const desc = (t.descripcion || '').toUpperCase();
          const cod = (t.codigo || '').toUpperCase();
          return !desc.includes('XML') && !desc.includes('PDF') && cod !== 'FAC_XML' && cod !== 'FAC_PDF';
        });
      },
      error: (err) => console.error('Error cargando tipos de soporte:', err)
    });
  }

  // 2. Buscar prestador por el NIT del input manual (modo Admin/Gestor)
  buscarPrestador(): void {
    if (!this.nitBusqueda || this.nitBusqueda.trim() === '') {
      this.alertService.advertencia('Ingrese un NIT para realizar la búsqueda.');
      return;
    }
    this.buscarPrestadorConNit(this.nitBusqueda.trim());
  }

  /**
   * 🛠️ Método privado unificado para consulta de Prestador
   */
  private buscarPrestadorConNit(nit: string): void {
    this.cargando = true;
    this.prestadorService.obtenerPorNit(nit).subscribe({
      next: (prestador) => {
        this.prestadorActual = prestador;
        if (prestador.id !== undefined) {
          this.cargarSoportesExistentes(prestador.id);
        }
        this.cargando = false;
      },
      error: () => {
        this.prestadorActual = null;
        this.soportesCargados.clear();
        this.cargando = false;
        
        if (!this.esUsuarioPrestador) {
          this.alertService.info(`No se encontró un prestador registrado con NIT: ${nit}`, 'No encontrado');
        } else {
          this.alertService.error(`No se encontró su registro de Prestador con NIT: ${nit}. Contacte al administrador.`);
        }
      }
    });
  }

  // 3. Cargar los soportes empresariales activos
  cargarSoportesExistentes(prestadorId: number | string): void {
    this.prestadorService.listarSoportes(Number(prestadorId)).subscribe({
      next: (response) => {
        this.soportesCargados.clear();
        const listaDocs: Documento[] = response.content || response;

        listaDocs.forEach(doc => {
          const key = doc.codigoTipo ? Number(doc.codigoTipo) : (doc.tipoId ? Number(doc.tipoId) : null);
          if (key !== null) {
            this.soportesCargados.set(key, doc);
          }
        });
      },
      error: (err) => console.error('Error cargando soportes del prestador:', err)
    });
  }

  // 🧹 Limpiar búsqueda (Sólo visible para Administradores)
  limpiarFiltro(): void {
    if (this.esUsuarioPrestador) return;
    this.nitBusqueda = '';
    this.prestadorActual = null;
    this.soportesCargados.clear();
  }

  // 🛠️ Helpers del template
  tieneSoporte(tipoId: number | string): boolean {
    return this.soportesCargados.has(Number(tipoId));
  }

  obtenerSoporte(tipoId: number | string): Documento | undefined {
    return this.soportesCargados.get(Number(tipoId));
  }

  // 4. Subir un archivo
  onFileSelected(event: any, tipo: Tipo): void {
    const archivo: File = event.target.files[0];
    if (!archivo || !this.prestadorActual) return;

    let codigoExtension = '02';
    const nameLower = archivo.name.toLowerCase();
    if (nameLower.endsWith('.pdf')) {
      codigoExtension = '02';
    } else if (nameLower.endsWith('.xml')) {
      codigoExtension = '01';
    } else if (nameLower.endsWith('.zip')) {
      codigoExtension = '03';
    }

    this.alertService.cargando(`Cargando soporte ${tipo.descripcion || tipo.codigo}`, 'Subiendo archivo...');

    const codTipo = tipo.codigo || (tipo.id ? String(tipo.id) : '');

    this.prestadorService.cargarSoporte(this.prestadorActual.nit, codTipo, codigoExtension, archivo).subscribe({
      next: (docGuardado) => {
        this.soportesCargados.set(Number(tipo.id), docGuardado);
        this.alertService.cerrar();
        this.alertService.toastExito(`Soporte ${tipo.descripcion || tipo.codigo} guardado correctamente.`);
      },
      error: (err) => {
        this.alertService.cerrar();
        this.alertService.toastError('No se pudo cargar el archivo. Inténtelo de nuevo.');
        console.error(err);
      }
    });
  }

  // 5. Visualizar el documento
  verDocumento(doc: Documento): void {
    if (!doc || !doc.id) return;
    this.documentoService.getDocumentoBlob(Number(doc.id)).subscribe({
      next: (blob) => {
        const fileURL = URL.createObjectURL(blob);
        window.open(fileURL, '_blank');
      },
      error: () => this.alertService.toastError('No se pudo generar la vista previa del documento.')
    });
  }

  // 6. Eliminar el soporte
  eliminarSoporte(tipoId: number | string, docId: number | string): void {
    this.alertService.confirmar('Se eliminará el soporte seleccionado.', '¿Está seguro?', 'Sí, eliminar')
      .then((result) => {
        if (result.isConfirmed) {
          this.prestadorService.eliminarSoporte(Number(docId)).subscribe({
            next: () => {
              this.soportesCargados.delete(Number(tipoId));
              this.alertService.toastExito('El soporte ha sido removido correctamente.');
            },
            error: (err) => {
              this.alertService.toastError('No se pudo eliminar el soporte.');
              console.error(err);
            }
          });
        }
      });
  }
}