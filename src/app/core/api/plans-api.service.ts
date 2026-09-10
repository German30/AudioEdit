import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../auth/error.interceptor';
import { Plan, PlanTier } from '../models';
import { skipAuth } from './auth-api.service';

@Injectable({ providedIn: 'root' })
export class PlansApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/plans`;

  /** Public plan catalogue — no auth required; callers show failures inline. */
  list(): Observable<Plan[]> {
    return this.http.get<Plan[]>(this.base, { context: skipAuth().set(SKIP_ERROR_TOAST, true) });
  }

  get(tier: PlanTier): Observable<Plan> {
    return this.http.get<Plan>(`${this.base}/${tier}`, { context: skipAuth().set(SKIP_ERROR_TOAST, true) });
  }
}
