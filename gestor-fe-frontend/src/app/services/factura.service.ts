import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CommonService } from './common.service';
import { Factura } from '../models/factura';
import { BECORE } from '../config/app';

@Injectable({
  providedIn: 'root'
})
export class FacturaService extends CommonService<Factura> {

  protected override endPointBase = BECORE + '/api/v1/factura';

  constructor(http: HttpClient) {
    super(http);
  }

  /**
   * 📋 1. Consulta para el PRESTADOR por su NIT
   */
  public getByNit(nit: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.endPointBase}/prestador/${nit}`, { params });
  }

  /**
   * 📋 2. Consulta para FASE 1 (Gestión)
   */
  public getFase1(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.endPointBase}/fase/1`, { params });
  }

  /**
   * 📋 3. Consulta para FASES ACTIVAS (Fases 2, 3 y 4)
   */
  public getFaseActiva(faseId: number, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.endPointBase}/fase/${faseId}`, { params });
  }

  /**
   * 📋 4. Consulta para FASE 5 (Seguimiento de Facturas)
   */
  public getSeguimiento(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.endPointBase}/seguimiento`, { params });
  }

  /**
   * ⚙️ 5. MÉTODO UNIFICADO DE TRANSICIÓN DE FASE (JSON / DTO)
   */
  public procesarTransicionFase(id: number, faseId: number, dto: any): Observable<Factura> {
    return this.http.put<Factura>(
      `${this.endPointBase}/${id}/procesar-fase/${faseId}`,
      dto,
      { headers: this.cabeceras }
    );
  }

  /**
   * 🏦 6. MÉTODO DE CAUSACIÓN MULTIPART (FASE 2)
   */
  public procesarCausacionFase2(
    id: number,
    tipoRegistroContableId: number, // 👈 Se envía como ID numérico
    numeroCausacion: string,
    archivo?: any 
  ): Observable<Factura> {
    const formData = new FormData();
    formData.append('tipoRegistroContableId', tipoRegistroContableId ? tipoRegistroContableId.toString() : '');
    formData.append('numeroCausacion', numeroCausacion || '');

    if (archivo && (archivo instanceof File || archivo instanceof Blob)) {
      const nombreArchivo = (archivo as File).name || 'soporte_causacion.pdf';
      formData.append('archivo', archivo, nombreArchivo);
    }

    return this.http.post<Factura>(`${this.endPointBase}/${id}/causacion`, formData);
  }

  /**
   * 💸 7. MÉTODO DE PAGO MULTIPART (FASE 4 - TESORERÍA)
   */
  public procesarPagoFase4(
    id: number,
    tipoRegistroContableId?: number, // 👈 Opcional ID numérico
    numeroCausacion?: string,
    soporteTb?: any,
    comprobantePago?: any
  ): Observable<Factura> {
    const formData = new FormData();
    formData.append('tipoRegistroContableId', tipoRegistroContableId ? tipoRegistroContableId.toString() : '');
    formData.append('numeroCausacion', numeroCausacion || '');

    if (soporteTb && (soporteTb instanceof File || soporteTb instanceof Blob)) {
      formData.append('soporteTb', soporteTb, (soporteTb as File).name || 'documento_tb.pdf');
    }

    if (comprobantePago && (comprobantePago instanceof File || comprobantePago instanceof Blob)) {
      formData.append('comprobantePago', comprobantePago, (comprobantePago as File).name || 'comprobante_pago.pdf');
    }

    return this.http.post<Factura>(`${this.endPointBase}/${id}/pago`, formData);
  }
}