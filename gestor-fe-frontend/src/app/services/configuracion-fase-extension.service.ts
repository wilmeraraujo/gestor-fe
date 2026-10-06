import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommonService } from './common.service';
import { ConfiguracionFaseExtension } from '../models/configuracion-fase-extension';
import { BEADMIN } from '../config/app';

@Injectable({
  providedIn: 'root'
})
export class ConfiguracionFaseExtensionService extends CommonService<ConfiguracionFaseExtension> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/configuracion-fase';

  constructor(http: HttpClient) {
    super(http);
  }

  public obtenerPorFase(faseId: number): Observable<ConfiguracionFaseExtension[]> {
    return this.http.get<ConfiguracionFaseExtension[]>(`${this.endPointBase}/fase/${faseId}`);
  }
}
