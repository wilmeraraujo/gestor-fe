import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { TipoOperacion } from '../models/tipo-operacion';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class TipoOperacionService extends CommonService<TipoOperacion> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/tipo-operacion';

  constructor(http: HttpClient) {
    super(http);
  }
}
