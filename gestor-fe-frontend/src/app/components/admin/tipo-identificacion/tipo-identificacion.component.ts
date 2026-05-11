import { Component, OnInit } from '@angular/core';
import { TipoIdentificacion } from '../../../models/tipo-identificacion';
import { TipoIdentificacionService } from '../../../services/tipo-identificacion.service';
import { CommonListarComponent } from '../../common-listar.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-tipo-identificacion',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './tipo-identificacion.component.html',
  styleUrl: './tipo-identificacion.component.css'
})
export class TipoIdentificacionComponent
  extends CommonListarComponent<TipoIdentificacion,TipoIdentificacionService> implements OnInit{

  override titulo = 'Tipo identificación';

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
        titulo: 'Nuevo Tipo Identificación',
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

  editar(row: TipoIdentificacion): void {

    const dialogRef = this.dialog.open(ModalComponent, {

      width: '500px',

      data: {

        titulo: 'Editar Tipo Identificación',
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

    console.log('Buscar:', texto);

  }
}
