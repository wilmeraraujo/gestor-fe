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
    { name: "Home", route: "/dashboard/home" },
    { name: "Administración", route: "/dashboard/admin" },
  ];

  toggleSidebar(): void {
    this.collapsed = !this.collapsed;
  }

  constructor(){}

  async ngOnInit(): Promise<void> {}
}

interface MenuItem {
  name: string;
  route?: string;
  roles?: string[];
  children?: MenuItem[];
}
