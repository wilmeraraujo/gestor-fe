import { Component, OnInit } from '@angular/core';
import { UnidadMedida } from '../../../../models/unidad-medida';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../../common-listar.component';
import { UnidadMedidaService } from '../../../../services/unidad-medida.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-unidad-medida',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './unidad-medida.component.html',
  styleUrl: './unidad-medida.component.css'
})
export class UnidadMedidaComponent extends CommonListarComponent<UnidadMedida, UnidadMedidaService> implements OnInit {

  override titulo = 'Unidades de Medida DIAN';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Código' },
    { field: 'descripcion', header: 'Descripción' },
    { field: 'estadoActivo', header: 'Estado' }
  ];

  campos = [
    { name: 'codigo', label: 'Código DIAN', type: 'text', required: true },
    { name: 'descripcion', label: 'Descripción', type: 'text', required: true }
  ];

  constructor(
    service: UnidadMedidaService,
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
        titulo: 'Nueva Unidad de Medida DIAN',
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

  editar(row: UnidadMedida): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Unidad de Medida DIAN',
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

  deletedAt(row: UnidadMedida): void {
    if (!confirm(`¿Desea eliminar la unidad de medida ${row.codigo} - ${row.descripcion}?`)) {
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
