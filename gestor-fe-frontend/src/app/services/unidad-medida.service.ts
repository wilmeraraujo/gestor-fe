import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { UnidadMedida } from '../models/unidad-medida';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class UnidadMedidaService extends CommonService<UnidadMedida> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/unidad-medida';

  constructor(http: HttpClient) {
    super(http);
  }
}
