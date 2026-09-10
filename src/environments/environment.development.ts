/**
 * Local development environment. Points at the Auralis backend running from its
 * `compose.yaml` / `mvn spring-boot:run` on port 8080.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api/v1',
  serverOrigin: 'http://localhost:8080',
};
