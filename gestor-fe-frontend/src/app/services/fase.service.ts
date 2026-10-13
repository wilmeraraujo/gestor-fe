import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Fase } from '../models/fase';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class FaseService extends CommonService<Fase>{

  protected override endPointBase = BEADMIN + '/api/v1/admin/fase';

  constructor(http: HttpClient) {
      super(http);
    }
}
