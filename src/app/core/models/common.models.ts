/** Slim pagination envelope returned by the Auralis API (mirrors `PageResponse<T>`). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/** Uniform error body returned for every non-2xx response (mirrors `ApiError`). */
export interface ApiError {
  code: string;
  message: string;
  status: number;
  path: string;
  fieldErrors: Record<string, string[]>;
  timestamp: string;
}

/** Spring `Pageable` query params. `sort` is `property,(asc|desc)`. */
export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string;
}
