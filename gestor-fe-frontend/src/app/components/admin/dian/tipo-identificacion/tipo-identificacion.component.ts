import { Component, OnInit } from '@angular/core';
import { TipoIdentificacion } from '../../../../models/tipo-identificacion';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../../common-listar.component';
import { TipoIdentificacionService } from '../../../../services/tipo-identificacion.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-tipo-identificacion',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './tipo-identificacion.component.html',
  styleUrl: './tipo-identificacion.component.css'
})
export class TipoIdentificacionComponent extends CommonListarComponent<TipoIdentificacion, TipoIdentificacionService> implements OnInit {

  override titulo = 'Tipos de Identificación DIAN';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Código' },
    { field: 'descripcion', header: 'Descripción' },
    { field: 'estadoActivo', header: 'Estado' }
  ];

  campos = [
    { name: 'codigo', label: 'Código DIAN (ej: 13, 31, 22, PPT)', type: 'text', required: true },
    { name: 'descripcion', label: 'Descripción', type: 'text', required: true }
  ];

  constructor(
    service: TipoIdentificacionService,
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
        titulo: 'Nuevo Tipo de Identificación DIAN',
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

  editar(row: TipoIdentificacion): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Tipo de Identificación DIAN',
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

  deletedAt(row: TipoIdentificacion): void {
    if (!confirm(`¿Desea eliminar el tipo de identificación ${row.codigo} - ${row.descripcion}?`)) {
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
