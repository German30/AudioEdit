import { computed, Injectable, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import {
  Observable,
  catchError,
  filter,
  finalize,
  map,
  of,
  shareReplay,
  switchMap,
  take,
  tap,
  throwError,
} from 'rxjs';
import { AuthApi } from '../api/auth-api.service';
import { ProfileApi } from '../api/profile-api.service';
import { StoredSession, TokenResponse, UserProfile } from '../models';
import { TokenStorage } from './token-storage.service';

/** Refresh the access token this many ms before it actually expires. */
const REFRESH_SKEW_MS = 45_000;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authApi = inject(AuthApi);
  private readonly profileApi = inject(ProfileApi);
  private readonly storage = inject(TokenStorage);

  private readonly _session = signal<StoredSession | null>(this.storage.read());
  private readonly _user = signal<UserProfile | null>(null);
  private readonly _ready = signal(false);

  /** Current stored token pair, or null when signed out. */
  readonly session = this._session.asReadonly();
  /** The authenticated user's profile once `restore()` has resolved. */
  readonly user = this._user.asReadonly();
  /** True once the initial session restore has completed (success or not). */
  readonly ready = this._ready.asReadonly();

  readonly isAuthenticated = computed(() => this._session() !== null);
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  /** In-flight refresh, shared so concurrent 401s wait on one network call. */
  private refresh$: Observable<string> | null = null;

  get accessToken(): string | null {
    return this._session()?.accessToken ?? null;
  }

  /** Called once at bootstrap: revive a persisted session and load the profile. */
  restore(): Observable<boolean> {
    const session = this._session();
    if (!session) {
      this._ready.set(true);
      return of(false);
    }
    const primeToken =
      session.expiresAt <= Date.now() + REFRESH_SKEW_MS
        ? this.doRefresh(session.refreshToken).pipe(map(() => true))
        : of(true);

    return primeToken.pipe(
      switchMap(() => this.loadProfile()),
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      }),
      finalize(() => this._ready.set(true)),
    );
  }

  devLogin(email: string, displayName?: string): Observable<UserProfile> {
    return this.authApi.devLogin({ email, displayName }).pipe(
      tap((tokens) => this.persist(tokens)),
      switchMap(() => this.loadProfile()),
    );
  }

  /** Store tokens delivered by the OAuth2 success redirect and load the profile. */
  consumeOAuthTokens(accessToken: string, refreshToken: string, expiresIn: number): Observable<UserProfile> {
    this.persist({ accessToken, refreshToken, expiresIn, tokenType: 'Bearer' });
    return this.loadProfile();
  }

  logout(): Observable<void> {
    const refreshToken = this._session()?.refreshToken ?? null;
    this.clearSession();
    return this.authApi.logout(refreshToken).pipe(catchError(() => of(void 0)));
  }

  refreshProfile(): Observable<UserProfile> {
    return this.loadProfile();
  }

  /**
   * A valid access token — refreshed first when the current one sits inside the
   * skew window. Used by the auth interceptor before attaching the header.
   */
  getFreshToken(): Observable<string> {
    const session = this._session();
    if (!session) return throwError(() => new Error('no-session'));
    if (session.expiresAt > Date.now() + REFRESH_SKEW_MS) return of(session.accessToken);
    return this.doRefresh(session.refreshToken);
  }

  /** Force a token rotation now (used to retry a request that still got a 401). */
  forceRefresh(): Observable<string> {
    const session = this._session();
    if (!session) return throwError(() => new Error('no-session'));
    return this.doRefresh(session.refreshToken);
  }

  // --- internals ---------------------------------------------------------

  private doRefresh(refreshToken: string): Observable<string> {
    if (this.refresh$) return this.refresh$;
    this.refresh$ = this.authApi.refresh({ refreshToken }).pipe(
      map((tokens) => {
        this.persist(tokens);
        return tokens.accessToken;
      }),
      catchError((err) => {
        this.clearSession();
        return throwError(() => err);
      }),
      finalize(() => {
        this.refresh$ = null;
      }),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    return this.refresh$;
  }

  private loadProfile(): Observable<UserProfile> {
    return this.profileApi.me().pipe(tap((user) => this._user.set(user)));
  }

  private persist(tokens: TokenResponse): void {
    const session: StoredSession = {
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      expiresAt: Date.now() + tokens.expiresIn * 1000,
    };
    this._session.set(session);
    this.storage.write(session);
  }

  private clearSession(): void {
    this._session.set(null);
    this._user.set(null);
    this.storage.clear();
  }
}

/**
 * Gate a guard on the initial session restore: emits the auth outcome exactly
 * once `AuthService.restore()` has settled.
 */
export function authReady(auth: AuthService): Observable<boolean> {
  return toObservable(auth.ready).pipe(
    filter((ready) => ready),
    take(1),
    map(() => auth.isAuthenticated()),
  );
}
