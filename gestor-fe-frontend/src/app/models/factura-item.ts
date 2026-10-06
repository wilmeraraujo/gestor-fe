export interface FacturaItem {
  id?: number;
  facturaId?: number;
  numeroLinea?: number;
  codigoProducto?: string;
  descripcion?: string;
  cantidad?: number;
  unidadMedida?: string;
  precioUnitario?: number;
  valorTotal?: number;
  createdAt?: Date | string;
}
