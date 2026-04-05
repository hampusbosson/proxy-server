import type { LoadableListState, Transaction } from '../types';

type TransactionsTableProps = {
  state: LoadableListState<Transaction>;
};

export default function TransactionsTable({ state }: TransactionsTableProps) {
  if (state.status === 'loading') {
    return (
      <div className="table-state">
        <div className="spinner" />
        <p>Loading traffic data…</p>
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className="table-state error-state">
        <p>{state.error}</p>
      </div>
    );
  }

  if (!state.data.length) {
    return (
      <div className="table-state empty-state">
        <p>No transactions yet.</p>
      </div>
    );
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Time</th>
            <th>Method</th>
            <th>Host</th>
            <th>Path</th>
            <th>Verdict</th>
            <th>Duration</th>
            <th>Bytes</th>
            <th>Error</th>
          </tr>
        </thead>
        <tbody>
          {state.data.map((transaction, index) => (
            <tr key={`${transaction.timestampMs}-${transaction.host}-${transaction.path}-${index}`}>
              <td>{formatRelativeTime(transaction.timestampMs)}</td>
              <td>{transaction.method}</td>
              <td className="mono-cell">{transaction.host}:{transaction.port}</td>
              <td className="mono-cell path-cell" title={transaction.path}>
                {transaction.path}
              </td>
              <td>
                <span className={`verdict-pill verdict-${transaction.verdict.toLowerCase()}`}>
                  {transaction.verdict}
                </span>
              </td>
              <td>{transaction.durationMs} ms</td>
              <td>{formatBytes(transaction.bytesFromServer)}</td>
              <td className="error-cell">{transaction.errorMessage || '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatRelativeTime(timestampMs: number): string {
  if (!timestampMs) {
    return '—';
  }

  const diffMs = Date.now() - timestampMs;
  if (diffMs < 5000) {
    return 'just now';
  }

  const seconds = Math.floor(diffMs / 1000);
  if (seconds < 60) {
    return `${seconds}s ago`;
  }

  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `${minutes}m ago`;
  }

  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours}h ago`;
  }

  return new Date(timestampMs).toLocaleString();
}

function formatBytes(bytes: number): string {
  if (!bytes) {
    return '0 B';
  }

  if (bytes < 1024) {
    return `${bytes} B`;
  }

  const units = ['KB', 'MB', 'GB', 'TB'] as const;
  let value = bytes / 1024;
  let unit = units[0];

  for (let index = 1; index < units.length && value >= 1024; index += 1) {
    value /= 1024;
    unit = units[index];
  }

  return `${value.toFixed(value >= 10 ? 0 : 1)} ${unit}`;
}
