import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../models';
import { ToastService } from '../ui/toast.service';

/** Set on requests whose failure the calling screen already renders inline. */
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

/** Best-effort read of the API's uniform error body. */
export function readApiError(err: unknown): ApiError | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object' && 'code' in err.error) {
    return err.error as ApiError;
  }
  return null;
}

/** Flattens `fieldErrors` into a single readable sentence. */
export function fieldErrorText(err: unknown): string | null {
  const api = readApiError(err);
  if (!api?.fieldErrors) return null;
  const parts = Object.entries(api.fieldErrors).map(([field, msgs]) => `${field}: ${msgs.join(', ')}`);
  return parts.length ? parts.join(' · ') : null;
}

/**
 * Surfaces the failures a user can do nothing about (network down, 5xx, quota,
 * forbidden) as a toast. Everything else is rethrown untouched for the calling
 * component to render inline.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && !req.context.get(SKIP_ERROR_TOAST)) {
        const api = readApiError(err);
        if (err.status === 0) {
          toast.error('Sin conexión con el servidor', 'Comprueba que la API de Auralis está en marcha.');
        } else if (err.status >= 500) {
          toast.error('Error del servidor', api?.message ?? 'Vuelve a intentarlo en unos segundos.');
        } else if (err.status === 403) {
          toast.error('Sin acceso', api?.message ?? 'No tienes permisos para esta acción.');
        } else if (api?.code === 'quota_exceeded') {
          toast.error('Límite del plan alcanzado', api.message);
        }
      }
      return throwError(() => err);
    }),
  );
};
