import { Generic } from "./generic";

export class Global implements Generic{
    id!: number;
    codigo!: string;
    descripcion!: string;
    createdAt?: Date;
    deletedAt?: Date;
    updatedAt?: Date;
}
