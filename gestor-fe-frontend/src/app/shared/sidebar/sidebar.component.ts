import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit{

  @Input() collapsed: boolean = false;

  username: any;
  roles: string[] = [];
  isLoggedIn: boolean = false;

  menuNav: MenuItem[] = [
    {
      name: "Home",
      icon: "home",
      route: "/dashboard/home"
    },
    {
      name: "Administración",
      icon: "settings",
      expanded: false,
      children: [
        /*
        {
          name: "Tipo identificación",
          icon: "badge",
          route: "/dashboard/admin/tipo-identificacion"
        },*/
        {
          name: "Estado",
          icon: "check_circle",
          route: "/dashboard/admin/estado"
        },
        {
          name: "Tipo",
          icon: "category",
          route: "/dashboard/admin/tipo"
        },
        {
          name: "Extensión",
          icon: "extension",
          route: "/dashboard/admin/extension"
        },
        {
          name: "Clasificación",
          icon: "class",
          route: "/dashboard/admin/clasificacion"
        },
        {
          name: "Fase",
          icon: "schema",
          route: "/dashboard/admin/fase"
        }
      ]
    },
    {
      name: "Gestión",
      icon: "badge",
      expanded: false,
      children: [
        {
          name: "Cargue soportes",
          icon: "upload",
          route: "/dashboard/cargue"
        },
        {
          name: "Faturas",
          icon: "receipt_long",
          route: "/dashboard/factura"
        }
      ]
    }
  ];


  constructor(){}

  async ngOnInit(): Promise<void> {}

  toggleSidebar(): void {
    this.collapsed = !this.collapsed;
  }

  toggleMenu(item: MenuItem): void {
    item.expanded = !item.expanded;
  }

}

interface MenuItem {
  name: string;
  icon?: string;
  route?: string;
  roles?: string[];
  expanded?: boolean;
  children?: MenuItem[];
}
