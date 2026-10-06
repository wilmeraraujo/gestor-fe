import { Component, OnInit } from '@angular/core';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../common-listar.component';
import { Movimiento } from '../../../models/movimiento';
import { MovimientoService } from '../../../services/movimiento.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-movimiento',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './movimiento.component.html',
  styleUrl: './movimiento.component.css'
})
export class MovimientoComponent extends CommonListarComponent<Movimiento, MovimientoService> implements OnInit {

  override titulo = 'Movimiento';

  columnas = [
    {
      field: 'id',
      header: 'ID'
    },
    {
      field: 'codigo',
      header: 'Código'
    },
    {
      field: 'descripcion',
      header: 'Descripción'
    },
    {
      field: 'estadoActivo',
      header: 'Estado'
    }
  ];

  campos = [
    {
      name: 'codigo',
      label: 'Código',
      type: 'text',
      required: true
    },
    {
      name: 'descripcion',
      label: 'Descripción',
      type: 'text',
      required: true
    }
  ];

  constructor(
    service: MovimientoService,
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
        titulo: 'Nuevo movimiento',
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

  editar(row: Movimiento): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar movimiento',
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

  deletedAt(row: Movimiento): void {
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
