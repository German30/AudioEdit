export type ProjectRole = 'VIEWER' | 'EDITOR' | 'OWNER';
export type EffectScope = 'CLIP' | 'TRACK' | 'MASTER';
export type EffectType =
  | 'GAIN'
  | 'NORMALIZE'
  | 'FADE'
  | 'EQ'
  | 'LOWPASS'
  | 'HIGHPASS'
  | 'REVERB'
  | 'COMPRESSOR'
  | 'PITCH'
  | 'TEMPO';

/* ---- project -------------------------------------------------------------- */
export interface CreateProjectRequest {
  name: string;
  description?: string;
  sampleRate?: number;
  tempoBpm?: number;
}

export interface UpdateProjectRequest {
  name?: string;
  description?: string;
  tempoBpm?: number;
  masterGainDb?: number;
  archived?: boolean;
}

export interface Project {
  id: string;
  ownerUserId: string;
  name: string;
  description: string | null;
  sampleRate: number;
  tempoBpm: number | null;
  masterGainDb: number;
  archived: boolean;
  lastOpenedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/* ---- members ------------------------------------------------------------- */
export interface UpsertMemberRequest {
  userId: string;
  role: ProjectRole;
}

export interface ProjectMember {
  userId: string;
  role: ProjectRole;
  addedAt: string;
}

/* ---- tracks ------------------------------------------------------------ */
export interface CreateTrackRequest {
  name?: string;
  color?: string;
}

export interface UpdateTrackRequest {
  name?: string;
  gainDb?: number;
  pan?: number;
  muted?: boolean;
  soloed?: boolean;
  color?: string;
}

export interface ReorderTracksRequest {
  trackIds: string[];
}

export interface Track {
  id: string;
  name: string;
  position: number;
  gainDb: number;
  pan: number;
  muted: boolean;
  soloed: boolean;
  color: string | null;
}

/* ---- clips ------------------------------------------------------------- */
export interface CreateClipRequest {
  trackId: string;
  sourceAssetId: string;
  name?: string;
  timelineStartSeconds?: number;
  sourceOffsetSeconds?: number;
  durationSeconds?: number;
  gainDb?: number;
  fadeInSeconds?: number;
  fadeOutSeconds?: number;
  playbackRate?: number;
  reversed?: boolean;
}

export type UpdateClipRequest = Partial<Omit<CreateClipRequest, 'sourceAssetId'>>;

export interface SplitClipRequest {
  atTimelineSeconds: number;
}

export interface Clip {
  id: string;
  trackId: string;
  sourceAssetId: string;
  name: string;
  timelineStartSeconds: number;
  sourceOffsetSeconds: number;
  durationSeconds: number;
  gainDb: number;
  fadeInSeconds: number;
  fadeOutSeconds: number;
  playbackRate: number;
  reversed: boolean;
}

/* ---- effects --------------------------------------------------------- */
export interface CreateEffectRequest {
  scope: EffectScope;
  clipId?: string;
  trackId?: string;
  type: EffectType;
  enabled?: boolean;
  params?: Record<string, unknown>;
}

export interface UpdateEffectRequest {
  enabled?: boolean;
  position?: number;
  params?: Record<string, unknown>;
}

export interface Effect {
  id: string;
  scope: EffectScope;
  clipId: string | null;
  trackId: string | null;
  type: EffectType;
  enabled: boolean;
  position: number;
  params: Record<string, unknown>;
}

/* ---- versions ------------------------------------------------------- */
export interface SnapshotRequest {
  label?: string;
}

export interface EditVersion {
  id: string;
  revision: number;
  label: string | null;
  authorUserId: string;
  createdAt: string;
}

/* ---- resolved edit graph ----------------------------------------------- */
export interface EffectNode {
  id: string;
  type: EffectType;
  enabled: boolean;
  params: Record<string, unknown>;
}

export interface ClipNode {
  id: string;
  sourceAssetId: string;
  assetObjectKey: string;
  assetFilename: string;
  timelineStartSeconds: number;
  sourceOffsetSeconds: number;
  durationSeconds: number;
  gainDb: number;
  fadeInSeconds: number;
  fadeOutSeconds: number;
  playbackRate: number;
  reversed: boolean;
  effects: EffectNode[];
}

export interface TrackNode {
  id: string;
  name: string;
  position: number;
  gainDb: number;
  pan: number;
  muted: boolean;
  soloed: boolean;
  clips: ClipNode[];
  effects: EffectNode[];
}

export interface ProjectGraph {
  projectId: string;
  name: string;
  sampleRate: number;
  tempoBpm: number | null;
  masterGainDb: number;
  tracks: TrackNode[];
  masterEffects: EffectNode[];
}
