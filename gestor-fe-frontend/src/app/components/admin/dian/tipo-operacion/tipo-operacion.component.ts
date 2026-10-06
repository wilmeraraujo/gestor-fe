import { Component, OnInit } from '@angular/core';
import { TipoOperacion } from '../../../../models/tipo-operacion';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../../common-listar.component';
import { TipoOperacionService } from '../../../../services/tipo-operacion.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-tipo-operacion',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './tipo-operacion.component.html',
  styleUrl: './tipo-operacion.component.css'
})
export class TipoOperacionComponent extends CommonListarComponent<TipoOperacion, TipoOperacionService> implements OnInit {

  override titulo = 'Tipos de Operación DIAN';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Código' },
    { field: 'descripcion', header: 'Descripción' },
    { field: 'estadoActivo', header: 'Estado' }
  ];

  campos = [
    { name: 'codigo', label: 'Código DIAN (ej: 10, 09, 30)', type: 'text', required: true },
    { name: 'descripcion', label: 'Descripción', type: 'text', required: true }
  ];

  constructor(
    service: TipoOperacionService,
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
        titulo: 'Nuevo Tipo de Operación DIAN',
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

  editar(row: TipoOperacion): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Tipo de Operación DIAN',
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

  deletedAt(row: TipoOperacion): void {
    if (!confirm(`¿Desea eliminar el tipo de operación ${row.codigo} - ${row.descripcion}?`)) {
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
