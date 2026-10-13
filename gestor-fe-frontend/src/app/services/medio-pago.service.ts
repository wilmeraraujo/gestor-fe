import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { MedioPago } from '../models/medio-pago';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class MedioPagoService extends CommonService<MedioPago> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/medio-pago';

  constructor(http: HttpClient) {
    super(http);
  }
}
