import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { Cargue } from '../../models/cargue';
import { CargueService } from '../../services/cargue.service';
import { AlertService } from '../../services/alert.service';
import { LoginService } from '../../services/login.service';

@Component({
  selector: 'app-cargue',
  standalone: true,
  imports: [CommonModule, DataTableComponent],
  templateUrl: './cargue.component.html',
  styleUrl: './cargue.component.css'
})
export class CargueComponent extends CommonListarComponent<Cargue, CargueService> implements OnInit, OnDestroy {

  override titulo = 'Cargue de soportes';
  textoBotonAgregar = 'Cargar ZIP';
  tooltipAgregar = 'Cargar archivo zip de facturas';

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  usuarioActivo: string = '';
  rolesUsuario: string[] = [];

  esAdmin: boolean = false;
  esPrestador: boolean = false;

  private sseSubscription?: Subscription;

  // 📝 Mapeo de columnas agregando la propiedad calculada 'estadoNombre'
  columnas = [
    { field: 'id', header: 'Id', filtrable: false },
    { field: 'nombreArchivo', header: 'Nombre del Archivo', filtrable: true },
    { field: 'numeroRegistro', header: 'Facturas Procesadas', filtrable: true },
    { field: 'estadoNombre', header: 'Estado', filtrable: true },
    { field: 'usuario', header: 'Usuario', filtrable: true },
    { field: 'createdAt', header: 'Fecha de Creación', filtrable: true }
  ];

  constructor(
    service: CargueService,
    private router: Router,
    private alertService: AlertService,
    private loginService: LoginService
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.cargarDatosSesion();
    this.calcularRangos();
    this.iniciarSuscripcionSSE();
  }

  ngOnDestroy(): void {
    if (this.sseSubscription) {
      this.sseSubscription.unsubscribe();
    }
  }

  private cargarDatosSesion(): void {
    this.usuarioActivo = this.loginService.getUserName();
    this.rolesUsuario = this.loginService.getUserRoles() || [];

    this.esAdmin = this.loginService.isAdmin || this.loginService.isGAdmin || this.loginService.isGCargue;
    this.esPrestador = this.loginService.isPrestador;
  }

  /**
   * 📡 Suscripción SSE para actualizar la grilla en tiempo real al finalizar Spring Batch
   */
  private iniciarSuscripcionSSE(): void {
    if (!this.usuarioActivo) return;

    this.sseSubscription = this.service.conectarSSE(this.usuarioActivo).subscribe({
      next: (evento) => {
        this.calcularRangos();

        if (evento.exiteError) {
          this.alertService.advertencia(
            `El cargue finalizó con errores. Revisa el reporte de inconsistencias.`,
            'Cargue con Errores'
          );
        } else {
          this.alertService.exito(
            `El cargue finalizó exitosamente. Facturas procesadas: ${evento.numeroRegistro}`,
            'Cargue Exitoso'
          );
        }
      },
      error: (err) => console.error('Error en conexión SSE:', err)
    });
  }

  /**
   * 🏷️ Transforma los flags de la BD a texto descriptivo de Estado
   */
  private procesarEstados(cargues: Cargue[]): any[] {
    return (cargues || []).map(c => {
      let estadoTxt = 'PROCESANDO';

      // Si el Job ya finalizó en Spring Batch (jobExecutionId != null)
      if (c.jobExecutionId) {
        estadoTxt = c.exiteError ? 'CON ERRORES' : 'CARGADO';
      }

      return {
        ...c,
        estadoNombre: estadoTxt
      };
    });
  }

  override calcularRangos(): void {
    this.usuarioActivo = this.loginService.getUserName();

    this.service.getPaginableActivosConRoles(
      this.paginaActual.toString(),
      this.totalPorPagina.toString(),
      this.usuarioActivo,
      this.rolesUsuario
    ).subscribe({
      next: (res: any) => {
        this.lista = this.procesarEstados(res.content);
        this.totalRegistros = res.totalElements || 0;
      },
      error: (err) => console.error('Error al consultar lista de cargues:', err)
    });
  }

  agregar(): void {
    if (this.fileInput) {
      this.fileInput.nativeElement.click();
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      if (file.name.split('.').pop()?.toLowerCase() !== 'zip') {
        this.alertService.advertencia('Formato inválido. Selecciona un archivo comprimido .zip', 'Formato no permitido');
        this.resetFileInput();
        return;
      }

      this.alertService.confirmar(
        `¿Desea iniciar el procesamiento asíncrono para: ${file.name}?`,
        'Confirmar Cargue Masivo',
        'Sí, iniciar cargue'
      ).then((result) => {
        if (result.isConfirmed) {

          const usuarioEnvio = this.loginService.getUserName();
          this.alertService.cargando('Subiendo archivo ZIP...', 'Procesando archivo');

          this.service.cargarZip(file, usuarioEnvio).subscribe({
            next: (response) => {
              this.alertService.exito(
                `Archivo recibido correctamente. Cargue ID asignado: ${response.id}. Procesando lote...`,
                'Cargue Exitoso'
              );
              this.calcularRangos();
            },
            error: (err) => {
              console.error('Error al subir el archivo:', err);
              const mensajeError = err.error?.message || err.error || err.message || 'Ocurrió un error inesperado';
              this.alertService.error(`Error al iniciar el cargue masivo: ${mensajeError}`);
            },
            complete: () => this.resetFileInput()
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

  descargarExcelErrores(row: Cargue): void {
    if (!row.id) return;

    this.alertService.cargando('Generando reporte en Excel...', 'Un momento por favor');

    this.service.descargarExcelErrores(row.id).subscribe({
      next: (blob: Blob) => {
        this.alertService.cerrar();
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
      this.lista = this.procesarEstados(response);
      this.totalRegistros = response.length;
    });
  }

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