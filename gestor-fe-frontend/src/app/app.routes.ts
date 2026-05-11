import { Routes } from '@angular/router';
import { DashboardComponent } from './layouts/dashboard/dashboard.component';
import { HomeComponent } from './components/home/home.component';
import { CargueComponent } from './components/cargue/cargue.component';
import { TipoIdentificacionComponent } from './components/admin/tipo-identificacion/tipo-identificacion.component';
import { AdminComponent } from './components/admin/admin.component';


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
          }

        ]
      },

      {
        path: 'cargue',
        component: CargueComponent
      }

    ]
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }

];
