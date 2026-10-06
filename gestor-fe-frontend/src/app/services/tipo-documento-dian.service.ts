import { Injectable } from '@angular/core';
import { CommonService } from './common.service';
import { TipoDocumentoDian } from '../models/tipo-documento-dian';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class TipoDocumentoDianService extends CommonService<TipoDocumentoDian> {

  protected override endPointBase = BEADMIN + '/api/v1/admin/tipo-documento-dian';

  constructor(http: HttpClient) {
    super(http);
  }
}
