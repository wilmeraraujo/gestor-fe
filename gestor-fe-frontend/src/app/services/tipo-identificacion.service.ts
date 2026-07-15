import { Injectable } from '@angular/core';
import { BEADMIN } from '../config/app';
import { CommonService } from './common.service';
import { TipoIdentificacion } from '../models/tipo-identificacion';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class TipoIdentificacionService extends CommonService<TipoIdentificacion>{

  protected override endPointBase = BEADMIN + '/api/v1/admin/tipo-identificacion';

  constructor(http: HttpClient) {
      super(http);
    }
}
