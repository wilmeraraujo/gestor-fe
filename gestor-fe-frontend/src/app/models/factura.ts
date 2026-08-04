import { Documento } from './documento'; // O ajusta la ruta a tu modelo de Documento
import { Gestion } from './gestion';

export interface Factura {
  id: number;
  nit: string;
  numeroFactura: string;
  cufe: string;
  identificadorCargue: number;
  linea: number;
  razonSocialEmisor?: string;
  valorTotal?: number; // Representa BigDecimal en Java
  fechaEmision?: Date | string; // Representa LocalDate en Java
  
  // 📌 Estado actual (punteros de rápida lectura)
  estado?: string;
  faseId: number;
  observacion?: string;
  causalDevolucionId?: number;
  tipoRegistroContableId?: number; // 👈 Mapeado como ID numérico (FC, GV, ORC, NI, TB)
  numeroCausacion?: string;
  
  createdAt: Date | string;
  deletedAt?: Date | string | null;
  
  // 🔗 Relaciones Bidireccionales
  documentos?: Documento[] | any[]; // Relación OneToMany de soportes
  gestiones?: Gestion[];            // 👈 Historial de trazabilidad (Gestión OneToMany)
}