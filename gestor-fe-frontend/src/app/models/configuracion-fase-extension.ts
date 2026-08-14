import { Global } from "./global";

export class ConfiguracionFaseExtension extends Global {
    faseId!: number;
    extensionId!: number;
    tamanoMaximoMb: number = 10;
    obligatorio: boolean = false;
    permiteMultiple: boolean = true;
}
