import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LoginService } from '../../services/login.service'; // 👈 Inyección de LoginService

interface MenuItem {
  name: string;
  icon?: string;
  route?: string;
  expanded?: boolean;
  visible?: boolean;
  children?: MenuItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit {

  @Input() collapsed: boolean = false;

  // 🚀 Servicio de Autenticación
  private loginService = inject(LoginService);

  // 🚀 Estado temporal para el efecto Hover cuando está contraído
  isHovered: boolean = false;

  username: string = '';
  menuNav: MenuItem[] = [];

  ngOnInit(): void {
    this.username = this.loginService.getUserName();
    this.construirMenuSegunRoles();
  }

  /**
   * 🔒 Filtra y construye dinámicamente el menú según los roles de Keycloak
   */
  private construirMenuSegunRoles(): void {
    // Forzamos la evaluación de roles en LoginService
    this.loginService.getUserRoles();

    const isAdminOGAdmin = this.loginService.isAdmin || this.loginService.isGAdmin;

    // 1. HOME (Visible para todos)
    const itemHome: MenuItem = {
      name: "Home",
      icon: "home",
      route: "/dashboard/home",
      visible: true
    };

    // 2. ADMINISTRACIÓN (Solo admin y gestor-fe-admin)
    const itemAdmin: MenuItem = {
      name: "Administración",
      icon: "settings",
      expanded: false,
      visible: isAdminOGAdmin,
      children: [
        { name: "Estado", icon: "check_circle", route: "/dashboard/admin/estado", visible: true },
        { name: "Observación", icon: "comment", route: "/dashboard/admin/observacion", visible: true },
        { name: "Causal devolución", icon: "assignment_return", route: "/dashboard/admin/causal-devolucion", visible: true },
        { name: "Tipo", icon: "category", route: "/dashboard/admin/tipo", visible: true },
        { name: "Extensión", icon: "extension", route: "/dashboard/admin/extension", visible: true },
        { name: "Clasificación", icon: "class", route: "/dashboard/admin/clasificacion", visible: true },
        { name: "Fase", icon: "schema", route: "/dashboard/admin/fase", visible: true }
      ]
    };

    // 3. CARGUE DE SOPORTES
    const itemCargue: MenuItem = {
      name: "Cargue soportes",
      icon: "cloud_upload",
      expanded: false,
      visible: true, // El grupo es visible porque 'Prestador' aplica a todos
      children: [
        {
          name: "Prestador",
          icon: "domain_add",
          route: "/dashboard/prestador",
          visible: true // 👈 Todos los roles pueden ingresar a cargar sus soportes de prestador
        },
        {
          name: "Facturas",
          icon: "upload_file",
          route: "/dashboard/cargue",
          visible: isAdminOGAdmin || this.loginService.isPrestador || this.loginService.isGCargue // 👈 Admin, GAdmin o Gestor Cargue
        }
      ]
    };

    // 4. GESTIÓN SEGÚN LA FASE Y EL ROL
    const itemGestion: MenuItem = {
      name: "Gestión",
      icon: "badge",
      expanded: false,
      visible: true,
      children: [
        {
          name: "Gestión inicial",
          icon: "receipt_long",
          route: "/dashboard/gestion-inicial",
          visible: isAdminOGAdmin || this.loginService.isGFaseUno // 👈 Fase 1
        },
        {
          name: "Reconocimiento contable",
          icon: "account_balance",
          route: "/dashboard/reconocimiento-contable",
          visible: isAdminOGAdmin || this.loginService.isGFaseDos // 👈 Fase 2
        },
        {
          name: "Impuestos",
          icon: "request_quote",
          route: "/dashboard/impuestos",
          visible: isAdminOGAdmin || this.loginService.isGFaseTres // 👈 Fase 3
        },
        {
          name: "Pendiente de pago",
          icon: "paid",
          route: "/dashboard/pendiente-pago",
          visible: isAdminOGAdmin || this.loginService.isGFaseCuatro // 👈 Fase 4
        },
        {
          name: "Seguimiento de facturas",
          icon: "alt_route",
          route: "/dashboard/seguimiento-factura",
          visible: true // 👈 Todos los roles ven la trazabilidad (Prestador ve sus facturas, Admin/Gestor ven todas)
        },
        {
          name: "Reportes",
          icon: "analytics",
          route: "/dashboard/documento",
          visible: isAdminOGAdmin || this.loginService.isGFaseCinco // 👈 Fase 5 / Reportes
        }
      ]
    };

    // Mapear solo los elementos y submódulos que tengan 'visible: true'
    this.menuNav = [itemHome, itemAdmin, itemCargue, itemGestion]
      .filter(item => item.visible)
      .map(item => {
        if (item.children) {
          item.children = item.children.filter(child => child.visible);
        }
        return item;
      })
      .filter(item => !item.children || item.children.length > 0); // Oculta la categoría si se queda sin hijos
  }

  toggleSidebar(): void {
    this.collapsed = !this.collapsed;
  }

  toggleMenu(item: MenuItem): void {
    item.expanded = !item.expanded;
  }

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

  get isExpanded(): boolean {
    return !this.collapsed || this.isHovered;
  }
}