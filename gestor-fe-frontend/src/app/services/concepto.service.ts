import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Concepto } from '../models/concepto';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ConceptoService extends CommonService<Concepto> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/concepto';

  constructor(http: HttpClient) {
    super(http);
  }

  /**
   * 📋 Obtiene exclusivamente los conceptos activos (deleted_at IS NULL)
   */
  public listarActivos(): Observable<Concepto[]> {
    return this.http.get<Concepto[]>(`${this.endPointBase}/activos`);
  }
}

