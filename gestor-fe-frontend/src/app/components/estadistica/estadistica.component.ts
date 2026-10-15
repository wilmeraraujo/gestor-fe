import { Component, OnInit, OnDestroy, ViewChild, ElementRef, inject } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

import { EstadisticaService } from '../../services/estadistica.service';
import { FacturaService } from '../../services/factura.service';
import { AlertService } from '../../services/alert.service';
import { LoginService } from '../../services/login.service';
import { ResumenEstadisticasResponse } from '../../models/estadistica';

@Component({
  selector: 'app-estadistica',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressBarModule
  ],
  providers: [CurrencyPipe],
  templateUrl: './estadistica.component.html',
  styleUrl: './estadistica.component.css'
})
export class EstadisticaComponent implements OnInit, OnDestroy {

  private estadisticaService = inject(EstadisticaService);
  private facturaService = inject(FacturaService);
  private alertService = inject(AlertService);
  private loginService = inject(LoginService);
  private currencyPipe = inject(CurrencyPipe);

  @ViewChild('canvasBarrasRanking') canvasBarrasRanking?: ElementRef<HTMLCanvasElement>;
  @ViewChild('canvasTortaEstados') canvasTortaEstados?: ElementRef<HTMLCanvasElement>;
  @ViewChild('canvasUsuarios') canvasUsuarios?: ElementRef<HTMLCanvasElement>;

  chartBarras?: Chart;
  chartTorta?: Chart;
  chartUsuarios?: Chart;

  cargando: boolean = false;
  datos: ResumenEstadisticasResponse | null = null;

  // Filtros
  fechaInicio: string = '';
  fechaFin: string = '';
  nitFiltro: string = '';
  presetActivo: string = 'todo';

  // Datos de usuario y roles
  usuarioActivo: string = '';
  rolesUsuario: string[] = [];
  esPrestador: boolean = false;
  esAdminOGestor: boolean = false;
  isAdmin: boolean = false;

  // 🏛️ Sección Información Exógena DIAN (Formatos 1001, 1003, 1005, 1006, 1007 y Consolidado)
  formatoExogenaSeleccionado: '1001' | '1003' | '1005' | '1006' | '1007' | 'consolidado' = '1001';
  anioGravableExogena: number | null = null;
  aniosDisponibles: number[] = [];
  fechaExogenaDesde: string = '';
  fechaExogenaHasta: string = '';
  descargandoExogena: boolean = false;
  formatoEnDescarga: string | null = null;

