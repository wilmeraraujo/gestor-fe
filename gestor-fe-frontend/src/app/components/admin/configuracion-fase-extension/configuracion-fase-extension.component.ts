import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { CommonListarComponent } from '../../common-listar.component';
import { ConfiguracionFaseExtension } from '../../../models/configuracion-fase-extension';
import { ConfiguracionFaseExtensionService } from '../../../services/configuracion-fase-extension.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-configuracion-fase-extension',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './configuracion-fase-extension.component.html',
  styleUrl: './configuracion-fase-extension.component.css'
})
export class ConfiguracionFaseExtensionComponent extends CommonListarComponent<ConfiguracionFaseExtension, ConfiguracionFaseExtensionService> implements OnInit {

  override titulo = 'Configuración Fase / Extensiones';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'codigo', header: 'Código' },
    { field: 'descripcion', header: 'Descripción' },
    { field: 'faseId', header: 'ID Fase' },
    { field: 'extensionId', header: 'ID Extensión' },
    { field: 'tamanoMaximoMb', header: 'Tamaño Máx (MB)' },
    { field: 'obligatorio', header: 'Obligatorio' },
    { field: 'permiteMultiple', header: 'Permite Múltiples' }
  ];

  campos = [
    { name: 'codigo', label: 'Código', type: 'text', required: true },
    { name: 'descripcion', label: 'Descripción', type: 'text', required: true },
    { name: 'faseId', label: 'ID de la Fase', type: 'number', required: true },
    { name: 'extensionId', label: 'ID de la Extensión', type: 'number', required: true },
    { name: 'tamanoMaximoMb', label: 'Tamaño Máximo (MB)', type: 'number', required: true },
    { name: 'obligatorio', label: '¿Es Obligatorio?', type: 'checkbox', required: false },
    { name: 'permiteMultiple', label: '¿Permite Múltiples Archivos?', type: 'checkbox', required: false }
  ];

  constructor(
    service: ConfiguracionFaseExtensionService,
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
        titulo: 'Nueva Regla Fase/Extensión',
        campos: this.campos,
        formData: {
          tamanoMaximoMb: 10,
          obligatorio: false,
          permiteMultiple: true
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

  editar(row: ConfiguracionFaseExtension): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Regla Fase/Extensión',
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

  deletedAt(row: ConfiguracionFaseExtension): void {
    if (!confirm(`¿Desea eliminar la regla ${row.descripcion}?`)) {
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
