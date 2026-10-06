import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PageEvent } from '@angular/material/paginator';
import { Observable, forkJoin } from 'rxjs';

import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { Documento } from '../../models/documento';
import { DocumentoService } from '../../services/documento.service';
import { AlertService } from '../../services/alert.service';
import { TipoService } from '../../services/tipo.service';
import { ExtensionService } from '../../services/extension.service';

@Component({
  selector: 'app-documento',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    DataTableComponent,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatTooltipModule
  ],
  templateUrl: './documento.component.html',
  styleUrl: './documento.component.css'
})
export class DocumentoComponent extends CommonListarComponent<Documento, DocumentoService> implements OnInit {

  override titulo = 'Buscador y Visor de Soportes Documentales';

  // Control del visor de PDF inline reactivo
  pdfUrlSafe: SafeResourceUrl | null = null;
  documentoActivo: string = '';

  // Filtros interactivos vinculados por [(ngModel)] en el HTML
  filtro = {
    numeroFactura: '',
    nit: '',
    tipoId: null as number | null,
    extensionId: null as number | null
  };

  tiposLista: any[] = [];
  extensionesLista: any[] = [];

  // Bandera para saber si actualmente estamos mostrando un resultado filtrado o el listado general
  aplicandoFiltro: boolean = false;

  documentosSeleccionados: Documento[] = [];

