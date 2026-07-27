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
import { AlertService } from '../../services/alert.service'; // 👈 Inyección del nuevo servicio
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

  constructor(
    private tipoService: TipoService,
    private prestadorService: PrestadorService,
    private documentoService: DocumentoService,
    private alertService: AlertService // 👈 Inyectado aquí
  ) { }

  ngOnInit(): void {
    this.cargarTiposSoporte();
  }

  // 1. Cargar catálogo de tipos
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

  // 2. Buscar prestador por NIT
  buscarPrestador(): void {
    if (!this.nitBusqueda || this.nitBusqueda.trim() === '') {
      this.alertService.advertencia('Ingrese un NIT para realizar la búsqueda.');
      return;
    }

    this.cargando = true;
    this.prestadorService.obtenerPorNit(this.nitBusqueda.trim()).subscribe({
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
        this.alertService.info(`No se encontró un prestador registrado con NIT: ${this.nitBusqueda}`, 'No encontrado');
      }
    });
  }

  // 3. Cargar los soportes
  cargarSoportesExistentes(prestadorId: number | string): void {
    this.prestadorService.listarSoportes(Number(prestadorId)).subscribe({
      next: (response) => {
        this.soportesCargados.clear();
        const listaDocs: Documento[] = response.content || response;

        listaDocs.forEach(doc => {
          if (doc.tipoId) {
            this.soportesCargados.set(Number(doc.tipoId), doc);
          }
        });
      },
      error: (err) => console.error('Error cargando soportes del prestador:', err)
    });
  }

  // 🧹 Limpiar búsqueda
  limpiarFiltro(): void {
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

    const extensionId = archivo.name.toLowerCase().endsWith('.pdf') ? 2 : 1;

    // 🔄 Muestra alerta de carga
    this.alertService.cargando(`Cargando soporte ${tipo.descripcion || tipo.codigo}`, 'Subiendo archivo...');

    this.prestadorService.cargarSoporte(this.prestadorActual.nit, Number(tipo.id), extensionId, archivo).subscribe({
      next: (docGuardado) => {
        this.soportesCargados.set(Number(tipo.id), docGuardado);
        this.alertService.exito(`Soporte ${tipo.descripcion || tipo.codigo} guardado correctamente.`);
      },
      error: (err) => {
        this.alertService.error('No se pudo cargar el archivo. Inténtelo de nuevo.');
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
      error: () => this.alertService.error('No se pudo generar la vista previa del documento.')
    });
  }

  // 6. Eliminar el soporte
  eliminarSoporte(tipoId: number | string, docId: number | string): void {
    // ❓ Confirmación con SweetAlert desde el servicio
    this.alertService.confirmar('Se eliminará el soporte seleccionado.', '¿Está seguro?', 'Sí, eliminar')
      .then((result) => {
        if (result.isConfirmed) {
          this.prestadorService.eliminarSoporte(Number(docId)).subscribe({
            next: () => {
              this.soportesCargados.delete(Number(tipoId));
              this.alertService.exito('El soporte ha sido removido.', 'Eliminado');
            },
            error: (err) => {
              this.alertService.error('No se pudo eliminar el soporte.');
              console.error(err);
            }
          });
        }
      });
  }
}
