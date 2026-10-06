import { Global } from "./global";
import { Municipio } from "./municipio";

export class Departamento extends Global {
  municipios?: Municipio[];
}
