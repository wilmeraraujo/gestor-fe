import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TipoService } from '../../services/tipo.service';
import { PrestadorService } from '../../services/prestador.service';
import { DocumentoService } from '../../services/documento.service';
import { Tipo } from '../../models/tipo';
import { Prestador } from '../../models/prestador';
import { Documento } from '../../models/documento';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-prestador',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './prestador.component.html',
  styleUrls: ['./prestador.component.css']
})
export class PrestadorComponent implements OnInit {

  public Number = Number;

  public nitBusqueda: string = '';
  public prestadorActual: Prestador | null = null;
  public tiposSoporte: Tipo[] = [];
  public soportesCargados: Map<number, Documento> = new Map(); // Mapa [tipoId -> Documento]
  public cargando: boolean = false;

  constructor(
    private tipoService: TipoService,
    private prestadorService: PrestadorService,
    private documentoService: DocumentoService
  ) { }

  ngOnInit(): void {
    this.cargarTiposSoporte();
  }

  // 1. Cargar catálogo de tipos filtrando los de factura
  cargarTiposSoporte(): void {
    this.tipoService.listar().subscribe({
      next: (tipos: any[]) => {
        this.tiposSoporte = tipos.filter(t => {
          const desc = (t.descripcion || '').toUpperCase();
          const cod = (t.codigo || '').toUpperCase();
          return !desc.includes('XML') && !desc.includes('PDF') && cod !== 'FAC_XML' && cod !== 'FAC_PDF';
        });
      },
      error: (err) => console.error('Error cargando tipos de soporte:', err)
    });
  }

  // 2. Buscar prestador por el NIT digitado en el input
  buscarPrestador(): void {
    if (!this.nitBusqueda || this.nitBusqueda.trim() === '') {
      Swal.fire('Advertencia', 'Ingrese un NIT para realizar la búsqueda.', 'warning');
      return;
    }

    this.cargando = true;
    this.prestadorService.obtenerPorNit(this.nitBusqueda.trim()).subscribe({
      next: (prestador) => {
        this.prestadorActual = prestador;
        if (prestador.id !== undefined) {
          this.cargarSoportesExistentes(prestador.id);
        }
        this.cargando = false;
      },
      error: () => {
        this.prestadorActual = null;
        this.soportesCargados.clear();
        this.cargando = false;
        Swal.fire('No encontrado', `No se encontró un prestador registrado con NIT: ${this.nitBusqueda}`, 'info');
      }
    });
  }

  // 3. Cargar los soportes del prestador
  cargarSoportesExistentes(prestadorId: number | string): void {
    this.prestadorService.listarSoportes(Number(prestadorId)).subscribe({
      next: (response) => {
        this.soportesCargados.clear();
        const listaDocs: Documento[] = response.content || response;

        listaDocs.forEach(doc => {
          if (doc.tipoId) {
            this.soportesCargados.set(Number(doc.tipoId), doc);
          }
        });
      },
      error: (err) => console.error('Error cargando soportes del prestador:', err)
    });
  }

  // 🛠️ MÉTODOS HELPER PARA EL TEMPLATE
  tieneSoporte(tipoId: number | string): boolean {
    return this.soportesCargados.has(Number(tipoId));
  }

  obtenerSoporte(tipoId: number | string): Documento | undefined {
    return this.soportesCargados.get(Number(tipoId));
  }

  // 4. Cargar un archivo desde la tarjeta
  onFileSelected(event: any, tipo: Tipo): void {
    const archivo: File = event.target.files[0];
    if (!archivo || !this.prestadorActual) return;

    const extensionId = archivo.name.toLowerCase().endsWith('.pdf') ? 2 : 1;

    Swal.fire({
      title: 'Subiendo archivo...',
      text: `Cargando soporte ${tipo.descripcion || tipo.codigo}`,
      allowOutsideClick: false,
      didOpen: () => Swal.showLoading()
    });

    this.prestadorService.cargarSoporte(this.prestadorActual.nit, Number(tipo.id), extensionId, archivo).subscribe({
      next: (docGuardado) => {
        this.soportesCargados.set(Number(tipo.id), docGuardado);
        Swal.fire('Éxito', `Soporte ${tipo.descripcion || tipo.codigo} guardado correctamente.`, 'success');
      },
      error: (err) => {
        Swal.fire('Error', 'No se pudo cargar el archivo. Inténtelo de nuevo.', 'error');
        console.error(err);
      }
    });
  }

  // 5. Visualizar el documento
  verDocumento(doc: Documento): void {
    if (!doc || !doc.id) return;
    this.documentoService.getDocumentoBlob(Number(doc.id)).subscribe({
      next: (blob) => {
        const fileURL = URL.createObjectURL(blob);
        window.open(fileURL, '_blank');
      },
      error: () => Swal.fire('Error', 'No se pudo generar la vista previa del documento.', 'error')
    });
  }

  // 6. Eliminar el soporte
  eliminarSoporte(tipoId: number | string, docId: number | string): void {
    Swal.fire({
      title: '¿Está seguro?',
      text: 'Se eliminará el soporte seleccionado.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.prestadorService.eliminarSoporte(Number(docId)).subscribe({
          next: () => {
            this.soportesCargados.delete(Number(tipoId));
            Swal.fire('Eliminado', 'El soporte ha sido removido.', 'success');
          },
          error: (err) => {
            Swal.fire('Error', 'No se pudo eliminar el soporte.', 'error');
            console.error(err);
          }
        });
      }
    });
  }
}
