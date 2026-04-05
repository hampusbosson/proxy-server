export type Verdict = 'ALL' | 'ALLOWED' | 'BLOCKED' | 'ERROR';

export type Transaction = {
  timestampMs: number;
  method: string;
  host: string;
  port: number;
  path: string;
  verdict: Exclude<Verdict, 'ALL'>;
  bytesFromServer: number;
  durationMs: number;
  errorMessage: string | null;
};

export type Stats = {
  total: number;
  allowed: number;
  blocked: number;
  error: number;
  bytesFromServerTotal: number;
  avgDurationMs: number;
};

export type LoadStatus = 'loading' | 'ready' | 'error';

export type LoadableState<T> = {
  status: LoadStatus;
  data: T | null;
  error: string | null;
};

export type LoadableListState<T> = {
  status: LoadStatus;
  data: T[];
  error: string | null;
};

export type HealthState = {
  status: 'loading' | 'healthy' | 'unreachable';
  message: string;
};

export type DashboardFilters = {
  verdict: Verdict;
  limit: number;
  autoRefresh: boolean;
};
