import { HttpParams } from '@angular/common/http';
import { PageQuery } from '../models';

/** Build `HttpParams` from a partial page query, skipping undefined values. */
export function pageParams(q: PageQuery | undefined): HttpParams {
  let params = new HttpParams();
  if (!q) return params;
  if (q.page != null) params = params.set('page', q.page);
  if (q.size != null) params = params.set('size', q.size);
  if (q.sort) params = params.set('sort', q.sort);
  return params;
}
