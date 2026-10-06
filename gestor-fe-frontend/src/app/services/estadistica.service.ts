import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ResumenEstadisticasResponse } from '../models/estadistica';
import { BECORE } from '../config/app';

@Injectable({
  providedIn: 'root'
})
export class EstadisticaService {

  private endPoint = BECORE + '/api/v1/estadisticas';

  constructor(private http: HttpClient) {}

  public getResumen(
    nit?: string | null,
    fechaInicio?: string | null,
    fechaFin?: string | null,
    usuario?: string | null,
    roles?: string[] | null
  ): Observable<ResumenEstadisticasResponse> {
    let params = new HttpParams();

    if (nit && nit.trim() !== '') {
      params = params.set('nit', nit.trim());
    }

    if (fechaInicio && fechaInicio.trim() !== '') {
      params = params.set('fechaInicio', fechaInicio.trim());
    }

    if (fechaFin && fechaFin.trim() !== '') {
      params = params.set('fechaFin', fechaFin.trim());
    }

    if (usuario && usuario.trim() !== '') {
      params = params.set('usuario', usuario.trim());
    }

    if (roles && roles.length > 0) {
      roles.forEach(r => {
        params = params.append('roles', r);
      });
    }

    return this.http.get<ResumenEstadisticasResponse>(`${this.endPoint}/resumen`, { params });
  }
}
