import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, throwError } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';

import { DataTableComponent } from '../../shared/components/data-table/data-table.component';
import { CommonListarComponent } from '../common-listar.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { Cargue } from '../../models/cargue';
import { CargueService } from '../../services/cargue.service';
import { MovimientoService } from '../../services/movimiento.service';
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

  // Permisos de interfaz
  esAdminOPerfilAdmin: boolean = false;
  esPrestador: boolean = false;

  private sseSubscription?: Subscription;

  opcionesMovimiento: { value: any, label: string }[] = [];
  mapaMovimientos: { [id: number]: string } = {};

  columnas = [
    { field: 'id', header: 'ID', filtrable: false },
    { field: 'nombreArchivo', header: 'Nombre del Archivo', filtrable: true },
    { field: 'movimientoNombre', header: 'Tipo Movimiento', filtrable: true },
    { field: 'numeroRegistro', header: 'Facturas Procesadas', filtrable: true },
    { field: 'estadoNombre', header: 'Estado', filtrable: true },
    { field: 'usuario', header: 'Usuario', filtrable: true },
    { field: 'createdAt', header: 'Fecha de Creación', filtrable: true }
  ];

  constructor(
    service: CargueService,
    private router: Router,
    private alertService: AlertService,
    private loginService: LoginService,
    private dialog: MatDialog,
    private movimientoService: MovimientoService
  ) {
    super(service);
  }

  ngOnInit(): void {
    this.cargarDatosSesion();
    this.cargarMovimientos();
    this.calcularRangos();
    this.iniciarSuscripcionSSE();
  }

  override ngOnDestroy(): void {
    if (this.sseSubscription) {
      this.sseSubscription.unsubscribe();
    }
  }

  private cargarDatosSesion(): void {
    this.usuarioActivo = this.loginService.getUserName();
    this.rolesUsuario = this.loginService.getUserRoles() || [];

    // Habilita visibilidad y borrado para Administradores
    this.esAdminOPerfilAdmin = this.loginService.isAdmin || this.loginService.isGAdmin || this.loginService.isGCargue;
    this.esPrestador = this.loginService.isPrestador;
  }

  /**
   * 📡 Suscripción SSE para refrescar la grilla en tiempo real al terminar Spring Batch
   */
  private iniciarSuscripcionSSE(): void {
    if (!this.usuarioActivo) return;

    this.sseSubscription = this.service.conectarSSE(this.usuarioActivo).subscribe({
      next: () => {
        this.calcularRangos(); // Refresca grilla en tiempo real sin mostrar popups
      },
      error: (err) => console.error('Error en conexión SSE:', err)
    });
  }

  cargarMovimientos(): void {
    this.movimientoService.listar().subscribe({
      next: (movs) => {
        this.opcionesMovimiento = (movs || [])
          .filter(m => !m.deletedAt)
          .map(m => ({
            value: m.id,
            label: m.codigo ? `${m.codigo} - ${m.descripcion}` : m.descripcion
          }));

        this.mapaMovimientos = (movs || []).reduce((acc: any, m) => {
          acc[m.id] = m.codigo ? `${m.codigo} - ${m.descripcion}` : m.descripcion;
          return acc;
        }, {});
      },
      error: (err) => console.error('Error al cargar movimientos:', err)
    });
  }

  /**
   * 🏷️ Asigna el texto descriptivo del estado y del movimiento
   */
  private procesarEstados(cargues: Cargue[]): any[] {
    return (cargues || []).map(c => {
      let estadoTxt = 'PROCESANDO';

      if (c.jobExecutionId) {
        estadoTxt = c.exiteError ? 'CON ERRORES' : 'CARGADO';
      }

      return {
        ...c,
        estadoNombre: estadoTxt,
        movimientoNombre: c.movimientoId ? (this.mapaMovimientos[c.movimientoId] || `Movimiento #${c.movimientoId}`) : 'Sin especificar'
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
    // Cargue estándar de soportes de otros prestadores o prestador directo
    if (this.fileInput) {
      this.fileInput.nativeElement.click();
    }
  }

  abrirCargueInterno(): void {
    // Cargue interno exclusivo con selección obligatoria de Tipo de Movimiento
    this.abrirModalCargueAdmin();
  }

  private abrirModalCargueAdmin(): void {
    this.movimientoService.listar().subscribe({
      next: (movs) => {
        this.opcionesMovimiento = (movs || [])
          .filter(m => !m.deletedAt)
          .map(m => ({
            value: m.id,
            label: m.codigo ? `${m.codigo} - ${m.descripcion}` : m.descripcion
          }));

        this.mapaMovimientos = (movs || []).reduce((acc: any, m) => {
          acc[m.id] = m.codigo ? `${m.codigo} - ${m.descripcion}` : m.descripcion;
          return acc;
        }, {});

        this.ejecutarModalCargue();
      },
      error: () => {
        this.ejecutarModalCargue();
      }
    });
  }

  private ejecutarModalCargue(): void {
    const dialogRef = this.dialog.open(ModalComponent, {
      width: '550px',
      data: {
        titulo: 'Cargar Archivo ZIP de Facturas',
        campos: [
          {
            name: 'movimientoId',
            label: 'Tipo de Movimiento * [Requerido]',
            type: 'select',
            options: this.opcionesMovimiento,
            required: true
          },
          {
            name: 'archivoZip',
            label: 'Archivo ZIP * [Requerido]',
            type: 'file',
            accept: '.zip',
            required: true,
            hint: 'Formatos admitidos: .ZIP'
          }
        ],
        formData: {},
        service: {
          crear: (model: any) => {
            let file: File | undefined = undefined;

            if (model.archivoZip instanceof File) {
              file = model.archivoZip;
            } else if (model.archivosSubidos?.['archivoZip'] instanceof File) {
              file = model.archivosSubidos['archivoZip'];
            } else {
              // Buscar específicamente en los inputs dentro del modal dialog
              const inputs = document.querySelectorAll('mat-dialog-container input[type="file"]') as NodeListOf<HTMLInputElement>;
              for (let i = 0; i < inputs.length; i++) {
                if (inputs[i].files && inputs[i].files!.length > 0) {
                  file = inputs[i].files![0];
                  break;
                }
              }
              if (!file) {
                const allInputs = document.querySelectorAll('input[type="file"]') as NodeListOf<HTMLInputElement>;
                for (let i = 0; i < allInputs.length; i++) {
                  if (allInputs[i].files && allInputs[i].files!.length > 0) {
                    file = allInputs[i].files![0];
                    break;
                  }
                }
              }
            }

            if (!model.movimientoId) {
              this.alertService.advertencia('Debe seleccionar obligatoriamente el Tipo de Movimiento.', 'Campo Requerido');
              return throwError(() => new Error('Debe seleccionar el Tipo de Movimiento.'));
            }

            if (!file) {
              this.alertService.advertencia('Debe seleccionar un archivo comprimido .ZIP.', 'Archivo Requerido');
              return throwError(() => new Error('Debe seleccionar un archivo ZIP.'));
            }

            if (!file.name.toLowerCase().endsWith('.zip')) {
              this.alertService.advertencia('Formato inválido. El archivo debe ser extensión .zip', 'Formato no permitido');
              return throwError(() => new Error('Formato inválido. El archivo debe tener extensión .zip'));
            }

            const usuarioEnvio = this.loginService.getUserName();
            const rolesEnvio = this.loginService.getUserRoles() || [];

            this.alertService.cargando('Subiendo archivo ZIP y procesando facturas...', 'Cargue Masivo');

            return this.service.cargarZip(file, usuarioEnvio, rolesEnvio, Number(model.movimientoId));
          }
        }
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.alertService.exito('Cargue masivo iniciado correctamente.', 'Proceso en Marcha');
        this.calcularRangos();
        setTimeout(() => this.calcularRangos(), 1500);
        setTimeout(() => this.calcularRangos(), 3500);
      }
    });
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
          const rolesEnvio = this.loginService.getUserRoles() || [];
          this.alertService.cargando('Subiendo archivo ZIP...', 'Procesando archivo');

          this.service.cargarZip(file, usuarioEnvio, rolesEnvio).subscribe({
            next: () => {
              this.alertService.cerrar(); // Cierra el modal de cargando
              this.calcularRangos();
              // Reintentos automáticos de refresco como respaldo
              setTimeout(() => this.calcularRangos(), 1500);
              setTimeout(() => this.calcularRangos(), 3500);
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

  /**
   * 🗑️ Elimina lógicamente el registro del cargue y sus dependencias en el Backend
   */
  deletedAt(row: Cargue): void {
    const detalleTxt = row.exiteError ? 'con errores' : 'exitoso y sus facturas/documentos asociados';

    this.alertService.confirmar(
      `¿Desea eliminar lógicamente el historial del cargue #${row.id} (${detalleTxt})?`,
      '¿Eliminar Historial?',
      'Sí, eliminar'
    ).then((result) => {
      if (result.isConfirmed) {

        this.alertService.cargando('Eliminando registro...', 'Un momento');

        this.service.deletedAt(row.id).subscribe({
          next: () => {
            this.alertService.exito('El historial del cargue y sus dependencias han sido removidos.', 'Registro Eliminado');
            this.calcularRangos();
          },
          error: (err) => {
            console.error('Error al eliminar el cargue:', err);
            this.alertService.error('No se pudo eliminar el registro seleccionado.');
          }
        });

      }
    });
  }
}