  formatosExogena = [
    {
      id: '1001' as const,
      codigo: 'Formato 1001',
      titulo: 'Pagos o Abonos en Cuenta y Retenciones Practicadas',
      badge: 'Costos, Gastos y Ret. Practicadas',
      icono: 'payments',
      color: '#10b981',
      columnasCount: 20,
      descripcion: 'Pagos o abonos en cuenta y retenciones en la fuente practicadas / asumidas a terceros (Gastos y Costos según especificación técnica DIAN).',
      columnas: 'concepto, tipo_docto, numero_identificacion, primer_Apellido, segundo_apellido, primer_nombre, segundo_nombre, razon_social, direccion, codigo_departamento, codigo_municipio, codigo_pais, pago_abono_en_cuenta_gto_deducible, apgo_abono_en_cuenta_no_deducible, iva_mayor_vr_gasto, iva_mayor_vr_del_cosot_gasto_no_deducible, retencion_en_la_fuente, fte_asumida, retfte_iva, fte_iva_no_residentes'
    },
    {
      id: '1003' as const,
      codigo: 'Formato 1003',
      titulo: 'Retenciones en la Fuente que le Practicaron',
      badge: 'Retenciones que le Practicaron',
      icono: 'account_balance_wallet',
      color: '#3b82f6',
      columnasCount: 14,
      descripcion: 'Retenciones en la fuente a título de renta e IVA que le practicaron clientes u otros agentes de retención por servicios, honorarios, etc.',
      columnas: 'Concepto, TDocto.tercero, NIT, DV, 1 AP, 2 AP, 1 NM, 2 NM, RAZON SOCIAL, Dirección tercero, Departamento tercero, Municipio tercero, Valor acumulado del pago o abono sujeto a Retención, Retención en la fuente que le practicaron'
    },
    {
      id: '1005' as const,
      codigo: 'Formato 1005',
      titulo: 'Impuesto Descontable (IVA Descontable)',
      badge: 'IVA Descontable en Compras',
      icono: 'remove_shopping_cart',
      color: '#8b5cf6',
      columnasCount: 10,
      descripcion: 'Impuesto sobre las ventas que le facturaron y descontó en adquisiciones de bienes y servicios gravados según proveedores.',
      columnas: 'TIPO DOCTO, NIT, DV, 1 APELLIDO, 2 APELLIDO, 1 NOMBRE, 2 NOMBRE, RAZON SOCIAL, IMP DESCONTABLE, DEVOLUCIONES'
    },
    {
      id: '1006' as const,
      codigo: 'Formato 1006',
      titulo: 'Impuesto Generado (IVA Generado)',
      badge: 'IVA Generado en Ventas',
      icono: 'add_shopping_cart',
      color: '#06b6d4',
      columnasCount: 10,
      descripcion: 'Impuesto sobre las ventas por pagar (generado) facturado a clientes en operaciones y servicios prestados gravados.',
      columnas: 'TIPO DOCTO, NIT, DV, 1 APELLIDO, 2 APELLIDO, 1 NOMBRE, 2 NOMBRE, RAZON SOCIAL, IVA GENERADO, DEVOLUCIONES'
    },
    {
      id: '1007' as const,
      codigo: 'Formato 1007',
      titulo: 'Ingresos Propios Recibidos en el Período',
      badge: 'Ingresos Brutos Recibidos',
      icono: 'trending_up',
      color: '#f59e0b',
      columnasCount: 11,
      descripcion: 'Ingresos brutos operacionales y no operacionales recibidos durante el año fiscal, detallados por tercero.',
      columnas: 'Concepto, TDocto.tercero, Id.tercero, Primer apellido, Segundo apellido, Primer nombre, Segundo nombre, Nombre tercero, Pais, Ingresos brutos recibidos, Devoluciones, rebajas y descuentos'
    },
    {
      id: 'consolidado' as const,
      codigo: 'Consolidado',
      titulo: 'Libro Excel Consolidado (5 Formatos en Pestañas)',
      badge: 'Multi-Formato DIAN',
      icono: 'auto_stories',
      color: '#ec4899',
      columnasCount: 5,
      descripcion: 'Libro de trabajo completo que reúne los 5 formatos en pestañas individuales: [Formato 1001], [1003], [1005], [1006] y [1007].',
      columnas: 'Pestañas: [Formato 1001 DIAN] | [FORMATO 1003 DIAN] | [F 1005 IVA DESCONTABLE] | [FORMATO 1006 IVA GENERADO] | [FORMATO 1007]'
    }
  ];

  get formatoActualInfo() {
    return this.formatosExogena.find(f => f.id === this.formatoExogenaSeleccionado) || this.formatosExogena[0];
  }

  ngOnInit(): void {
    this.generarAniosDisponibles();
    this.cargarSesion();
    this.consultarEstadisticas();
  }

  /**
   * 📅 Genera dinámicamente la lista de años gravables (año actual del sistema y los últimos 10 años)
   */
  private generarAniosDisponibles(aniosAtras: number = 10): void {
    const anioActual = new Date().getFullYear();
    this.aniosDisponibles = Array.from({ length: aniosAtras + 1 }, (_, i) => anioActual - i);
  }

  ngOnDestroy(): void {
    this.destruirGraficas();
  }

