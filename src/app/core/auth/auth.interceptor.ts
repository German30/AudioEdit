import { HttpContextToken, HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { SKIP_AUTH } from '../api/auth-api.service';
import { AuthService } from './auth.service';

/** Marks a request that has already been retried once after a 401. */
const RETRIED = new HttpContextToken<boolean>(() => false);

function withBearer<T>(req: HttpRequest<T>, token: string): HttpRequest<T> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

/**
 * Attaches a fresh bearer token to every API call, proactively rotating it when
 * it is about to expire, and transparently retries once on a server-side 401.
 */
export const authInterceptor: HttpInterceptorFn = (req, next: HttpHandlerFn) => {
  if (req.context.get(SKIP_AUTH)) return next(req);

  const auth = inject(AuthService);
  if (!auth.accessToken) return next(req);

  return auth.getFreshToken().pipe(
    switchMap((token) => next(withBearer(req, token))),
    catchError((err: unknown) => {
      const is401 = err instanceof HttpErrorResponse && err.status === 401;
      if (!is401 || req.context.get(RETRIED) || !auth.accessToken) {
        return throwError(() => err);
      }
      req.context.set(RETRIED, true);
      return auth.forceRefresh().pipe(
        switchMap((token) => next(withBearer(req, token))),
        catchError(() => throwError(() => err)),
      );
    }),
  );
};
