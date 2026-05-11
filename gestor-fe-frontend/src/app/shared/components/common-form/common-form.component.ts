import { CommonModule } from '@angular/common';
import { Component, OnInit, Input } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-common-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './common-form.component.html',
  styleUrl: './common-form.component.css'
})

export class CommonFormComponent implements OnInit{

  @Input() campos: any[] = [];

  @Input() data: any = {};

  @Input() service: any;

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CommonFormComponent>
  ) {}

  ngOnInit(): void {

    const group: any = {};

    this.campos.forEach(campo => {

      group[campo.name] = [

        this.data[campo.name] || '',

        campo.required
          ? [Validators.required]
          : []

      ];

    });

    this.form = this.fb.group(group);

  }

  guardar(): void {

    if(this.form.invalid){

      this.form.markAllAsTouched();

      return;

    }

  const model = {

    ...this.data,

    ...this.form.value

  };

    const request = model.id
      ? this.service.editar(model)
      : this.service.crear(model);

    request.subscribe({

      next: () => {

        Swal.fire({
          icon: 'success',
          title: 'Éxito',
          text: model.id
            ? 'Registro actualizado con éxito'
            : 'Registro creado con éxito'
        });

        this.dialogRef.close(true);

      },

      error: (err: any) => {

        console.error(err);

        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'Ocurrió un error'
        });

      }

    });

  }

  cerrar(): void {
    this.dialogRef.close();
  }
}
