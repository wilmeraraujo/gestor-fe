import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Clasificacion } from '../models/clasificacion';
import { BE } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ClasificacionService extends CommonService<Clasificacion>{

  protected override endPointBase = BE + '/api/v1/admin/clasificacion';

  constructor(http: HttpClient) {
      super(http);
    }
}