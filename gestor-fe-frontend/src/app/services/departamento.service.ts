import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Departamento } from '../models/departamento';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class DepartamentoService extends CommonService<Departamento> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/departamento';

  constructor(http: HttpClient) {
    super(http);
  }
}
