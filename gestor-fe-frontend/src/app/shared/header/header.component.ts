import { Component, EventEmitter, Output, OnInit, inject } from '@angular/core';
import { UserMenuComponent } from '../components/user-menu/user-menu.component';
import { LoginService } from '../../services/login.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [UserMenuComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent implements OnInit {

  @Output() toggle = new EventEmitter<void>();

  private loginService = inject(LoginService);

  user: string = '';
  role: string = '';
  email: string = '';

  ngOnInit(): void {
    this.cargarDatosUsuario();
  }

  private cargarDatosUsuario(): void {
    // 1. Obtener usuario (preferred_username)
    this.user = this.loginService.getUserName();

    // 2. Obtener rol legible (ADMIN / PRESTADOR)
    this.role = this.loginService.userRole;

    // 3. Obtener email decodificándolo del token
    this.email = this.obtenerEmailToken();
  }

  private obtenerEmailToken(): string {
    try {
      const instance = (this.loginService as any)['keycloakService']?.getKeycloakInstance();
      const tokenParsed: any = instance?.idTokenParsed || instance?.tokenParsed;
      return tokenParsed?.['email'] || `${this.user.toLowerCase()}@empresa.co`;
    } catch {
      return `${this.user.toLowerCase()}@empresa.co`;
    }
  }

  toggleSidebar() {
    this.toggle.emit();
  }
}
