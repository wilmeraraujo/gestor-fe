import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
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
  @Output() hoverChange = new EventEmitter<boolean>();

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
        {
          name: "DIAN",
          icon: "receipt_long",
          expanded: false,
          visible: true,
          children: [
            { name: "Tipos de identificación", icon: "badge", route: "/dashboard/admin/dian/tipo-identificacion", visible: true },
            { name: "Tipos de operación", icon: "swap_horiz", route: "/dashboard/admin/dian/tipo-operacion", visible: true },
            { name: "Tipos de documento", icon: "description", route: "/dashboard/admin/dian/tipo-documento-dian", visible: true },
            { name: "Medios de pago", icon: "payments", route: "/dashboard/admin/dian/medio-pago", visible: true },
            { name: "Unidades de medida", icon: "straighten", route: "/dashboard/admin/dian/unidad-medida", visible: true },
            { name: "Responsabilidad fiscal", icon: "gavel", route: "/dashboard/admin/dian/responsabilidad-fiscal", visible: true }
          ]
        },
        {
          name: "Ubicación",
          icon: "place",
          expanded: false,
          visible: true,
          children: [
            { name: "Departamentos", icon: "map", route: "/dashboard/admin/ubicacion/departamento", visible: true },
            { name: "Municipios", icon: "location_city", route: "/dashboard/admin/ubicacion/municipio", visible: true }
          ]
        },
        {
          name: "Configuración",
          icon: "tune",
          expanded: false,
          visible: true,
          children: [
            { name: "Sistema", icon: "settings_suggest", route: "/dashboard/admin/configuracion-sistema", visible: true },
            { name: "Fase / Extensión", icon: "rule", route: "/dashboard/admin/configuracion-fase-extension", visible: true }
          ]
        },
        { name: "Observación", icon: "comment", route: "/dashboard/admin/observacion", visible: true },
        { name: "Causal devolución", icon: "assignment_return", route: "/dashboard/admin/causal-devolucion", visible: true },
        { name: "Tipo", icon: "category", route: "/dashboard/admin/tipo", visible: true },
        { name: "Extensión", icon: "extension", route: "/dashboard/admin/extension", visible: true },
        { name: "Concepto", icon: "receipt", route: "/dashboard/admin/concepto", visible: true },
        { name: "Prestador", icon: "domain", route: "/dashboard/admin/prestador", visible: true },
        { name: "Clasificación", icon: "class", route: "/dashboard/admin/clasificacion", visible: true },
        { name: "Fase", icon: "schema", route: "/dashboard/admin/fase", visible: true },
        { name: "Proceso", icon: "account_tree", route: "/dashboard/admin/proceso", visible: true },
        { name: "Movimiento", icon: "sync_alt", route: "/dashboard/admin/movimiento", visible: true }
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
        },
        {
          name: "Estadísticas",
          icon: "insights",
          route: "/dashboard/estadistica",
          visible: true
        }
      ]
    };

    this.menuNav = [itemHome, itemAdmin, itemCargue, itemGestion]
      .filter(item => item.visible)
      .map(item => {
        if (item.children) {
          item.children = item.children
            .filter(child => child.visible)
            .map(child => {
              if (child.children) {
                child.children = child.children.filter(subChild => subChild.visible);
              }
              return child;
            })
            .filter(child => !child.children || child.children.length > 0);
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
   * Al alternar un menú con submenús, primero contrae todos los demás módulos y submódulos.
   */
  toggleMenu(targetItem: MenuItem): void {
    const estaExpandido = targetItem.expanded;

    // 1. Contraer todos los grupos de menú y sus subgrupos
    this.menuNav.forEach(item => {
      if (item.children) {
        item.expanded = false;
        item.children.forEach(child => {
          if (child.children) {
            child.expanded = false;
          }
        });
      }
    });

    // 2. Si el que presionamos estaba cerrado, lo abrimos
    targetItem.expanded = !estaExpandido;
  }

  /**
   * 📂 EFECTO ACORDEÓN PARA SUBGRUPOS (ej: DIAN, Ubicación, Configuración):
   * Contrae los demás subgrupos hermanos y abre/cierra el seleccionado.
   */
  toggleSubMenu(parentItem: MenuItem, targetChild: MenuItem, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    const estaExpandido = targetChild.expanded;

    // Contraer todos los subgrupos hermanos dentro del mismo módulo
    if (parentItem && parentItem.children) {
      parentItem.children.forEach(child => {
        if (child.children) {
          child.expanded = false;
        }
      });
    }

    targetChild.expanded = !estaExpandido;
  }

  /**
   * 🏠 Clic en ítems directos (ej: Home):
   * Cierra todos los submenús abiertos al navegar.
   */
  onDirectItemClick(): void {
    this.menuNav.forEach(item => {
      if (item.children) {
        item.expanded = false;
        item.children.forEach(child => {
          if (child.children) {
            child.expanded = false;
          }
        });
      }
    });
  }

  onMouseEnter(): void {
    if (this.collapsed) {
      this.isHovered = true;
      this.hoverChange.emit(true);
    }
  }

  onMouseLeave(): void {
    if (this.collapsed) {
      this.isHovered = false;
      this.hoverChange.emit(false);
    }
  }

  get isExpanded(): boolean {
    return !this.collapsed || this.isHovered;
  }
}
