import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { Extension } from '../models/extension';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ExtensionService extends CommonService<Extension>{

  protected override endPointBase = BEADMIN + '/api/v1/admin/extension';

  constructor(http: HttpClient) {
      super(http);
    }
}
