import {
  Component,
  Input,
  HostListener,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoginService } from '../../../services/login.service';

@Component({
  selector: 'app-user-menu',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-menu.component.html',
  styleUrl: './user-menu.component.css'
})
export class UserMenuComponent {
  // 💉 Inyección del servicio de Login/Keycloak
  private loginService = inject(LoginService);

  @Input() user = '';
  @Input() email = '';

  isOpen = false;

  toggleMenu(): void {
    this.isOpen = !this.isOpen;
  }

  cerrarSesion(): void {
    this.isOpen = false;
    this.loginService.logout(); // 🚀 Llama al logout de Keycloak
  }

  perfil(): void {
    console.log('Ir a perfil');
    this.isOpen = false;
  }

  /*
  ==========================================
  CERRAR AL DAR CLICK AFUERA
  ==========================================
  */
  @HostListener('document:click')
  closeMenu(): void {
    this.isOpen = false;
  }

  stopPropagation(event: Event): void {
    event.stopPropagation();
  }
}