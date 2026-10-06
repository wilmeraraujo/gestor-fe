import { Component, OnInit } from '@angular/core';
import { ResponsabilidadFiscal } from '../../../../models/responsabilidad-fiscal';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../../common-listar.component';
import { ResponsabilidadFiscalService } from '../../../../services/responsabilidad-fiscal.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-responsabilidad-fiscal',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './responsabilidad-fiscal.component.html',
  styleUrl: './responsabilidad-fiscal.component.css'
})
export class ResponsabilidadFiscalComponent extends CommonListarComponent<ResponsabilidadFiscal, ResponsabilidadFiscalService> implements OnInit {

  override titulo = 'Responsabilidades Fiscales DIAN';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Código' },
    { field: 'descripcion', header: 'Descripción' },
    { field: 'estadoActivo', header: 'Estado' }
  ];

  campos = [
    { name: 'codigo', label: 'Código DIAN (ej: O-13, R-99-PN)', type: 'text', required: true },
    { name: 'descripcion', label: 'Descripción', type: 'text', required: true }
  ];

  constructor(
    service: ResponsabilidadFiscalService,
    private dialog: MatDialog
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.calcularRangos();
  }

  agregar(): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Nueva Responsabilidad Fiscal DIAN',
        campos: this.campos,
        formData: {},
        service: this.service
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.calcularRangos();
      }
    });
  }

  editar(row: ResponsabilidadFiscal): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Responsabilidad Fiscal DIAN',
        campos: this.campos,
        formData: row,
        service: this.service
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.calcularRangos();
      }
    });
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

  deletedAt(row: ResponsabilidadFiscal): void {
    if (!confirm(`¿Desea eliminar la responsabilidad fiscal ${row.codigo} - ${row.descripcion}?`)) {
      return;
    }

    this.service.deletedAt(row.id).subscribe({
      next: () => {
        this.calcularRangos();
      },
      error: (err) => {
        console.error(err);
      }
    });
  }
}
