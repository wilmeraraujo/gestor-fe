import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';

import { Factura } from '../../models/factura';
import { Documento } from '../../models/documento';
import { FacturaService } from '../../services/factura.service';
import { DocumentoService } from '../../services/documento.service';
import { FaseService } from '../../services/fase.service';

@Component({
  selector: 'app-seguimiento-facturas',
  standalone: true,
  imports: [
    CommonModule, 
    DataTableComponent,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './seguimiento-facturas.component.html',
  styleUrl: './seguimiento-facturas.component.css'
})
export class SeguimientoFacturasComponent extends CommonListarComponent<Factura, FacturaService> implements OnInit {

  override titulo = 'Módulo de Seguimiento y Trazabilidad Global';

  tabSeleccionada: number = 0; 
  facturaSeleccionada: Factura | null = null;
  soportesFactura: Documento[] = [];
  pdfUrlSafe: SafeResourceUrl | null = null;
  documentoActivo: string = '';

  mapaFases: { [key: number]: string } = {};

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'estado', header: 'Estado' },
    { field: 'faseNombre', header: 'Etapa / Fase Actual' },
    { field: 'nit', header: 'NIT Emisor' },
    { field: 'razonSocialEmisor', header: 'Razón Social' },
    { field: 'numeroFactura', header: 'No. Factura' },
    { field: 'tipoRegistroContable', header: 'Tipo Reg.' },
    { field: 'numeroCausacion', header: 'No. Causación' },
    { field: 'valorTotal', header: 'Valor Total' },
    { field: 'fechaEmision', header: 'Fecha Emisión' },
    { field: 'observacion', header: 'Observaciones / Causal' }
  ];

  columnasSoportes = [
    { field: 'id', header: 'ID' },
    { field: 'nombreOriginal', header: 'Nombre Archivo' },
    { field: 'tipoId', header: 'Tipo ID' }
  ];

  constructor(
    service: FacturaService,
    private documentoService: DocumentoService,
    private faseService: FaseService,
    private sanitizer: DomSanitizer
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.cargarFases();
  }

  private cargarFases(): void {
    this.faseService.listar().subscribe({
      next: (fases: any[]) => {
        this.mapaFases = (fases || [])
          .filter((f: any) => !f.deletedAt)
          .reduce((acc: any, f: any) => {
            acc[f.id] = f.descripcion || f.nombre || `Fase ${f.id}`;
            return acc;
          }, {});

        this.cargarDatosPaginados();
      },
      error: (err) => {
        console.error('Error al cargar catálogo de fases:', err);
        this.cargarDatosPaginados();
      }
    });
  }

  /**
   * 📊 Carga consolidada global de TODAS las facturas registradas en el sistema
   */
  cargarDatosPaginados(): void {
    this.service.getSeguimiento(this.paginaActual, this.totalPorPagina)
      .subscribe(res => {
        this.lista = this.mapearFaseNombre(res.content);
        this.totalRegistros = res.totalElements;
      });
  }

  private mapearFaseNombre(facturas: Factura[]): any[] {
    return (facturas || []).map(f => ({
      ...f,
      faseNombre: this.mapaFases[f.faseId] || `Fase ${f.faseId}`
    }));
  }

  /**
   * 👁️ Carga los soportes documentales de la factura y abre la Pestaña 2
   */
  verSoportesFactura(row: Factura): void {
    this.facturaSeleccionada = row;
    this.pdfUrlSafe = null;
    this.documentoActivo = '';

    this.documentoService.filtrarDocumentosPaginado(
      row.numeroFactura,
      row.nit,
      null,
      '0',
      '50'
    ).subscribe({
      next: (res: any) => {
        this.soportesFactura = res.content || [];
        this.tabSeleccionada = 1;
      },
      error: (err) => {
        console.error('Error al consultar expedientes:', err);
        alert('No se pudieron consultar los soportes de esta factura.');
      }
    });
  }

  cargarSoporteEnVisor(doc: Documento): void {
    this.documentoActivo = doc.nombreOriginal;

    this.documentoService.getDocumentoBlob(doc.id).subscribe({
      next: (blob: Blob) => {
        const fileUrl = URL.createObjectURL(blob);
        this.pdfUrlSafe = this.sanitizer.bypassSecurityTrustResourceUrl(fileUrl);
      },
      error: (err) => {
        console.error('Error al cargar el archivo en el iframe:', err);
        alert('Este soporte no se puede previsualizar en el navegador.');
      }
    });
  }

  regresarABandeja(): void {
    this.tabSeleccionada = 0;
    this.facturaSeleccionada = null;
    this.pdfUrlSafe = null;
    this.documentoActivo = '';
  }

  buscar(texto: string): void {
    if (!texto || texto.trim() === '') {
      this.cargarDatosPaginados();
      return;
    }

    this.service.buscar(texto).subscribe(response => {
      this.lista = this.mapearFaseNombre(response);
      this.totalRegistros = response.length;
    });
  }
}