import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { Subject, Subscription, throwError } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

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
import { LoginService } from '../../services/login.service';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-pendiente-de-pago',
  standalone: true,
  imports: [
    CommonModule,
    DataTableComponent,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './pendiente-de-pago.component.html',
  styleUrl: './pendiente-de-pago.component.css'
})
export class PendienteDePagoComponent extends CommonListarComponent<Factura, FacturaService> implements OnInit, OnDestroy {

  override titulo = 'Pendiente de Pago - Tesorería (Etapa 4)';

  // 💉 Inyecciones mediante Inject
  private loginService = inject(LoginService);
  private alertService = inject(AlertService);

  tabSeleccionada: number = 0;
  facturaSeleccionada: Factura | null = null;
  soportesFactura: Documento[] = [];
  pdfUrlSafe: SafeResourceUrl | null = null;
  documentoActivo: string = '';

  esGestorF4: boolean = false;
  usuarioActivo: string = '';

  opcionesCausal: { value: any, label: string }[] = [];
  opcionesObservacion: { value: any, label: string }[] = [];
  mapaFases: { [key: number]: string } = {};

  // 🎯 SUBJECT Y SUBSCRIPCIÓN PARA RETARDO DE FILTROS (DEBOUNCE 400ms)
  private filtroSubject = new Subject<{ [key: string]: string }>();
  private filtroSubscription?: Subscription;

