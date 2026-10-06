import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { Factura } from '../../../models/factura';
import { FacturaItem } from '../../../models/factura-item';
import { FacturaService } from '../../../services/factura.service';

export interface FichaFacturaDialogData {
  facturaId?: number;
  factura?: Factura;
}

@Component({
  selector: 'app-ficha-factura-modal',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  templateUrl: './ficha-factura-modal.component.html',
  styleUrl: './ficha-factura-modal.component.css'
})
export class FichaFacturaModalComponent implements OnInit {

  factura!: Factura;
  items: FacturaItem[] = [];
  cargando: boolean = true;
  cufeCopiado: boolean = false;

  columnasItems: string[] = [
    'linea',
    'codigo',
    'descripcion',
    'cantidad',
    'unidad',
    'precioUnitario',
    'total'
  ];

  constructor(
    public dialogRef: MatDialogRef<FichaFacturaModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: FichaFacturaDialogData,
    private facturaService: FacturaService
  ) {}

  ngOnInit(): void {
    if (this.data.factura) {
      this.factura = { ...this.data.factura };
    }

    const id = this.data.facturaId || (this.data.factura ? this.data.factura.id : null);
    if (id) {
      this.cargarDetalleCompleto(id);
    } else {
      this.cargando = false;
    }
  }

  cargarDetalleCompleto(id: number): void {
    this.cargando = true;

    // Consultamos la factura completa para traer todos los datos de cliente, retenciones y notas
    this.facturaService.ver(id).subscribe({
      next: (f: Factura) => {
        if (f) {
          this.factura = { ...this.factura, ...f };
        }
        // Consultamos la lista de ítems de la factura
        this.facturaService.obtenerItems(id).subscribe({
          next: (items: FacturaItem[]) => {
            this.items = items || [];
            this.cargando = false;
          },
          error: (err) => {
            console.warn('No fue posible cargar los ítems de la factura:', err);
            // Si la entidad factura ya traía items en memoria
            if (this.factura && this.factura.items) {
              this.items = this.factura.items;
            }
            this.cargando = false;
          }
        });
      },
      error: (err) => {
        console.error('Error cargando detalle de factura:', err);
        this.cargando = false;
      }
    });
  }

  copiarCufe(): void {
    if (this.factura?.cufe) {
      navigator.clipboard.writeText(this.factura.cufe).then(() => {
        this.cufeCopiado = true;
        setTimeout(() => (this.cufeCopiado = false), 2500);
      });
    }
  }

  cerrar(): void {
    this.dialogRef.close();
  }
}
