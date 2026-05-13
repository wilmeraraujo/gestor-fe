import { Component, OnInit } from '@angular/core';
import { CommonListarComponent } from '../../common-listar.component';
import { Estado } from '../../../models/estado';
import { EstadoService } from '../../../services/estado.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-estado',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './estado.component.html',
  styleUrl: './estado.component.css'
})
export class EstadoComponent extends CommonListarComponent<Estado,EstadoService> implements OnInit{

  override titulo = 'Estado';

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
      service: EstadoService,
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
        titulo: 'Nuevo estado',
        campos: this.campos,
        formData: {},
        service: this.service
      }

    });

    dialogRef.afterClosed().subscribe(result => {

      if(result){
        this.calcularRangos();
      }

    });

  }

  editar(row: Estado): void {

    const dialogRef = this.dialog.open(ModalComponent, {

      width: '500px',

      data: {

        titulo: 'Editar Estado',
        campos: this.campos,
        formData: row,
        service: this.service

      }

    });

    dialogRef.afterClosed().subscribe(result => {

      if(result){
        this.calcularRangos();
      }

    });

  }


  buscar(texto: string): void {

    if(!texto || texto.trim() === ''){
      this.calcularRangos();
      return;
    }

    this.service.buscar(texto).subscribe(response => {
      this.lista = response;
      this.totalRegistros = response.length;
    });

  }

  deletedAt(row: Estado): void {

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
