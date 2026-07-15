import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Cargue } from '../models/cargue';
import { HttpClient } from '@angular/common/http';
import { BECORE } from '../config/app';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CargueService extends CommonService<Cargue>{

  protected override endPointBase = BECORE + '/api/v1/cargue';
  
  constructor(http: HttpClient) {
    super(http);
  }

  /**
   * Envía el archivo ZIP y el nombre de usuario al servidor para iniciar el Batch asíncrono
   * @param file Archivo ZIP seleccionado por el usuario
   * @param usuario Nombre o identificador del usuario que realiza el cargue
   */
  public cargarZip(file: File, usuario: string): Observable<Cargue> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('usuario', usuario);
    const url = `${this.endPointBase}/procesar-zip`;
    return this.http.post<Cargue>(url, formData);
  }

  /**
   * Descarga el archivo Excel de errores personalizado asociado al ID del cargue
   * @param cargueId ID de la tabla gestor.cargue
   */
  public descargarExcelErrores(cargueId: number): Observable<Blob> {
    const url = `${this.endPointBase}/error-cargue/${cargueId}`;
    return this.http.get(url, { responseType: 'blob' });
  }

}
