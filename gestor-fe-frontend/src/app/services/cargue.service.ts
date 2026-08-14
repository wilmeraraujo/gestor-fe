import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Cargue } from '../models/cargue';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BECORE } from '../config/app';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CargueService extends CommonService<Cargue> {

  protected override endPointBase = BECORE + '/api/v1/cargue';
  
  constructor(http: HttpClient) {
    super(http);
  }

  /**
   * 🎯 Paginable para activos enviando usuario y sus roles para discriminación
   */
  public getPaginableActivosConRoles(page: string, size: string, usuario: string, roles: string[]): Observable<any> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('usuario', usuario);

    // Adjuntar roles como lista de parámetros
    if (roles && roles.length > 0) {
      roles.forEach(rol => {
        params = params.append('roles', rol);
      });
    }

    return this.http.get<any>(`${this.endPointBase}/paginable/activos`, { params });
  }

  public cargarZip(file: File, usuario: string): Observable<Cargue> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('usuario', usuario);
    const url = `${this.endPointBase}/procesar-zip`;
    return this.http.post<Cargue>(url, formData);
  }

  public descargarExcelErrores(cargueId: number): Observable<Blob> {
    const url = `${this.endPointBase}/error-cargue/${cargueId}`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Crea un canal SSE con el servidor para recibir notificaciones en tiempo real
   */
  public conectarSSE(usuario: string): Observable<any> {
    return new Observable(observer => {
      const eventSource = new EventSource(`${this.endPointBase}/sse/subscribir/${usuario}`);

      eventSource.addEventListener('FIN_CARGUE', (event: any) => {
        const data = JSON.parse(event.data);
        observer.next(data);
      });

      eventSource.onerror = (error) => {
        console.warn('⚠️ Conexión SSE interrumpida. Reconectando...', error);
      };

      return () => {
        eventSource.close();
      };
    });
  }

}