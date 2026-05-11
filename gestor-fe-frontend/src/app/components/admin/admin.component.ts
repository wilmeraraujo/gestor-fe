import { Component } from '@angular/core';

import { CommonListarComponent } from '../common-listar.component';

import { AdminService } from '../../services/admin.service';

import { Admin } from '../../models/admin';

import { DataTableComponent } from '../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    DataTableComponent
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent
  extends CommonListarComponent<Admin, AdminService>{

  override titulo = 'Administración';

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
    service: AdminService
  ) {
    super(service);
  }

  override lista = [

    {
      id: 1,
      codigo: 'ADM-001',
      descripcion: 'Administración General'
    },

    {
      id: 2,
      codigo: 'ADM-002',
      descripcion: 'Gestión Documental'
    },

    {
      id: 3,
      codigo: 'ADM-003',
      descripcion: 'Facturación Electrónica'
    },

    {
      id: 4,
      codigo: 'ADM-004',
      descripcion: 'Auditoría Médica'
    }

  ];

  override totalRegistros = this.lista.length;

  override totalPorPagina = 5;

  agregar(): void {

    console.log('Adicionar registro');

  }

  editar(row: Admin): void {

    console.log('Editar:', row);

  }


  buscar(texto: string): void {

    console.log('Buscar:', texto);

  }


}
