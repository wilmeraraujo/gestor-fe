import { Documento } from './documento';
import { Gestion } from './gestion';
import { FacturaItem } from './factura-item';

export interface Factura {
  id: number;
  nit: string;
  dv?: string;
  numeroFactura: string;
  cufe: string;
  identificadorCargue: number;
  linea: number;
  razonSocialEmisor?: string;
  direccion?: string;
  codigoMunicipio?: string;
  codigoDepartamento?: string;
  codigoPais?: string;
  
  valorSubtotal?: number;
  valorIva?: number;
  valorTotal?: number;
  fechaEmision?: Date | string;

  // 👤 Datos del Cliente / Adquirente
  nitCliente?: string;
  dvCliente?: string;
  razonSocialCliente?: string;
  direccionCliente?: string;
  ciudadCliente?: string;
  departamentoCliente?: string;
  telefonoCliente?: string;
  emailCliente?: string;

  // 💰 Condiciones Comerciales y Retenciones
  fechaVencimiento?: Date | string;
  formaPago?: string; // 1 = Contado, 2 = Crédito
  medioPago?: string;
  notas?: string;
  valorRetefuente?: number;
  valorReteica?: number;
  valorReteiva?: number;
  totalRetenciones?: number;
  
  // 📌 Estado actual (punteros de rápida lectura)
  estado?: string;
  faseId: number;
  observacion?: string;
  causalDevolucionId?: number;
  tipoRegistroContableId?: number; // 👈 Mapeado como ID numérico (FC, GV, ORC, NI, TB)
  movimientoId?: number | null;
  numeroCausacion?: string;
  
  createdAt: Date | string;
  deletedAt?: Date | string | null;
  
  // 🔗 Relaciones Bidireccionales
  documentos?: Documento[] | any[]; // Relación OneToMany de soportes
  gestiones?: Gestion[];            // 👈 Historial de trazabilidad (Gestión OneToMany)
  items?: FacturaItem[];            // 📦 Líneas / Ítems facturados
}