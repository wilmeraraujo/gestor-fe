import { Routes } from '@angular/router';
import { DashboardComponent } from './layouts/dashboard/dashboard.component';
import { HomeComponent } from './components/home/home.component';
import { AdminComponent } from './components/admin/admin.component';
import { CargueComponent } from './components/cargue/cargue.component';

export const routes: Routes = [

  // 🔒 Dashboard privado
  {
    path: 'dashboard',
    component: DashboardComponent,
    //canActivate: [authGuard],
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
        path: 'administracion',
        component: AdminComponent
      },
      {
        path: 'cargue',
        component: CargueComponent
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }

];

