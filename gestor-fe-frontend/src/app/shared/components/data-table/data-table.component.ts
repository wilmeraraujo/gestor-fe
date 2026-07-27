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
  @Input() mostrarAgregar = true;
  @Input() mostrarAcciones = true;
  @Input() mostrarDescargaErrores = false;
  @Input() mostrarDetalle = false;
  @Input() mostrarSeleccion = false;

  @Output() agregar = new EventEmitter<void>();
  @Output() editar = new EventEmitter<any>();
  @Output() eliminar = new EventEmitter<any>();
  @Output() descargarErrores = new EventEmitter<any>();
  @Output() verDetalle = new EventEmitter<any>();
  @Output() buscar = new EventEmitter<string>();
  @Output() paginar = new EventEmitter<PageEvent>();
  @Output() selecciononChange = new EventEmitter<any[]>();

  @ViewChild('paginatorInferior') paginatorInferior!: MatPaginator;

  dataSource = new MatTableDataSource<any>();
  displayedColumns: string[] = [];

  // Nombres de las columnas especiales para la fila de filtros
  filterColumns: string[] = [];

  // 🎯 Estado para activar/desactivar la fila de filtros por columna
  mostrarFiltrosColumnas: boolean = false;

  // Mapa de valores de filtro por cada propiedad: { 'nombreArchivo': 'zip', 'usuario': 'weap' }
  filtrosPorColumna: { [key: string]: string } = {};

  selection = new SelectionModel<any>(true, []);

  ngOnInit(): void {
    this.configurarColumnas();
    this.dataSource.data = this.datos;
    this.configurarFiltroCustom();

    this.selection.changed.subscribe(() => {
      this.selecciononChange.emit(this.selection.selected);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['datos']) {
      this.dataSource.data = this.datos;
      this.selection.clear();
      this.aplicarFiltrosColumnas(); // Re-aplica filtros si la data cambia
    }
  }

  ngAfterViewInit(): void {
    if (this.paginatorInferior) {
      this.paginatorInferior._intl.itemsPerPageLabel = 'Registros por página:';
    }
  }

  configurarColumnas(): void {
    this.displayedColumns = [];
    this.filterColumns = [];

    if (this.mostrarSeleccion) {
      this.displayedColumns.push('select');
      this.filterColumns.push('select-filter');
    }

    this.displayedColumns.push(...this.columnas.map(c => c.field));
    this.filterColumns.push(...this.columnas.map(c => `filter-${c.field}`));

    if (this.mostrarAcciones) {
      this.displayedColumns.push('acciones');
      this.filterColumns.push('acciones-filter');
    }
  }

  // 🛠️ Configuración de predicado personalizado de filtrado local respetando la propiedad "filtrable"
  configurarFiltroCustom(): void {
    this.dataSource.filterPredicate = (data: any, filter: string) => {
      const searchTerms = JSON.parse(filter);
      let isMatch = true;

      for (const col of Object.keys(searchTerms)) {
        const val = data[col] !== null && data[col] !== undefined ? String(data[col]).toLowerCase() : '';
        const searchVal = searchTerms[col].toLowerCase();
        if (searchVal && !val.includes(searchVal)) {
          isMatch = false;
          break;
        }
      }
      return isMatch;
    };
  }

  // 🎯 Alternar visibilidad de los campos de filtrado por columna
  toggleFiltros(): void {
    this.mostrarFiltrosColumnas = !this.mostrarFiltrosColumnas;
    if (!this.mostrarFiltrosColumnas) {
      this.limpiarFiltrosColumnas();
    }
  }

  // 🔍 Aplica solo filtros de columnas configuradas con filtrable !== false
  aplicarFiltrosColumnas(): void {
    const camposFiltrables = this.columnas
      .filter(c => c.filtrable !== false)
      .map(c => c.field);

    const filtrosValidos: { [key: string]: string } = {};

    for (const key of Object.keys(this.filtrosPorColumna)) {
      if (camposFiltrables.includes(key) && this.filtrosPorColumna[key]) {
        filtrosValidos[key] = this.filtrosPorColumna[key];
      }
    }

    this.dataSource.filter = JSON.stringify(filtrosValidos);
  }

  limpiarFiltrosColumnas(): void {
    this.filtrosPorColumna = {};
    this.dataSource.filter = '';
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
}
