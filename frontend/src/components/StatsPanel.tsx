import type { LoadableState, Stats } from '../types';

const STAT_CARDS: Array<{ key: keyof Stats; label: string }> = [
  { key: 'total', label: 'Total requests' },
  { key: 'allowed', label: 'Allowed' },
  { key: 'blocked', label: 'Blocked' },
  { key: 'error', label: 'Errors' },
  { key: 'avgDurationMs', label: 'Avg response time' },
  { key: 'bytesFromServerTotal', label: 'Total bytes relayed' }
];

type StatsPanelProps = {
  state: LoadableState<Stats>;
};

export default function StatsPanel({ state }: StatsPanelProps) {
  return (
    <section className="stats-grid">
      {STAT_CARDS.map((card) => {
        const value = state.data ? formatValue(card.key, state.data[card.key]) : '...';
        return (
          <article className="stat-card" key={card.key}>
            <span className="stat-label">{card.label}</span>
            <strong className="stat-value">{value}</strong>
          </article>
        );
      })}
    </section>
  );
}

function formatValue(key: keyof Stats, value: number): string {
  if (key === 'avgDurationMs') {
    return `${value.toLocaleString()} ms`;
  }

  if (key === 'bytesFromServerTotal') {
    return formatBytes(value);
  }

  return value.toLocaleString();
}

function formatBytes(bytes: number): string {
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
