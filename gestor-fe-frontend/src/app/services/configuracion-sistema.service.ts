import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommonService } from './common.service';
import { ConfiguracionSistema } from '../models/configuracion-sistema';
import { BEADMIN } from '../config/app';

@Injectable({
  providedIn: 'root'
})
export class ConfiguracionSistemaService extends CommonService<ConfiguracionSistema> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/configuracion-sistema';

  constructor(http: HttpClient) {
    super(http);
  }

  public obtenerPorCodigo(codigo: string): Observable<ConfiguracionSistema> {
    return this.http.get<ConfiguracionSistema>(`${this.endPointBase}/codigo/${codigo}`);
  }
}
