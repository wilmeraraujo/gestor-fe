import { Factura } from './factura';

export interface Gestion {
  id?: number;
  factura?: Factura;
  facturaId?: number;
  faseId: number;
  accion: 'APROBADO' | 'RECHAZADO' | string;
  estadoResultado: string; // Ej: 'EN GESTIÓN', 'CAUSADO', 'IMPUESTOS VERIFICADOS', 'PAGADO', 'ANULADO'
  tipoRegistroContableId?: number | null; // ID mapeado a la tabla maestra de tipos de registro
  numeroCausacion?: string | null;
  causalDevolucionId?: number | null;
  observacion?: string | null;
  usuario?: string | null;
  createdAt?: Date | string;
  deletedAt?: Date | string | null;
}