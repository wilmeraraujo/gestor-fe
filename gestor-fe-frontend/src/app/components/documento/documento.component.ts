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
import { PageEvent } from '@angular/material/paginator';

import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { Documento } from '../../models/documento';
import { DocumentoService } from '../../services/documento.service';

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
    MatSelectModule
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
    tipoDocumento: 'Todos' // 'Todos', 'PDF' (ID: 2), o 'XML' (ID: 1)
  };

  // Bandera para saber si actualmente estamos mostrando un resultado filtrado o el listado general
  aplicandoFiltro: boolean = false;

  documentosSeleccionados: Documento[] = [];

  // Columnas que consume la tabla reutilizable
  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'nombreOriginal', header: 'Nombre Archivo' },
    { field: 'tipoId', header: 'Tipo ID' } // Opcional: Representa el ID del tipo asociado
  ];

  constructor(
    service: DocumentoService,
    private sanitizer: DomSanitizer
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.calcularRangos();
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

  // 2. Gestiona la paginación heredada distinguiendo si se está filtrando o si está en listado general
  override paginar(event: PageEvent): void {
    this.paginaActual = event.pageIndex;
    this.totalPorPagina = event.pageSize;
    
    if (this.aplicandoFiltro) {
      this.ejecutarConsultaFiltrada();
    } else {
      this.calcularRangos();
    }
  }

  // 3. Método de filtrado principal del botón "Buscar"
  filtrar(): void {
    this.paginaActual = 0; // Al presionar buscar, reseteamos a la página 1
    this.aplicandoFiltro = true;
    this.ejecutarConsultaFiltrada();
  }

  // 4. Lógica de consulta paginada al servicio
  private ejecutarConsultaFiltrada(): void {
    // Mapeamos el string del selector a tus IDs de Tipo en BD (Ejemplo común: XML = 1, PDF = 2)
    let tipoIdMapeado: number | null = null;
    if (this.filtro.tipoDocumento === 'XML') {
      tipoIdMapeado = 1;
    } else if (this.filtro.tipoDocumento === 'PDF') {
      tipoIdMapeado = 2;
    }

    this.service.filtrarDocumentosPaginado(
      this.filtro.numeroFactura,
      this.filtro.nit,
      tipoIdMapeado,
      this.paginaActual.toString(),
      this.totalPorPagina.toString()
    ).subscribe({
      next: (paginator: any) => {
        this.lista = paginator.content as Documento[];
        this.totalRegistros = paginator.totalElements as number;
      },
      error: (err) => {
        console.error('Error al ejecutar el filtro de soportes:', err);
        alert('No se pudo procesar la búsqueda en el servidor.');
      }
    });
  }

  // 5. Reinicia la vista y vuelve a la paginación global limpia
  limpiar(): void {
    this.filtro.nit = '';
    this.filtro.numeroFactura = '';
    this.filtro.tipoDocumento = 'Todos';
    this.pdfUrlSafe = null;
    this.documentoActivo = '';
    this.aplicandoFiltro = false;
    this.paginaActual = 0;
    this.calcularRangos();
  }

  onSeleccionChange(selectedRows: Documento[]): void {
    this.documentosSeleccionados = selectedRows;
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
}