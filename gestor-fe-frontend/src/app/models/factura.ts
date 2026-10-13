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
  horaEmision?: string;

  // Metadatos DIAN y Moneda
  codigoTipoDocumento?: string;
  codigoTipoOperacion?: string;
  codigoAmbienteEjecucion?: string;
  moneda?: string;
  tasaCambio?: number;
  fechaTasaCambio?: Date | string;

  // Emisor Fiscal
  codigoTipoIdentificacionEmisor?: string;
  codigoTipoPersonaEmisor?: string;
  codigoResponsabilidadFiscalEmisor?: string;

  // Totales Detallados
  valorBruto?: number;
  totalDescuentos?: number;
  totalCargos?: number;
  totalAnticipos?: number;
  valorImpoconsumo?: number;
  otrosImpuestos?: number;

  // 👤 Datos del Cliente / Adquirente
  codigoTipoIdentificacionCliente?: string;
  codigoTipoPersonaCliente?: string;
  codigoResponsabilidadFiscalCliente?: string;
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
  codigoFormaPago?: string; // 1 = Contado, 2 = Crédito
  codigoMedioPago?: string;
  notas?: string;
  valorRetefuente?: number;
  valorReteica?: number;
  valorReteiva?: number;
  totalRetenciones?: number;
  
  // 📌 Estado actual (punteros de rápida lectura)
  estado?: string;
  faseId: number;
  observacion?: string;
  codigoCausalDevolucion?: string;
  codigoTipoRegistroContable?: string; // 👈 Mapeado como código (FC, GV, ORC, NI, TB)
  codigoMovimiento?: string | null;
  numeroCausacion?: string;
  codigoConcepto?: string;
  
  createdAt: Date | string;
  deletedAt?: Date | string | null;
  
  // 🔗 Relaciones Bidireccionales
  documentos?: Documento[] | any[]; // Relación OneToMany de soportes
  gestiones?: Gestion[];            // 👈 Historial de trazabilidad (Gestión OneToMany)
  items?: FacturaItem[];            // 📦 Líneas / Ítems facturados
}