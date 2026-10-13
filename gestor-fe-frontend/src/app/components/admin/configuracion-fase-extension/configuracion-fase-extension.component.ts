import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import Swal from 'sweetalert2';
import { CommonListarComponent } from '../../common-listar.component';
import { ConfiguracionFaseExtension } from '../../../models/configuracion-fase-extension';
import { ConfiguracionFaseExtensionService } from '../../../services/configuracion-fase-extension.service';
import { FaseService } from '../../../services/fase.service';
import { ExtensionService } from '../../../services/extension.service';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { ModalFaseExtensionComponent } from './modal-fase-extension/modal-fase-extension.component';

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
    { field: 'permiteMultiple', header: 'Permite Múltiples' },
    { field: 'estadoActivo', header: 'Estado' }
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
      this.fasesOptions = (fases || []).filter(f => !f.deletedAt).map(f => ({ value: f.id, label: f.descripcion }));
      this.extensionesOptions = (exts || []).filter(e => !e.deletedAt).map(e => ({ value: e.id, label: e.descripcion }));

      this.calcularRangos();
    });
  }

  override calcularRangos(): void {
    const servicio = this.service.getPaginableFiltrado(
      this.filtrosMap,
      this.paginaActual.toString(),
      this.totalPorPagina.toString()
    );

    servicio.subscribe({
      next: (p: any) => {
        this.lista = this.mapearNombres(p.content as ConfiguracionFaseExtension[]);
        this.totalRegistros = (p.totalElements || 0) as number;
        this.dataSource.data = this.lista;
      },
      error: (err) => console.error('Error al consultar lista:', err)
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
      extensionNombre: this.obtenerNombreExtension(item.extensionId),
      estadoActivo: !item.deletedAt
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

  agregar(): void {
    const dialogRef = this.dialog.open(ModalFaseExtensionComponent, {
      width: '900px',
      maxWidth: '95vw',
      disableClose: true,
      data: {
        fasesOptions: this.fasesOptions,
        extensionesOptions: this.extensionesOptions,
        configuracionesExistentes: this.lista,
        isEdit: false
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.calcularRangos();
    });
  }

  editar(row: ConfiguracionFaseExtension): void {
    const dialogRef = this.dialog.open(ModalFaseExtensionComponent, {
      width: '800px',
      maxWidth: '95vw',
      disableClose: true,
      data: {
        fasesOptions: this.fasesOptions,
        extensionesOptions: this.extensionesOptions,
        configuracionesExistentes: this.lista,
        item: row,
        isEdit: true
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
