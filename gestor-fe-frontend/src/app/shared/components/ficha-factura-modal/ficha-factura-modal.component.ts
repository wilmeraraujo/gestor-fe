import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { Factura } from '../../../models/factura';
import { FacturaItem } from '../../../models/factura-item';
import { FacturaService } from '../../../services/factura.service';
import { MedioPagoService } from '../../../services/medio-pago.service';
import { TipoDocumentoDianService } from '../../../services/tipo-documento-dian.service';
import { UnidadMedidaService } from '../../../services/unidad-medida.service';
import { ResponsabilidadFiscalService } from '../../../services/responsabilidad-fiscal.service';
import { DepartamentoService } from '../../../services/departamento.service';
import { MunicipioService } from '../../../services/municipio.service';
import { MedioPago } from '../../../models/medio-pago';
import { TipoDocumentoDian } from '../../../models/tipo-documento-dian';
import { UnidadMedida } from '../../../models/unidad-medida';
import { ResponsabilidadFiscal } from '../../../models/responsabilidad-fiscal';
import { Departamento } from '../../../models/departamento';
import { Municipio } from '../../../models/municipio';

import { TipoIdentificacionService } from '../../../services/tipo-identificacion.service';
import { TipoOperacionService } from '../../../services/tipo-operacion.service';
import { TipoIdentificacion } from '../../../models/tipo-identificacion';
import { TipoOperacion } from '../../../models/tipo-operacion';

export interface FichaFacturaDialogData {
  facturaId?: number;
  factura?: Factura;
}

@Component({
  selector: 'app-ficha-factura-modal',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  templateUrl: './ficha-factura-modal.component.html',
  styleUrl: './ficha-factura-modal.component.css'
})
export class FichaFacturaModalComponent implements OnInit {

  factura!: Factura;
  items: FacturaItem[] = [];
  cargando: boolean = true;
  cufeCopiado: boolean = false;

  // Catálogos dinámicos
  mediosPagoMap = new Map<string, string>();
  tiposDocumentoMap = new Map<string, string>();
  unidadesMedidaMap = new Map<string, string>();
  responsabilidadesFiscalesMap = new Map<string, string>();
  tiposIdentificacionMap = new Map<string, string>();
  tiposOperacionMap = new Map<string, string>();
  departamentosMap = new Map<string, string>();
  municipiosMap = new Map<string, string>();

  columnasItems: string[] = [
    'linea',
    'codigo',
    'descripcion',
    'cantidad',
    'unidad',
    'precioUnitario',
    'total'
  ];

  constructor(
    public dialogRef: MatDialogRef<FichaFacturaModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: FichaFacturaDialogData,
    private facturaService: FacturaService,
    private medioPagoService: MedioPagoService,
    private tipoDocumentoDianService: TipoDocumentoDianService,
    private unidadMedidaService: UnidadMedidaService,
    private responsabilidadFiscalService: ResponsabilidadFiscalService,
    private tipoIdentificacionService: TipoIdentificacionService,
    private tipoOperacionService: TipoOperacionService,
    private departamentoService: DepartamentoService,
    private municipioService: MunicipioService
  ) {}

  ngOnInit(): void {
    this.cargarCatalogosAdministracion();

    if (this.data.factura) {
      this.factura = { ...this.data.factura };
    }

    const id = this.data.facturaId || (this.data.factura ? this.data.factura.id : null);
    if (id) {
      this.cargarDetalleCompleto(id);
    } else {
      this.cargando = false;
    }
  }

