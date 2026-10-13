import { Component, OnInit } from '@angular/core';
import { Municipio } from '../../../../models/municipio';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../../common-listar.component';
import { MunicipioService } from '../../../../services/municipio.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-municipio',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './municipio.component.html',
  styleUrl: './municipio.component.css'
})
export class MunicipioComponent extends CommonListarComponent<Municipio, MunicipioService> implements OnInit {

  override titulo = 'Municipios de Colombia';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Código DANE' },
    { field: 'descripcion', header: 'Municipio' },
    { field: 'estadoActivo', header: 'Estado' }
  ];

  campos = [
    { name: 'codigo', label: 'Código DANE (ej: 05001, 11001, 52001)', type: 'text', required: true },
    { name: 'descripcion', label: 'Nombre del Municipio', type: 'text', required: true }
  ];

  constructor(
    service: MunicipioService,
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
        titulo: 'Nuevo Municipio',
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

  editar(row: Municipio): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Municipio',
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

  deletedAt(row: Municipio): void {
    if (!confirm(`¿Desea eliminar el municipio ${row.codigo} - ${row.descripcion}?`)) {
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
