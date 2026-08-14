import { Component, OnInit } from '@angular/core';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../common-listar.component';
import { Extension } from '../../../models/extension';
import { ExtensionService } from '../../../services/extension.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-extension',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './extension.component.html',
  styleUrl: './extension.component.css'
})
export class ExtensionComponent extends CommonListarComponent<Extension,ExtensionService> implements OnInit{

  override titulo = 'Extensión';

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
      service: ExtensionService,
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
        titulo: 'Nueva extensión',
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

  editar(row: Extension): void {

    const dialogRef = this.dialog.open(ModalComponent, {

      width: '500px',

      data: {

        titulo: 'Editar extensión',
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

  deletedAt(row: Extension): void {

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
