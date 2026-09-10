export type PlanTier = 'FREE' | 'PRO' | 'STUDIO';
export type SubscriptionStatus = 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED';

/** `EntitlementsView` — the concrete limits a plan grants. `-1` means unlimited. */
export interface Entitlements {
  maxProjects: number;
  maxCollaboratorsPerProject: number;
  monthlyRenderMinutes: number;
  maxStorageBytes: number;
  maxUploadBytes: number;
  allowedExportFormats: string[];
  allowStemExport: boolean;
  maxExportBitrateKbps: number;
  historyRetentionDays: number;
}

/** `PlanResponse` — one purchasable plan from the public catalogue. */
export interface Plan {
  tier: PlanTier;
  name: string;
  description: string;
  priceCents: number;
  currency: string;
  entitlements: Entitlements;
}

/** `UsageView` — the caller's current billing-period consumption. */
export interface Usage {
  renderSecondsUsed: number;
  renderSecondsLimit: number;
  storageBytesUsed: number;
  storageBytesLimit: number;
}

/** `SubscriptionResponse` — the caller's plan, status, entitlements and usage. */
export interface Subscription {
  tier: PlanTier;
  status: SubscriptionStatus;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;
  entitlements: Entitlements;
  usage: Usage;
}

export interface ChangePlanRequest {
  tier: PlanTier;
}

export interface CancelRequest {
  immediately: boolean;
}
