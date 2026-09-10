import { HttpClient, HttpEvent, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AudioAsset,
  DownloadUrl,
  InitiateUploadRequest,
  InitiateUploadResponse,
  PageQuery,
  PageResponse,
  PresignedUpload,
} from '../models';
import { skipAuth } from './auth-api.service';
import { pageParams } from './http-params';

@Injectable({ providedIn: 'root' })
export class AssetsApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/assets`;

  list(query?: PageQuery): Observable<PageResponse<AudioAsset>> {
    return this.http.get<PageResponse<AudioAsset>>(this.base, { params: pageParams(query) });
  }

  get(assetId: string): Observable<AudioAsset> {
    return this.http.get<AudioAsset>(`${this.base}/${assetId}`);
  }

  initiateUpload(body: InitiateUploadRequest): Observable<InitiateUploadResponse> {
    return this.http.post<InitiateUploadResponse>(`${this.base}/uploads`, body);
  }

  completeUpload(assetId: string): Observable<AudioAsset> {
    return this.http.post<AudioAsset>(`${this.base}/${assetId}/complete`, {});
  }

  downloadUrl(assetId: string): Observable<DownloadUrl> {
    return this.http.get<DownloadUrl>(`${this.base}/${assetId}/download-url`);
  }

  delete(assetId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${assetId}`);
  }

  /**
   * PUT the raw file straight to object storage using the presigned target.
   * Bypasses the auth interceptor — the URL is already signed. Emits upload
   * progress events when `reportProgress` is set.
   */
  putToStorage(target: PresignedUpload, file: File): Observable<HttpEvent<unknown>> {
    let headers = new HttpHeaders();
    for (const [k, v] of Object.entries(target.headers ?? {})) headers = headers.set(k, v);
    if (!headers.has('Content-Type')) headers = headers.set('Content-Type', file.type || 'application/octet-stream');
    return this.http.request(target.method || 'PUT', target.url, {
      body: file,
      headers,
      context: skipAuth(),
      reportProgress: true,
      observe: 'events',
    });
  }
}
