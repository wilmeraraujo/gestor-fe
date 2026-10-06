export interface KpisDto {
  totalFacturas: number;
  montoTotal: number;
  facturasEnTramite: number;
  montoEnTramite: number;
  facturasAprobadas: number;
  montoAprobadas: number;
  facturasRechazadas: number;
  montoRechazadas: number;
  totalPrestadores: number;
}

export interface FaseDto {
  faseId: number;
  faseNombre: string;
  cantidad: number;
  montoTotal: number;
  porcentaje: number;
}

export interface EstadoDto {
  estado: string;
  cantidad: number;
  porcentaje: number;
}

export interface PrestadorDto {
  nit: string;
  razonSocial: string;
  totalFacturas: number;
  montoTotal: number;
  facturasAprobadas: number;
  facturasRechazadas: number;
  facturasPendientes: number;
}

export interface UsuarioGestionDto {
  usuario: string;
  totalGestiones: number;
  aprobadas: number;
  rechazadas: number;
  porcentajeEfectividad: number;
}

export interface ResumenEstadisticasResponse {
  kpis: KpisDto;
  porFase: FaseDto[];
  porEstado: EstadoDto[];
  topPrestadores: PrestadorDto[];
  gestionesPorUsuario?: UsuarioGestionDto[];
}
