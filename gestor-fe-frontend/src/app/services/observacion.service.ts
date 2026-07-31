import { Injectable } from '@angular/core';
import { Observacion } from '../models/observacion';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';
import { CommonService } from './common.service';

@Injectable({
  providedIn: 'root'
})
export class ObservacionService extends CommonService<Observacion>{

  protected override endPointBase = BEADMIN + '/api/v1/admin/observacion';

  constructor(http: HttpClient) {
      super(http);
    }
}
