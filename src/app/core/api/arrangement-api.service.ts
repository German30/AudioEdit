import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Clip,
  CreateClipRequest,
  CreateEffectRequest,
  CreateTrackRequest,
  Effect,
  ReorderTracksRequest,
  SplitClipRequest,
  Track,
  UpdateClipRequest,
  UpdateEffectRequest,
  UpdateTrackRequest,
} from '../models';

/** Tracks, clips and effects — all nested under a project for per-project access. */
@Injectable({ providedIn: 'root' })
export class ArrangementApi {
  private readonly http = inject(HttpClient);
  private base(projectId: string): string {
    return `${environment.apiBaseUrl}/projects/${projectId}`;
  }

  /* ---- tracks --------------------------------------------------------- */
  listTracks(projectId: string): Observable<Track[]> {
    return this.http.get<Track[]>(`${this.base(projectId)}/tracks`);
  }

  addTrack(projectId: string, body: CreateTrackRequest): Observable<Track> {
    return this.http.post<Track>(`${this.base(projectId)}/tracks`, body);
  }

  updateTrack(projectId: string, trackId: string, body: UpdateTrackRequest): Observable<Track> {
    return this.http.patch<Track>(`${this.base(projectId)}/tracks/${trackId}`, body);
  }

  deleteTrack(projectId: string, trackId: string): Observable<void> {
    return this.http.delete<void>(`${this.base(projectId)}/tracks/${trackId}`);
  }

  reorderTracks(projectId: string, body: ReorderTracksRequest): Observable<Track[]> {
    return this.http.put<Track[]>(`${this.base(projectId)}/tracks/order`, body);
  }

  /* ---- clips --------------------------------------------------------- */
  listClips(projectId: string): Observable<Clip[]> {
    return this.http.get<Clip[]>(`${this.base(projectId)}/clips`);
  }

  addClip(projectId: string, body: CreateClipRequest): Observable<Clip> {
    return this.http.post<Clip>(`${this.base(projectId)}/clips`, body);
  }

  updateClip(projectId: string, clipId: string, body: UpdateClipRequest): Observable<Clip> {
    return this.http.patch<Clip>(`${this.base(projectId)}/clips/${clipId}`, body);
  }

  splitClip(projectId: string, clipId: string, body: SplitClipRequest): Observable<Clip[]> {
    return this.http.post<Clip[]>(`${this.base(projectId)}/clips/${clipId}/split`, body);
  }

  deleteClip(projectId: string, clipId: string): Observable<void> {
    return this.http.delete<void>(`${this.base(projectId)}/clips/${clipId}`);
  }

  /* ---- effects ----------------------------------------------------- */
  listEffects(projectId: string): Observable<Effect[]> {
    return this.http.get<Effect[]>(`${this.base(projectId)}/effects`);
  }

  addEffect(projectId: string, body: CreateEffectRequest): Observable<Effect> {
    return this.http.post<Effect>(`${this.base(projectId)}/effects`, body);
  }

  updateEffect(projectId: string, effectId: string, body: UpdateEffectRequest): Observable<Effect> {
    return this.http.patch<Effect>(`${this.base(projectId)}/effects/${effectId}`, body);
  }

  deleteEffect(projectId: string, effectId: string): Observable<void> {
    return this.http.delete<void>(`${this.base(projectId)}/effects/${effectId}`);
  }
}
