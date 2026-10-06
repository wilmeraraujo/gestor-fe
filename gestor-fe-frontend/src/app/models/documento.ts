export interface Documento {
  id: number;
  ruta: string;
  nombreOriginal: string; // <-- Sincronizado con tu backend
  tamano: number;
  estadoId: number;
  extensionId: number;
  tipoId: number;         // <-- Representa el ID del tipo (XML: 1, PDF: 2, etc.)
  createdAt: Date | string;
  deletedAt?: Date | string | null;
}