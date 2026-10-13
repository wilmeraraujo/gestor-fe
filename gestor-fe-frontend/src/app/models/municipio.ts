import { Global } from "./global";
import { Departamento } from "./departamento";

export class Municipio extends Global {
  departamentoId?: number;
  departamento?: Departamento;
}
