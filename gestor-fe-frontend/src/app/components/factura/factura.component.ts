import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { FacturaService } from '../../services/factura.service';
import { MatDialog } from '@angular/material/dialog';
import { Factura } from '../../models/factura';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-factura',
  standalone: true,
  imports: [CommonModule, DataTableComponent],
  templateUrl: './factura.component.html',
  styleUrl: './factura.component.css'
})
export class FacturaComponent extends CommonListarComponent<Factura, FacturaService> implements OnInit {

  override titulo = 'Facturas';

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'nit', header: 'NIT Emisor' },
    { field: 'razonSocialEmisor', header: 'Razón Social' },
    { field: 'numeroFactura', header: 'No. Factura' },
    { field: 'valorTotal', header: 'Valor Total' },
    { field: 'fechaEmision', header: 'Fecha Emisión' },
    { field: 'cufe', header: 'CUFE' },
    { field: 'estado', header: 'Estado' },
    { field: 'observacion', header: 'Observación' },
    { field: 'faseId', header: 'Fase' },
    //{ field: 'linea', header: 'Línea Archivo' }
  ];

  // 📝 Configuración de campos para el formulario dinámico
  camposFactura = [
    {
      name: 'estado',
      label: 'Estado de la Factura',
      type: 'select',
      required: true,
      options: [
        { value: 'APROBADA', label: 'Aprobada' },
        { value: 'RECHAZADA', label: 'Rechazada' }
      ],
      // 🎯 Lógica condicional al cambiar de estado
      onChange: (val: string, campos: any[], form: FormGroup) => {
        const campoPdf = campos.find(c => c.name === 'archivoPdf');
        const campoObs = campos.find(c => c.name === 'observacion');

        if (val === 'APROBADA') {
          // Mostrar PDF y hacer obligatorio
          if (campoPdf) campoPdf.visible = true;
          form.get('archivoPdf')?.setValidators([Validators.required]);

          // Ocultar Observación y quitar validación
          if (campoObs) campoObs.visible = false;
          form.get('observacion')?.clearValidators();
          form.get('observacion')?.setValue('');
        } else if (val === 'RECHAZADA') {
          // Ocultar PDF y quitar validación
          if (campoPdf) campoPdf.visible = false;
          form.get('archivoPdf')?.clearValidators();
          form.get('archivoPdf')?.setValue('');

          // Mostrar Observación y hacer obligatorio
          if (campoObs) campoObs.visible = true;
          form.get('observacion')?.setValidators([Validators.required]);
        } else {
          // Si no hay selección
          if (campoPdf) campoPdf.visible = false;
          if (campoObs) campoObs.visible = false;
          form.get('archivoPdf')?.clearValidators();
          form.get('observacion')?.clearValidators();
        }

        // Actualizar el estado de las validaciones en el formulario
        form.get('archivoPdf')?.updateValueAndValidity();
        form.get('observacion')?.updateValueAndValidity();
      }
    },

    // 📄 Campo Archivo PDF (Solo si es APROBADA)
    {
      name: 'archivoPdf',
      label: 'Soportes de Aprobación (PDF)',
      type: 'file',
      accept: '.pdf',
      hint: 'Sube la factura o documento de aprobación en formato PDF.',
      visible: false
    },

    // 📝 Campo Observación (Solo si es RECHAZADA)
    {
      name: 'observacion',
      label: 'Motivo de Rechazo',
      type: 'textarea',
      placeholder: 'Escriba detalladamente la razón por la cual se rechaza la factura...',
      visible: false
    }
  ];

  constructor(
    service: FacturaService,
    private dialog: MatDialog
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.calcularRangos();
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

  deletedAt(row: Factura): void {
    if (!confirm(`¿Desea eliminar la factura No. ${row.numeroFactura}?`)) {
      return;
    }

    this.service.deletedAt(row.id).subscribe({
      next: () => {
        this.calcularRangos();
      },
      error: (err) => {
        console.error('Error al intentar eliminar la factura:', err);
      }
    });
  }

  verDocumentos(row: Factura): void {
    console.log('Documentos cargados para esta factura:', row.documentos);
    
    // Aquí puedes abrir un modal especializado para ver los documentos adjuntos
    /*
    this.dialog.open(VisorDocumentosModalComponent, {
      width: '700px',
      data: { documentos: row.documentos }
    });
    */
  }

  /**
   * 🧾 Abrir Modal Reutilizable para Gestionar/Editar la Factura
   */
  abrirModalGestionar(row: Factura): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '550px',
      data: {
        titulo: `Gestionar Factura No. ${row.numeroFactura}`,
        campos: this.camposFactura,
        formData: row,              // Pasa los datos actuales de la fila seleccionada
        service: this.service       // Pasa el servicio para consumir .editar(model)
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.calcularRangos();     // Recarga la tabla al guardar cambios exitosamente
      }
    });
  }
}