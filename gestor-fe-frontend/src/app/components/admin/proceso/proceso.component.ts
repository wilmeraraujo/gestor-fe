import { Component, OnInit } from '@angular/core';
import { CommonListarComponent } from '../../common-listar.component';
import { Proceso } from '../../../models/proceso';
import { ProcesoService } from '../../../services/proceso.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-proceso',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './proceso.component.html',
  styleUrl: './proceso.component.css'
})
export class ProcesoComponent extends CommonListarComponent<Proceso, ProcesoService> implements OnInit {

  override titulo = 'Proceso';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Código' },
    { field: 'descripcion', header: 'Descripción' },
    { field: 'estadoActivo', header: 'Estado' }
  ];

  campos = [
    { name: 'codigo', label: 'Código', type: 'text', required: true },
    { name: 'descripcion', label: 'Descripción', type: 'text', required: true }
  ];

  constructor(
    service: ProcesoService,
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
        titulo: 'Nuevo Proceso',
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

  editar(row: Proceso): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Proceso',
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

  deletedAt(row: Proceso): void {
    if (!confirm(`¿Desea eliminar el registro ${row.descripcion}?`)) {
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
