import { HttpClient, HttpContext, HttpContextToken } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../auth/error.interceptor';
import {
  DevLoginRequest,
  ProviderInfo,
  RefreshRequest,
  TokenResponse,
} from '../models';

/** When true, the auth interceptor skips attaching / refreshing the bearer token. */
export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);
export const skipAuth = () => new HttpContext().set(SKIP_AUTH, true);

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/auth`;

  providers(): Observable<ProviderInfo[]> {
    return this.http.get<ProviderInfo[]>(`${this.base}/providers`, {
      context: skipAuth().set(SKIP_ERROR_TOAST, true),
    });
  }

  devLogin(body: DevLoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.base}/dev-login`, body, { context: skipAuth() });
  }

  refresh(body: RefreshRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.base}/token`, body, { context: skipAuth() });
  }

  logout(refreshToken: string | null): Observable<void> {
    return this.http.post<void>(`${this.base}/logout`, { refreshToken }, { context: skipAuth() });
  }

  logoutAll(): Observable<void> {
    return this.http.post<void>(`${this.base}/logout-all`, {});
  }

  /** Absolute URL that starts the provider's OAuth2 login on the backend origin. */
  authorizeUrl(provider: ProviderInfo): string {
    return `${environment.serverOrigin}${provider.authorizeUrl}`;
  }
}
