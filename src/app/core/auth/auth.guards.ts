import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService, authReady } from './auth.service';

/** Requires an authenticated session; bounces to `/login` keeping the target. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return authReady(auth).pipe(
    map((ok) => ok || router.createUrlTree(['/login'], { queryParams: { next: state.url } })),
  );
};

/** For pages only anonymous visitors should see (login, OAuth callback). */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return authReady(auth).pipe(map((ok) => !ok || router.createUrlTree(['/app'])));
};

/** Requires the `ADMIN` account role. */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return authReady(auth).pipe(
    map((ok) => (ok && auth.isAdmin()) || router.createUrlTree(['/app'])),
  );
};
