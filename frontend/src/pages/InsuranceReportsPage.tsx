import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { insuranceApi } from '../api/insurance';
import { ApiError } from '../api/client';
import { exportRowsToCsv } from '../utils/exportCsv';

const REPORTS = [
  { path: 'active-policies', label: 'Active policies' },
  { path: 'expired-policies', label: 'Expired policies' },
  { path: 'coverage-by-crop', label: 'Coverage by crop' },
  { path: 'premium-revenue', label: 'Premium revenue' },
  { path: 'portfolio-distribution', label: 'Portfolio distribution' },
  { path: 'renewals', label: 'Renewals' },
  { path: 'cancellations', label: 'Cancellations' }
] as const;

export default function InsuranceReportsPage() {
  const [path, setPath] = useState<(typeof REPORTS)[number]['path']>('active-policies');
  const reportQuery = useQuery({
    queryKey: ['insurance-report', path],
    queryFn: () => insuranceApi.report(path)
  });

  const rows = reportQuery.data?.rows ?? [];
  const columns = rows.length > 0 ? Object.keys(rows[0]) : [];

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-textSecondary">Insurance core</p>
        <h1 className="text-3xl font-semibold">Insurance reports</h1>
      </div>

      <div className="flex flex-wrap gap-2">
        {REPORTS.map((r) => (
          <button
            key={r.path}
            type="button"
            onClick={() => setPath(r.path)}
            className={`rounded-full px-3 py-1.5 text-sm ${
              path === r.path ? 'bg-primary text-white' : 'border border-border bg-surface'
            }`}
          >
            {r.label}
          </button>
        ))}
      </div>

      {reportQuery.isError ? (
        <p className="text-sm text-red-600">
          {reportQuery.error instanceof ApiError ? reportQuery.error.message : 'Failed to load report'}
        </p>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold">{REPORTS.find((r) => r.path === path)?.label}</h2>
            <p className="text-sm text-textSecondary">
              {reportQuery.data?.totalCount ?? 0} rows
              {reportQuery.data?.totalAmount != null
                ? ` · total ${Number(reportQuery.data.totalAmount).toLocaleString()}`
                : ''}
            </p>
          </div>
          <button
            type="button"
            className="rounded-xl border border-border px-3 py-2 text-sm font-medium hover:bg-background disabled:opacity-50"
            disabled={rows.length === 0}
            onClick={() => exportRowsToCsv(`insurance-${path}`, rows, columns)}
          >
            Export CSV
          </button>
        </div>

        <div className="mt-4 overflow-x-auto">
          {reportQuery.isLoading ? (
            <p className="text-sm text-textSecondary">Loading…</p>
          ) : rows.length === 0 ? (
            <p className="text-sm text-textSecondary">No rows for this report.</p>
          ) : (
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border text-textSecondary">
                  {columns.map((col) => (
                    <th key={col} className="px-3 py-2 font-medium">
                      {col}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row, idx) => (
                  <tr key={idx} className="border-b border-border/60">
                    {columns.map((col) => (
                      <td key={col} className="px-3 py-2">
                        {formatCell(row[col])}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>
    </div>
  );
}

function formatCell(value: unknown): string {
  if (value == null) return '—';
  if (typeof value === 'number') return value.toLocaleString();
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}
