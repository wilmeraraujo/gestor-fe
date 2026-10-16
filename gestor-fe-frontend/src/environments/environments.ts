export const environment = {
  production: true,
  keycloak: {
    // Para pruebas locales con Keycloak en tu PC:
    // url: 'http://localhost:8082',
    // Para servidor remoto VM3 (Subdominio):
    url: 'https://auth.assolidaria.org.co',
    // url: 'http://10.3.26.4:8080',
    realm: 'gestor-fe',
    clientId: 'gestor-fe-frontend'
  }
};

