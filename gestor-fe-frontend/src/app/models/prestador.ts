import { Generic } from './generic';
import { Documento } from './documento';

export interface Prestador extends Generic {
  nit: string;
  razonSocial: string;
  direccion: string;
  telefono: string;
  email: string;
  identificadorCargue: number;
  soportes?: Documento[];
  createdAt?: Date;
}
