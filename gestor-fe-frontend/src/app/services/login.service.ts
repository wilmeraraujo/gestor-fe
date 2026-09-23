import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class LoginService {

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

  constructor(private keycloakService: KeycloakService) {}

  /**
   * 🎯 Extrae el nombre de usuario legible decodificando el JWT sin lanzar excepciones
   * y omitiendo explícitamente el UUID ('sub').
   */
  getUserName(): string {
    try {
      const instance = this.keycloakService.getKeycloakInstance();
      // Obtenemos el token parseado (idTokenParsed o tokenParsed)
      const tokenParsed: any = instance?.idTokenParsed || instance?.tokenParsed;

      if (tokenParsed) {
        // 💡 Jerarquía estricta: Busca preferred_username mapeado en Keycloak
        const username = tokenParsed['preferred_username']
                      || tokenParsed['username']
                      || tokenParsed['given_name']
                      || tokenParsed['name']
                      || tokenParsed['email'];

        if (username && typeof username === 'string' && username.trim() !== '') {
          return username.trim();
        }
      }
    } catch (error) {
      console.warn('No se pudo decodificar el username del token Keycloak:', error);
    }

    return 'GESTOR_SISTEMA';
  }

  /**
   * 🆔 Extrae el número de identificación o NIT desde los atributos del token
   */
  getUserCedula(): string {
    try {
      const instance = this.keycloakService.getKeycloakInstance();
      const tokenParsed: any = instance?.idTokenParsed || instance?.tokenParsed;

      if (tokenParsed) {
        return tokenParsed['numero_identificacion'] || tokenParsed['nit'] || '';
      }
    } catch (error) {
      console.error('Error al decodificar la identificación:', error);
    }
    return '';
  }

  /**
   * 🛡️ Evalúa los roles del usuario activo
   */
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
      return [];
    }
  }

  get userRole(): string {
    if (this.isAdmin || this.isGAdmin) return 'ADMIN';
    if (this.isPrestador) return 'PRESTADOR';
    return 'PRESTADOR';
  }

  logout(): void {
    sessionStorage.clear();
    localStorage.clear();
    this.keycloakService.logout(window.location.origin);
  }
}
