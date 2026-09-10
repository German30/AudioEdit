export type AuthProviderId = 'google' | 'github';
export type AccountRole = 'USER' | 'ADMIN';

/** `TokenResponse` — issued by dev-login, refresh and the OAuth2 success flow. */
export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  refreshToken: string;
}

/** `ProviderInfo` — one enabled OAuth2 identity provider. */
export interface ProviderInfo {
  id: AuthProviderId;
  authorizeUrl: string;
}

/** `UserResponse` — the authenticated user's profile. */
export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  role: AccountRole;
  provider: 'GOOGLE' | 'GITHUB' | 'LOCAL';
  createdAt: string;
  lastLoginAt: string | null;
  /** Present only on some admin payloads; the public `/me` response omits it. */
  enabled?: boolean;
}

export interface DevLoginRequest {
  email: string;
  displayName?: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface UpdateProfileRequest {
  displayName?: string;
  avatarUrl?: string;
}

/** Persisted client-side session (access + refresh tokens and expiry instant). */
export interface StoredSession {
  accessToken: string;
  refreshToken: string;
  /** Epoch milliseconds at which the access token expires. */
  expiresAt: number;
}
