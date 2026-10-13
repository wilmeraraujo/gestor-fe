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

  ngOnInit(): void {
    this.cargarSesion();
    this.consultarEstadisticas();
  }

  ngOnDestroy(): void {
    this.destruirGraficas();
  }

  private cargarSesion(): void {
    this.usuarioActivo = this.loginService.getUserName();
    this.rolesUsuario = this.loginService.getUserRoles() || [];

    this.isAdmin = this.loginService.isAdmin || this.loginService.isGAdmin;

    this.esAdminOGestor = this.isAdmin ||
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
}
