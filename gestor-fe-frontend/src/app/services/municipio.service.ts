import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Municipio } from '../models/municipio';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class MunicipioService extends CommonService<Municipio> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/municipio';

  constructor(http: HttpClient) {
    super(http);
  }
}
