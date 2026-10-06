import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Movimiento } from '../models/movimiento';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class MovimientoService extends CommonService<Movimiento> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/movimiento';

  constructor(http: HttpClient) {
    super(http);
  }
}
