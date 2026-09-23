import { Injectable } from '@angular/core';
import { Admin } from '../models/admin';
import { CommonService } from './common.service';
import { BEADMIN } from '../config/app';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService extends CommonService<Admin>{

  protected override endPointBase = BEADMIN + '/api/v1/gestion-fe/admin';

  constructor(http: HttpClient) {
    super(http);
  }
}
