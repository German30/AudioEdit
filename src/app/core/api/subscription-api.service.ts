import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CancelRequest, ChangePlanRequest, Subscription } from '../models';

@Injectable({ providedIn: 'root' })
export class SubscriptionApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/subscription`;

  current(): Observable<Subscription> {
    return this.http.get<Subscription>(this.base);
  }

  change(body: ChangePlanRequest): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.base}/change`, body);
  }

  cancel(body: CancelRequest): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.base}/cancel`, body);
  }

  reactivate(): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.base}/reactivate`, {});
  }
}
