import { Component, OnInit } from '@angular/core';
import { Departamento } from '../../../../models/departamento';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../../common-listar.component';
import { DepartamentoService } from '../../../../services/departamento.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-departamento',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './departamento.component.html',
  styleUrl: './departamento.component.css'
})
export class DepartamentoComponent extends CommonListarComponent<Departamento, DepartamentoService> implements OnInit {

  override titulo = 'Departamentos de Colombia';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Código DANE' },
    { field: 'descripcion', header: 'Departamento' },
    { field: 'estadoActivo', header: 'Estado' }
  ];

  campos = [
    { name: 'codigo', label: 'Código DANE (ej: 05, 11, 52)', type: 'text', required: true },
    { name: 'descripcion', label: 'Nombre del Departamento', type: 'text', required: true }
  ];

  constructor(
    service: DepartamentoService,
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
        titulo: 'Nuevo Departamento',
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

  editar(row: Departamento): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Departamento',
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

  deletedAt(row: Departamento): void {
    if (!confirm(`¿Desea eliminar el departamento ${row.codigo} - ${row.descripcion}?`)) {
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
