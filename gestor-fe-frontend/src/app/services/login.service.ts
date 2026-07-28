import { Injectable, inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class LoginService {
  private keycloakService = inject(KeycloakService);

  // 👥 Definición de roles requeridos
  readonly adminRoles: string[] = ['cm-noreps-admin'];
  readonly prestadorRoles: string[] = ['cm-noreps-prestador'];

  // 🚩 Flags de permisos
  isAdmin: boolean = false;
  isPrestador: boolean = false;

  // 👤 Datos del usuario autenticado
  userName: string = '';
  userCedula: string = '';

  constructor() {
    this.initUserData();
  }

  /**
   * Carga la información inicial del usuario desde el token de Keycloak
   */
  private initUserData(): void {
    try {
      if (this.keycloakService.isLoggedIn()) {
        this.userName = this.keycloakService.getUsername();
        this.getUserRoles();
        this.getExtendedUserData();
      }
    } catch (error) {
      console.warn('Keycloak no está completamente inicializado aún o no hay sesión activa:', error);
    }
  }

  /**
   * Devuelve un string simplificado del rol principal
   */
  get userRole(): 'ADMIN' | 'PRESTADOR' {
    if (this.isAdmin) return 'ADMIN';
    return 'PRESTADOR'; // Rol por defecto
  }

  /**
   * Obtiene y evalúa los roles asignados en Keycloak
   */
  getUserRoles(): string[] {
    try {
      const roles = this.keycloakService.getUserRoles();
      
      this.isAdmin = roles.some(role => this.adminRoles.includes(role));
      this.isPrestador = roles.some(role => this.prestadorRoles.includes(role));

      return roles;
    } catch (error) {
      console.error('Error al obtener los roles del usuario:', error);
      return [];
    }
  }

  /**
   * Extrae atributos personalizados agregados al token (ej. número de identificación)
   */
  private getExtendedUserData(): void {
    try {
      const tokenParsed: any = this.keycloakService.getKeycloakInstance()?.idTokenParsed;

      if (tokenParsed) {
        this.userCedula = tokenParsed['numero_identificacion'] || '';
      }
    } catch (error) {
      console.error('Error al decodificar los datos extendidos del token:', error);
    }
  }

  /**
   * Cierra la sesión activa en Keycloak y limpia el almacenamiento local
   */
  logout(): void {
    sessionStorage.clear();
    localStorage.clear();
    this.keycloakService.logout(window.location.origin);
  }
}