  cargarCatalogosAdministracion(): void {
    this.medioPagoService.listar().subscribe({
      next: (lista: MedioPago[]) => {
        if (lista) {
          lista.forEach(m => {
            if (m.codigo && m.descripcion) {
              this.mediosPagoMap.set(m.codigo.trim(), m.descripcion.trim());
            }
          });
        }
      },
      error: () => {}
    });

    this.tipoDocumentoDianService.listar().subscribe({
      next: (lista: TipoDocumentoDian[]) => {
        if (lista) {
          lista.forEach(t => {
            if (t.codigo && t.descripcion) {
              this.tiposDocumentoMap.set(t.codigo.trim(), t.descripcion.trim());
            }
          });
        }
      },
      error: () => {}
    });

    this.unidadMedidaService.listar().subscribe({
      next: (lista: UnidadMedida[]) => {
        if (lista) {
          lista.forEach(u => {
            if (u.codigo && u.descripcion) {
              this.unidadesMedidaMap.set(u.codigo.trim(), u.descripcion.trim());
            }
          });
        }
      },
      error: () => {}
    });

    this.responsabilidadFiscalService.listar().subscribe({
      next: (lista: ResponsabilidadFiscal[]) => {
        if (lista) {
          lista.forEach(r => {
            if (r.codigo && r.descripcion) {
              this.responsabilidadesFiscalesMap.set(r.codigo.trim(), r.descripcion.trim());
            }
          });
        }
      },
      error: () => {}
    });

    this.tipoIdentificacionService.listar().subscribe({
      next: (lista: TipoIdentificacion[]) => {
        if (lista) {
          lista.forEach(i => {
            if (i.codigo && i.descripcion) {
              this.tiposIdentificacionMap.set(i.codigo.trim(), i.descripcion.trim());
            }
          });
        }
      },
      error: () => {}
    });

    this.tipoOperacionService.listar().subscribe({
      next: (lista: TipoOperacion[]) => {
        if (lista) {
          lista.forEach(o => {
            if (o.codigo && o.descripcion) {
              this.tiposOperacionMap.set(o.codigo.trim(), o.descripcion.trim());
            }
          });
        }
      },
      error: () => {}
    });

    this.departamentoService.listar().subscribe({
      next: (lista: Departamento[]) => {
        if (lista) {
          lista.forEach(d => {
            if (d.codigo && d.descripcion) {
              this.departamentosMap.set(d.codigo.trim(), d.descripcion.trim());
            }
          });
        }
      },
      error: () => {}
    });

    this.municipioService.listar().subscribe({
      next: (lista: Municipio[]) => {
        if (lista) {
          lista.forEach(m => {
            if (m.codigo && m.descripcion) {
              this.municipiosMap.set(m.codigo.trim(), m.descripcion.trim());
            }
          });
        }
      },
      error: () => {}
    });
  }

  cargarDetalleCompleto(id: number): void {
    this.cargando = true;

    // Consultamos la factura completa para traer todos los datos de cliente, retenciones y notas
    this.facturaService.ver(id).subscribe({
      next: (f: Factura) => {
        if (f) {
          this.factura = { ...this.factura, ...f };
        }
        // Consultamos la lista de ítems de la factura
        this.facturaService.obtenerItems(id).subscribe({
          next: (items: FacturaItem[]) => {
            this.items = items || [];
            this.cargando = false;
          },
          error: (err) => {
            console.warn('No fue posible cargar los ítems de la factura:', err);
            // Si la entidad factura ya traía items en memoria
            if (this.factura && this.factura.items) {
              this.items = this.factura.items;
            }
            this.cargando = false;
          }
        });
      },
      error: (err) => {
        console.error('Error cargando detalle de factura:', err);
        this.cargando = false;
      }
    });
  }

  copiarCufe(): void {
    if (this.factura?.cufe) {
      navigator.clipboard.writeText(this.factura.cufe).then(() => {
        this.cufeCopiado = true;
        setTimeout(() => (this.cufeCopiado = false), 2500);
      });
    }
  }

  // 💳 Mapeo de Medios de Pago (Catálogo Admin + Fallback DIAN)
  obtenerDescripcionMedioPago(codigo?: string): string {
    if (!codigo) return 'No especificado';
    const c = codigo.trim();
    if (this.mediosPagoMap.has(c)) {
      return `${c} - ${this.mediosPagoMap.get(c)}`;
    }
    const mapa: { [key: string]: string } = {
      '1': '1 - Instrumento no definido / Acuerdo mutuo',
      '10': '10 - Efectivo',
      '20': '20 - Cheque',
      '31': '31 - Pago por compensación',
      '41': '41 - Transferencia Crédito bancaria',
      '42': '42 - Consignación bancaria',
      '47': '47 - Transferencia Débito bancaria',
      '48': '48 - Tarjeta Crédito',
      '49': '49 - Tarjeta Débito',
      '71': '71 - Bonos',
      '72': '72 - Vales',
      'ZZZ': 'ZZZ - Otro / Mutuo acuerdo'
    };
    return mapa[c] || c;
  }

  // 📄 Mapeo de Tipo de Documento DIAN
  obtenerDescripcionTipoDoc(codigo?: string): string {
    if (!codigo) return '';
    const c = codigo.trim();
    if (this.tiposDocumentoMap.has(c)) {
      return `${c} - ${this.tiposDocumentoMap.get(c)}`;
    }
    const mapa: { [key: string]: string } = {
      '01': '01 - Factura de Venta',
      '02': '02 - Factura de Exportación',
      '03': '03 - Factura Contingencia Facturador',
      '04': '04 - Factura Contingencia DIAN',
      '05': '05 - Factura de Talonario o Papel',
      '40': '40 - Documento Soporte a no obligados',
      '91': '91 - Nota Crédito',
      '92': '92 - Nota Débito',
      '93': '93 - Nota de Ajuste Documento Soporte'
    };
    return mapa[c] || `Tipo ${c}`;
  }

