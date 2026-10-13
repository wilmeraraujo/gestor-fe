import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin, Observable } from 'rxjs';
import Swal from 'sweetalert2';
import { ConfiguracionFaseExtensionService } from '../../../../services/configuracion-fase-extension.service';
import { ConfiguracionFaseExtension } from '../../../../models/configuracion-fase-extension';
import { LoginService } from '../../../../services/login.service';

export interface ExtensionConfigItem {
  extensionId: number;
  extensionNombre: string;
  tamanoMaximoMb: number;
  unidad: 'MB' | 'KB';
  tamanoInput: number;
  obligatorio: boolean;
  permiteMultiple: boolean;
  descripcion: string;
}

@Component({
  selector: 'app-modal-fase-extension',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatIconModule
  ],
  templateUrl: './modal-fase-extension.component.html',
  styleUrl: './modal-fase-extension.component.css'
})
export class ModalFaseExtensionComponent implements OnInit {

  isEdit: boolean = false;
  faseId: number | null = null;
  fasesOptions: { value: number, label: string }[] = [];
  extensionesOptions: { value: number, label: string }[] = [];
  configuracionesExistentes: ConfiguracionFaseExtension[] = [];

  // Dropdown de selección múltiple
  dropdownAbierto: boolean = false;
  filtroExtension: string = '';

  // Lista dinámica de extensiones seleccionadas con sus configuraciones
  extensionesSeleccionadas: ExtensionConfigItem[] = [];

  // Tamaño global para aplicar a todas
  tamanoGlobal: number = 10;
  unidadGlobal: 'MB' | 'KB' = 'MB';

  // Datos para modo edición individual
  itemEdicion: any = {
    id: null,
    faseId: null,
    extensionId: null,
    tamanoMaximoMb: 10,
    obligatorio: false,
    permiteMultiple: true,
    descripcion: ''
  };

  guardando: boolean = false;

  constructor(
    public dialogRef: MatDialogRef<ModalFaseExtensionComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private service: ConfiguracionFaseExtensionService,
    private loginService: LoginService
  ) {}

  ngOnInit(): void {
    this.fasesOptions = this.data.fasesOptions || [];
    this.extensionesOptions = this.data.extensionesOptions || [];
    this.configuracionesExistentes = this.data.configuracionesExistentes || [];
    this.isEdit = !!this.data.isEdit;

    if (this.isEdit && this.data.item) {
      const it = this.data.item;
      this.itemEdicion = {
        id: it.id,
        faseId: it.faseId,
        extensionId: it.extensionId,
        tamanoMaximoMb: it.tamanoMaximoMb || 10,
        obligatorio: it.obligatorio || false,
        permiteMultiple: it.permiteMultiple !== undefined ? it.permiteMultiple : true,
        descripcion: it.descripcion || '',
        deletedAt: it.deletedAt
      };
      this.faseId = it.faseId;
    } else {
      // Modo creación: si hay fases, seleccionar la primera por defecto
      if (this.fasesOptions.length > 0) {
        this.faseId = this.fasesOptions[0].value;
      }
    }
  }

  // Comprueba si una extensión ya está configurada activamente en la fase seleccionada
  isExtensionYaAsignada(extId: number): boolean {
    if (!this.faseId) return false;
    return this.configuracionesExistentes.some(
      c => Number(c.faseId) === Number(this.faseId) &&
           Number(c.extensionId) === Number(extId) &&
           !c.deletedAt &&
           (!this.isEdit || c.id !== this.itemEdicion.id)
    );
  }

  // Lista de extensiones filtradas por búsqueda
  get extensionesFiltradas(): { value: number, label: string }[] {
    let list = this.extensionesOptions;
    if (this.filtroExtension && this.filtroExtension.trim() !== '') {
      const q = this.filtroExtension.toLowerCase().trim();
      list = list.filter(e => e.label.toLowerCase().includes(q));
    }
    return list;
  }

  // Verifica si una extensión está seleccionada en la lista dinámica
  isExtensionSelected(extId: number): boolean {
    return this.extensionesSeleccionadas.some(e => e.extensionId === extId);
  }

