import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UpdateProfileRequest, UserProfile } from '../models';

@Injectable({ providedIn: 'root' })
export class ProfileApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/me`;

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.base);
  }

  update(body: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.patch<UserProfile>(this.base, body);
  }
}
