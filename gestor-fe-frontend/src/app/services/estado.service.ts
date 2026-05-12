import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Estado } from '../models/estado';
import { HttpClient } from '@angular/common/http';
import { BE } from '../config/app';

@Injectable({
  providedIn: 'root'
})
export class EstadoService extends CommonService<Estado>{

  protected override endPointBase = BE + '/api/v1/admin/estado';

  constructor(http: HttpClient) {
      super(http);
    }
}
