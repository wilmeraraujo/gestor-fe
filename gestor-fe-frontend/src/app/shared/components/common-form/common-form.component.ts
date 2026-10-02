import { CommonModule } from '@angular/common';
import { Component, OnInit, Input } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import Swal from 'sweetalert2';
import { LoginService } from '../../../services/login.service';

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
    private dialogRef: MatDialogRef<CommonFormComponent>,
    private loginService: LoginService
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

  // Capturar archivo cuando sea type === 'file' con validación dinámica
  onFileChange(event: any, fieldName: string): void {
    if (event.target.files && event.target.files.length > 0) {
      const file: File = event.target.files[0];
      const campo = this.campos.find(c => c.name === fieldName);

      if (campo) {
        const nombreLower = file.name.toLowerCase();
        const ext = nombreLower.includes('.') ? nombreLower.substring(nombreLower.lastIndexOf('.') + 1) : '';

        // 🛡️ Validación de extensión permitida
        if (campo.allowedExtensions && Array.isArray(campo.allowedExtensions) && campo.allowedExtensions.length > 0) {
          const permitidas = campo.allowedExtensions.map((e: string) => e.toLowerCase().replace('.', '').trim());
          if (!permitidas.includes(ext)) {
            Swal.fire({
              icon: 'warning',
              title: 'Formato no permitido',
              text: `El archivo "${file.name}" (.${ext.toUpperCase()}) no es válido. Formatos permitidos: .${permitidas.join(', .').toUpperCase()}`,
              confirmButtonColor: '#1DA6BA'
            });
            event.target.value = '';
            delete this.archivosSubidos[fieldName];
            this.form.get(fieldName)?.setValue('');
            return;
          }
        }

        // 🛡️ Validación de tamaño máximo en MB
        if (campo.maxSizeMb && campo.maxSizeMb > 0) {
          const maxMb = campo.maxSizeMb;
          const maxBytes = maxMb * 1024 * 1024;
          if (file.size > maxBytes) {
            const pesoRealMb = (file.size / (1024 * 1024)).toFixed(2);
            Swal.fire({
              icon: 'warning',
              title: 'Archivo demasiado grande',
              text: `El archivo "${file.name}" pesa ${pesoRealMb} MB y supera el tamaño máximo permitido de ${maxMb} MB.`,
              confirmButtonColor: '#1DA6BA'
            });
            event.target.value = '';
            delete this.archivosSubidos[fieldName];
            this.form.get(fieldName)?.setValue('');
            return;
          }
        }
      }

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

    // ⚡ Normalizar código (quitar espacios al inicio y final)
    if (formValues.codigo && typeof formValues.codigo === 'string') {
      formValues.codigo = formValues.codigo.trim();
    }

    // ⚡ Normalizar IDs numéricos si vienen como String desde el <select>
    if (formValues.faseId) formValues.faseId = Number(formValues.faseId);
    if (formValues.extensionId) formValues.extensionId = Number(formValues.extensionId);
    if (formValues.tamanoMaximoMb) formValues.tamanoMaximoMb = Number(formValues.tamanoMaximoMb);

    const usuarioActivo = this.loginService.getUserName();

    const request = formValues.id
      ? this.service.editar(formValues, usuarioActivo)
      : this.service.crear(formValues, usuarioActivo);

    request.subscribe({
      next: () => {
        Swal.fire({
          icon: 'success',
          title: 'Éxito',
          text: formValues.id
            ? 'Registro actualizado con éxito'
            : 'Registro creado con éxito',
          confirmButtonColor: '#1DA6BA'
        });
        this.dialogRef.close(true);
      },
      error: (err: any) => {
        console.error(err);
        const errorMsg = err.error?.error || err.error?.mensaje || (typeof err.error === 'string' ? err.error : 'Ocurrió un error al procesar la solicitud');
        Swal.fire({
          icon: 'error',
          title: 'Error de Validación',
          text: errorMsg,
          confirmButtonColor: '#1DA6BA'
        });
      }
    });
  }

  cerrar(): void {
    this.dialogRef.close();
  }
}
