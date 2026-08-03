export interface Factura {
  id: number;
  nit: string;
  numeroFactura: string;
  cufe: string;
  identificadorCargue: number;
  linea: number;
  razonSocialEmisor?: string;
  valorTotal?: number; // Representa el BigDecimal de Java
  fechaEmision?: Date | string; // Representa el LocalDate
  estado?: string;
  observacion?: string;
  faseId: number;
  numeroCausacion?:string;
  createdAt: Date | string;
  deletedAt?: Date | string | null;
  documentos?: any[]; // Relación OneToMany de documentos asociados
}