  // Columnas que consume la tabla reutilizable
  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'nombreOriginal', header: 'Nombre Archivo' },
    { field: 'numeroFactura', header: 'No. Factura' }
  ];

  constructor(
    service: DocumentoService,
    private sanitizer: DomSanitizer,
    private alertService: AlertService,
    private tipoService: TipoService,
    private extensionService: ExtensionService
  ) {
    super(service);
  }

  ngOnInit(): void {
    // Inicialmente los datos en la tabla permanecen vacíos hasta que se ingrese al menos el NIT
    this.lista = [];
    this.totalRegistros = 0;

    // Carga de catálogo de Tipos desde el microservicio de Administración
    this.tipoService.listar().subscribe({
      next: (tipos) => this.tiposLista = tipos || [],
      error: (err) => console.error('Error al cargar tipos de documento:', err)
    });

    // Carga de catálogo de Extensiones desde el microservicio de Administración
    this.extensionService.listar().subscribe({
      next: (exts) => this.extensionesLista = exts || [],
      error: (err) => console.error('Error al cargar extensiones:', err)
    });
  }

  // 1. Carga reactiva del documento (PDF/XML) en el visor al dar clic en el ojo (👁️)
  cargarSoporteEnVisor(row: Documento): void {
    this.documentoActivo = row.nombreOriginal;
    
    this.service.getDocumentoBlob(row.id).subscribe({
      next: (blob: Blob) => {
        const fileUrl = URL.createObjectURL(blob);
        this.pdfUrlSafe = this.sanitizer.bypassSecurityTrustResourceUrl(fileUrl);
      },
      error: (err) => {
        console.error('No se pudo visualizar el soporte:', err);
        alert('Este archivo no se puede previsualizar en el navegador o la ruta física no existe.');
        this.pdfUrlSafe = null;
        this.documentoActivo = '';
      }
    });
  }

  // Mapea cada soporte garantizando la lectura directa de 'nit' y 'numeroFactura'
  private mapearSoportes(items: any[]): any[] {
    return (items || []).map(doc => ({
      ...doc,
      nit: doc.nit || doc.factura?.nit || doc.prestador?.nit || '-',
      numeroFactura: doc.numeroFactura || doc.factura?.numeroFactura || '-'
    }));
  }

  // 2. Override de paginación/rangos: Solo ejecuta consulta si hay parámetros de búsqueda válidos
  override calcularRangos(): void {
    if (!this.aplicandoFiltro || !this.filtro.nit || !this.filtro.nit.trim()) {
      this.lista = [];
      this.totalRegistros = 0;
      return;
    }
    this.ejecutarConsultaFiltrada();
  }

  override paginar(event: PageEvent): void {
    this.paginaActual = event.pageIndex;
    this.totalPorPagina = event.pageSize;
    
    if (this.aplicandoFiltro) {
      this.ejecutarConsultaFiltrada();
    }
  }

  // 3. Método de filtrado principal del botón "Buscar" (requiere NIT obligatoriamente)
  filtrar(): void {
    if (!this.filtro.nit || !this.filtro.nit.trim()) {
      this.alertService.advertencia('Por favor ingrese obligatoriamente el NIT para realizar la búsqueda.', 'Campo Requerido');
      return;
    }
    this.paginaActual = 0;
    this.aplicandoFiltro = true;
    this.ejecutarConsultaFiltrada();
  }

  override onFiltrosChange(filtrosColumnas: { [key: string]: string }): void {
    this.filtrosMap = filtrosColumnas;
    this.paginaActual = 0;
    if (this.filtro.nit && this.filtro.nit.trim()) {
      this.aplicandoFiltro = true;
      this.ejecutarConsultaFiltrada();
    }
  }

  // 4. Lógica de consulta paginada al servicio
  private ejecutarConsultaFiltrada(): void {
    if (!this.filtro.nit || !this.filtro.nit.trim()) {
      this.lista = [];
      this.totalRegistros = 0;
      return;
    }

    const colId = this.filtrosMap['id'] ? Number(this.filtrosMap['id']) : null;
    const colNombre = this.filtrosMap['nombreOriginal'] || null;
    const colNit = this.filtrosMap['nit'] || this.filtro.nit;
    const colNumeroFactura = this.filtrosMap['numeroFactura'] || this.filtro.numeroFactura;

    this.service.filtrarDocumentosPaginado(
      colNumeroFactura,
      colNit,
      this.filtro.tipoId,
      this.filtro.extensionId,
      this.paginaActual.toString(),
      this.totalPorPagina.toString(),
      colNombre,
      colId
    ).subscribe({
      next: (paginator: any) => {
        this.lista = this.mapearSoportes(paginator.content);
        this.totalRegistros = paginator.totalElements as number;
      },
      error: (err) => {
        console.error('Error al ejecutar el filtro de soportes:', err);
        this.alertService.error('No se pudo procesar la búsqueda en el servidor.');
      }
    });
  }

  // 5. Reinicia la vista, vacía los filtros y deja la tabla totalmente limpia
  limpiar(): void {
    this.filtro.nit = '';
    this.filtro.numeroFactura = '';
    this.filtro.tipoId = null;
    this.filtro.extensionId = null;
    this.pdfUrlSafe = null;
    this.documentoActivo = '';
    this.aplicandoFiltro = false;
    this.paginaActual = 0;
    this.lista = [];
    this.totalRegistros = 0;
    this.documentosSeleccionados = [];
  }

  onSeleccionChange(selectedRows: Documento[]): void {
    this.documentosSeleccionados = selectedRows;
  }

  // Descarga individual por registro al hacer clic en el botón de descarga de la fila
  descargarSoporte(row: Documento): void {
    if (!row || !row.id) return;

    this.service.getDocumentoBlob(row.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = row.nombreOriginal || `soporte_${row.id}`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error al descargar el soporte individual:', err);
        this.alertService.error('Ocurrió un error al intentar descargar el soporte documental.');
      }
    });
  }

  descargarMasivoZip(): void {
    if (this.documentosSeleccionados.length === 0) return;

    const ids = this.documentosSeleccionados.map(doc => doc.id);
    
    this.service.descargarDocumentosMasivo(ids).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `soportes_seleccionados_${new Date().getTime()}.zip`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error en descarga masiva:', err);
        alert('Ocurrió un error al procesar y empaquetar los soportes en un archivo ZIP.');
      }
    });
  }

  // 6. Eliminación masiva de soportes seleccionados en la tabla
  eliminarSoporte(doc: Documento): void {
    if (!doc || !doc.id) return;

    const nombre = doc.nombreOriginal || `soporte #${doc.id}`;

    this.alertService.confirmar(
      `Se inactivará / eliminará el soporte "${nombre}".`,
      '¿Está seguro de eliminar este soporte?',
      'Sí, eliminar'
    ).then((result) => {
      if (result.isConfirmed) {
        this.alertService.cargando('Eliminando soporte...', 'Procesando');

        this.service.inactivarDocumento(doc.id).subscribe({
          next: () => {
            this.alertService.exito(`El soporte "${nombre}" fue eliminado correctamente.`, 'Eliminación Exitosa');
            if (this.documentoActivo === doc.nombreOriginal) {
              this.pdfUrlSafe = null;
              this.documentoActivo = '';
            }
            if (this.aplicandoFiltro) {
              this.ejecutarConsultaFiltrada();
            } else {
              this.calcularRangos();
            }
          },
          error: (err) => {
            console.error('Error al inactivar el soporte:', err);
            this.alertService.error('Ocurrió un error al intentar eliminar el soporte.');
          }
        });
      }
    });
  }

  // 7. Eliminación masiva de soportes seleccionados en la tabla
  eliminarMasivoSoportes(docs?: Documento[]): void {
    const seleccionados = (docs && docs.length > 0) ? docs : this.documentosSeleccionados;
    if (!seleccionados || seleccionados.length === 0) {
      this.alertService.advertencia('Debe seleccionar al menos un soporte de la tabla.', 'Sin Selección');
      return;
    }

    this.alertService.confirmar(
      `Se eliminarán / inactivarán ${seleccionados.length} soporte(s) seleccionado(s).`,
      '¿Está seguro de la eliminación masiva?',
      'Sí, eliminar'
    ).then((result) => {
      if (result.isConfirmed) {
        this.alertService.cargando('Eliminando soportes seleccionados...', 'Procesando');

        const peticiones = seleccionados.map(doc => this.service.inactivarDocumento(doc.id));

        forkJoin(peticiones).subscribe({
          next: () => {
            this.alertService.exito(`${seleccionados.length} soporte(s) eliminado(s) correctamente.`, 'Eliminación Masiva Exitosa');
            this.documentosSeleccionados = [];
            this.pdfUrlSafe = null;
            this.documentoActivo = '';

            if (this.aplicandoFiltro) {
              this.ejecutarConsultaFiltrada();
            } else {
              this.calcularRangos();
            }
          },
          error: (err) => {
            console.error('Error en eliminación masiva de soportes:', err);
            this.alertService.error('Ocurrió un error al intentar eliminar los soportes seleccionados.');
          }
        });
      }
    });
  }
}