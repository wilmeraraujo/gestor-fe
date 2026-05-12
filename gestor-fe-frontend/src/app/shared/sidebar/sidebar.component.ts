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
        {
          name: "Tipo identificación",
          icon: "badge",
          route: "/dashboard/admin/tipo-identificacion"
        },
        {
          name: "Estado",
          icon: "badge",
          route: "/dashboard/admin/estado"
        }
      ]
    },
    {
      name: "Cargue",
      icon: "upload",
      route: "/dashboard/cargue" }
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