  private cargarSesion(): void {
    this.usuarioActivo = this.loginService.getUserName();
    this.rolesUsuario = this.loginService.getUserRoles() || [];

    this.isAdmin = this.loginService.isAdmin || this.loginService.isGAdmin;

    this.esAdminOGestor = this.isAdmin ||
                          this.loginService.isGReporte ||
                          this.loginService.isGFaseUno ||
                          this.loginService.isGFaseDos ||
                          this.loginService.isGFaseTres ||
                          this.loginService.isGFaseCuatro ||
                          this.loginService.isGFaseCinco;

    this.esPrestador = this.loginService.isPrestador && !this.esAdminOGestor;

    if (this.esPrestador && this.usuarioActivo && this.usuarioActivo !== 'GESTOR_SISTEMA') {
      this.nitFiltro = this.usuarioActivo;
    }
  }

  aplicarPreset(preset: string): void {
    this.presetActivo = preset;
    const hoy = new Date();
    const formatoFecha = (d: Date) => {
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    if (preset === 'hoy') {
      this.fechaInicio = formatoFecha(hoy);
      this.fechaFin = formatoFecha(hoy);
    } else if (preset === '7d') {
      const hace7Dias = new Date();
      hace7Dias.setDate(hoy.getDate() - 7);
      this.fechaInicio = formatoFecha(hace7Dias);
      this.fechaFin = formatoFecha(hoy);
    } else if (preset === 'mes') {
      const primerDiaMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
      this.fechaInicio = formatoFecha(primerDiaMes);
      this.fechaFin = formatoFecha(hoy);
    } else if (preset === 'anio') {
      const primerDiaAnio = new Date(hoy.getFullYear(), 0, 1);
      this.fechaInicio = formatoFecha(primerDiaAnio);
      this.fechaFin = formatoFecha(hoy);
    } else if (preset === 'todo') {
      this.fechaInicio = '';
      this.fechaFin = '';
    }
    this.consultarEstadisticas();
  }

  onCustomDateChange(): void {
    this.presetActivo = 'personalizado';
    this.consultarEstadisticas();
  }

  consultarEstadisticas(): void {
    this.cargando = true;

    this.estadisticaService.getResumen(
      this.nitFiltro,
      this.fechaInicio,
      this.fechaFin,
      this.usuarioActivo,
      this.rolesUsuario
    ).subscribe({
      next: (res) => {
        this.datos = res;
        this.cargando = false;
        setTimeout(() => {
          this.renderizarGraficas();
        }, 100);
      },
      error: (err) => {
        console.error('Error al cargar estadísticas:', err);
        this.cargando = false;
      }
    });
  }

  limpiarFiltros(): void {
    this.fechaInicio = '';
    this.fechaFin = '';
    this.presetActivo = 'todo';
    if (!this.esPrestador) {
      this.nitFiltro = '';
    }
    this.consultarEstadisticas();
  }

  private destruirGraficas(): void {
    if (this.chartBarras) {
      this.chartBarras.destroy();
      this.chartBarras = undefined;
    }
    if (this.chartTorta) {
      this.chartTorta.destroy();
      this.chartTorta = undefined;
    }
    if (this.chartUsuarios) {
      this.chartUsuarios.destroy();
      this.chartUsuarios = undefined;
    }
  }

  private renderizarGraficas(): void {
    if (!this.datos) return;
    this.destruirGraficas();

    // 1. GRÁFICA DE BARRAS (Ordenada de mayor a menor número de facturas)
    if (this.canvasBarrasRanking && this.datos.porFase && this.datos.porFase.length > 0) {
      const fasesOrdenadas = [...this.datos.porFase].sort((a, b) => b.cantidad - a.cantidad);
      const labels = fasesOrdenadas.map(f => f.faseNombre);
      const cantidades = fasesOrdenadas.map(f => f.cantidad);
      const montos = fasesOrdenadas.map(f => f.montoTotal);

      const ctx = this.canvasBarrasRanking.nativeElement.getContext('2d');
      if (ctx) {
        this.chartBarras = new Chart(ctx, {
          type: 'bar',
          data: {
            labels: labels,
            datasets: [{
              label: 'Cantidad de Facturas',
              data: cantidades,
              backgroundColor: [
                'rgba(29, 166, 186, 0.85)',
                'rgba(37, 99, 235, 0.85)',
                'rgba(2, 132, 199, 0.85)',
                'rgba(124, 58, 237, 0.85)',
                'rgba(217, 119, 6, 0.85)',
                'rgba(71, 85, 105, 0.85)'
              ],
              borderColor: [
                '#1DA6BA',
                '#2563eb',
                '#0284c7',
                '#7c3aed',
                '#d97706',
                '#475569'
              ],
              borderWidth: 1.5,
              borderRadius: 8,
              barPercentage: 0.55
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: false },
              tooltip: {
                backgroundColor: '#0f172a',
                titleFont: { size: 13, weight: 'bold' },
                bodyFont: { size: 12 },
                padding: 12,
                cornerRadius: 8,
                callbacks: {
                  label: (context) => {
                    const idx = context.dataIndex;
                    const cantidad = cantidades[idx];
                    const montoFmt = this.currencyPipe.transform(montos[idx], 'COP', 'symbol-narrow', '1.0-0');
                    return [
                      `📊 Cantidad: ${cantidad} facturas`,
                      `💰 Valor Total: ${montoFmt}`
                    ];
                  }
                }
              }
            },
            scales: {
              x: {
                grid: { display: false },
                ticks: { color: '#334155', font: { size: 11, weight: 600 } }
              },
              y: {
                beginAtZero: true,
                grid: { color: '#f1f5f9' },
                ticks: { color: '#64748b', stepSize: 1 }
              }
            }
          }
        });
      }
    }

    // 2. GRÁFICA DE TORTA / DONA (Con Cantidades y Valores $)
    if (this.canvasTortaEstados && this.datos.porEstado && this.datos.porEstado.length > 0) {
      const labels = this.datos.porEstado.map(e => e.estado);
      const cantidades = this.datos.porEstado.map(e => e.cantidad);
      const totalGeneralFacturas = this.datos.kpis.totalFacturas || 1;
      const montoTotalGeneral = this.datos.kpis.montoTotal || 0;

      const colorPalette: { [key: string]: string } = {
        'APROBADO': '#10b981',
        'PAGADO': '#059669',
        'CAUSADO': '#34d399',
        'VALIDADO': '#22c55e',
        'RADICADO': '#1DA6BA',
        'EN GESTION': '#3b82f6',
        'PROCESANDO': '#60a5fa',
        'RECHAZADO': '#ef4444',
        'ANULADO': '#dc2626',
        'FACTURA NO CONFORME': '#f87171',
        'CON ERRORES': '#b91c1c',
        'IMPUESTOS VERIFICADOS': '#8b5cf6'
      };

      const backgroundColors = labels.map(l => {
        const key = Object.keys(colorPalette).find(k => l.toUpperCase().includes(k));
        return key ? colorPalette[key] : '#94a3b8';
      });

      const ctx = this.canvasTortaEstados.nativeElement.getContext('2d');
      if (ctx) {
        this.chartTorta = new Chart(ctx, {
          type: 'doughnut',
          data: {
            labels: labels,
            datasets: [{
              data: cantidades,
              backgroundColor: backgroundColors,
              borderColor: '#ffffff',
              borderWidth: 2,
              hoverOffset: 6
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '62%',
            plugins: {
              legend: {
                position: 'bottom',
                labels: {
                  boxWidth: 12,
                  boxHeight: 12,
                  color: '#334155',
                  font: { size: 11.5, weight: 600 },
                  padding: 12
                }
              },
              tooltip: {
                backgroundColor: '#0f172a',
                padding: 12,
                cornerRadius: 8,
                callbacks: {
                  label: (context) => {
                    const total = cantidades.reduce((a, b) => a + b, 0);
                    const val = Number(context.raw) || 0;
                    const pct = total > 0 ? ((val * 100) / total).toFixed(1) : '0';
                    const valorProporcional = (montoTotalGeneral * val) / totalGeneralFacturas;
                    const montoFmt = this.currencyPipe.transform(valorProporcional, 'COP', 'symbol-narrow', '1.0-0');

                    return [
                      `📌 ${context.label}: ${val} facturas (${pct}%)`,
                      `💰 Valor aprox: ${montoFmt}`
                    ];
                  }
                }
              }
            }
          }
        });
      }
    }

    // 3. GRÁFICA DE USUARIOS Y GESTIONES (Exclusivo Administrador)
    if (this.canvasUsuarios && this.datos.gestionesPorUsuario && this.datos.gestionesPorUsuario.length > 0) {
      const labels = this.datos.gestionesPorUsuario.map(u => u.usuario);
      const aprobadas = this.datos.gestionesPorUsuario.map(u => u.aprobadas);
      const rechazadas = this.datos.gestionesPorUsuario.map(u => u.rechazadas);
      const totales = this.datos.gestionesPorUsuario.map(u => u.totalGestiones);
      const efectividades = this.datos.gestionesPorUsuario.map(u => u.porcentajeEfectividad);

      const ctx = this.canvasUsuarios.nativeElement.getContext('2d');
      if (ctx) {
        this.chartUsuarios = new Chart(ctx, {
          type: 'bar',
          data: {
            labels: labels,
            datasets: [
              {
                label: 'Facturas Aprobadas',
                data: aprobadas,
                backgroundColor: 'rgba(16, 185, 129, 0.85)',
                borderColor: '#10b981',
                borderWidth: 1.5,
                borderRadius: 6
              },
              {
                label: 'Facturas Rechazadas / Devueltas',
                data: rechazadas,
                backgroundColor: 'rgba(239, 68, 68, 0.85)',
                borderColor: '#ef4444',
                borderWidth: 1.5,
                borderRadius: 6
              }
            ]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                position: 'top',
                labels: {
                  color: '#334155',
                  font: { size: 12, weight: 600 },
                  padding: 10
                }
              },
              tooltip: {
                backgroundColor: '#0f172a',
                padding: 12,
                cornerRadius: 8,
                callbacks: {
                  afterBody: (context) => {
                    const idx = context[0].dataIndex;
                    const tot = totales[idx];
                    const ef = efectividades[idx];
                    return `--------------------\nTotal Gestiones: ${tot}\nEfectividad: ${ef}%`;
                  }
                }
              }
            },
            scales: {
              x: {
                stacked: false,
                grid: { display: false },
                ticks: { color: '#334155', font: { size: 11, weight: 600 } }
              },
              y: {
                stacked: false,
                beginAtZero: true,
                grid: { color: '#f1f5f9' },
                ticks: { color: '#64748b', stepSize: 1 }
              }
            }
          }
        });
      }
    }
  }

  obtenerClaseFase(faseId: number): string {
    switch (faseId) {
      case 1: return 'fase-1';
      case 2: return 'fase-2';
      case 3: return 'fase-3';
      case 4: return 'fase-4';
      case 5: return 'fase-5';
      default: return 'fase-default';
    }
  }

  obtenerClaseEstado(estado: string): string {
    const st = (estado || '').toUpperCase();
    if (st.includes('APROBADO') || st.includes('PAGADO') || st.includes('CAUSADO') || st.includes('VALIDADO') || st.includes('ACTIVO')) {
      return 'badge-verde';
    }
    if (st.includes('RECHAZADO') || st.includes('ANULADO') || st.includes('ERROR') || st.includes('DEVUELT') || st.includes('NO CONFORME') || st.includes('INACTIVO')) {
      return 'badge-rojo';
    }
    if (st.includes('PROCESANDO') || st.includes('GESTION') || st.includes('TRAMITE') || st.includes('RADICADO')) {
      return 'badge-azul';
    }
    return 'badge-gris';
  }

  /**
   * 🏛️ Limpia los filtros de la sección de Información Exógena
   */
  limpiarFiltrosExogena(): void {
    this.anioGravableExogena = null;
    this.fechaExogenaDesde = '';
    this.fechaExogenaHasta = '';
  }

  /**
   * 🏛️ Selecciona el formato exógena activo para visualización o descarga
   */
  seleccionarFormatoExogena(formatoId: '1001' | '1003' | '1005' | '1006' | '1007' | 'consolidado'): void {
    this.formatoExogenaSeleccionado = formatoId;
  }

  /**
   * 🏛️ Al hacer clic en cualquier cuadro, lo selecciona y descarga automáticamente con los filtros aplicados
   */
  clickDescargarCuadroExogena(formatoId: '1001' | '1003' | '1005' | '1006' | '1007' | 'consolidado'): void {
    this.formatoExogenaSeleccionado = formatoId;
    this.descargarFormatoExogena(formatoId);
  }

  /**
   * 🏛️ Descarga el archivo Excel estructurado para el Formato DIAN seleccionado o especificado
   */
  descargarFormatoExogena(formatoId?: '1001' | '1003' | '1005' | '1006' | '1007' | 'consolidado'): void {
    if (this.descargandoExogena) return;

    const targetFormato = formatoId || this.formatoExogenaSeleccionado;

    if (!this.anioGravableExogena && !this.fechaExogenaDesde && !this.fechaExogenaHasta) {
      this.alertService.advertencia(
        `Por favor seleccione un Año Gravable o ingrese un rango de fechas para generar el Formato ${targetFormato}.`,
        'Filtro requerido'
      );
      return;
    }

    this.descargandoExogena = true;
    this.formatoEnDescarga = targetFormato;
    const descPeriodo = this.anioGravableExogena
      ? `año gravable ${this.anioGravableExogena}`
      : 'periodo de fechas seleccionado';

    const info = this.formatosExogena.find(f => f.id === targetFormato) || this.formatosExogena[0];

    this.alertService.cargando(
      `Generando ${info.codigo} (${info.titulo}) para el ${descPeriodo}...`,
      'Generando Información Exógena DIAN'
    );

    const filtro: any = {};
    if (this.fechaExogenaDesde) {
      filtro.fechaEmisionDesde = this.fechaExogenaDesde;
    }
    if (this.fechaExogenaHasta) {
      filtro.fechaEmisionHasta = this.fechaExogenaHasta;
    }
    if (this.esPrestador && this.nitFiltro) {
      filtro.nit = this.nitFiltro;
    }

    this.facturaService.exportarFormatoExogenaExcel(
      targetFormato,
      this.nitFiltro || undefined,
      this.rolesUsuario,
      this.anioGravableExogena || undefined,
      filtro
    ).subscribe({
      next: (blob: Blob) => {
        this.descargandoExogena = false;
        this.formatoEnDescarga = null;
        this.alertService.cerrar();

        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const anioNom = this.anioGravableExogena ? this.anioGravableExogena : new Date().getFullYear();

        let nombreArchivo = `formato_${targetFormato}_dian_${anioNom}.xlsx`;
        if (targetFormato === '1005') {
          nombreArchivo = `formato_1005_iva_descontable_${anioNom}.xlsx`;
        } else if (targetFormato === '1006') {
          nombreArchivo = `formato_1006_iva_generado_${anioNom}.xlsx`;
        } else if (targetFormato === '1007') {
          nombreArchivo = `formato_1007_ingresos_${anioNom}.xlsx`;
        } else if (targetFormato === 'consolidado') {
          nombreArchivo = `exogena_dian_consolidado_${anioNom}.xlsx`;
        }

        a.download = nombreArchivo;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        this.alertService.exito(
          `${info.codigo} DIAN (${descPeriodo}) descargado exitosamente.`,
          'Descarga Completa'
        );
      },
      error: (err) => {
        this.descargandoExogena = false;
        this.formatoEnDescarga = null;
        this.alertService.cerrar();
        console.error(`Error al generar ${info.codigo}:`, err);
        this.alertService.error(
          `Ocurrió un error al intentar generar el archivo Excel de ${info.codigo}.`,
          'Error en la descarga'
        );
      }
    });
  }

  /**
   * 🏛️ Descarga directa del Formato 1001 (mantiene compatibilidad)
   */
  descargarFormato1001(): void {
    this.descargarFormatoExogena('1001');
  }
}
