import { Component, OnInit } from '@angular/core';
import { TipoIdentificacion } from '../../../models/tipo-identificacion';
import { TipoIdentificacionService } from '../../../services/tipo-identificacion.service';
import { CommonListarComponent } from '../../common-listar.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

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

  constructor(
      service: TipoIdentificacionService
  ) {
    super(service);
  }

  ngOnInit(): void {

    this.calcularRangos();

  }


  agregar(): void {

    console.log('Adicionar registro');

  }

  editar(row: TipoIdentificacion): void {

    console.log('Editar:', row);

  }


  buscar(texto: string): void {

    console.log('Buscar:', texto);

  }
}
