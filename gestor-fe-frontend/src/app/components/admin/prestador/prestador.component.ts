import { Component, OnInit } from '@angular/core';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../common-listar.component';
import { Prestador } from '../../../models/prestador';
import { PrestadorService } from '../../../services/prestador.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-prestador-admin',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './prestador.component.html',
  styleUrl: './prestador.component.css'
})
export class PrestadorAdminComponent extends CommonListarComponent<Prestador, PrestadorService> implements OnInit {

  override titulo = 'Prestador';

  columnas = [
    {
      field: 'id',
      header: 'ID'
    },
    {
      field: 'nit',
      header: 'NIT'
    },
    {
      field: 'razonSocial',
      header: 'Razón Social'
    },
    {
      field: 'direccion',
      header: 'Dirección'
    },
    {
      field: 'telefono',
      header: 'Teléfono'
    },
    {
      field: 'email',
      header: 'Email'
    },
    {
      field: 'createdAt',
      header: 'Fecha Creación',
      type: 'date'
    },
    {
      field: 'estadoActivo',
      header: 'Estado'
    }
  ];

  campos = [
    {
      name: 'nit',
      label: 'NIT',
      type: 'text',
      required: true
    },
    {
      name: 'razonSocial',
      label: 'Razón Social',
      type: 'text',
      required: true
    },
    {
      name: 'direccion',
      label: 'Dirección',
      type: 'text',
      required: true
    },
    {
      name: 'telefono',
      label: 'Teléfono',
      type: 'text',
      required: true
    },
    {
      name: 'email',
      label: 'Email',
      type: 'email',
      required: true
    }
  ];

  constructor(
    service: PrestadorService,
    private dialog: MatDialog
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.calcularRangos();
  }

  agregar(): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '550px',
      data: {
        titulo: 'Nuevo Prestador',
        campos: this.campos,
        formData: {
          identificadorCargue: 0
        },
        service: this.service
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.calcularRangos();
      }
    });
  }

  editar(row: Prestador): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '550px',
      data: {
        titulo: 'Editar Prestador',
        campos: this.campos,
        formData: {
          ...row,
          identificadorCargue: 0
        },
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
}
