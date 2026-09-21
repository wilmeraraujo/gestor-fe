import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  AfterViewInit,
  OnInit,
  OnChanges,
  OnDestroy,
  SimpleChanges
} from '@angular/core';

import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { SelectionModel } from '@angular/cdk/collections';
import { FormsModule } from '@angular/forms';
import { MATERIAL_MODULES } from '../../material';
import { CommonModule } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

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
export class DataTableComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {

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
  @Input() mostrarDescargar = false;
  @Input() mostrarDetalle = false;
  @Input() mostrarSeleccion = false;
  @Input() mostrarGestionarFactura = false;
  @Input() mostrarEditar = true;
  @Input() mostrarEliminar = true;
  @Input() mostrarHistorial: boolean = false;
  @Input() mostrarEliminarMasivo: boolean = false;

  @Output() filtrosChange = new EventEmitter<{ [key: string]: string }>();
  @Output() verHistorial = new EventEmitter<any>();
  @Output() agregar = new EventEmitter<void>();
  @Output() editar = new EventEmitter<any>();
  @Output() eliminar = new EventEmitter<any>();
  @Output() descargar = new EventEmitter<any>();
  @Output() descargarErrores = new EventEmitter<any>();
  @Output() verDetalle = new EventEmitter<any>();
  @Output() buscar = new EventEmitter<string>();
  @Output() paginar = new EventEmitter<PageEvent>();
  @Output() selecciononChange = new EventEmitter<any[]>();
  @Output() gestionarFactura = new EventEmitter<any>();
  @Output() eliminarMasivo = new EventEmitter<any[]>();

  @ViewChild('paginatorInferior') paginatorInferior!: MatPaginator;

  dataSource = new MatTableDataSource<any>();
  displayedColumns: string[] = [];
  filterColumns: string[] = [];

  mostrarFiltrosColumnas: boolean = true;
  filtrosPorColumna: { [key: string]: string } = {};

  cargandoFiltro: boolean = false;
  campoFiltrando: string | null = null;
  private fallbackTimer: any = null;
  private filtrosSubject = new Subject<{ [key: string]: string }>();
  private filtrosSubscription!: Subscription;

  selection = new SelectionModel<any>(true, [], true, (o1, o2) => {
    if (o1 && o2 && o1.id !== undefined && o2.id !== undefined && o1.id !== null && o2.id !== null) {
      return String(o1.id) === String(o2.id);
    }
    return o1 === o2;
  });

  ngOnInit(): void {
    this.configurarFilterPredicate();
    this.configurarColumnas();
    this.actualizarDataSource();

    this.filtrosSubscription = this.filtrosSubject.pipe(
      debounceTime(350),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(filtrosValidos => {
      this.filtrosChange.emit(filtrosValidos);
      if (!this.filtrosChange.observed) {
        this.dataSource.filter = JSON.stringify(filtrosValidos);
        this.cargandoFiltro = false;
        this.campoFiltrando = null;
      }
    });

    this.selection.changed.subscribe(() => {
      this.selecciononChange.emit(this.selection.selected);
    });
  }

  ngOnDestroy(): void {
    if (this.filtrosSubscription) {
      this.filtrosSubscription.unsubscribe();
    }
    if (this.fallbackTimer) {
      clearTimeout(this.fallbackTimer);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['columnas']) {
      this.configurarColumnas();
    }
    if (changes['datos']) {
      this.actualizarDataSource();
      this.cargandoFiltro = false;
      this.campoFiltrando = null;
      if (this.fallbackTimer) {
        clearTimeout(this.fallbackTimer);
      }
      if (!this.datos || this.datos.length === 0) {
        this.selection.clear();
      }
    }
  }

  ngAfterViewInit(): void {
    if (this.paginatorInferior) {
      this.paginatorInferior._intl.itemsPerPageLabel = 'Registros por página:';
      this.actualizarDataSource();
    }
  }

  private configurarFilterPredicate(): void {
    this.dataSource.filterPredicate = (data: any, filter: string) => {
      if (!filter || filter === '{}') return true;
      try {
        const filtros = JSON.parse(filter);
        return Object.keys(filtros).every(key => {
          const valorFiltro = String(filtros[key]).toLowerCase().trim();
          if (!valorFiltro) return true;
          const valorCelda = String(data[key] || '').toLowerCase().trim();
          return valorCelda.includes(valorFiltro);
        });
      } catch (e) {
        return true;
      }
    };
  }

  private actualizarDataSource(): void {
    this.dataSource.data = this.datos || [];
    if (this.paginatorInferior) {
      if (!this.totalRegistros || this.totalRegistros === this.datos.length) {
        this.dataSource.paginator = this.paginatorInferior;
      } else {
        this.dataSource.paginator = null;
      }
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

  aplicarFiltrosColumnas(field?: string): void {
    if (field) {
      this.campoFiltrando = field;
    }

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

    if (Object.keys(filtrosValidos).length > 0) {
      this.cargandoFiltro = true;
      if (this.fallbackTimer) clearTimeout(this.fallbackTimer);
      this.fallbackTimer = setTimeout(() => {
        this.cargandoFiltro = false;
        this.campoFiltrando = null;
      }, 4000);
    } else {
      this.cargandoFiltro = false;
      this.campoFiltrando = null;
    }

    this.filtrosSubject.next(filtrosValidos);
  }

  limpiarFiltrosColumnas(): void {
    this.filtrosPorColumna = {};
    this.dataSource.filter = '';
    this.cargandoFiltro = false;
    this.campoFiltrando = null;
    if (this.fallbackTimer) clearTimeout(this.fallbackTimer);
    this.selection.clear();
    this.filtrosChange.emit({});
  }

  isAllSelected(): boolean {
    if (!this.dataSource.data || this.dataSource.data.length === 0) {
      return false;
    }
    return this.dataSource.data.every(row => this.selection.isSelected(row));
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.dataSource.data.forEach(row => this.selection.deselect(row));
    } else {
      this.dataSource.data.forEach(row => this.selection.select(row));
    }
  }

  limpiarSeleccion(): void {
    this.selection.clear();
  }

  /**
   * 🛑 EVALÚA SI LA FACTURA PUEDE SER GESTIONADA O DICTAMINADA
   * Oculta el botón verde ÚNICAMENTE si la factura está en estado ANULADO o PROCESANDO.
   * Si está en estado RECHAZADO, PENDIENTE, RADICADO, etc., el botón SE MANTIENE VISIBLE.
   */
  puedeGestionarFactura(row: any): boolean {
    if (!this.mostrarGestionarFactura || !row) return false;

    // Extraemos el valor del estado en mayúsculas
    const estado = String(row.estado || row.estadoNombre || '').trim().toUpperCase();

    // Oculta el botón ÚNICAMENTE si el estado es ANULADO o PROCESANDO
    return estado !== 'ANULADO' && estado !== 'PROCESANDO';
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
    if (estadoUpper === 'CON ERRORES' || estadoUpper === 'RECHAZADO' || estadoUpper === 'ANULADO') {
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

  onDescargar(row: any): void {
    this.descargar.emit(row);
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

  onEliminarMasivo(): void {
    if (this.selection.selected.length > 0) {
      this.eliminarMasivo.emit(this.selection.selected);
    }
  }
}
