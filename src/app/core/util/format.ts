/** Human-readable byte size. `-1` (the API's "unlimited" sentinel) → "∞". */
export function formatBytes(bytes: number | null | undefined, digits = 1): string {
  if (bytes == null) return '—';
  if (bytes < 0) return '∞';
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)));
  const value = bytes / 1024 ** i;
  return `${value.toFixed(i === 0 ? 0 : digits)} ${units[i]}`;
}

/** `h:mm:ss` / `m:ss` from seconds. */
export function formatDuration(totalSeconds: number | null | undefined): string {
  if (totalSeconds == null || Number.isNaN(totalSeconds)) return '—';
  const s = Math.max(0, Math.round(totalSeconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const mm = h > 0 ? String(m).padStart(2, '0') : String(m);
  return `${h > 0 ? `${h}:` : ''}${mm}:${String(sec).padStart(2, '0')}`;
}

/** Minutes from seconds, rounded, for render-quota copy. `-1` → "∞". */
export function formatMinutes(seconds: number | null | undefined): string {
  if (seconds == null) return '—';
  if (seconds < 0) return '∞';
  return `${Math.round(seconds / 60)} min`;
}

/** Monthly price from cents. `0` → "Gratis". */
export function formatPrice(priceCents: number, currency = 'USD'): string {
  if (priceCents === 0) return 'Gratis';
  const amount = priceCents / 100;
  try {
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency,
      minimumFractionDigits: amount % 1 === 0 ? 0 : 2,
    }).format(amount);
  } catch {
    return `${amount} ${currency}`;
  }
}

/** `-1` → "Ilimitado", otherwise the number with an optional unit. */
export function formatLimit(value: number, unit = ''): string {
  if (value < 0) return 'Ilimitado';
  return unit ? `${value} ${unit}` : String(value);
}

/** Relative time in Spanish ("hace 3 h", "en 2 días"). */
export function formatRelative(iso: string | null | undefined): string {
  if (!iso) return '—';
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '—';
  const diffSec = Math.round((then - Date.now()) / 1000);
  const abs = Math.abs(diffSec);
  const rtf = new Intl.RelativeTimeFormat('es', { numeric: 'auto' });
  const table: [number, Intl.RelativeTimeFormatUnit][] = [
    [60, 'second'],
    [3600, 'minute'],
    [86400, 'hour'],
    [604800, 'day'],
    [2629800, 'week'],
    [31557600, 'month'],
    [Infinity, 'year'],
  ];
  const divisors = [1, 60, 3600, 86400, 604800, 2629800, 31557600];
  for (let i = 0; i < table.length; i++) {
    if (abs < table[i][0]) return rtf.format(Math.round(diffSec / divisors[i]), table[i][1]);
  }
  return rtf.format(Math.round(diffSec / 31557600), 'year');
}

/** Short absolute date, e.g. "10 sept 2026". */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short', year: 'numeric' }).format(d);
}

/** Clamped 0–100 percentage; a `-1` limit (unlimited) always reads as 0. */
export function usagePercent(used: number, limit: number): number {
  if (limit < 0 || limit === 0) return 0;
  return Math.max(0, Math.min(100, Math.round((used / limit) * 100)));
}
