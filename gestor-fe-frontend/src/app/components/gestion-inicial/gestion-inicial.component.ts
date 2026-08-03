import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { KeycloakService } from 'keycloak-angular';

import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';

import { Factura } from '../../models/factura';
import { FacturaService } from '../../services/factura.service';
import { CausalDevolucionService } from '../../services/causal-devolucion.service';
import { ObservacionService } from '../../services/observacion.service';
import { FaseService } from '../../services/fase.service';

@Component({
  selector: 'app-gestion-inicial',
  standalone: true,
  imports: [CommonModule, DataTableComponent],
  templateUrl: './gestion-inicial.component.html',
  styleUrl: './gestion-inicial.component.css'
})
export class GestionInicialComponent extends CommonListarComponent<Factura, FacturaService> implements OnInit {

  override titulo = 'Gestión Inicial de Facturas (Etapa 1)';

  // Bandera de permisos
  esGestorF1: boolean = false;
  mostrarEditar: boolean = false;
  mostrarEliminar: boolean = false;
  mostrarDetalle: boolean = false;
  mostrarAgregar: boolean = false;

  // Catalogos para desplegables
  opcionesCausal: { value: any, label: string }[] = [];
  opcionesObservacion: { value: any, label: string }[] = [];
  mapaFases: { [key: number]: string } = {};

  
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
    { field: 'faseNombre', header: 'Fase / Etapa' },
    { field: 'observacion', header: 'Observación' }
  ];

  constructor(
    service: FacturaService,
    private causalService: CausalDevolucionService,
    private observacionService: ObservacionService,
    private faseService: FaseService,
    private keycloakService: KeycloakService,
    private dialog: MatDialog
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.evaluarRolesUsuario();
    this.cargarFases();
    this.cargarListasMaestras();
  }

  /**
   * 🔑 Verifica si el usuario cuenta con el rol operativo asignado
   */
  private evaluarRolesUsuario(): void {
    const roles = this.keycloakService.getUserRoles();
    this.esGestorF1 = roles.includes('gestor-fe-f1-g') || roles.includes('gestor-fe-admin');
  }

  /**
   * 🏷️ Carga el catálogo de fases activas desde el backend Admin
   */
  private cargarFases(): void {
    this.faseService.listar().subscribe({
      next: (fases: any[]) => {
        this.mapaFases = (fases || [])
          .filter((f: any) => !f.deletedAt) // Filtra fases no eliminadas
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
   * 📋 Carga exclusivamente las facturas en Fase 1 (Bandeja de trabajo)
   */
  cargarDatosPaginados(): void {
    this.service.getFase1(this.paginaActual, this.totalPorPagina)
      .subscribe(res => {
        this.lista = this.mapearFaseNombre(res.content);
        this.totalRegistros = res.totalElements;
      });
  }

  /**
   * 🏷️ Asigna el nombre descriptivo de la Fase
   */
  private mapearFaseNombre(facturas: Factura[]): any[] {
    return (facturas || []).map(f => ({
      ...f,
      faseNombre: this.mapaFases[f.faseId] || `Fase ${f.faseId}`
    }));
  }

  /**
   * 📚 Carga las causales y observaciones activas para la gestión
   */
  private cargarListasMaestras(): void {
    if (!this.esGestorF1) return;

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
   * ⚙️ Abrir modal de aprobación/rechazo
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

            return this.service.procesarTransicionFase(row.id, 1, model);
          }
        }
      }
    });

    dialogRef.afterClosed().subscribe(resultado => {
      if (resultado) {
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

  verDocumentos(row: Factura): void {
    console.log('Soportes cargados:', row.documentos);
  }

  deletedAt(row: Factura): void {
    if (!confirm(`¿Desea anular la factura No. ${row.numeroFactura}?`)) return;

    this.service.deletedAt(row.id).subscribe({
      next: () => this.cargarDatosPaginados(),
      error: (err) => console.error('Error al anular:', err)
    });
  }
}