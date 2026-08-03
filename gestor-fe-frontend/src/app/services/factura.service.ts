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
   * Retorna todas las facturas cargadas por un prestador específico (fase 1 a 5).
   */
  public getByNit(nit: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.endPointBase}/prestador/${nit}`, { params });
  }

  /**
   * 📋 2. Consulta para FASE 1 (Gestión)
   * Retorna facturas en etapa de verificación inicial.
   */
  public getFase1(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.endPointBase}/fase/1`, { params });
  }

  /**
   * 📋 3. Consulta para FASES ACTIVAS (Fases 2, 3 y 4)
   * Retorna facturas vigentes para Reconocimiento Contable (2), Impuestos (3) o Tesorería (4).
   */
  public getFaseActiva(faseId: number, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.endPointBase}/fase/${faseId}`, { params });
  }

  /**
   * 📋 4. Consulta para FASE 5 (Seguimiento de Facturas)
   * Vista consolidada global de trazabilidad en tiempo real.
   */
  public getSeguimiento(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.endPointBase}/seguimiento`, { params });
  }

  /**
   * ⚙️ 5. MÉTODO UNIFICADO DE TRANSICIÓN DE FASE (JSON / Texto)
   * Procesa decisiones estándar (Aprobación simple o Rechazos).
   * 
   * @param id ID de la factura
   * @param faseId ID de la fase actual (1, 2, 3 o 4)
   * @param dto GestionDto con estadoAccion, observacion, causalDevolucionId, etc.
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
   * Procesa la aprobación en Reconocimiento Contable permitiendo adjuntar
   * opcionalmente el soporte PDF de causación.
   * 
   * @param id ID de la factura
   * @param tipoRegistro FC, GV, ORC, NI
   * @param numeroCausacion Número de documento de la causación
   * @param archivo Archivo PDF con el soporte de causación (Opcional)
   */
  public procesarCausacionFase2(
    id: number,
    tipoRegistro: string,
    numeroCausacion: string,
    archivo?: any 
  ): Observable<Factura> {
    const formData = new FormData();
    formData.append('tipoRegistroContable', tipoRegistro || '');
    formData.append('numeroCausacion', numeroCausacion || '');

    // Validamos que exista y sea un objeto Blob/File válido
    if (archivo && (archivo instanceof File || archivo instanceof Blob)) {
      const nombreArchivo = (archivo as File).name || 'soporte_causacion.pdf';
      formData.append('archivo', archivo, nombreArchivo);
    }

    return this.http.post<Factura>(`${this.endPointBase}/${id}/causacion`, formData);
  }

  // En factura.service.ts

  /**
   * 💸 7. MÉTODO DE PAGO MULTIPART (FASE 4 - TESORERÍA)
   * Registra el desembolso permitiendo adjuntar el documento TB y el comprobante bancario.
   */
  public procesarPagoFase4(
    id: number,
    numeroCausacion: string,
    soporteTb?: any,
    comprobantePago?: any
  ): Observable<Factura> {
    const formData = new FormData();
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