  // 🪪 Mapeo de Tipo de Identificación DIAN
  obtenerDescripcionTipoIdentificacion(codigo?: string): string {
    if (!codigo) return 'NIT';
    const c = codigo.trim();
    if (this.tiposIdentificacionMap.has(c)) {
      return `${c} - ${this.tiposIdentificacionMap.get(c)}`;
    }
    const mapa: { [key: string]: string } = {
      '11': '11 - Registro civil',
      '12': '12 - Tarjeta de identidad',
      '13': '13 - Cédula de ciudadanía',
      '21': '21 - Tarjeta de extranjería',
      '22': '22 - Cédula de extranjería',
      '31': '31 - NIT',
      '41': '41 - Pasaporte',
      '42': '42 - Documento extranjero',
      '47': '47 - PEP',
      '48': '48 - PPT',
      '50': '50 - NIT otro país',
      '91': '91 - NUIP'
    };
    return mapa[c] || c;
  }

  // 🔄 Mapeo de Tipo de Operación DIAN
  obtenerDescripcionTipoOperacion(codigo?: string): string {
    if (!codigo) return '';
    const c = codigo.trim();
    if (this.tiposOperacionMap.has(c)) {
      return `${c} - ${this.tiposOperacionMap.get(c)}`;
    }
    const mapa: { [key: string]: string } = {
      '10': '10 - Estándar',
      '09': '09 - AIU',
      '11': '11 - Mandatos bienes',
      '12': '12 - Mandatos servicios',
      '20': '20 - Nota ref. Factura',
      '22': '22 - Nota sin ref.',
      '30': '30 - Sector Salud (RIPS)',
      '32': '32 - Salud (Cuotas/Copagos)'
    };
    return mapa[c] || c;
  }

  // 📦 Mapeo de Unidad de Medida
  obtenerDescripcionUnidad(codigo?: string): string {
    if (!codigo) return 'EA';
    const c = codigo.trim();
    if (this.unidadesMedidaMap.has(c)) {
      return `${c} (${this.unidadesMedidaMap.get(c)})`;
    }
    const mapa: { [key: string]: string } = {
      'EA': 'EA (Unidad)',
      '94': '94 (Servicio)',
      'HUR': 'HUR (Hora)',
      'KGM': 'KGM (Kg)',
      'MTR': 'MTR (Metro)',
      'LTR': 'LTR (Litro)',
      'DAY': 'DAY (Día)',
      'MON': 'MON (Mes)'
    };
    return mapa[c] || c;
  }

  // ⚖️ Mapeo de Responsabilidad Fiscal DIAN
  obtenerDescripcionRespFiscal(codigo?: string): string {
    if (!codigo) return '';
    const c = codigo.trim();
    if (this.responsabilidadesFiscalesMap.has(c)) {
      return `${c} - ${this.responsabilidadesFiscalesMap.get(c)}`;
    }
    const mapa: { [key: string]: string } = {
      'O-13': 'O-13 - Gran contribuyente',
      'O-15': 'O-15 - Autorretenedor',
      'O-23': 'O-23 - Agente de retención IVA',
      'O-47': 'O-47 - Régimen simple de tributación',
      'R-99-PN': 'R-99-PN - No responsable de IVA'
    };
    return mapa[c] || c;
  }

  // 🗺️ Mapeo de Departamento
  obtenerDescripcionDepartamento(codigo?: string): string {
    if (!codigo) return '';
    const c = codigo.trim();
    if (this.departamentosMap.has(c)) {
      return `${c} - ${this.departamentosMap.get(c)}`;
    }
    return c;
  }

  // 🏙️ Mapeo de Municipio
  obtenerDescripcionMunicipio(codigo?: string): string {
    if (!codigo) return '';
    const c = codigo.trim();
    if (this.municipiosMap.has(c)) {
      return `${c} - ${this.municipiosMap.get(c)}`;
    }
    return c;
  }

  // ✉️ Formateador de múltiples correos electrónicos en lista limpia
  obtenerListaEmails(emailStr?: string): string[] {
    if (!emailStr) return [];
    return emailStr
      .split(/[,;]+/)
      .map(e => e.trim())
      .filter(e => e.length > 0);
  }

  // 💰 Cálculo de Neto a Girar (Total Factura - Total Retenciones)
  calcularNetoAGirar(): number {
    if (!this.factura) return 0;
    const total = this.factura.valorTotal || 0;
    const retenciones = this.factura.totalRetenciones || 
      ((this.factura.valorRetefuente || 0) + (this.factura.valorReteica || 0) + (this.factura.valorReteiva || 0));
    return total - retenciones;
  }

  cerrar(): void {
    this.dialogRef.close();
  }
}
