import { CommonModule } from '@angular/common';
import { Component, OnInit, Input } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-common-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './common-form.component.html',
  styleUrl: './common-form.component.css'
})
export class CommonFormComponent implements OnInit {

  @Input() campos: any[] = [];
  @Input() data: any = {};
  @Input() service: any;

  form!: FormGroup;
  archivosSubidos: { [key: string]: File } = {};

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CommonFormComponent>
  ) {}

  ngOnInit(): void {
    const group: any = {};

    this.campos.forEach(campo => {
      group[campo.name] = [
        this.data[campo.name] || '',
        campo.required ? [Validators.required] : []
      ];
    });

    this.form = this.fb.group(group);

    // 🔔 Escuchar cambios para campos condicionales (como 'estado')
    this.campos.forEach(campo => {
      if (campo.onChange) {
        this.form.get(campo.name)?.valueChanges.subscribe(val => {
          campo.onChange(val, this.campos, this.form);
        });
        // Ejecutar una vez al inicio con el valor por defecto
        campo.onChange(this.form.get(campo.name)?.value, this.campos, this.form);
      }
    });
  }

  // Capturar archivo cuando sea type === 'file'
  onFileChange(event: any, fieldName: string): void {
    if (event.target.files && event.target.files.length > 0) {
      const file = event.target.files[0];
      this.archivosSubidos[fieldName] = file;
      this.form.get(fieldName)?.setValue(file.name);
    }
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const formValues = { ...this.data, ...this.form.value };

    // ⚡ Normalizar IDs numéricos si vienen como String desde el <select>
    if (formValues.faseId) formValues.faseId = Number(formValues.faseId);
    if (formValues.extensionId) formValues.extensionId = Number(formValues.extensionId);
    if (formValues.tamanoMaximoMb) formValues.tamanoMaximoMb = Number(formValues.tamanoMaximoMb);

    const request = formValues.id
      ? this.service.editar(formValues)
      : this.service.crear(formValues);

    request.subscribe({
      next: () => {
        Swal.fire({
          icon: 'success',
          title: 'Éxito',
          text: formValues.id
            ? 'Regla actualizada con éxito'
            : 'Regla creada con éxito'
        });
        this.dialogRef.close(true);
      },
      error: (err: any) => {
        console.error(err);
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'Ocurrió un error al procesar la solicitud'
        });
      }
    });
  }

  cerrar(): void {
    this.dialogRef.close();
  }
}
