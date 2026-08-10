import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  AfterViewInit,
  OnInit,
  OnChanges,
  SimpleChanges
} from '@angular/core';

import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { SelectionModel } from '@angular/cdk/collections';
import { FormsModule } from '@angular/forms';
import { MATERIAL_MODULES } from '../../material';
import { CommonModule } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCheckboxModule,
    ...MATERIAL_MODULES
  ],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.css'
})
export class DataTableComponent implements OnInit, AfterViewInit, OnChanges {

  @Input() titulo = '';
  @Input() columnas: any[] = [];
  @Input() datos: any[] = [];
  @Input() totalRegistros = 0;
  @Input() totalPorPagina = 5;
  @Input() pageSizeOptions: number[] = [5, 10, 20, 50, 100];
  
  @Input() textoBotonAgregar: string = 'Adicionar';
  @Input() tooltipAgregar: string = 'Adicionar nuevo registro';

  @Input() mostrarAgregar = true;
  @Input() mostrarAcciones = true;
  @Input() mostrarDescargaErrores = false;
  @Input() mostrarDetalle = false;
  @Input() mostrarSeleccion = false;
  @Input() mostrarGestionarFactura = false;
  @Input() mostrarEditar = true;
  @Input() mostrarEliminar = true;
  @Input() mostrarHistorial: boolean = false;

  @Output() filtrosChange = new EventEmitter<{ [key: string]: string }>();
  @Output() verHistorial = new EventEmitter<any>();
  @Output() agregar = new EventEmitter<void>();
  @Output() editar = new EventEmitter<any>();
  @Output() eliminar = new EventEmitter<any>();
  @Output() descargarErrores = new EventEmitter<any>();
  @Output() verDetalle = new EventEmitter<any>();
  @Output() buscar = new EventEmitter<string>();
  @Output() paginar = new EventEmitter<PageEvent>();
  @Output() selecciononChange = new EventEmitter<any[]>();
  @Output() gestionarFactura = new EventEmitter<any>();

  @ViewChild('paginatorInferior') paginatorInferior!: MatPaginator;

  dataSource = new MatTableDataSource<any>();
  displayedColumns: string[] = [];
  filterColumns: string[] = [];

  mostrarFiltrosColumnas: boolean = true;
  filtrosPorColumna: { [key: string]: string } = {};

  selection = new SelectionModel<any>(true, []);

  ngOnInit(): void {
    this.configurarColumnas();
    this.actualizarDataSource();

    this.selection.changed.subscribe(() => {
      this.selecciononChange.emit(this.selection.selected);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['columnas'] || changes['datos']) {
      this.configurarColumnas();
      this.actualizarDataSource();
      this.selection.clear();
    }
  }

  ngAfterViewInit(): void {
    if (this.paginatorInferior) {
      this.paginatorInferior._intl.itemsPerPageLabel = 'Registros por página:';
      if (!this.totalRegistros || this.totalRegistros === this.datos.length) {
        this.dataSource.paginator = this.paginatorInferior;
      }
    }
  }

  private actualizarDataSource(): void {
    this.dataSource.data = this.datos || [];
    if (this.paginatorInferior && (!this.totalRegistros || this.totalRegistros === this.datos.length)) {
      this.dataSource.paginator = this.paginatorInferior;
    }
  }

  configurarColumnas(): void {
    this.displayedColumns = [];
    this.filterColumns = [];

    if (!this.columnas || this.columnas.length === 0) {
      return;
    }

    if (this.mostrarSeleccion) {
      this.displayedColumns.push('select');
      this.filterColumns.push('select-filter');
    }

    const colsValidas = this.columnas.filter(c => c && c.field);
    this.displayedColumns.push(...colsValidas.map(c => c.field));
    this.filterColumns.push(...colsValidas.map(c => `filter-${c.field}`));

    if (this.mostrarAcciones) {
      this.displayedColumns.push('acciones');
      this.filterColumns.push('acciones-filter');
    }
  }

  /**
   * 📤 Evalúa los campos filtrables y emite el objeto hacia el componente padre
   */
  aplicarFiltrosColumnas(): void {
    const camposFiltrables = this.columnas
      .filter(c => c && c.filtrable !== false)
      .map(c => c.field);

    const filtrosValidos: { [key: string]: string } = {};

    for (const key of Object.keys(this.filtrosPorColumna)) {
      const val = this.filtrosPorColumna[key];
      if (camposFiltrables.includes(key) && val !== null && val !== undefined) {
        const texto = String(val).trim();
        if (texto !== '') {
          filtrosValidos[key] = texto;
        }
      }
    }

    this.filtrosChange.emit(filtrosValidos);
  }

  /**
   * 🧹 Limpia los filtros e informa al backend
   */
  limpiarFiltrosColumnas(): void {
    this.filtrosPorColumna = {};
    this.filtrosChange.emit({});
  }

  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }
    this.selection.select(...this.dataSource.data);
  }

  obtenerClaseEstado(valorEstado: any): string {
    if (!valorEstado) return 'badge-estado badge-default';

    const estadoUpper = String(valorEstado).trim().toUpperCase();

    switch (estadoUpper) {
      case 'ANULADO':
      case 'RECHAZADO':
      case 'FACTURA NO CONFORME':
      case 'CON ERRORES':
        return 'badge-estado badge-rojo';

      case 'RADICADO':
      case 'REGISTRADO':
      case 'PENDIENTE':
        return 'badge-estado badge-gris';

      case 'EN GESTIÓN':
      case 'EN GESTION':
      case 'EN PROCESO':
      case 'PROCESANDO':
        return 'badge-estado badge-azul';

      case 'VALIDADO':
      case 'APROBADO':
      case 'CAUSADO':
      case 'PAGADO':
      case 'CARGADO':
      case 'CARGUE FINALIZADO':
      case 'IMPUESTOS VERIFICADOS':
        return 'badge-estado badge-verde';

      default:
        return 'badge-estado badge-default';
    }
  }

  obtenerIconoEstado(valorEstado: any): string {
    if (!valorEstado) return 'help_outline';

    const estadoUpper = String(valorEstado).trim().toUpperCase();

    if (estadoUpper === 'CARGADO' || estadoUpper === 'CARGUE FINALIZADO') {
      return 'check';
    }
    if (estadoUpper === 'CON ERRORES' || estadoUpper === 'RECHAZADO') {
      return 'close';
    }
    if (estadoUpper === 'PROCESANDO') {
      return 'sync';
    }
    return 'info';
  }

  onBuscar(valor: string): void {
    this.buscar.emit(valor);
  }

  onEditar(row: any): void {
    this.editar.emit(row);
  }

  onEliminar(row: any): void {
    this.eliminar.emit(row);
  }

  onPaginar(event: PageEvent): void {
    this.paginar.emit(event);
  }

  onDescargarErrores(row: any): void {
    this.descargarErrores.emit(row);
  }

  onVerDetalle(row: any): void {
    this.verDetalle.emit(row);
  }

  onGestionarFactura(row: any): void {
    this.gestionarFactura.emit(row);
  }

  onVerHistorial(row: any): void {
    this.verHistorial.emit(row);
  }
}