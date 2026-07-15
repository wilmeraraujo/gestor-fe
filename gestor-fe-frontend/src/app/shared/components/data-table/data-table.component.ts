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


import {
  MatTableDataSource,
} from '@angular/material/table';

import {
  MatPaginator,
  PageEvent
} from '@angular/material/paginator';


import { FormsModule } from '@angular/forms';

import { MATERIAL_MODULES } from '../../material';

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [
    FormsModule,
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

  @Output() agregar = new EventEmitter<void>();

  @Output() editar = new EventEmitter<any>();

  @Output() eliminar = new EventEmitter<any>();

  @Output() descargarErrores = new EventEmitter<any>();
  
  @Output() verDetalle = new EventEmitter<any>();

  @Output() buscar = new EventEmitter<string>();

  @Output() paginar = new EventEmitter<PageEvent>();

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  dataSource = new MatTableDataSource<any>();

  displayedColumns: string[] = [];

  ngOnInit(): void {

    this.displayedColumns = this.columnas.map(c => c.field);

    if (this.mostrarAcciones) {
      this.displayedColumns.push('acciones');
    }

    this.dataSource.data = this.datos;
  }

  ngOnChanges(changes: SimpleChanges): void {

    if (changes['datos']) {
      this.dataSource.data = this.datos;
    }

  }

  ngAfterViewInit(): void {

    this.paginator._intl.itemsPerPageLabel =
      'Registros por página:';
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
