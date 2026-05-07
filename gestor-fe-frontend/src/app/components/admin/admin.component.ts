import { Component, ViewChild } from '@angular/core';
import { MATERIAL_MODULES } from '../../shared/material';
import { MatPaginator } from '@angular/material/paginator';
import { CommonListarComponent } from '../common-listar.component';
import { AdminService } from '../../services/admin.service';
import { Admin } from '../../models/admin';
import { MatTooltip } from "@angular/material/tooltip";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [...MATERIAL_MODULES, MatTooltip],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent extends CommonListarComponent<Admin,AdminService>{

  tituloCardHeader : string = 'Administración';
  mostrarColumnas: string[] = ['id', 'codigo', 'descripcion', 'acciones'];
  @ViewChild(MatPaginator) override paginator!: MatPaginator;

  constructor(
    service: AdminService,
  ) {
    super(service);
    this.nombreModel = 'Categoria de documento';
  }

  override calcularRangos(): void {

    const datos: Admin[] = [

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
      }];

      this.lista = datos;

      this.totalRegistros = datos.length;

      this.dataSource.data = datos;

      this.dataSource.paginator = this.paginator;

      this.paginator._intl.itemsPerPageLabel =
        'Registros por página:';

    }

}
