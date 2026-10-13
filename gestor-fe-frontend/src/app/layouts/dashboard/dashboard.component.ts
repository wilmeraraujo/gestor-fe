import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, inject } from '@angular/core';
import { HeaderComponent } from '../../shared/header/header.component';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { FooterComponent } from '../../shared/footer/footer.component';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, HeaderComponent, SidebarComponent, FooterComponent, RouterOutlet],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  sidebarCollapsed = false;
  sidebarHovered = false;
  isMobile = false;

  private router = inject(Router);

  ngOnInit(): void {
    this.checkScreenSize();

    // Auto-cerrar sidebar en navegación móvil
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      if (this.isMobile) {
        this.sidebarCollapsed = true;
      }
    });
  }

  @HostListener('window:resize')
  onResize(): void {
    this.checkScreenSize();
  }

  private checkScreenSize(): void {
    const wasMobile = this.isMobile;
    this.isMobile = window.innerWidth <= 992;
    if (this.isMobile && !wasMobile) {
      this.sidebarCollapsed = true;
    } else if (!this.isMobile && wasMobile) {
      this.sidebarCollapsed = false;
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
    if (!this.sidebarCollapsed) {
      this.sidebarHovered = false;
    }
  }

  closeSidebarMobile(): void {
    if (this.isMobile) {
      this.sidebarCollapsed = true;
    }
  }

  onSidebarHover(isHovered: boolean): void {
    if (!this.isMobile) {
      this.sidebarHovered = isHovered;
    }
  }

}

