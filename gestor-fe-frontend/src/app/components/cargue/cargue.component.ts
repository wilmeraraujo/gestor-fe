import { Component } from '@angular/core';
import { DataTableComponent } from '../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-cargue',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './cargue.component.html',
  styleUrl: './cargue.component.css'
})
export class CargueComponent {

  titulo = 'Cargue de documentos';

  columnas = [

    {
      field: 'id',
      header: 'ID'
    },

    {
      field: 'nombre',
      header: 'Nombre documento'
    },

    {
      field: 'tipo',
      header: 'Tipo documento'
    },

    {
      field: 'fecha',
      header: 'Fecha cargue'
    }

  ];

  lista = [

    {
      id: 1,
      nombre: 'Contrato_001.pdf',
      tipo: 'PDF',
      fecha: '2026-05-08'
    },

    {
      id: 2,
      nombre: 'Factura_102.xml',
      tipo: 'XML',
      fecha: '2026-05-08'
    },

    {
      id: 3,
      nombre: 'Soporte.png',
      tipo: 'Imagen',
      fecha: '2026-05-07'
    }

  ];

  totalRegistros = this.lista.length;

  totalPorPagina = 5;

  agregar(): void {

    console.log('Adicionar registro');

  }

  editar(row: any): void {

    console.log('Editar:', row);

  }

  eliminar(row: any): void {

    console.log('Eliminar:', row);

  }

  buscar(texto: string): void {

    console.log('Buscar:', texto);

  }

  paginar(event: any): void {

    console.log('Paginación:', event);

  }

}
