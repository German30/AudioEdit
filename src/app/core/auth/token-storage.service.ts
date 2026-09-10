import { Injectable } from '@angular/core';
import { StoredSession } from '../models';

const KEY = 'auralis.session';

/**
 * Persists the token pair in `localStorage`. Every read is guarded — private
 * windows, cleared site data or storage-blocking contexts must degrade to
 * "no session" rather than throw.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorage {
  read(): StoredSession | null {
    try {
      const raw = localStorage.getItem(KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as StoredSession;
      if (!parsed?.accessToken || !parsed?.refreshToken) return null;
      return parsed;
    } catch {
      return null;
    }
  }

  write(session: StoredSession): void {
    try {
      localStorage.setItem(KEY, JSON.stringify(session));
    } catch {
      /* storage unavailable — session lives in memory only for this tab */
    }
  }

  clear(): void {
    try {
      localStorage.removeItem(KEY);
    } catch {
      /* ignore */
    }
  }
}
