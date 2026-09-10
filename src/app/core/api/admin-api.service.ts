import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccountRole,
  Entitlements,
  PageQuery,
  PageResponse,
  Plan,
  PlanTier,
  UserProfile,
} from '../models';
import { pageParams } from './http-params';

export interface UpdateUserRequest {
  enabled?: boolean;
  role?: AccountRole;
}

export interface PlanUpdate {
  name?: string;
  description?: string;
  priceCents?: number;
  currency?: string;
  active?: boolean;
  sortOrder?: number;
  entitlements?: Partial<Entitlements>;
}

/** `/api/v1/admin/**` — requires the `ADMIN` account role. */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/admin`;

  users(query?: PageQuery): Observable<PageResponse<UserProfile>> {
    return this.http.get<PageResponse<UserProfile>>(`${this.base}/users`, { params: pageParams(query) });
  }

  user(id: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.base}/users/${id}`);
  }

  updateUser(id: string, body: UpdateUserRequest): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.base}/users/${id}`, body);
  }

  updatePlan(tier: PlanTier, body: PlanUpdate): Observable<Plan> {
    return this.http.put<Plan>(`${this.base}/plans/${tier}`, body);
  }
}
