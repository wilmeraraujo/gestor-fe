import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommonService } from './common.service';
import { Documento } from '../models/documento'; // O '../models/documento.model' según tu estructura
import { BECORE } from '../config/app';

@Injectable({
  providedIn: 'root'
})
export class DocumentoService extends CommonService<Documento> {

  protected override endPointBase = BECORE + '/api/v1/documento';

  constructor(http: HttpClient) {
    super(http);
  }

  /**
   * Obtiene el Blob (binario) de un documento específico para renderizarlo inline en el iframe
   */
  public getDocumentoBlob(id: number): Observable<Blob> {
    const url = `${this.endPointBase}/preview/${id}`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Envía una lista de IDs de documentos para generar y descargar un ZIP masivo
   */
  public descargarDocumentosMasivo(ids: number[]): Observable<Blob> {
    const url = `${this.endPointBase}/descarga-masiva`;
    return this.http.post(url, ids, { responseType: 'blob' });
  }

  /**
   * Realiza la búsqueda paginada utilizando los filtros avanzados combinados
   */
  public filtrarDocumentosPaginado(
    numeroFactura: string, 
    nit: string, 
    tipoId: number | null, 
    page: string | number, 
    size: string | number
  ): Observable<any> {
    // 1. Inicializamos los parámetros obligatorios de paginación
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    // 2. Agregamos los parámetros condicionales REASIGNANDO 'params' (HttpParams es inmutable)
    if (numeroFactura && numeroFactura.trim() !== '') {
      params = params.set('numeroFactura', numeroFactura.trim());
    }
    
    if (nit && nit.trim() !== '') {
      params = params.set('nit', nit.trim());
    }
    
    if (tipoId !== null && tipoId !== undefined && tipoId > 0) {
      params = params.set('tipoId', tipoId.toString());
    }

    // 3. Realizamos la petición GET enviando los parámetros corregidos
    return this.http.get<any>(`${this.endPointBase}/paginable/buscar`, { params });
  }
}