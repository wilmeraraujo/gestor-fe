import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { CommonListarComponent } from '../../common-listar.component';
import { ConfiguracionSistema } from '../../../models/configuracion-sistema';
import { ConfiguracionSistemaService } from '../../../services/configuracion-sistema.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-configuracion-sistema',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './configuracion-sistema.component.html',
  styleUrl: './configuracion-sistema.component.css'
})
export class ConfiguracionSistemaComponent extends CommonListarComponent<ConfiguracionSistema, ConfiguracionSistemaService> implements OnInit {

  override titulo = 'Configuración Global del Sistema';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Clave / Código' },
    { field: 'descripcion', header: 'Descripción' },
    { field: 'valor', header: 'Valor' },
    { field: 'categoria', header: 'Categoría' }
  ];

  campos = [
    { name: 'codigo', label: 'Código / Clave', type: 'text', required: true },
    { name: 'descripcion', label: 'Descripción', type: 'text', required: true },
    { name: 'valor', label: 'Valor', type: 'text', required: true },
    { name: 'categoria', label: 'Categoría', type: 'text', required: true }
  ];

  constructor(
    service: ConfiguracionSistemaService,
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
        titulo: 'Nueva Configuración de Sistema',
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

  editar(row: ConfiguracionSistema): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Configuración de Sistema',
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

  deletedAt(row: ConfiguracionSistema): void {
    if (!confirm(`¿Desea eliminar la configuración ${row.codigo}?`)) {
      return;
    }

    this.service.deletedAt(row.id).subscribe({
      next: () => {
        this.calcularRangos();
      },
      error: (err) => console.error(err)
    });
  }
}
