import {
  Component,
  Input,
  HostListener
} from '@angular/core';

import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-user-menu',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-menu.component.html',
  styleUrl: './user-menu.component.css'
})
export class UserMenuComponent {

  @Input() user = '';

  @Input() email = '';

  isOpen = false;

  toggleMenu(): void {

    this.isOpen = !this.isOpen;

  }

  cerrarSesion(): void {

    console.log('Cerrar sesión');

  }

  perfil(): void {

    console.log('Ir a perfil');

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
