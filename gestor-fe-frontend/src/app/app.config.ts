import { ApplicationConfig, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { KeycloakBearerInterceptor, KeycloakService } from 'keycloak-angular';

import { routes } from './app.routes';
import { environment } from '../environments/environments';

/**
 * Inicializador de Keycloak para Angular Standalone
 */
function initializeKeycloak(keycloak: KeycloakService): () => Promise<boolean> {
  return () =>
    keycloak.init({
      config: {
        url: environment.keycloak.url,
        realm: environment.keycloak.realm,
        clientId: environment.keycloak.clientId
      },
      initOptions: {
        onLoad: 'login-required',
        flow: 'standard',
        pkceMethod: 'S256',
        checkLoginIframe: false
      },
      loadUserProfileAtStartUp: false, // 👈 Evita peticiones a /account (Elimina error 401 y CORS)
      bearerExcludedUrls: ['/assets']
    });
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(withInterceptorsFromDi()),
    KeycloakService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService]
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: KeycloakBearerInterceptor,
      multi: true
    }
  ]
};
