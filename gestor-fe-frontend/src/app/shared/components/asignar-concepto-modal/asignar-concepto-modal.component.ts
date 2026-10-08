import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Factura } from '../../../models/factura';
import { Concepto } from '../../../models/concepto';
import { ConceptoService } from '../../../services/concepto.service';
import { FacturaService } from '../../../services/factura.service';
import { LoginService } from '../../../services/login.service';
import { AlertService } from '../../../services/alert.service';

export interface AsignarConceptoDialogData {
  factura: Factura;
}

@Component({
  selector: 'app-asignar-concepto-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './asignar-concepto-modal.component.html',
  styleUrl: './asignar-concepto-modal.component.css'
})
export class AsignarConceptoModalComponent implements OnInit {

  factura: Factura;
  conceptos: Concepto[] = [];
  conceptosFiltrados: Concepto[] = [];
  
  codigoConceptoSeleccionado: string = '';
  conceptoSeleccionadoObj: Concepto | null = null;
  filtroTexto: string = '';

  cargando: boolean = true;
  guardando: boolean = false;

  private conceptoService = inject(ConceptoService);
  private facturaService = inject(FacturaService);
  private loginService = inject(LoginService);
  private alertService = inject(AlertService);

  constructor(
    public dialogRef: MatDialogRef<AsignarConceptoModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AsignarConceptoDialogData
  ) {
    this.factura = data.factura;
    this.codigoConceptoSeleccionado = data.factura?.codigoConcepto || '';
  }

  ngOnInit(): void {
    this.cargarConceptos();
  }

  cargarConceptos(): void {
    this.cargando = true;
    this.conceptoService.listarActivos().subscribe({
      next: (lista: Concepto[]) => {
        // Asegurar que solo se incluyan conceptos activos (sin deletedAt)
        this.conceptos = (lista || [])
          .filter(c => c && !c.deletedAt)
          .sort((a, b) => (a.codigo || '').localeCompare(b.codigo || ''));
        
        this.conceptosFiltrados = [...this.conceptos];

        if (this.codigoConceptoSeleccionado) {
          this.conceptoSeleccionadoObj = this.conceptos.find(
            c => c.codigo === this.codigoConceptoSeleccionado
          ) || null;
        }

        this.cargando = false;
      },
      error: (err) => {
        console.error('Error al cargar conceptos activos de administración:', err);
        // Fallback al listar tradicional filtrando en memoria
        this.conceptoService.listar().subscribe({
          next: (listaFallback: Concepto[]) => {
            this.conceptos = (listaFallback || [])
              .filter(c => c && !c.deletedAt)
              .sort((a, b) => (a.codigo || '').localeCompare(b.codigo || ''));
            this.conceptosFiltrados = [...this.conceptos];
            this.cargando = false;
          },
          error: () => {
            this.cargando = false;
            this.alertService.error('No se pudieron cargar los conceptos de administración.');
          }
        });
      }
    });
  }

  filtrarConceptos(): void {
    const term = (this.filtroTexto || '').toLowerCase().trim();
    if (!term) {
      this.conceptosFiltrados = [...this.conceptos];
      return;
    }

    this.conceptosFiltrados = this.conceptos.filter(c => 
      (c.codigo && c.codigo.toLowerCase().includes(term)) ||
      (c.descripcion && c.descripcion.toLowerCase().includes(term))
    );
  }

  seleccionarConcepto(concepto: Concepto): void {
    if (this.codigoConceptoSeleccionado === concepto.codigo) {
      // Si hace click en el mismo, no deselecciona obligatoriamente pero mantiene el objeto
      this.conceptoSeleccionadoObj = concepto;
    } else {
      this.codigoConceptoSeleccionado = concepto.codigo;
      this.conceptoSeleccionadoObj = concepto;
    }
  }

  asignar(): void {
    if (!this.codigoConceptoSeleccionado) {
      this.alertService.advertencia('Por favor seleccione un concepto para asignar.', 'Concepto Requerido');
      return;
    }

    const usuario = this.loginService.getUserName();
    this.guardando = true;
    this.alertService.cargando('Asignando concepto a la factura...', 'Procesando');

    this.facturaService.asignarConcepto(this.factura.id, this.codigoConceptoSeleccionado, usuario).subscribe({
      next: (facturaActualizada: Factura) => {
        this.guardando = false;
        this.alertService.exito(
          `El concepto [${this.codigoConceptoSeleccionado}] fue asignado exitosamente a la factura No. ${this.factura.numeroFactura}.`,
          'Concepto Asignado'
        );
        this.dialogRef.close(facturaActualizada || true);
      },
      error: (err) => {
        this.guardando = false;
        console.error('Error al asignar concepto:', err);
        this.alertService.error(
          err?.error?.message || 'No fue posible asignar el concepto a la factura. Verifique e intente nuevamente.'
        );
      }
    });
  }

  cerrar(): void {
    this.dialogRef.close(false);
  }
}
