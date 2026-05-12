import { Generic } from "./generic";

export class Global implements Generic{
    id!: number;
    codigo!: string;
    descripcion!: string;
    createdAd?: Date;
    deletedAd?: Date;
    updatedAd?: Date;
}
