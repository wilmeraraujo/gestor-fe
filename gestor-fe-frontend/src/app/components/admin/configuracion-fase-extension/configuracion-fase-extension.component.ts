import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import Swal from 'sweetalert2';
import { CommonListarComponent } from '../../common-listar.component';
import { ConfiguracionFaseExtension } from '../../../models/configuracion-fase-extension';
import { ConfiguracionFaseExtensionService } from '../../../services/configuracion-fase-extension.service';
import { FaseService } from '../../../services/fase.service';
import { ExtensionService } from '../../../services/extension.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-configuracion-fase-extension',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './configuracion-fase-extension.component.html',
  styleUrl: './configuracion-fase-extension.component.css'
})
export class ConfiguracionFaseExtensionComponent
  extends CommonListarComponent<ConfiguracionFaseExtension, ConfiguracionFaseExtensionService>
  implements OnInit {

  override titulo = 'Configuración de Extensiones por Fase';

  fasesOptions: { value: number, label: string }[] = [];
  extensionesOptions: { value: number, label: string }[] = [];

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'faseNombre', header: 'Fase' },
    { field: 'extensionNombre', header: 'Extensión' },
    { field: 'descripcion', header: 'Descripción' },
    { field: 'tamanoMaximoMb', header: 'Tamaño Máx (MB)' },
    { field: 'obligatorio', header: 'Obligatorio' },
    { field: 'permiteMultiple', header: 'Permite Múltiples' }
  ];

  constructor(
    service: ConfiguracionFaseExtensionService,
    private faseService: FaseService,
    private extensionService: ExtensionService,
    private dialog: MatDialog
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.cargarCatalogosYLista();
  }

  private cargarCatalogosYLista(): void {
    forkJoin({
      fases: this.faseService.listar(),
      exts: this.extensionService.listar()
    }).subscribe(({ fases, exts }) => {
      this.fasesOptions = fases.map(f => ({ value: f.id, label: f.descripcion }));
      this.extensionesOptions = exts.map(e => ({ value: e.id, label: e.descripcion }));

      this.calcularRangos();
    });
  }

  override calcularRangos(): void {
    this.service.getPaginableActivos(this.paginaActual.toString(), this.totalPorPagina.toString()).subscribe(p => {
      this.lista = this.mapearNombres(p.content as ConfiguracionFaseExtension[]);
      this.totalRegistros = p.totalElements as number;
      this.dataSource.data = this.lista;
    });
  }

  buscar(texto: string): void {
    if (!texto || texto.trim() === '') {
      this.calcularRangos();
      return;
    }
    this.service.buscar(texto).subscribe(response => {
      this.lista = this.mapearNombres(response);
      this.totalRegistros = response.length;
      this.dataSource.data = this.lista;
    });
  }

  private mapearNombres(datos: ConfiguracionFaseExtension[]): any[] {
    if (!datos) return [];
    return datos.map(item => ({
      ...item,
      faseNombre: this.obtenerNombreFase(item.faseId),
      extensionNombre: this.obtenerNombreExtension(item.extensionId)
    }));
  }

  private obtenerNombreFase(faseId: number): string {
    const fase = this.fasesOptions.find(f => f.value === Number(faseId));
    return fase ? fase.label : `Fase ${faseId}`;
  }

  private obtenerNombreExtension(extensionId: number): string {
    const ext = this.extensionesOptions.find(e => e.value === Number(extensionId));
    return ext ? ext.label : `Extensión ${extensionId}`;
  }

  // ⚡ Genera campos dinámicos y filtra las extensiones según la fase elegida
  getCamposModal(faseIdSeleccionada?: number, idEdicion?: number) {
    let extensionesFiltradas = [...this.extensionesOptions];

    // Si hay una fase seleccionada, excluir las extensiones que ya se le asignaron
    if (faseIdSeleccionada) {
      const extensionesYaAsignadas = this.lista
        .filter(item => Number(item.faseId) === Number(faseIdSeleccionada) && item.id !== idEdicion)
        .map(item => Number(item.extensionId));

      extensionesFiltradas = this.extensionesOptions.filter(
        ext => !extensionesYaAsignadas.includes(ext.value)
      );
    }

    return [
      {
        name: 'faseId',
        label: 'Seleccionar Fase',
        type: 'select',
        options: this.fasesOptions,
        required: true,
        // 🔔 Evento para recalcular las extensiones disponibles al cambiar de fase
        onChange: (faseIdVal: any, campos: any[], form: any) => {
          if (!faseIdVal) return;
          const asignadas = this.lista
            .filter(item => Number(item.faseId) === Number(faseIdVal) && item.id !== idEdicion)
            .map(item => Number(item.extensionId));

          const campoExt = campos.find(c => c.name === 'extensionId');
          if (campoExt) {
            campoExt.options = this.extensionesOptions.filter(ext => !asignadas.includes(ext.value));
          }
        }
      },
      {
        name: 'extensionId',
        label: 'Seleccionar Extensión Permitida',
        type: 'select',
        options: extensionesFiltradas,
        required: true
      },
      {
        name: 'descripcion',
        label: 'Descripción de la regla',
        type: 'text',
        required: false
      },
      {
        name: 'tamanoMaximoMb',
        label: 'Tamaño Máximo Permitido (MB)',
        type: 'number',
        required: true
      },
      {
        name: 'obligatorio',
        label: '¿Es Obligatorio en esta fase?',
        type: 'checkbox',
        required: false
      },
      {
        name: 'permiteMultiple',
        label: '¿Permite Múltiples Archivos?',
        type: 'checkbox',
        required: false
      }
    ];
  }

  agregar(): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Asignar Extensión a Fase',
        campos: this.getCamposModal(),
        formData: {
          faseId: '',
          extensionId: '',
          descripcion: '',
          tamanoMaximoMb: 10,
          obligatorio: false,
          permiteMultiple: true
        },
        service: this.service
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.calcularRangos();
    });
  }

  editar(row: ConfiguracionFaseExtension): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Regla de Extensión',
        campos: this.getCamposModal(row.faseId, row.id),
        formData: row,
        service: this.service
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.calcularRangos();
    });
  }

  deletedAt(row: ConfiguracionFaseExtension): void {
    if (!confirm(`¿Desea eliminar la regla asignada?`)) return;

    this.service.deletedAt(row.id).subscribe({
      next: () => this.calcularRangos(),
      error: (err) => console.error(err)
    });
  }
}
