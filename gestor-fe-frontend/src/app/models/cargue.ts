export interface Cargue {
  id: number; // El '?' indica que es opcional (por ejemplo, cuando se va a crear y aún no tiene ID)
  nombreArchivo: string;
  nitPrestador: string;
  exiteError: boolean;
  numeroRegistro: number;
  usuario: string;
  jobExecutionId?: number; // Opcional por si el lote está encolado o aún no inicia
  createdAt: Date | string; // Puede venir como string ISO-8601 o convertirse a objeto Date
  deletedAt?: Date | string | null; // Opcional y nullable para lógicas de borrado lógico
}