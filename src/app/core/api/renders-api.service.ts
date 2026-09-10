import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateRenderRequest, DownloadUrl, PageQuery, PageResponse, RenderJob } from '../models';
import { pageParams } from './http-params';

@Injectable({ providedIn: 'root' })
export class RendersApi {
  private readonly http = inject(HttpClient);
  private base(projectId: string): string {
    return `${environment.apiBaseUrl}/projects/${projectId}/renders`;
  }

  enqueue(projectId: string, body: CreateRenderRequest): Observable<RenderJob> {
    return this.http.post<RenderJob>(this.base(projectId), body);
  }

  list(projectId: string, query?: PageQuery): Observable<PageResponse<RenderJob>> {
    return this.http.get<PageResponse<RenderJob>>(this.base(projectId), { params: pageParams(query) });
  }

  get(projectId: string, jobId: string): Observable<RenderJob> {
    return this.http.get<RenderJob>(`${this.base(projectId)}/${jobId}`);
  }

  cancel(projectId: string, jobId: string): Observable<RenderJob> {
    return this.http.post<RenderJob>(`${this.base(projectId)}/${jobId}/cancel`, {});
  }

  downloadUrl(projectId: string, jobId: string): Observable<DownloadUrl> {
    return this.http.get<DownloadUrl>(`${this.base(projectId)}/${jobId}/download-url`);
  }

  stemsUrl(projectId: string, jobId: string): Observable<DownloadUrl> {
    return this.http.get<DownloadUrl>(`${this.base(projectId)}/${jobId}/stems-url`);
  }
}
