import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { KeycloakService } from 'keycloak-angular';
import { throwError } from 'rxjs';

import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';

import { Factura } from '../../models/factura';
import { Documento } from '../../models/documento';
import { FacturaService } from '../../services/factura.service';
import { DocumentoService } from '../../services/documento.service';
import { CausalDevolucionService } from '../../services/causal-devolucion.service';
import { ObservacionService } from '../../services/observacion.service';
import { FaseService } from '../../services/fase.service';

@Component({
  selector: 'app-reconocimiento-contable',
  standalone: true,
  imports: [
    CommonModule,
    DataTableComponent,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './reconocimiento-contable.component.html',
  styleUrl: './reconocimiento-contable.component.css'
})
export class ReconocimientoContableComponent extends CommonListarComponent<Factura, FacturaService> implements OnInit {

  override titulo = 'Reconocimiento Contable (Etapa 2)';

  tabSeleccionada: number = 0;
  facturaSeleccionada: Factura | null = null;
  soportesFactura: Documento[] = [];
  pdfUrlSafe: SafeResourceUrl | null = null;
  documentoActivo: string = '';

  esGestorF2: boolean = false;

  opcionesCausal: { value: any, label: string }[] = [];
  opcionesObservacion: { value: any, label: string }[] = [];
  mapaFases: { [key: number]: string } = {};

  // Opciones de Tipo de Registro Contable mapeadas con sus IDs numéricos
  opcionesTipoRegistro = [
    { value: 1, label: 'FC - Factura de Compra' },
    { value: 2, label: 'GV - Gastos de Viáticos' },
    { value: 3, label: 'ORC - Otros Registros Contables' },
    { value: 4, label: 'NI - Nota Interna' }
  ];

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'estado', header: 'Estado' },
    { field: 'nit', header: 'NIT Emisor' },
    { field: 'razonSocialEmisor', header: 'Razón Social' },
    { field: 'numeroFactura', header: 'No. Factura' },
    { field: 'valorTotal', header: 'Valor Total' },
    { field: 'fechaEmision', header: 'Fecha Emisión' },
    { field: 'cufe', header: 'CUFE' },
    { field: 'faseNombre', header: 'Fase / Etapa' },
    { field: 'observacion', header: 'Observación' }
  ];

  columnasSoportes = [
    { field: 'id', header: 'ID' },
    { field: 'nombreOriginal', header: 'Nombre Archivo' },
    { field: 'tipoId', header: 'Tipo ID' }
  ];

  constructor(
    service: FacturaService,
    private documentoService: DocumentoService,
    private causalService: CausalDevolucionService,
    private observacionService: ObservacionService,
    private faseService: FaseService,
    private keycloakService: KeycloakService,
    private dialog: MatDialog,
    private sanitizer: DomSanitizer
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.evaluarRolesUsuario();
    this.cargarFases();
    this.cargarListasMaestras();
  }

  private evaluarRolesUsuario(): void {
    const roles = this.keycloakService.getUserRoles();
    this.esGestorF2 = roles.includes('admin') || roles.includes('gestor-fe-f2-rc') ||
                      roles.includes('gestor-fe-admin') || roles.includes('default-roles-fe');
  }

  private cargarFases(): void {
    this.faseService.listar().subscribe({
      next: (fases: any[]) => {
        this.mapaFases = (fases || [])
          .filter((f: any) => !f.deletedAt)
          .reduce((acc: any, f: any) => {
            acc[f.id] = f.descripcion || f.nombre || `Fase ${f.id}`;
            return acc;
          }, {});

        this.cargarDatosPaginados();
      },
      error: (err) => {
        console.error('Error al cargar fases:', err);
        this.cargarDatosPaginados();
      }
    });
  }

  cargarDatosPaginados(): void {
    this.service.getFaseActiva(2, this.paginaActual, this.totalPorPagina)
      .subscribe(res => {
        this.lista = this.mapearFaseNombre(res.content);
        this.totalRegistros = res.totalElements;
      });
  }

  private mapearFaseNombre(facturas: Factura[]): any[] {
    return (facturas || []).map(f => ({
      ...f,
      faseNombre: this.mapaFases[f.faseId] || `Fase ${f.faseId}`
    }));
  }

  private cargarListasMaestras(): void {
    this.causalService.listar().subscribe(data => {
      this.opcionesCausal = (data || [])
        .filter(c => !c.deletedAt)
        .map(c => ({
          value: c.id,
          label: `${c.codigo || ''} - ${c.descripcion}`
        }));
    });

    this.observacionService.listar().subscribe(data => {
      this.opcionesObservacion = (data || [])
        .filter(o => !o.deletedAt)
        .map(o => ({
          value: o.descripcion,
          label: o.descripcion
        }));
      this.opcionesObservacion.push({ value: 'OTRO', label: 'OTRO (Especificar texto libre...)' });
    });
  }

  verSoportesFactura(row: Factura): void {
    this.facturaSeleccionada = row;
    this.pdfUrlSafe = null;
    this.documentoActivo = '';

    this.documentoService.filtrarDocumentosPaginado(
      row.numeroFactura,
      row.nit,
      null,
      '0',
      '50'
    ).subscribe({
      next: (res: any) => {
        this.soportesFactura = res.content || [];
        this.tabSeleccionada = 1;
      },
      error: (err) => {
        console.error('Error al consultar soportes:', err);
        alert('No se pudieron consultar los soportes de esta factura.');
      }
    });
  }

  cargarSoporteEnVisor(doc: Documento): void {
    this.documentoActivo = doc.nombreOriginal;

    this.documentoService.getDocumentoBlob(doc.id).subscribe({
      next: (blob: Blob) => {
        const fileUrl = URL.createObjectURL(blob);
        this.pdfUrlSafe = this.sanitizer.bypassSecurityTrustResourceUrl(fileUrl);
      },
      error: (err) => {
        console.error('Error al cargar la vista previa:', err);
        alert('Este archivo no se puede previsualizar en el navegador.');
      }
    });
  }

  regresarABandeja(): void {
    this.tabSeleccionada = 0;
    this.facturaSeleccionada = null;
    this.pdfUrlSafe = null;
    this.documentoActivo = '';
  }

  /**
   * 📝 Configuración de campos dinámicos obligatorios
   */
  getCamposGestion() {
    return [
      {
        name: 'estadoAccion',
        label: 'Dictamen de la Factura',
        type: 'select',
        required: true,
        options: [
          { value: 'APROBADO', label: 'Aprobar (Causar y enviar a Impuestos)' },
          { value: 'RECHAZADO', label: 'Rechazar (Devolver factura electrónica)' }
        ],
        onChange: (val: string, campos: any[], form: FormGroup) => {
          const isAprobado = val === 'APROBADO';
          const isRechazado = val === 'RECHAZADO';

          // CAMPOS DE APROBACIÓN
          this.toggleCampoVisibilidad(campos, form, 'tipoRegistroContableId', isAprobado, true);
          this.toggleCampoVisibilidad(campos, form, 'numeroCausacion', isAprobado, true);
          this.toggleCampoVisibilidad(campos, form, 'archivoCausacion', isAprobado, true);

          // CAMPOS DE RECHAZO
          this.toggleCampoVisibilidad(campos, form, 'causalDevolucionId', isRechazado, true);
          this.toggleCampoVisibilidad(campos, form, 'observacionId', isRechazado, true);
        }
      },
      {
        name: 'tipoRegistroContableId', // 👈 Mapeado como ID numérico
        label: 'Tipo de Registro Contable',
        type: 'select',
        options: this.opcionesTipoRegistro,
        visible: false
      },
      {
        name: 'numeroCausacion',
        label: 'Número de Documento de Causación',
        type: 'text',
        placeholder: 'Ej: CAUS-2026-0089',
        visible: false
      },
      {
        name: 'archivoCausacion',
        label: 'Soporte de Causación (PDF)',
        type: 'file',
        accept: '.pdf',
        visible: false
      },
      {
        name: 'causalDevolucionId',
        label: 'Causal de Devolución',
        type: 'select',
        options: this.opcionesCausal,
        visible: false
      },
      {
        name: 'observacionId',
        label: 'Observaciones Predeterminadas',
        type: 'select',
        options: this.opcionesObservacion,
        visible: false,
        onChange: (val: any, campos: any[], form: FormGroup) => {
          this.toggleCampoVisibilidad(campos, form, 'observacion', val === 'OTRO', val === 'OTRO');
        }
      },
      {
        name: 'observacion',
        label: 'Detalle de la Observación (Otro)',
        type: 'textarea',
        placeholder: 'Escriba la razón de la devolución...',
        visible: false
      }
    ];
  }

  private toggleCampoVisibilidad(campos: any[], form: FormGroup, fieldName: string, visible: boolean, esRequerido: boolean = true): void {
    const campo = campos.find(c => c.name === fieldName);
    if (campo) campo.visible = visible;

    const control = form.get(fieldName);
    if (control) {
      if (visible && esRequerido) {
        control.setValidators([Validators.required]);
      } else {
        control.clearValidators();
        control.setValue('');
      }
      control.updateValueAndValidity();
    }
  }

  abrirModalGestionar(row: Factura): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '600px',
      data: {
        titulo: `Reconocimiento Contable - Factura No. ${row.numeroFactura}`,
        campos: this.getCamposGestion(),
        formData: { id: row.id },
        service: {
          editar: (model: any) => {
            // 👤 Inyectar nombre del usuario para el historial de auditoría
            try {
              model.usuario = this.keycloakService.getUsername() || 'SISTEMA';
            } catch (error) {
              console.warn('No se pudo obtener el username de Keycloak. Se asigna valor por defecto:', error);
              model.usuario = 'GESTOR_SISTEMA';
            }

            if (model.estadoAccion === 'APROBADO') {

              // 🎯 CAPTURA DEL ARCHIVO DESDE EL INPUT DEL DOM
              let archivoFile: File | undefined = undefined;

              const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
              if (fileInput && fileInput.files && fileInput.files.length > 0) {
                archivoFile = fileInput.files[0];
              } else {
                const rawVal = model.archivoCausacion;
                if (rawVal) {
                  if (rawVal instanceof File) archivoFile = rawVal;
                  else if (rawVal instanceof FileList) archivoFile = rawVal[0];
                  else if (rawVal.target?.files?.[0]) archivoFile = rawVal.target.files[0];
                }
              }

              // 🛑 VALIDACIÓN ESTRICTA
              if (!archivoFile) {
                alert('⚠️ Debe adjuntar obligatoriamente el archivo PDF con el Soporte de Causación.');
                return throwError(() => new Error('El archivo soporte de causación es obligatorio.'));
              }

              return this.service.procesarCausacionFase2(
                row.id,
                Number(model.tipoRegistroContableId), // 👈 Mapeado como ID numérico
                model.numeroCausacion,
                archivoFile
              );

            } else {
              // RECHAZO
              if (model.causalDevolucionId) {
                model.causalDevolucionId = Number(model.causalDevolucionId);
              }
              if (model.observacionId && model.observacionId !== 'OTRO') {
                model.observacion = model.observacionId;
              }
              return this.service.procesarTransicionFase(row.id, 2, model);
            }
          }
        }
      }
    });

    dialogRef.afterClosed().subscribe(resultado => {
      if (resultado) {
        this.regresarABandeja();
        this.cargarDatosPaginados();
      }
    });
  }

  buscar(texto: string): void {
    if (!texto || texto.trim() === '') {
      this.cargarDatosPaginados();
      return;
    }

    this.service.buscar(texto).subscribe(response => {
      this.lista = this.mapearFaseNombre(response);
      this.totalRegistros = response.length;
    });
  }
}