  // DTO activo para la API con contexto de Fase 4 por defecto
  filtrosActivos: any = { faseId: 4 };

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'estado', header: 'Estado' },
    { field: 'nit', header: 'NIT Emisor' },
    { field: 'razonSocialEmisor', header: 'Razón Social' },
    { field: 'numeroFactura', header: 'No. Factura' },
    { field: 'valorTotal', header: 'Valor Total' },
    { field: 'fechaEmision', header: 'Fecha Emisión' },
    { field: 'cufe', header: 'CUFE' },
    { field: 'faseNombre', header: 'Fase' },
    { field: 'observacion', header: 'Observación' }
  ];

  columnasSoportes = [
    { field: 'nombreOriginal', header: 'Nombre Archivo' }
  ];

  constructor(
    service: FacturaService,
    private documentoService: DocumentoService,
    private causalService: CausalDevolucionService,
    private observacionService: ObservacionService,
    private faseService: FaseService,
    private dialog: MatDialog,
    private sanitizer: DomSanitizer
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.cargarDatosSesion();
    this.cargarFases();
    this.cargarListasMaestras();
    this.configurarDebounceFiltros();
  }

  override ngOnDestroy(): void {
    if (this.filtroSubscription) {
      this.filtroSubscription.unsubscribe();
    }
  }

  /**
   * 👤 Carga los datos de sesión y evalúa los permisos utilizando LoginService
   */
  private cargarDatosSesion(): void {
    this.usuarioActivo = this.loginService.getUserName();
    this.esGestorF4 = this.loginService.isAdmin ||
                      this.loginService.isGAdmin ||
                      this.loginService.isGFaseCuatro;
  }

  /**
   * ⏱️ Pipeline con debounce de 400ms tras la última tecla pulsada
   */
  private configurarDebounceFiltros(): void {
    this.filtroSubscription = this.filtroSubject.pipe(
      debounceTime(400),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(filtrosColumnas => {
      this.paginaActual = 0; // Reinicia la página al filtrar
      this.procesarFiltrosYConsultar(filtrosColumnas);
    });
  }

  /**
   * 📥 Captura las emisiones de filtros provenientes de DataTableComponent
   */
  override onFiltrosChange(filtrosColumnas: { [key: string]: string }): void {
    this.filtroSubject.next(filtrosColumnas);
  }

  /**
   * 🛠️ Mapea los campos filtrables al DTO que procesará JPA Criteria en el Backend
   */
  private procesarFiltrosYConsultar(filtrosColumnas: { [key: string]: string }): void {
    this.filtrosActivos = {
      faseId: 4, // Mantiene fija la Fase 4
      id: filtrosColumnas['id'] ? Number(filtrosColumnas['id']) : null,
      nit: filtrosColumnas['nit'] || filtrosColumnas['nitEmisor'] || null,
      numeroFactura: filtrosColumnas['numeroFactura'] || null,
      razonSocialEmisor: filtrosColumnas['razonSocialEmisor'] || null,
      cufe: filtrosColumnas['cufe'] || null,
      estado: filtrosColumnas['estado'] || null,
      observacion: filtrosColumnas['observacion'] || null,
      textoBusquedaGlobal: this.filtrosActivos.textoBusquedaGlobal || null
    };

    this.cargarDatosPaginados();
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

  /**
   * 📋 Carga exclusivamente las facturas activas en Fase 4 mediante Criteria
   */
  cargarDatosPaginados(): void {
    this.service.buscarConCriteria(this.filtrosActivos, this.paginaActual, this.totalPorPagina)
      .subscribe({
        next: (res: any) => {
          this.lista = this.mapearFaseNombre(res.content || []);
          this.totalRegistros = res.totalElements || 0;
        },
        error: (err) => {
          console.error('Error al consultar lista por Criteria en Fase 4:', err);
        }
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

  /**
   * 👁️ Carga los soportes documentales de la factura y pasa a Pestaña 2
   */
  /**
   * 👁️ Carga los soportes documentales de la factura y pasa a Pestaña 2
   */
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
        const rawDocs = res.content || [];
        this.soportesFactura = rawDocs.map((doc: any) => ({
          ...doc,
          permitirEliminar: this.esGestorF4 && row.faseId === 4 && (doc.ruta?.includes('_TB_') || doc.ruta?.includes('_PAGO_'))
        }));
        this.tabSeleccionada = 1;
      },
      error: (err) => {
        console.error('Error al consultar soportes:', err);
        this.alertService.error('No se pudieron consultar los soportes de esta factura.');
      }
    });
  }

  /**
   * 🗑️ Inactivación (borrado lógico) de soporte de Tesorería desde el visor
   */
  eliminarSoporte(doc: Documento): void {
    if (!doc || !doc.id) return;
    this.alertService.confirmar(
      `¿Está seguro de inactivar el soporte "${doc.nombreOriginal}"?`,
      'Confirmar Inactivación'
    ).then((result) => {
      if (result.isConfirmed) {
        this.alertService.cargando('Inactivando soporte...', 'Procesando');
        this.documentoService.inactivarDocumento(doc.id).subscribe({
          next: () => {
            this.alertService.exito('Soporte inactivado correctamente.', 'Operación Completada');
            if (this.facturaSeleccionada) {
              this.verSoportesFactura(this.facturaSeleccionada);
            }
          },
          error: (err) => {
            console.error('Error al inactivar soporte:', err);
            this.alertService.error('No se pudo inactivar el soporte seleccionado.');
          }
        });
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
        this.alertService.advertencia('Este archivo no se puede previsualizar directamente en el navegador.');
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
   * 📝 Campos dinámicos del modal para la Etapa 4 (Tesorería)
   */
  getCamposGestion(tieneTbActivo: boolean = false, tienePagoActivo: boolean = false) {
    return [
      {
        name: 'estadoAccion',
        label: 'Dictamen de Pago / Tesorería',
        type: 'select',
        required: true,
        options: [
          { value: 'APROBADO', label: 'Aprobar (Registrar Pago TB y Finalizar Proceso)' },
          { value: 'RECHAZADO', label: 'Rechazar (Devolución de factura)' }
        ],
        onChange: (val: string, campos: any[], form: FormGroup) => {
          const isAprobado = val === 'APROBADO';
          const isRechazado = val === 'RECHAZADO';

          // CAMPOS DE APROBACIÓN DE PAGO
          this.toggleCampoVisibilidad(campos, form, 'numeroCausacion', isAprobado, false);
          this.toggleCampoVisibilidad(campos, form, 'soporteTb', isAprobado, !tieneTbActivo);
          this.toggleCampoVisibilidad(campos, form, 'comprobantePago', isAprobado, !tienePagoActivo);

          // CAMPOS DE RECHAZO
          this.toggleCampoVisibilidad(campos, form, 'causalDevolucionId', isRechazado, true);
          this.toggleCampoVisibilidad(campos, form, 'observacionId', isRechazado, true);
        }
      },
      {
        name: 'numeroCausacion',
        label: 'Número de Comprobante / Egreso (TB)',
        type: 'text',
        placeholder: 'Ej: TB-2026-0045',
        visible: false
      },
      {
        name: 'soporteTb',
        label: tieneTbActivo
          ? 'Documento Registro Contable TB (PDF) - Opcional (Ya existe activo)'
          : 'Documento Registro Contable TB (PDF) * [Requerido]',
        type: 'file',
        accept: '.pdf',
        visible: false
      },
      {
        name: 'comprobantePago',
        label: tienePagoActivo
          ? 'Comprobante de Pago Bancario (PDF) - Opcional (Ya existe activo)'
          : 'Comprobante de Pago Bancario (PDF) * [Requerido]',
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
        placeholder: 'Escriba el motivo de la devolución...',
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

  /**
   * ⚙️ Abrir modal de aprobación de pago / rechazo consultando antes los soportes activos
   */
  abrirModalGestionar(row: Factura): void {
    this.documentoService.getSoportesActivosFactura(row.id).subscribe({
      next: (docsActivos: Documento[]) => {
        const tieneTbActivo = (docsActivos || []).some(d => d.ruta?.includes('_TB_'));
        const tienePagoActivo = (docsActivos || []).some(d => d.ruta?.includes('_PAGO_'));
        this.ejecutarModalGestionar(row, tieneTbActivo, tienePagoActivo);
      },
      error: () => {
        this.ejecutarModalGestionar(row, false, false);
      }
    });
  }

  private ejecutarModalGestionar(row: Factura, tieneTbActivo: boolean, tienePagoActivo: boolean): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '600px',
      data: {
        titulo: `Tesorería y Registro de Pago - Factura No. ${row.numeroFactura}`,
        campos: this.getCamposGestion(tieneTbActivo, tienePagoActivo),
        formData: { id: row.id, numeroCausacion: row.numeroCausacion || '' },
        service: {
          editar: (model: any) => {
            const usuarioAccion = this.loginService.getUserName();

            if (model.estadoAccion === 'APROBADO') {

              const inputsFile = Array.from(document.querySelectorAll('input[type="file"]')) as HTMLInputElement[];

              let archivoTb: File | undefined = undefined;
              let archivoComprobante: File | undefined = undefined;

              if (inputsFile.length >= 2) {
                archivoTb = inputsFile[0].files?.[0];
                archivoComprobante = inputsFile[1].files?.[0];
              } else if (inputsFile.length === 1) {
                archivoTb = inputsFile[0].files?.[0];
              }

              if (!archivoTb && model.soporteTb) {
                archivoTb = model.soporteTb instanceof File ? model.soporteTb : model.soporteTb[0];
              }
              if (!archivoComprobante && model.comprobantePago) {
                archivoComprobante = model.comprobantePago instanceof File ? model.comprobantePago : model.comprobantePago[0];
              }

              const requiereTb = !tieneTbActivo && !archivoTb;
              const requierePago = !tienePagoActivo && !archivoComprobante;

              if (requiereTb || requierePago) {
                this.alertService.advertencia('Debe adjuntar obligatoriamente los soportes de pago requeridos que no existan activos.', 'Archivos Requeridos');
                return throwError(() => new Error('Los archivos de pago requeridos son obligatorios.'));
              }

              const tipoRegistroIdNum = model.tipoRegistroContableId
                ? Number(model.tipoRegistroContableId)
                : (row.tipoRegistroContableId ? Number(row.tipoRegistroContableId) : undefined);

              this.alertService.cargando('Registrando pago y subiendo soportes...', 'Procesando Tesorería');

              return this.service.procesarPagoFase4(
                row.id,
                tipoRegistroIdNum,
                model.numeroCausacion,
                usuarioAccion,
                archivoTb,
                archivoComprobante
              );

            } else {
              model.usuario = usuarioAccion;

              if (model.causalDevolucionId) {
                model.causalDevolucionId = Number(model.causalDevolucionId);
              }
              if (model.observacionId && model.observacionId !== 'OTRO') {
                model.observacion = model.observacionId;
              }

              this.alertService.cargando('Registrando devolución...', 'Procesando Rechazo');

              return this.service.procesarTransicionFase(row.id, 4, model);
            }
          }
        }
      }
    });

    dialogRef.afterClosed().subscribe(resultado => {
      if (resultado) {
        this.alertService.exito(`Pago registrado y verificado para la Factura No. ${row.numeroFactura}.`, 'Proceso Finalizado');
        this.regresarABandeja();
        this.cargarDatosPaginados();
      }
    });
  }

  /**
   * 🔎 Búsqueda global superior
   */
  buscar(texto: string): void {
    this.filtrosActivos.textoBusquedaGlobal = texto && texto.trim() !== '' ? texto.trim() : null;
    this.paginaActual = 0;
    this.cargarDatosPaginados();
  }

  /**
   * 📟 Evento de paginación
   */
  override paginar(event: PageEvent): void {
    this.paginaActual = event.pageIndex;
    this.totalPorPagina = event.pageSize;
    this.cargarDatosPaginados();
  }
}
