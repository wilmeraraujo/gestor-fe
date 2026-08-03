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
export class SidebarComponent implements OnInit {

  @Input() collapsed: boolean = false;

  // 🚀 Estado temporal para el efecto Hover cuando está contraído
  isHovered: boolean = false;

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
          name: "Estado",
          icon: "check_circle",
          route: "/dashboard/admin/estado"
        },
        {
          name: "Observación",
          icon: "comment",
          route: "/dashboard/admin/observacion"
        },
        {
          name: "Causal devolución",
          icon: "assignment_return",
          route: "/dashboard/admin/causal-devolucion"
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
          name: "Soportes prestador",
          icon: "domain_add",
          route: "/dashboard/prestador"
        },
        {
          name: "Soportes FE",
          icon: "cloud_upload",
          route: "/dashboard/cargue"
        },
        {
          name: "Gestión inicial",
          icon: "receipt_long",
          route: "/dashboard/gestion-inicial"
        },
        {
          name: "Reconocimiento contable",
          icon: "account_balance",
          route: "/dashboard/reconocimiento-contable"
        },
        {
          name: "Impuestos",
          icon: "request_quote",
          route: "/dashboard/impuestos"
        },
        {
          name: "Pendiente de pago",
          icon: "paid",
          route: "/dashboard/pendiente-pago"
        },
        {
          name: "Seguimiento de facturas",
          icon: "alt_route",
          route: "/dashboard/seguimiento-factura"
        },
        {
          name: "Reportes",
          icon: "analytics",
          route: "/dashboard/documento"
        }
      ]
    }
  ];

  constructor() {}

  async ngOnInit(): Promise<void> {}

  toggleSidebar(): void {
    this.collapsed = !this.collapsed;
  }

  toggleMenu(item: MenuItem): void {
    item.expanded = !item.expanded;
  }

  // 🚀 Eventos para controlar cuando el ratón entra o sale del Sidebar
  onMouseEnter(): void {
    if (this.collapsed) {
      this.isHovered = true;
    }
  }

  onMouseLeave(): void {
    if (this.collapsed) {
      this.isHovered = false;
    }
  }

  // 🚀 Retorna true si el sidebar está completamente visible (ya sea por toggle o por hover)
  get isExpanded(): boolean {
    return !this.collapsed || this.isHovered;
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
