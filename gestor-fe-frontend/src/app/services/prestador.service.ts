import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommonService } from './common.service';
import { Documento } from '../models/documento';
import { BECORE } from '../config/app';
import { Prestador } from '../models/prestador';

@Injectable({
  providedIn: 'root'
})
export class PrestadorService extends CommonService<Prestador> {

  protected override endPointBase = BECORE + '/api/v1/prestadores';

  constructor(http: HttpClient) {
    super(http);
  }

  public obtenerPorNit(nit: string): Observable<Prestador> {
    return this.http.get<Prestador>(`${this.endPointBase}/nit/${nit}`);
  }

  public cargarSoporte(nitPrestador: string, tipoId: number, extensionId: number, archivo: File): Observable<Documento> {
    const formData = new FormData();
    formData.append('nitPrestador', nitPrestador);
    formData.append('tipoId', tipoId.toString());
    formData.append('extensionId', extensionId.toString());
    formData.append('archivo', archivo);

    return this.http.post<Documento>(`${this.endPointBase}/soportes/cargar`, formData);
  }

  public listarSoportes(prestadorId: number, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.endPointBase}/${prestadorId}/soportes`, { params });
  }

  public eliminarSoporte(documentoId: number): Observable<void> {
    return this.http.delete<void>(`${this.endPointBase}/soportes/${documentoId}`);
  }
}
