import { Routes } from '@angular/router';
import { DashboardComponent } from './layouts/dashboard/dashboard.component';
import { HomeComponent } from './components/home/home.component';
import { CargueComponent } from './components/cargue/cargue.component';
import { AdminComponent } from './components/admin/admin.component';
import { EstadoComponent } from './components/admin/estado/estado.component';
import { TipoComponent } from './components/admin/tipo/tipo.component';
import { ClasificacionComponent } from './components/admin/clasificacion/clasificacion.component';
import { ExtensionComponent } from './components/admin/extension/extension.component';
import { FaseComponent } from './components/admin/fase/fase.component';
import { GestionInicialComponent } from './components/gestion-inicial/gestion-inicial.component';
import { DocumentoComponent } from './components/documento/documento.component';
import { PrestadorComponent } from './components/prestador/prestador.component';
import { CausalDevolucionComponent } from './components/admin/causal-devolucion/causal-devolucion.component';
import { ObservacionComponent } from './components/admin/observacion/observacion.component';
import { ReconocimientoContableComponent } from './components/reconocimiento-contable/reconocimiento-contable.component';
import { ImpuestosComponent } from './components/impuestos/impuestos.component';
import { PendienteDePagoComponent } from './components/pendiente-de-pago/pendiente-de-pago.component';
import { SeguimientoFacturasComponent } from './components/seguimiento-facturas/seguimiento-facturas.component';
import { ConfiguracionSistemaComponent } from './components/admin/configuracion-sistema/configuracion-sistema.component';
import { ConfiguracionFaseExtensionComponent } from './components/admin/configuracion-fase-extension/configuracion-fase-extension.component';
import { ProcesoComponent } from './components/admin/proceso/proceso.component';
import { EstadisticaComponent } from './components/estadistica/estadistica.component';
import { MovimientoComponent } from './components/admin/movimiento/movimiento.component';
import { MedioPagoComponent } from './components/admin/dian/medio-pago/medio-pago.component';
import { TipoDocumentoDianComponent } from './components/admin/dian/tipo-documento-dian/tipo-documento-dian.component';
import { UnidadMedidaComponent } from './components/admin/dian/unidad-medida/unidad-medida.component';
import { ResponsabilidadFiscalComponent } from './components/admin/dian/responsabilidad-fiscal/responsabilidad-fiscal.component';
import { TipoIdentificacionComponent } from './components/admin/dian/tipo-identificacion/tipo-identificacion.component';
import { TipoOperacionComponent } from './components/admin/dian/tipo-operacion/tipo-operacion.component';
import { DepartamentoComponent } from './components/admin/ubicacion/departamento/departamento.component';
import { MunicipioComponent } from './components/admin/ubicacion/municipio/municipio.component';

export const routes: Routes = [

  {
    path: 'dashboard',
    component: DashboardComponent,
    children: [

      {
        path: '',
        redirectTo: 'home',
        pathMatch: 'full'
      },

      {
        path: 'home',
        component: HomeComponent
      },

      {
        path: 'admin',
        children: [

          {
            path: '',
            redirectTo: 'admin',
            pathMatch: 'full',
          },

          {
            path: 'admin',
            component: AdminComponent
          },
          {
            path: 'tipo-identificacion',
            component: TipoIdentificacionComponent
          },
          {
            path: 'extension',
            component: ExtensionComponent
          },
          {
            path: 'tipo',
            component: TipoComponent
          },
          {
            path: 'clasificacion',
            component: ClasificacionComponent
          },
          {
            path: 'fase',
            component: FaseComponent
          },
          {
            path: 'estado',
            component: EstadoComponent
          },
          {
            path: 'causal-devolucion',
            component: CausalDevolucionComponent
          },
          {
            path: 'observacion',
            component: ObservacionComponent
          },
          { path: 'proceso', component: ProcesoComponent },
          { path: 'movimiento', component: MovimientoComponent },
          { path: 'configuracion-sistema', component: ConfiguracionSistemaComponent },
          { path: 'configuracion-fase-extension', component: ConfiguracionFaseExtensionComponent },

          // 🏛️ Catálogos DIAN
          { path: 'dian/tipo-identificacion', component: TipoIdentificacionComponent },
          { path: 'dian/tipo-operacion', component: TipoOperacionComponent },
          { path: 'dian/medio-pago', component: MedioPagoComponent },
          { path: 'dian/tipo-documento-dian', component: TipoDocumentoDianComponent },
          { path: 'dian/unidad-medida', component: UnidadMedidaComponent },
          { path: 'dian/responsabilidad-fiscal', component: ResponsabilidadFiscalComponent },

          // 🗺️ Catálogos Ubicación
          { path: 'ubicacion/departamento', component: DepartamentoComponent },
          { path: 'ubicacion/municipio', component: MunicipioComponent }
        ]
      },

      {
        path: 'prestador',
        component: PrestadorComponent
      },
      {
        path: 'cargue',
        component: CargueComponent
      },
      {
        path: 'gestion-inicial',
        component: GestionInicialComponent
      },
      {
        path: 'reconocimiento-contable',
        component: ReconocimientoContableComponent
      },
      {
        path: 'impuestos',
        component: ImpuestosComponent
      },
      {
        path: 'pendiente-pago',
        component: PendienteDePagoComponent
      },
      {
        path: 'seguimiento-factura',
        component: SeguimientoFacturasComponent
      },
      {
        path: 'documento',
        component: DocumentoComponent
      },
      {
        path: 'estadistica',
        component: EstadisticaComponent
      }

    ]
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }

];
