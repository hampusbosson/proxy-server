import { apiGet, apiGetText } from './client';
import type { Stats, Transaction, Verdict } from '../types';

export async function fetchHealth(): Promise<string> {
  return apiGetText('/health');
}

export async function fetchStats(): Promise<Stats> {
  const payload = await apiGet<Stats>('/stats');
  return payload.data;
}

export async function fetchTransactions(
  limit: number,
  verdict: Verdict
): Promise<Transaction[]> {
  const searchParams = new URLSearchParams({ limit: String(limit) });
  if (verdict !== 'ALL') {
    searchParams.set('verdict', verdict);
  }

  const payload = await apiGet<Transaction[]>(
    `/transactions?${searchParams.toString()}`
  );
  return payload.data;
}
