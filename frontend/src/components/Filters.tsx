import type { Dispatch, SetStateAction } from 'react';
import type { DashboardFilters } from '../types';

const LIMIT_OPTIONS = [25, 50, 100] as const;
const VERDICT_OPTIONS = ['ALL', 'ALLOWED', 'BLOCKED', 'ERROR'] as const;

type FiltersProps = {
  filters: DashboardFilters;
  onChange: Dispatch<SetStateAction<DashboardFilters>>;
};

export default function Filters({ filters, onChange }: FiltersProps) {
  return (
    <div className="filters">
      <label>
        <span>Verdict</span>
        <select
          value={filters.verdict}
          onChange={(event) =>
            onChange((current) => ({ ...current, verdict: event.target.value as DashboardFilters['verdict'] }))
          }
        >
          {VERDICT_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </label>

      <label>
        <span>Limit</span>
        <select
          value={filters.limit}
          onChange={(event) =>
            onChange((current) => ({ ...current, limit: Number(event.target.value) }))
          }
        >
          {LIMIT_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </label>

      <label className="toggle">
        <span>Auto-refresh</span>
        <button
          type="button"
          className={filters.autoRefresh ? 'toggle-button active' : 'toggle-button'}
          onClick={() =>
            onChange((current) => ({ ...current, autoRefresh: !current.autoRefresh }))
          }
          aria-pressed={filters.autoRefresh}
        >
          <span className="toggle-thumb" />
        </button>
      </label>
    </div>
  );
}
