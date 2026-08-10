import { Injectable, inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class LoginService {
  private keycloakService = inject(KeycloakService);

  // 👥 Definición de roles requeridos
  readonly adminRoles: string[] = ['admin'];
  readonly gAdminRoles: string[] = ['gestor-fe-admin'];
  readonly gCargueRoles: string[] = ['gestor-fe-cargue'];
  readonly gFaseUnoRoles: string[] = ['gestor-fe-f1-g'];
  readonly gFaseDosRoles: string[] = ['gestor-fe-f2-rc'];
  readonly gFaseTresRoles: string[] = ['gestor-fe-f3-i'];
  readonly gFaseCuatroRoles: string[] = ['gestor-fe-f4-pp'];
  readonly gFaseCincoRoles: string[] = ['gestor-fe-f5-sf'];
  readonly gPrestadorRoles: string[] = ['gestor-fe-prestador'];

  // 🚩 Flags de permisos
  isAdmin: boolean = false;
  isGAdmin: boolean = false;
  isGCargue: boolean = false;
  isGFaseUno: boolean = false;
  isGFaseDos: boolean = false;
  isGFaseTres: boolean = false;
  isGFaseCuatro: boolean = false;
  isGFaseCinco: boolean = false;
  isPrestador: boolean = false;

  // 👤 Datos del usuario autenticado
  userName: string = '';
  userCedula: string = '';

  constructor() {
    this.initUserData();
  }

  /**
   * Carga la información inicial del usuario
   */
  private initUserData(): void {
    try {
      this.userName = this.getUserName();
      this.getUserRoles();
      this.getExtendedUserData();
    } catch (error) {
      console.warn('Error en inicialización de datos de sesión:', error);
    }
  }

  /**
   * 🎯 Extrae de forma segura el usuario leyendo directamente del JWT parseado sin lanzar excepciones
   */
  getUserName(): string {
    try {
      // 1. Decodificar directamente la instancia del Token JWT activo
      const instance = this.keycloakService.getKeycloakInstance();
      const tokenParsed: any = instance?.idTokenParsed || instance?.tokenParsed;

      if (tokenParsed) {
        const usernameParsed = tokenParsed['preferred_username'] || tokenParsed['username'] || tokenParsed['sub'] || tokenParsed['name'];
        if (usernameParsed) {
          this.userName = usernameParsed;
          return this.userName;
        }
      }

      // 2. Fallback si el objeto del servicio tiene el nombre guardado
      if (this.userName && this.userName.trim() !== '') {
        return this.userName;
      }
    } catch (error) {
      console.error('Error al decodificar el token JWT de Keycloak:', error);
    }

    return this.userName || 'GESTOR_SISTEMA';
  }

  get userRole(): 'ADMIN' | 'PRESTADOR' {
    if (this.isAdmin) return 'ADMIN';
    return 'PRESTADOR';
  }

  getUserRoles(): string[] {
    try {
      const roles = this.keycloakService.getUserRoles();

      this.isAdmin = roles.some(role => this.adminRoles.includes(role));
      this.isGAdmin = roles.some(role => this.gAdminRoles.includes(role));
      this.isGCargue = roles.some(role => this.gCargueRoles.includes(role));
      this.isGFaseUno = roles.some(role => this.gFaseUnoRoles.includes(role));
      this.isGFaseDos = roles.some(role => this.gFaseDosRoles.includes(role));
      this.isGFaseTres = roles.some(role => this.gFaseTresRoles.includes(role));
      this.isGFaseCuatro = roles.some(role => this.gFaseCuatroRoles.includes(role));
      this.isGFaseCinco = roles.some(role => this.gFaseCincoRoles.includes(role));
      this.isPrestador = roles.some(role => this.gPrestadorRoles.includes(role));

      return roles;
    } catch (error) {
      console.error('Error al obtener los roles del usuario:', error);
      return [];
    }
  }

  private getExtendedUserData(): void {
    try {
      const tokenParsed: any = this.keycloakService.getKeycloakInstance()?.idTokenParsed || this.keycloakService.getKeycloakInstance()?.tokenParsed;

      if (tokenParsed) {
        this.userCedula = tokenParsed['numero_identificacion'] || '';
      }
    } catch (error) {
      console.error('Error al decodificar los datos extendidos del token:', error);
    }
  }

  logout(): void {
    sessionStorage.clear();
    localStorage.clear();
    this.keycloakService.logout(window.location.origin);
  }
}