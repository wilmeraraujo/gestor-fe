export interface Documento {
  id: number;
  ruta: string;
  nombreOriginal: string; // <-- Sincronizado con tu backend
  tamano: number;
  codigoEstado?: string;
  codigoExtension?: string;
  codigoTipo?: string;         // <-- Representa el código del tipo
  estadoId?: number | null;
  extensionId?: number | null;
  tipoId?: number | null;
  createdAt: Date | string;
  deletedAt?: Date | string | null;
}