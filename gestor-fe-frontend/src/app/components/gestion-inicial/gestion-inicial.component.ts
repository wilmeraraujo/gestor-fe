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
import { Subject, Subscription } from 'rxjs';
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
  selector: 'app-gestion-inicial',
  standalone: true,
  imports: [
    CommonModule,
    DataTableComponent,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './gestion-inicial.component.html',
  styleUrl: './gestion-inicial.component.css'
})
export class GestionInicialComponent extends CommonListarComponent<Factura, FacturaService> implements OnInit, OnDestroy {

  override titulo = 'Gestión Inicial de Facturas (Fase 1)';

  // 💉 Inyecciones de Servicios mediante Inject
  private loginService = inject(LoginService);
  private alertService = inject(AlertService);

  // Control de pestañas y visores
  tabSeleccionada: number = 0;
  facturaSeleccionada: Factura | null = null;
  soportesFactura: Documento[] = [];
  pdfUrlSafe: SafeResourceUrl | null = null;
  documentoActivo: string = '';

  // Bandera de permisos
  esGestorF1: boolean = false;
  usuarioActivo: string = '';

  // Catálogos para desplegables
  opcionesCausal: { value: any, label: string }[] = [];
  opcionesObservacion: { value: any, label: string }[] = [];
  mapaFases: { [key: number]: string } = {};

  // 🎯 SUBJECT Y SUBSCRIPCIÓN PARA RETARDO (DEBOUNCE 400ms)
  private filtroSubject = new Subject<{ [key: string]: string }>();
  private filtroSubscription?: Subscription;

  // DTO activo para la API con contexto de Fase 1 por defecto
  filtrosActivos: any = { faseId: 1 };

  // Configuración de columnas
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

  ngOnDestroy(): void {
    if (this.filtroSubscription) {
      this.filtroSubscription.unsubscribe();
    }
  }

  /**
   * 👤 Carga el usuario activo y evalúa los permisos utilizando LoginService
   */
  private cargarDatosSesion(): void {
    this.usuarioActivo = this.loginService.getUserName();

    // Habilita el rol para administradores globales o gestores de Fase 1
    this.esGestorF1 = this.loginService.isAdmin ||
                      this.loginService.isGAdmin ||
                      this.loginService.isGFaseUno;
  }

  /**
   * ⏱️ Manejo del retardo (debounce) de 400ms tras la última tecla pulsada
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
  onFiltrosChange(filtrosColumnas: { [key: string]: string }): void {
    this.filtroSubject.next(filtrosColumnas);
  }

  /**
   * 🛠️ Mapea los campos filtrables al DTO que procesará JPA Criteria en el Backend
   */
  private procesarFiltrosYConsultar(filtrosColumnas: { [key: string]: string }): void {
    this.filtrosActivos = {
      faseId: 1, // Mantiene fija la Fase 1
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
        console.error('Error al cargar catálogo de fases:', err);
        this.cargarDatosPaginados();
      }
    });
  }

  /**
   * 📋 Carga exclusivamente las facturas en Fase 1 mediante Criteria
   */
  cargarDatosPaginados(): void {
    this.service.buscarConCriteria(this.filtrosActivos, this.paginaActual, this.totalPorPagina)
      .subscribe({
        next: (res: any) => {
          this.lista = this.mapearFaseNombre(res.content || []);
          this.totalRegistros = res.totalElements || 0;
        },
        error: (err) => {
          console.error('Error al consultar lista por Criteria:', err);
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
        this.alertService.error('No se pudieron consultar los soportes de esta factura.');
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
   * 📝 Campos del modal de dictamen para la Fase 1
   */
  getCamposGestion() {
    return [
      {
        name: 'estadoAccion',
        label: 'Dictamen de la Factura',
        type: 'select',
        required: true,
        options: [
          { value: 'APROBADO', label: 'Aprobar (Aceptar y enviar a Reconocimiento Contable)' },
          { value: 'RECHAZADO', label: 'Rechazar / Anular (Factura no conforme)' }
        ],
        onChange: (val: string, campos: any[], form: FormGroup) => {
          const isRechazado = val === 'RECHAZADO';
          this.toggleCampoVisibilidad(campos, form, 'causalDevolucionId', isRechazado);
          this.toggleCampoVisibilidad(campos, form, 'observacionId', isRechazado);
        }
      },
      {
        name: 'causalDevolucionId',
        label: 'Causal de Devolución / Anulación',
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
          this.toggleCampoVisibilidad(campos, form, 'observacion', val === 'OTRO');
        }
      },
      {
        name: 'observacion',
        label: 'Detalle de la Observación (Otro)',
        type: 'textarea',
        placeholder: 'Escriba detalladamente la justificación...',
        visible: false
      }
    ];
  }

  private toggleCampoVisibilidad(campos: any[], form: FormGroup, fieldName: string, visible: boolean): void {
    const campo = campos.find(c => c.name === fieldName);
    if (campo) campo.visible = visible;

    const control = form.get(fieldName);
    if (control) {
      if (visible) {
        control.setValidators([Validators.required]);
      } else {
        control.clearValidators();
        control.setValue('');
      }
      control.updateValueAndValidity();
    }
  }

  /**
   * ⚙️ Abrir modal de aprobación/rechazo asignando de forma segura el usuario activo
   */
  abrirModalGestionar(row: Factura): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '550px',
      data: {
        titulo: `Gestionar Factura No. ${row.numeroFactura}`,
        campos: this.getCamposGestion(),
        formData: { id: row.id },
        service: {
          editar: (model: any) => {
            if (model.causalDevolucionId) {
              model.causalDevolucionId = Number(model.causalDevolucionId);
            }

            if (model.observacionId && model.observacionId !== 'OTRO') {
              model.observacion = model.observacionId;
            }

            // ⚡ RECUPERAMOS Y ASIGNAMOS EL USUARIO LEGIBLE DE LOGINSERVICE
            model.usuario = this.loginService.getUserName();

            return this.service.procesarTransicionFase(row.id, 1, model);
          }
        }
      }
    });

    dialogRef.afterClosed().subscribe(resultado => {
      if (resultado) {
        this.alertService.exito(`La factura No. ${row.numeroFactura} ha sido procesada con éxito.`, 'Dictamen Registrado');
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
