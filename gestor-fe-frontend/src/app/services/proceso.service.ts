import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Proceso } from '../models/proceso';
import { HttpClient } from '@angular/common/http';
import { BEADMIN } from '../config/app';

@Injectable({
  providedIn: 'root'
})
export class ProcesoService extends CommonService<Proceso> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/proceso';

  constructor(http: HttpClient) {
    super(http);
  }
}
