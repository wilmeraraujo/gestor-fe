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
  selector: 'app-impuestos',
  standalone: true,
  imports: [
    CommonModule, 
    DataTableComponent,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './impuestos.component.html',
  styleUrl: './impuestos.component.css'
})
export class ImpuestosComponent extends CommonListarComponent<Factura, FacturaService> implements OnInit {

  override titulo = 'Verificación de Impuestos (Etapa 3)';

  // Control de pestañas y visores
  tabSeleccionada: number = 0; // 0: Bandeja Facturas, 1: Visor Soportes
  facturaSeleccionada: Factura | null = null;
  soportesFactura: Documento[] = [];
  pdfUrlSafe: SafeResourceUrl | null = null;
  documentoActivo: string = '';

  esGestorF3: boolean = false;

  opcionesCausal: { value: any, label: string }[] = [];
  opcionesObservacion: { value: any, label: string }[] = [];
  mapaFases: { [key: number]: string } = {};

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
    this.esGestorF3 = roles.includes('gestor-fe-f3-i') || roles.includes('gestor-fe-admin');
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
    this.service.getFaseActiva(3, this.paginaActual, this.totalPorPagina)
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
    if (!this.esGestorF3) return;

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
   * 📝 Formulario de Dictamen para Impuestos
   */
  getCamposGestion() {
    return [
      {
        name: 'estadoAccion',
        label: 'Dictamen de Impuestos',
        type: 'select',
        required: true,
        options: [
          { value: 'APROBADO', label: 'Aprobar (Impuestos Verificados y enviar a Tesorería)' },
          { value: 'RECHAZADO', label: 'Rechazar (Devolver factura electrónica por retenciones / IVA)' }
        ],
        onChange: (val: string, campos: any[], form: FormGroup) => {
          const isRechazado = val === 'RECHAZADO';
          this.toggleCampoVisibilidad(campos, form, 'causalDevolucionId', isRechazado);
          this.toggleCampoVisibilidad(campos, form, 'observacionId', isRechazado);
        }
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
          this.toggleCampoVisibilidad(campos, form, 'observacion', val === 'OTRO');
        }
      },
      {
        name: 'observacion',
        label: 'Detalle de la Observación (Otro)',
        type: 'textarea',
        placeholder: 'Escriba la inconsistencia tributaria encontrada...',
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
   * ⚙️ Abrir modal de dictamen (Invocable tanto desde la Pestaña 1 como de la Pestaña 2)
   */
  abrirModalGestionar(row: Factura): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '550px',
      data: {
        titulo: `Verificación de Impuestos - Factura No. ${row.numeroFactura}`,
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
            // Envia la decisión a la Fase 3 en Spring Boot
            return this.service.procesarTransicionFase(row.id, 3, model);
          }
        }
      }
    });

    dialogRef.afterClosed().subscribe(resultado => {
      if (resultado) {
        // Al procesar con éxito la factura, regresamos a la Pestaña 1 y refrescamos la lista
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