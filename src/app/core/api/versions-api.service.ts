import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EditVersion, PageQuery, PageResponse, ProjectGraph, SnapshotRequest } from '../models';
import { pageParams } from './http-params';

@Injectable({ providedIn: 'root' })
export class VersionsApi {
  private readonly http = inject(HttpClient);
  private base(projectId: string): string {
    return `${environment.apiBaseUrl}/projects/${projectId}/versions`;
  }

  snapshot(projectId: string, body: SnapshotRequest): Observable<EditVersion> {
    return this.http.post<EditVersion>(this.base(projectId), body);
  }

  list(projectId: string, query?: PageQuery): Observable<PageResponse<EditVersion>> {
    return this.http.get<PageResponse<EditVersion>>(this.base(projectId), { params: pageParams(query) });
  }

  get(projectId: string, versionId: string): Observable<EditVersion> {
    return this.http.get<EditVersion>(`${this.base(projectId)}/${versionId}`);
  }

  graphOf(projectId: string, versionId: string): Observable<ProjectGraph> {
    return this.http.get<ProjectGraph>(`${this.base(projectId)}/${versionId}/graph`);
  }

  restore(projectId: string, versionId: string): Observable<EditVersion> {
    return this.http.post<EditVersion>(`${this.base(projectId)}/${versionId}/restore`, {});
  }
}
