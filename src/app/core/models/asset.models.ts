export type AssetStatus = 'PENDING_UPLOAD' | 'PROCESSING' | 'READY' | 'FAILED';

export interface InitiateUploadRequest {
  filename: string;
  contentType: string;
  sizeBytes: number;
}

/** `PresignedUploadView` — where and how to PUT the raw bytes. */
export interface PresignedUpload {
  url: string;
  method: string;
  headers: Record<string, string>;
  expiresAt: string;
}

/** `InitiateUploadResponse` — a reserved asset row plus its upload target. */
export interface InitiateUploadResponse {
  assetId: string;
  objectKey: string;
  upload: PresignedUpload;
}

/** `AssetResponse` — an uploaded audio file and its ffprobe analysis. */
export interface AudioAsset {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  status: AssetStatus;
  durationSeconds: number | null;
  sampleRate: number | null;
  channels: number | null;
  codec: string | null;
  bitRateKbps: number | null;
  hasWaveform: boolean;
  failureReason: string | null;
  createdAt: string;
}

export interface DownloadUrl {
  url: string;
  expiresAt: string;
}
