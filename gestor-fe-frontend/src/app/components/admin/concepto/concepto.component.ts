import { Component, OnInit } from '@angular/core';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../../common-listar.component';
import { Concepto } from '../../../models/concepto';
import { ConceptoService } from '../../../services/concepto.service';
import { MatDialog } from '@angular/material/dialog';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-concepto',
  standalone: true,
  imports: [DataTableComponent],
  templateUrl: './concepto.component.html',
  styleUrl: './concepto.component.css'
})
export class ConceptoComponent extends CommonListarComponent<Concepto, ConceptoService> implements OnInit {

  override titulo = 'Concepto';

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
    },
    {
      field: 'estadoActivo',
      header: 'Estado'
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
    service: ConceptoService,
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
        titulo: 'Nuevo Concepto',
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

  editar(row: Concepto): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '500px',
      data: {
        titulo: 'Editar Concepto',
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
}
