export type RenderStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED';

export interface CreateRenderRequest {
  format?: string;
  bitrateKbps?: number;
  sampleRate?: number;
  includeStems?: boolean;
}

/** `RenderJobResponse` — one export job and its progress. */
export interface RenderJob {
  id: string;
  projectId: string;
  editVersionId: string;
  status: RenderStatus;
  format: string;
  bitrateKbps: number;
  sampleRate: number;
  includeStems: boolean;
  progress: number;
  attempts: number;
  renderedSeconds: number | null;
  billedSeconds: number | null;
  outputSizeBytes: number | null;
  hasOutput: boolean;
  hasStems: boolean;
  errorMessage: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
}

export function isTerminalRender(status: RenderStatus): boolean {
  return status === 'SUCCEEDED' || status === 'FAILED' || status === 'CANCELED';
}