  // Toggle de selección de una extensión
  toggleExtension(ext: { value: number, label: string }): void {
    if (this.isExtensionYaAsignada(ext.value)) return;

    const index = this.extensionesSeleccionadas.findIndex(e => e.extensionId === ext.value);
    if (index >= 0) {
      this.extensionesSeleccionadas.splice(index, 1);
    } else {
      this.extensionesSeleccionadas.push({
        extensionId: ext.value,
        extensionNombre: ext.label,
        tamanoMaximoMb: this.tamanoGlobal,
        unidad: this.unidadGlobal,
        tamanoInput: this.tamanoGlobal,
        obligatorio: false,
        permiteMultiple: true,
        descripcion: ''
      });
    }
  }

  // Seleccionar todas las extensiones disponibles
  seleccionarTodas(): void {
    const disponibles = this.extensionesOptions.filter(e => !this.isExtensionYaAsignada(e.value));
    disponibles.forEach(ext => {
      if (!this.isExtensionSelected(ext.value)) {
        this.extensionesSeleccionadas.push({
          extensionId: ext.value,
          extensionNombre: ext.label,
          tamanoMaximoMb: this.tamanoGlobal,
          unidad: this.unidadGlobal,
          tamanoInput: this.tamanoGlobal,
          obligatorio: false,
          permiteMultiple: true,
          descripcion: ''
        });
      }
    });
  }

  // Limpiar todas las seleccionadas
  limpiarSeleccion(): void {
    this.extensionesSeleccionadas = [];
  }

  // Eliminar una extensión específica de la lista
  eliminarExtension(index: number): void {
    this.extensionesSeleccionadas.splice(index, 1);
  }

  // Aplicar tamaño global a todas las extensiones seleccionadas
  aplicarTamanoGlobal(): void {
    if (!this.tamanoGlobal || this.tamanoGlobal <= 0) {
      Swal.fire({
        icon: 'warning',
        title: 'Tamaño inválido',
        text: 'Ingrese un tamaño mayor a 0 para aplicar a todas las extensiones.',
        confirmButtonColor: '#1DA6BA'
      });
      return;
    }

    this.extensionesSeleccionadas.forEach(ext => {
      ext.unidad = this.unidadGlobal;
      ext.tamanoInput = this.tamanoGlobal;
      ext.tamanoMaximoMb = this.calcularMb(this.tamanoGlobal, this.unidadGlobal);
    });
  }

  // Recalcular tamaño en MB al cambiar input o unidad
  onTamanoChange(item: ExtensionConfigItem): void {
    item.tamanoMaximoMb = this.calcularMb(item.tamanoInput, item.unidad);
  }

  private calcularMb(valor: number, unidad: 'MB' | 'KB'): number {
    if (!valor || valor <= 0) return 0;
    if (unidad === 'KB') {
      // Convertir KB a MB (mínimo 1 MB para almacenamiento entero en backend)
      return Math.max(1, Math.round(valor / 1024));
    }
    return Math.round(valor);
  }

  // Texto resumen para el input de selección múltiple
  get textoExtensionesSeleccionadas(): string {
    if (this.extensionesSeleccionadas.length === 0) {
      return 'Seleccione';
    }
    return this.extensionesSeleccionadas.map(e => e.extensionNombre).join(', ');
  }

  // Al cambiar de fase en creación, limpiar extensiones que ya estén asignadas a la nueva fase
  onFaseChange(): void {
    if (!this.faseId) return;
    this.extensionesSeleccionadas = this.extensionesSeleccionadas.filter(
      ext => !this.isExtensionYaAsignada(ext.extensionId)
    );
  }

