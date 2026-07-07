import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Fase } from '../models/fase';
import { BE } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class TipoService extends CommonService<Fase>{

  protected override endPointBase = BE + '/api/v1/admin/tipo';

  constructor(http: HttpClient) {
      super(http);
    }
}
