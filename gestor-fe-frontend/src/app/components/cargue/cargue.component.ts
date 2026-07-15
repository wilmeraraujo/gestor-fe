import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { Cargue } from '../../models/cargue';
import { CargueService } from '../../services/cargue.service';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';

@Component({
  selector: 'app-cargue',
  standalone: true,
  imports: [CommonModule, DataTableComponent],
  templateUrl: './cargue.component.html',
  styleUrl: './cargue.component.css'
})
export class CargueComponent extends CommonListarComponent<Cargue, CargueService> implements OnInit {

  override titulo = 'Cargue de soportes';

  // Referencia local al input de archivos oculto
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  usuarioActivo: string = 'weap'; // Cambiar por tu lógica de autenticación real

  columnas = [
    { field: 'id', header: 'ID' },
    { field: 'nombreArchivo', header: 'Nombre del Archivo' },
    { field: 'numeroRegistro', header: 'Facturas Procesadas' },
    { field: 'exiteError', header: '¿Existe Error?' },
    { field: 'usuario', header: 'Usuario' },
    { field: 'jobExecutionId', header: 'ID Ejecución (Batch)' },
    { field: 'createdAt', header: 'Fecha de Creación' }
  ];

  constructor(service: CargueService,private router: Router) {
    super(service);
  }

  ngOnInit(): void {
    this.calcularRangos();
  }

  // 1. Abre el selector de archivos nativo de Windows al presionar "Adicionar"
  agregar(): void {
    if (this.fileInput) {
      this.fileInput.nativeElement.click();
    }
  }

  verFacturas(row: Cargue): void {
    // Redirige al listado de facturas llevando el ID del cargue
    this.router.navigate(['/dashboard/factura'], { 
      queryParams: { cargueId: row.id } 
    });
  }

  // 2. Gestiona el archivo ZIP seleccionado por el usuario e inicia la carga asíncrona
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      // Validación de seguridad en el Frontend
      if (file.name.split('.').pop()?.toLowerCase() !== 'zip') {
        alert('Formato inválido. Por favor, selecciona un archivo comprimido .zip');
        this.resetFileInput();
        return;
      }

      if (confirm(`¿Desea iniciar el procesamiento asíncrono para: ${file.name}?`)) {
        this.service.cargarZip(file, this.usuarioActivo).subscribe({
          next: (response) => {
            alert(`Archivo recibido correctamente. Cargue ID asignado: ${response.id}. Procesando lote...`);
            this.calcularRangos(); // Refresca la tabla para ver el registro en estado inicial
          },
          error: (err) => {
            console.error('Error al subir el archivo:', err);
            alert('Error al iniciar el cargue masivo: ' + (err.error || err.message));
          },
          complete: () => {
            this.resetFileInput();
          }
        });
      } else {
        this.resetFileInput();
      }
    }
  }

  private resetFileInput(): void {
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  // 3. Descarga el reporte detallado de errores en un archivo Excel (.xlsx)
  descargarExcelErrores(row: Cargue): void {
    if (!row.id) return;
    
    this.service.descargarExcelErrores(row.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `reporte_errores_cargue_${row.id}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error al descargar el Excel:', err);
        alert('No se encontraron registros de error o no se pudo generar el reporte.');
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

  deletedAt(row: Cargue): void {
    if (!confirm(`¿Desea eliminar el historial del cargue #${row.id}?`)) {
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