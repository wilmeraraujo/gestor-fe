import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LoginService } from '../../services/login.service';

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

  private loginService = inject(LoginService);

  isHovered: boolean = false;
  username: string = '';
  menuNav: MenuItem[] = [];

  ngOnInit(): void {
    this.username = this.loginService.getUserName();
    this.construirMenuSegunRoles();
  }

  private construirMenuSegunRoles(): void {
    this.loginService.getUserRoles();

    const isAdminOGAdmin = this.loginService.isAdmin || this.loginService.isGAdmin;

    // 1. HOME
    const itemHome: MenuItem = {
      name: "Home",
      icon: "home",
      route: "/dashboard/home",
      visible: true
    };

    // 2. ADMINISTRACIÓN
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
        { name: "Fase", icon: "schema", route: "/dashboard/admin/fase", visible: true },
        { name: "Configuración Sistema", icon: "tune", route: "/dashboard/admin/configuracion-sistema", visible: true },
        { name: "Configuración Fase/Extensión", icon: "rule", route: "/dashboard/admin/configuracion-fase-extension", visible: true }
      ]
    };

    // 3. CARGUE DE SOPORTES
    const itemCargue: MenuItem = {
      name: "Cargue soportes",
      icon: "cloud_upload",
      expanded: false,
      visible: true,
      children: [
        {
          name: "Prestador",
          icon: "domain_add",
          route: "/dashboard/prestador",
          visible: true
        },
        {
          name: "Facturas",
          icon: "upload_file",
          route: "/dashboard/cargue",
          visible: isAdminOGAdmin || this.loginService.isPrestador || this.loginService.isGCargue
        }
      ]
    };

    // 4. GESTIÓN
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
          visible: isAdminOGAdmin || this.loginService.isGFaseUno
        },
        {
          name: "Reconocimiento contable",
          icon: "account_balance",
          route: "/dashboard/reconocimiento-contable",
          visible: isAdminOGAdmin || this.loginService.isGFaseDos
        },
        {
          name: "Impuestos",
          icon: "request_quote",
          route: "/dashboard/impuestos",
          visible: isAdminOGAdmin || this.loginService.isGFaseTres
        },
        {
          name: "Pendiente de pago",
          icon: "paid",
          route: "/dashboard/pendiente-pago",
          visible: isAdminOGAdmin || this.loginService.isGFaseCuatro
        },
        {
          name: "Seguimiento de facturas",
          icon: "alt_route",
          route: "/dashboard/seguimiento-factura",
          visible: true
        },
        {
          name: "Reportes",
          icon: "analytics",
          route: "/dashboard/documento",
          visible: isAdminOGAdmin || this.loginService.isGFaseCinco
        }
      ]
    };

    this.menuNav = [itemHome, itemAdmin, itemCargue, itemGestion]
      .filter(item => item.visible)
      .map(item => {
        if (item.children) {
          item.children = item.children.filter(child => child.visible);
        }
        return item;
      })
      .filter(item => !item.children || item.children.length > 0);
  }

  toggleSidebar(): void {
    this.collapsed = !this.collapsed;
  }

  /**
   * 🔄 EFECTO ACORDEÓN:
   * Al alternar un menú con submenús, primero contrae todos los demás.
   */
  toggleMenu(targetItem: MenuItem): void {
    const estaExpandido = targetItem.expanded;

    // 1. Contraer todos los grupos de menú
    this.menuNav.forEach(item => {
      if (item.children) {
        item.expanded = false;
      }
    });

    // 2. Si el que presionamos estaba cerrado, lo abrimos
    targetItem.expanded = !estaExpandido;
  }

  /**
   * 🏠 Clic en ítems directos (ej: Home):
   * Cierra todos los submenús abiertos al navegar.
   */
  onDirectItemClick(): void {
    this.menuNav.forEach(item => {
      if (item.children) {
        item.expanded = false;
      }
    });
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
