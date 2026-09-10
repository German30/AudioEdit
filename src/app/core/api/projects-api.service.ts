import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateProjectRequest,
  PageQuery,
  PageResponse,
  Project,
  ProjectGraph,
  ProjectMember,
  UpdateProjectRequest,
  UpsertMemberRequest,
} from '../models';
import { pageParams } from './http-params';

@Injectable({ providedIn: 'root' })
export class ProjectsApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/projects`;

  list(query?: PageQuery): Observable<PageResponse<Project>> {
    return this.http.get<PageResponse<Project>>(this.base, { params: pageParams(query) });
  }

  create(body: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.base, body);
  }

  /** Opens the project (updates `lastOpenedAt` server-side). */
  open(projectId: string): Observable<Project> {
    return this.http.get<Project>(`${this.base}/${projectId}`);
  }

  update(projectId: string, body: UpdateProjectRequest): Observable<Project> {
    return this.http.patch<Project>(`${this.base}/${projectId}`, body);
  }

  delete(projectId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${projectId}`);
  }

  graph(projectId: string): Observable<ProjectGraph> {
    return this.http.get<ProjectGraph>(`${this.base}/${projectId}/graph`);
  }

  members(projectId: string): Observable<ProjectMember[]> {
    return this.http.get<ProjectMember[]>(`${this.base}/${projectId}/members`);
  }

  upsertMember(projectId: string, body: UpsertMemberRequest): Observable<ProjectMember> {
    return this.http.put<ProjectMember>(`${this.base}/${projectId}/members`, body);
  }

  removeMember(projectId: string, memberUserId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${projectId}/members/${memberUserId}`);
  }
}
