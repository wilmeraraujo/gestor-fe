import { Routes } from '@angular/router';
import { DashboardComponent } from './layouts/dashboard/dashboard.component';
import { HomeComponent } from './components/home/home.component';
import { CargueComponent } from './components/cargue/cargue.component';
import { TipoIdentificacionComponent } from './components/admin/tipo-identificacion/tipo-identificacion.component';
import { AdminComponent } from './components/admin/admin.component';
import { EstadoComponent } from './components/admin/estado/estado.component';
import { TipoComponent } from './components/admin/tipo/tipo.component';
import { ClasificacionComponent } from './components/admin/clasificacion/clasificacion.component';
import { ExtensionComponent } from './components/admin/extension/extension.component';
import { FaseComponent } from './components/admin/fase/fase.component';
import { FacturaComponent } from './components/factura/factura.component';
import { DocumentoComponent } from './components/documento/documento.component';
import { PrestadorComponent } from './components/prestador/prestador.component';
import { CausalDevolucionComponent } from './components/admin/causal-devolucion/causal-devolucion.component';
import { ObservacionComponent } from './components/admin/observacion/observacion.component';


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
            component: ExtensionComponent
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
          }

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
        path: 'factura',
        component: FacturaComponent
      },
      {
        path: 'documento',
        component: DocumentoComponent
      }

    ]
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }

];
