import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { FacturaService } from '../../services/factura.service';
import { MatDialog } from '@angular/material/dialog';
import { Factura } from '../../models/factura';

@Component({
  selector: 'app-factura',
  standalone: true,
  imports: [CommonModule, DataTableComponent],
  templateUrl: './factura.component.html',
  styleUrl: './factura.component.css'
})
export class FacturaComponent extends CommonListarComponent<Factura, FacturaService> implements OnInit {

  override titulo = 'Facturas';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'nit', header: 'NIT Emisor' },
    { field: 'razonSocialEmisor', header: 'Razón Social' },
    { field: 'numeroFactura', header: 'No. Factura' },
    { field: 'valorTotal', header: 'Valor Total' },
    { field: 'fechaEmision', header: 'Fecha Emisión' },
    { field: 'linea', header: 'Línea Archivo' }
  ];

  constructor(
    service: FacturaService,
    private dialog: MatDialog
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.calcularRangos();
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

  deletedAt(row: Factura): void {
    if (!confirm(`¿Desea eliminar la factura No. ${row.numeroFactura}?`)) {
      return;
    }

    this.service.deletedAt(row.id).subscribe({
      next: () => {
        this.calcularRangos();
      },
      error: (err) => {
        console.error('Error al intentar eliminar la factura:', err);
      }
    });
  }

  verDocumentos(row: Factura): void {
    console.log('Documentos cargados para esta factura:', row.documentos);
    
    // Aquí puedes abrir un modal especializado para ver los documentos adjuntos
    /*
    this.dialog.open(VisorDocumentosModalComponent, {
      width: '700px',
      data: { documentos: row.documentos }
    });
    */
  }
}