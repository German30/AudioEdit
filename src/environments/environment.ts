/**
 * Production environment. Values are overridden for local development by
 * `environment.development.ts` via the `fileReplacements` entry in angular.json.
 */
export const environment = {
  production: true,
  /** Base URL of the Auralis API, including the `/api/v1` version prefix. */
  apiBaseUrl: '/api/v1',
  /** Origin of the backend, used for OAuth2 provider redirects (no version prefix). */
  serverOrigin: '',
};