  // Validación y guardado
  guardar(): void {
    if (!this.faseId) {
      Swal.fire({
        icon: 'warning',
        title: 'Fase Requerida',
        text: 'Por favor seleccione una fase.',
        confirmButtonColor: '#1DA6BA'
      });
      return;
    }

    const usuarioActivo = this.loginService.getUserName();

    if (this.isEdit) {
      // Modo Edición individual
      if (!this.itemEdicion.tamanoMaximoMb || this.itemEdicion.tamanoMaximoMb <= 0) {
        Swal.fire({
          icon: 'warning',
          title: 'Tamaño Inválido',
          text: 'El tamaño máximo permitido debe ser mayor a 0 MB.',
          confirmButtonColor: '#1DA6BA'
        });
        return;
      }

      this.guardando = true;
      const entidad: ConfiguracionFaseExtension = {
        ...this.itemEdicion,
        faseId: Number(this.faseId),
        extensionId: Number(this.itemEdicion.extensionId),
        tamanoMaximoMb: Number(this.itemEdicion.tamanoMaximoMb),
        codigo: `${this.faseId}-${this.itemEdicion.extensionId}`,
        deletedAt: this.itemEdicion.deletedAt
      };

      this.service.editar(entidad, usuarioActivo).subscribe({
        next: () => {
          this.guardando = false;
          Swal.fire({
            icon: 'success',
            title: 'Actualizado',
            text: 'Regla de extensión actualizada correctamente.',
            confirmButtonColor: '#1DA6BA',
            timer: 2000,
            showConfirmButton: false
          });
          this.dialogRef.close(true);
        },
        error: (err) => {
          this.guardando = false;
          console.error(err);
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: err.error?.mensaje || err.error?.error || 'No se pudo actualizar la configuración.',
            confirmButtonColor: '#1DA6BA'
          });
        }
      });

    } else {
      // Modo Creación Múltiple
      if (this.extensionesSeleccionadas.length === 0) {
        Swal.fire({
          icon: 'warning',
          title: 'Extensiones Requeridas',
          text: 'Debe seleccionar al menos una extensión permitida.',
          confirmButtonColor: '#1DA6BA'
        });
        return;
      }

      // Validar que todos los tamaños sean > 0
      const invalidas = this.extensionesSeleccionadas.filter(e => !e.tamanoInput || e.tamanoInput <= 0);
      if (invalidas.length > 0) {
        Swal.fire({
          icon: 'warning',
          title: 'Tamaños Inválidos',
          text: `Debe ingresar un tamaño válido (> 0) para todas las extensiones seleccionadas (${invalidas.map(i => i.extensionNombre).join(', ')}).`,
          confirmButtonColor: '#1DA6BA'
        });
        return;
      }

      this.guardando = true;

      // Crear las peticiones concurrentes para cada extensión seleccionada
      const requests: Observable<ConfiguracionFaseExtension>[] = this.extensionesSeleccionadas.map(ext => {
        const payload: ConfiguracionFaseExtension = {
          id: undefined as any,
          codigo: `${this.faseId}-${ext.extensionId}`,
          descripcion: ext.descripcion || `Extensión ${ext.extensionNombre} para fase ${this.faseId}`,
          faseId: Number(this.faseId),
          extensionId: Number(ext.extensionId),
          tamanoMaximoMb: Number(ext.tamanoMaximoMb),
          obligatorio: !!ext.obligatorio,
          permiteMultiple: ext.permiteMultiple !== undefined ? !!ext.permiteMultiple : true,
          createdAt: undefined as any,
          updatedAt: undefined as any,
          deletedAt: undefined as any
        };
        return this.service.crear(payload, usuarioActivo);
      });

      forkJoin(requests).subscribe({
        next: (results) => {
          this.guardando = false;
          Swal.fire({
            icon: 'success',
            title: 'Configuración Exitosa',
            text: `Se han configurado correctamente ${results.length} extensiones para la fase.`,
            confirmButtonColor: '#1DA6BA',
            timer: 2200,
            showConfirmButton: false
          });
          this.dialogRef.close(true);
        },
        error: (err) => {
          this.guardando = false;
          console.error(err);
          Swal.fire({
            icon: 'error',
            title: 'Error en Creación',
            text: err.error?.mensaje || err.error?.error || 'Ocurrió un error al registrar las extensiones para la fase.',
            confirmButtonColor: '#1DA6BA'
          });
        }
      });
    }
  }

  cerrar(): void {
    this.dialogRef.close(false);
  }
}
