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
import { SelectionModel } from '@angular/cdk/collections'; // 👈 Importante para los Checkboxes
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
  
  // 🚀 NUEVO: Activa o desactiva la columna de checkboxes para selección múltiple
  @Input() mostrarSeleccion = false; 

  @Output() agregar = new EventEmitter<void>();
  @Output() editar = new EventEmitter<any>();
  @Output() eliminar = new EventEmitter<any>();
  @Output() descargarErrores = new EventEmitter<any>();
  @Output() verDetalle = new EventEmitter<any>();
  @Output() buscar = new EventEmitter<string>();
  @Output() paginar = new EventEmitter<PageEvent>();
  
  // 🚀 NUEVO: Emisor de eventos que envía las filas seleccionadas al componente padre
  @Output() selecciononChange = new EventEmitter<any[]>(); 

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  dataSource = new MatTableDataSource<any>();
  displayedColumns: string[] = [];
  
  // Selección múltiple activa (guarda los objetos seleccionados)
  selection = new SelectionModel<any>(true, []);

  ngOnInit(): void {
    this.configurarColumnas();
    this.dataSource.data = this.datos;

    // Escuchar cambios de selección y emitir al componente padre
    this.selection.changed.subscribe(() => {
      this.selecciononChange.emit(this.selection.selected);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['datos']) {
      this.dataSource.data = this.datos;
      this.selection.clear(); // Limpia la selección si los datos de la tabla cambian o se paginan
    }
  }

  ngAfterViewInit(): void {
    this.paginator._intl.itemsPerPageLabel = 'Registros por página:';
  }

  configurarColumnas(): void {
    this.displayedColumns = [];

    // 1. Si la selección masiva está activa, colocamos los Checkboxes al extremo izquierdo
    if (this.mostrarSeleccion) {
      this.displayedColumns.push('select');
    }

    // 2. Columnas de datos dinámicas
    this.displayedColumns.push(...this.columnas.map(c => c.field));

    // 3. Columna de acciones al extremo derecho
    if (this.mostrarAcciones) {
      this.displayedColumns.push('acciones');
    }
  }

  /* =========================================================================
     MÉTODOS DE CONTROL PARA CHECKBOXES (SelectionModel)
     ========================================================================= */
  
  /** ¿Están todos los registros seleccionados en la página actual? */
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  /** Selecciona o deselecciona todas las filas de la tabla */
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