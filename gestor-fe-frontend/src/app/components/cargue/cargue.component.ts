import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { Cargue } from '../../models/cargue';
import { CargueService } from '../../services/cargue.service';
import { AlertService } from '../../services/alert.service'; // 👈 Inyección del AlertService
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

  usuarioActivo: string = 'weap'; // Lógica de autenticación

  columnas = [
    { field: 'id', header: 'ID', filtrable: false },
    { field: 'nombreArchivo', header: 'Nombre del Archivo', filtrable: true },
    { field: 'numeroRegistro', header: 'Facturas Procesadas', filtrable: true },
    { field: 'exiteError', header: '¿Existe Error?', filtrable: true },
    { field: 'usuario', header: 'Usuario', filtrable: true },
    { field: 'jobExecutionId', header: 'ID Ejecución (Batch)', filtrable: false },
    { field: 'createdAt', header: 'Fecha de Creación', filtrable: true }
  ];

  constructor(
    service: CargueService,
    private router: Router,
    private alertService: AlertService // 👈 Inyectado en el constructor
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.calcularRangos();
  }

  // 1. Abre el selector de archivos nativo
  agregar(): void {
    if (this.fileInput) {
      this.fileInput.nativeElement.click();
    }
  }

  verFacturas(row: Cargue): void {
    this.router.navigate(['/dashboard/factura'], {
      queryParams: { cargueId: row.id }
    });
  }

  // 2. Gestiona el archivo ZIP e inicia la carga asíncrona
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      // Validación de extensión en el Frontend
      if (file.name.split('.').pop()?.toLowerCase() !== 'zip') {
        this.alertService.advertencia('Formato inválido. Por favor, selecciona un archivo comprimido .zip', 'Formato no permitido');
        this.resetFileInput();
        return;
      }

      // Confirmación modal previa a la carga
      this.alertService.confirmar(
        `¿Desea iniciar el procesamiento asíncrono para: ${file.name}?`,
        'Confirmar Cargue Masivo',
        'Sí, iniciar cargue'
      ).then((result) => {
        if (result.isConfirmed) {

          // Muestra spinner de carga mientras se sube el archivo al servidor
          this.alertService.cargando('Subiendo archivo ZIP...', 'Procesando archivo');

          this.service.cargarZip(file, this.usuarioActivo).subscribe({
            next: (response) => {
              this.alertService.exito(
                `Archivo recibido correctamente. Cargue ID asignado: ${response.id}. Procesando lote...`,
                'Cargue Exitoso'
              );
              this.calcularRangos(); // Refresca la tabla
            },
            error: (err) => {
              console.error('Error al subir el archivo:', err);
              const mensajeError = err.error?.message || err.error || err.message || 'Ocurrió un error inesperado';
              this.alertService.error(`Error al iniciar el cargue masivo: ${mensajeError}`);
            },
            complete: () => {
              this.resetFileInput();
            }
          });
        } else {
          this.resetFileInput();
        }
      });
    }
  }

  private resetFileInput(): void {
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  // 3. Descarga el reporte de errores en Excel (.xlsx)
  descargarExcelErrores(row: Cargue): void {
    if (!row.id) return;

    this.alertService.cargando('Generando reporte en Excel...', 'Un momento por favor');

    this.service.descargarExcelErrores(row.id).subscribe({
      next: (blob: Blob) => {
        this.alertService.cerrar(); // Cierra la alerta de cargando

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
        this.alertService.info('No se encontraron registros de error o no se pudo generar el reporte.', 'Sin registros');
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

  // 4. Eliminación de historial
  deletedAt(row: Cargue): void {
    this.alertService.confirmar(
      `¿Desea eliminar el historial del cargue #${row.id}?`,
      '¿Eliminar Historial?',
      'Sí, eliminar'
    ).then((result) => {
      if (result.isConfirmed) {

        this.service.deletedAt(row.id).subscribe({
          next: () => {
            this.alertService.exito('El historial del cargue ha sido eliminado.', 'Registro Eliminado');
            this.calcularRangos();
          },
          error: (err) => {
            console.error(err);
            this.alertService.error('No se pudo eliminar el registro seleccionado.');
          }
        });

      }
    });
  }
}
