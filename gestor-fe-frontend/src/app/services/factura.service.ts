import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Factura } from '../models/factura';
import { BECORE } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class FacturaService extends CommonService<Factura>{

  protected override endPointBase = BECORE + '/api/v1/factura';

  constructor(http: HttpClient) {
      super(http);
    }
}
