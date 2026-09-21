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
   * Realiza la búsqueda paginada utilizando los filtros avanzados combinados y filtros por columna
   */
  public filtrarDocumentosPaginado(
    numeroFactura: string, 
    nit: string, 
    tipoId: number | null, 
    extensionId: number | null, 
    page: string | number, 
    size: string | number,
    nombreOriginal?: string | null,
    id?: number | null
  ): Observable<any>;

  public filtrarDocumentosPaginado(
    numeroFactura: string, 
    nit: string, 
    tipoId: number | null, 
    page: string | number, 
    size: string | number
  ): Observable<any>;

  public filtrarDocumentosPaginado(
    numeroFactura: string, 
    nit: string, 
    arg3: number | null, 
    arg4?: any, 
    arg5?: any, 
    arg6?: any,
    arg7?: any,
    arg8?: any
  ): Observable<any> {
    let finalTipoId: number | null = null;
    let finalExtensionId: number | null = null;
    let page: string | number = '0';
    let size: string | number = '10';
    let nombreOriginal: string | null = null;
    let id: number | null = null;

    if (arg6 !== undefined) {
      // 6-8 Argumentos: (numeroFactura, nit, tipoId, extensionId, page, size, nombreOriginal, id)
      finalTipoId = arg3;
      finalExtensionId = (typeof arg4 === 'number') ? arg4 : null;
      page = arg5 !== undefined ? arg5 : '0';
      size = arg6 !== undefined ? arg6 : '10';
      nombreOriginal = arg7 || null;
      id = (typeof arg8 === 'number') ? arg8 : null;
    } else {
      // 5 Argumentos: (numeroFactura, nit, tipoId, page, size)
      finalTipoId = arg3;
      page = arg4 !== undefined ? arg4 : '0';
      size = arg5 !== undefined ? arg5 : '10';
    }

    // 1. Inicializamos los parámetros obligatorios de paginación
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    // 2. Agregamos los parámetros condicionales
    if (numeroFactura && numeroFactura.trim() !== '') {
      params = params.set('numeroFactura', numeroFactura.trim());
    }
    
    if (nit && nit.trim() !== '') {
      params = params.set('nit', nit.trim());
    }
    
    if (finalTipoId !== null && finalTipoId !== undefined && finalTipoId > 0) {
      params = params.set('tipoId', finalTipoId.toString());
    }

    if (finalExtensionId !== null && finalExtensionId !== undefined && finalExtensionId > 0) {
      params = params.set('extensionId', finalExtensionId.toString());
    }

    if (nombreOriginal && nombreOriginal.trim() !== '') {
      params = params.set('nombreOriginal', nombreOriginal.trim());
    }

    if (id !== null && id !== undefined && id > 0) {
      params = params.set('id', id.toString());
    }

    // 3. Realizamos la petición GET enviando los parámetros corregidos
    return this.http.get<any>(`${this.endPointBase}/paginable/buscar`, { params });
  }

  /**
   * Inactiva (borrado lógico: deleted_at = NOW()) un soporte documental de forma segura
   */
  public inactivarDocumento(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endPointBase}/${id}`);
  }

  /**
   * Obtiene la lista de todos los documentos activos (deleted_at IS NULL) asociados a una factura
   */
  public getSoportesActivosFactura(facturaId: number): Observable<Documento[]> {
    return this.http.get<Documento[]>(`${this.endPointBase}/factura/${facturaId}/activos`);
  }
}