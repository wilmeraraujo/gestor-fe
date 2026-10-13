export interface FacturaItem {
  id?: number;
  facturaId?: number;
  numeroLinea?: number;
  codigoProducto?: string;
  codigoUnspsc?: string;
  descripcion?: string;
  cantidad?: number;
  unidadMedida?: string;
  precioUnitario?: number;
  precioReferencia?: number;
  porcentajeDescuento?: number;
  valorDescuento?: number;
  porcentajeIva?: number;
  valorIva?: number;
  porcentajeImpoconsumo?: number;
  valorImpoconsumo?: number;
  valorSubtotal?: number;
  valorTotal?: number;
  createdAt?: Date | string;
}
