import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { ResponsabilidadFiscal } from '../models/responsabilidad-fiscal';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ResponsabilidadFiscalService extends CommonService<ResponsabilidadFiscal> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/responsabilidad-fiscal';

  constructor(http: HttpClient) {
    super(http);
  }
}
