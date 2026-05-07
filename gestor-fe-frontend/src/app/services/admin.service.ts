import { Injectable } from '@angular/core';
import { Admin } from '../models/admin';
import { CommonService } from './common.service';
import { BE } from '../config/app';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService extends CommonService<Admin>{

  protected override endPointBase = BE + '/api/gestion-fe/admin';

  constructor(http: HttpClient) {
    super(http);
  }

  public listarCategoriasActivos(): Observable<Admin[]> {
    return this.http.get<Admin[]>(`${this.endPointBase}/activos`);
  }
}
