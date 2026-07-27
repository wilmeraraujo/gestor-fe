import { Injectable } from '@angular/core';
import Swal, { SweetAlertResult } from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  private colorTema = '#1DA6BA';

  constructor() { }

  // 🟢 Alerta de éxito
  exito(mensaje: string, titulo: string = '¡Éxito!'): void {
    Swal.fire({
      icon: 'success',
      title: titulo,
      text: mensaje,
      confirmButtonColor: this.colorTema,
      timer: 3000,
      timerProgressBar: true
    });
  }

  // 🔴 Alerta de error
  error(mensaje: string, titulo: string = 'Error'): void {
    Swal.fire({
      icon: 'error',
      title: titulo,
      text: mensaje,
      confirmButtonColor: this.colorTema
    });
  }

  // 🟡 Alerta de advertencia
  advertencia(mensaje: string, titulo: string = 'Advertencia'): void {
    Swal.fire({
      icon: 'warning',
      title: titulo,
      text: mensaje,
      confirmButtonColor: this.colorTema
    });
  }

  // ℹ️ Alerta informativa
  info(mensaje: string, titulo: string = 'Información'): void {
    Swal.fire({
      icon: 'info',
      title: titulo,
      text: mensaje,
      confirmButtonColor: this.colorTema
    });
  }

  // 🔄 Alerta de Carga (Loading modal)
  cargando(mensaje: string = 'Procesando...', titulo: string = 'Espere un momento'): void {
    Swal.fire({
      title: titulo,
      text: mensaje,
      allowOutsideClick: false,
      didOpen: () => {
        Swal.showLoading();
      }
    });
  }

  // ⛔ Cerrar alerta (útil para cerrar el loading)
  cerrar(): void {
    Swal.close();
  }

  // ❓ Modal de Confirmación
  confirmar(
    mensaje: string,
    titulo: string = '¿Está seguro?',
    textoBotonConfirmar: string = 'Sí, continuar'
  ): Promise<SweetAlertResult> {
    return Swal.fire({
      title: titulo,
      text: mensaje,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: this.colorTema,
      cancelButtonColor: '#d33',
      confirmButtonText: textoBotonConfirmar,
      cancelButtonText: 'Cancelar'
    });
  }
}
