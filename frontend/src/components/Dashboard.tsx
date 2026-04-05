import { useEffect, useMemo, useState } from 'react';
import Filters from './Filters';
import StatsPanel from './StatsPanel';
import TransactionsTable from './TransactionsTable';
import { fetchHealth, fetchStats, fetchTransactions } from '../api/requests';
import {
  DashboardFilters,
  HealthState,
  LoadableListState,
  LoadableState,
  Stats,
  Transaction
} from '../types';

const DEFAULT_LIMIT = 50;
const REFRESH_MS = 2500;

export default function Dashboard() {
  const [health, setHealth] = useState<HealthState>({
    status: 'loading',
    message: 'Checking API'
  });
  const [statsState, setStatsState] = useState<LoadableState<Stats>>({
    status: 'loading',
    data: null,
    error: null
  });
  const [transactionsState, setTransactionsState] = useState<LoadableListState<Transaction>>({
    status: 'loading',
    data: [],
    error: null
  });
  const [filters, setFilters] = useState<DashboardFilters>({
    verdict: 'ALL',
    limit: DEFAULT_LIMIT,
    autoRefresh: true
  });

  useEffect(() => {
    let cancelled = false;

    async function load() {
      const [healthResult, statsResult, transactionsResult] = await Promise.allSettled([
        fetchHealth(),
        fetchStats(),
        fetchTransactions(filters.limit, filters.verdict)
      ]);

      if (cancelled) {
        return;
      }

      if (healthResult.status === 'fulfilled') {
        setHealth({ status: 'healthy', message: healthResult.value });
      } else {
        setHealth({
          status: 'unreachable',
          message: healthResult.reason instanceof Error ? healthResult.reason.message : 'API unreachable'
        });
      }

      if (statsResult.status === 'fulfilled') {
        setStatsState({ status: 'ready', data: statsResult.value, error: null });
      } else {
        setStatsState({
          status: 'error',
          data: null,
          error: statsResult.reason instanceof Error ? statsResult.reason.message : 'Failed to load stats'
        });
      }

      if (transactionsResult.status === 'fulfilled') {
        const ordered = [...transactionsResult.value].reverse();
        setTransactionsState({ status: 'ready', data: ordered, error: null });
      } else {
        setTransactionsState({
          status: 'error',
          data: [],
          error:
            transactionsResult.reason instanceof Error
              ? transactionsResult.reason.message
              : 'Failed to load transactions'
        });
      }
    }

    void load();

    if (!filters.autoRefresh) {
      return () => {
        cancelled = true;
      };
    }

    const intervalId = window.setInterval(() => {
      void load();
    }, REFRESH_MS);

    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [filters.autoRefresh, filters.limit, filters.verdict]);

  const pageError = useMemo(() => {
    if (statsState.error && transactionsState.error) {
      return 'The API is not returning dashboard data. Check that the backend is running locally.';
    }
    return null;
  }, [statsState.error, transactionsState.error]);

  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Developer traffic monitor</p>
          <h1>Proxy Inspector</h1>
        </div>
        <div
          className={`status-badge ${
            health.status === 'healthy' ? 'status-healthy' : 'status-unreachable'
          }`}
        >
          <span className="status-dot" />
          {health.status === 'healthy' ? 'API healthy' : 'API unreachable'}
        </div>
      </header>

      <StatsPanel state={statsState} />

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Transactions</h2>
            <p className="panel-copy">Recent activity from the forward proxy.</p>
          </div>
          <Filters filters={filters} onChange={setFilters} />
        </div>

        {pageError ? <div className="error-banner">{pageError}</div> : null}

        <TransactionsTable state={transactionsState} />
      </section>
    </div>
  );
}
