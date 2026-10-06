import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { CommonListarComponent } from '../../common-listar.component';
import { ConfiguracionSistema } from '../../../models/configuracion-sistema';
import { ConfiguracionSistemaService } from '../../../services/configuracion-sistema.service';
import { ProcesoService } from '../../../services/proceso.service';
import { Proceso } from '../../../models/proceso';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-configuracion-sistema',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './configuracion-sistema.component.html',
  styleUrl: './configuracion-sistema.component.css'
})
export class ConfiguracionSistemaComponent extends CommonListarComponent<ConfiguracionSistema, ConfiguracionSistemaService> implements OnInit {

  override titulo = 'Configuración Global del Sistema';
  procesosList: Proceso[] = [];
  procesosOptions: { value: number | string, label: string }[] = [];

  columnas: any[] = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Clave / Código' },
    { field: 'descripcion', header: 'Descripción' },
    { field: 'valor', header: 'Valor' },
    { field: 'procesoId', header: 'Proceso', options: [] },
    { field: 'estadoActivo', header: 'Estado' }
  ];

  constructor(
    service: ConfiguracionSistemaService,
    private procesoService: ProcesoService,
    private dialog: MatDialog
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.cargarProcesosYLista();
  }

  private cargarProcesosYLista(): void {
    this.procesoService.listar().subscribe({
      next: (procesos) => {
        // 🛑 Filtrar ÚNICAMENTE los procesos activos (deletedAt es nulo o indefinido)
        this.procesosList = (procesos || []).filter(p => p && !p.deletedAt);
        this.procesosOptions = this.procesosList.map(p => ({
          value: p.id,
          label: `${p.codigo} - ${p.descripcion}`
        }));

        const opcionesFiltroProceso = this.procesosList.map(p => ({
          value: String(p.id),
          label: p.descripcion || p.codigo
        }));

        const colProceso = this.columnas.find(c => c && c.field === 'procesoId');
        if (colProceso) {
          colProceso.options = opcionesFiltroProceso;
        }
        this.columnas = [...this.columnas];

        this.calcularRangos();
      },
      error: (err) => {
        console.error('Error al cargar procesos:', err);
        this.calcularRangos();
      }
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
        const datos = (p.content || []) as any[];
        this.lista = datos.map(item => {
          let procesoNombre = 'SIN PROCESO';
          if (item.procesoId) {
            const proc = this.procesosList.find(pr => String(pr.id) === String(item.procesoId));
            if (proc) {
              procesoNombre = proc.descripcion || proc.codigo;
            }
          }
          return {
            ...item,
            procesoNombre
          };
        }) as ConfiguracionSistema[];

        this.totalRegistros = (p.totalElements || 0) as number;
        this.dataSource.data = this.lista;
      },
      error: (err: any) => {
        console.error('Error al consultar lista paginada y filtrada:', err);
      }
    });
  }

  private getCamposModal(): any[] {
    return [
      { name: 'codigo', label: 'Código / Clave', type: 'text', required: true },
      { name: 'descripcion', label: 'Descripción', type: 'text', required: true },
      { name: 'valor', label: 'Valor', type: 'text', required: true },
      {
        name: 'procesoId',
        label: 'Proceso Asociado',
        type: 'select',
        options: this.procesosOptions,
        required: true
      }
    ];
  }

  agregar(): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Nueva Configuración de Sistema',
        campos: this.getCamposModal(),
        formData: {},
        service: this.service
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.calcularRangos();
      }
    });
  }

  editar(row: ConfiguracionSistema): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Configuración de Sistema',
        campos: this.getCamposModal(),
        formData: row,
        service: this.service
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.calcularRangos();
      }
    });
  }

  buscar(texto: string): void {
    if (!texto || texto.trim() === '') {
      this.calcularRangos();
      return;
    }

    this.service.buscar(texto).subscribe(response => {
      this.lista = response;
      this.totalRegistros = response.length;
    });
  }

  deletedAt(row: ConfiguracionSistema): void {
    if (!confirm(`¿Desea eliminar la configuración ${row.codigo}?`)) {
      return;
    }

    this.service.deletedAt(row.id).subscribe({
      next: () => {
        this.calcularRangos();
      },
      error: (err) => console.error(err)
    });
  }
}
