import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Generic } from '../models/generic';
import { Estado } from '../models/estado';

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

  public getPaginable(page: string, size: string): Observable<any>{
    const params = new HttpParams()
    .set('page',page)
    .set('size',size)
    .set('sort', 'id,asc');
    return this.http.get<any>(`${this.endPointBase}/paginable`, { params: params });
  }

  public getPaginableActivos(page: string, size: string): Observable<any>{
    const params = new HttpParams()
    .set('page',page)
    .set('size',size);
    return this.http.get<any>(`${this.endPointBase}/paginable/activos`, { params: params });
  }

  public getPaginableFiltrado(filtros: any, page: string, size: string): Observable<any> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (filtros) {
      Object.keys(filtros).forEach(key => {
        const val = filtros[key];
        if (val !== null && val !== undefined && val !== '') {
          params = params.set(key, String(val).trim());
        }
      });
    }

    return this.http.get<any>(`${this.endPointBase}/paginable/buscar`, { params });
  }

  public buscar(desc: string): Observable<E[]>{
    return this.http.get<E[]>(`${this.endPointBase}/buscar/${desc}`);
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

  public deletedAt(id: number): Observable<Estado> {
    const endpoint = `${this.endPointBase}/deleted-at/${id}`;
    return this.http.put<Estado>(endpoint, {});
  }

  public toggleEstado(id: number | string, observacion?: string, username?: string): Observable<E> {
    const body = {
      observacion: observacion || '',
      username: username || ''
    };
    return this.http.patch<E>(`${this.endPointBase}/${id}/toggle-estado`, body, { headers: this.cabeceras });
  }
}
