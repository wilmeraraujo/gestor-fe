import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { CausalDevolucion } from '../models/causal-devolucion';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class CausalDevolucionService extends CommonService<CausalDevolucion>{

  protected override endPointBase = BEADMIN + '/api/v1/admin/causal-devolucion';

  constructor(http: HttpClient) {
      super(http);
    }
}
