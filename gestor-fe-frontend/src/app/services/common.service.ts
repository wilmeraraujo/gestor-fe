import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Generic } from '../models/generic';

@Injectable({
  providedIn: 'root'
})
export class CommonService <E extends Generic>{

  protected endPointBase: string='';
  protected cabeceras: HttpHeaders = new HttpHeaders({'Content-Type': 'application/json'});

  constructor(protected http: HttpClient) { }

  public listar(): Observable<E[]> {
    return this.http.get<E[]>(this.endPointBase);
  }

  public listarPaginas(page: string, size: string): Observable<any>{
    const params = new HttpParams()
    .set('page',page)
    .set('size',size)
    .set('sort', 'id,asc');
    return this.http.get<any>(`${this.endPointBase}/paginable`, { params: params });
  }

  public ver(id: number | string): Observable<E>{
    return this.http.get<E>(`${this.endPointBase}/${id}`);
  }

  public crear(e: E): Observable<E> {
    return this.http.post<E>(this.endPointBase, e, { headers: this.cabeceras });
  }

  public create(e: E, userName: string): Observable<E> {
    const params = new HttpParams().set('userName', userName);
    return this.http.post<E>(`${this.endPointBase}/create`, e, { params: params, headers: this.cabeceras}
    );
  }

  public editar(e: E): Observable<E> {
    return this.http.put<E>(`${this.endPointBase}/${e.id}`,e, { headers: this.cabeceras });
  }

  public update(e: E, userName: string): Observable<E> {
    const params = new HttpParams().set('userName', userName);
    return this.http.put<E>(`${this.endPointBase}/update/${e.id}`, e, { params: params, headers: this.cabeceras}
    );
  }

  public eliminar(id: number | string): Observable<void>{
    return this.http.delete<void>(`${this.endPointBase}/${id}`);
  }
}
