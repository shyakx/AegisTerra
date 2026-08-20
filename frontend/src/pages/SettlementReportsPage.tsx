import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useMemo, useState } from 'react';
import { settlementsApi, type SettlementReport } from '../api/settlements';
import { ApiError } from '../api/client';
import { exportRowsToCsv } from '../utils/exportCsv';

const NAMED_REPORTS = [
  { path: 'by-status', label: 'By status' },
  { path: 'by-provider', label: 'By provider' },
  { path: 'by-source', label: 'By source module' },
  { path: 'pending', label: 'Pending' },
  { path: 'failed', label: 'Failed payments' },
  { path: 'avg-cycle-time', label: 'Cycle time' }
] as const;

export default function SettlementReportsPage() {
  const [mode, setMode] = useState<'summary' | 'named'>('summary');
  const [path, setPath] = useState<(typeof NAMED_REPORTS)[number]['path']>('by-status');

  const summaryQuery = useQuery({
    queryKey: ['settlement-reports-summary'],
    queryFn: () => settlementsApi.reports(),
    enabled: mode === 'summary'
  });

  const namedQuery = useQuery({
    queryKey: ['settlement-report', path],
    queryFn: () => settlementsApi.report(path),
    enabled: mode === 'named'
  });

  const activeReport = useMemo(() => {
    if (mode === 'named') return namedQuery.data ?? null;
    return firstReport(summaryQuery.data);
  }, [mode, namedQuery.data, summaryQuery.data]);

  const reportsList = useMemo(() => asReportList(summaryQuery.data), [summaryQuery.data]);
  const rows = activeReport?.rows ?? [];
  const columns = rows.length > 0 ? Object.keys(rows[0]) : [];
  const loading = mode === 'summary' ? summaryQuery.isLoading : namedQuery.isLoading;
  const error = mode === 'summary' ? summaryQuery.error : namedQuery.error;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Financial settlement</p>
          <h1 className="text-3xl font-semibold">Settlement reports</h1>
        </div>
        <Link to="/settlements" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          Settlements
        </Link>
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setMode('summary')}
          className={`rounded-xl px-3 py-1.5 text-sm ${
            mode === 'summary' ? 'bg-primary text-white' : 'border border-border bg-surface'
          }`}
        >
          Summary
        </button>
        <button
          type="button"
          onClick={() => setMode('named')}
          className={`rounded-xl px-3 py-1.5 text-sm ${
            mode === 'named' ? 'bg-primary text-white' : 'border border-border bg-surface'
          }`}
        >
          Named reports
        </button>
      </div>

      {mode === 'named' ? (
        <div className="flex flex-wrap gap-2">
          {NAMED_REPORTS.map((r) => (
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
      ) : null}

      {mode === 'summary' && reportsList.length > 1 ? (
        <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {reportsList.map((r) => (
            <button
              key={r.reportCode}
              type="button"
              onClick={() => {
                setMode('named');
                const match = NAMED_REPORTS.find((n) => n.path === r.reportCode);
                if (match) setPath(match.path);
              }}
              className="rounded-2xl border border-border bg-surface p-4 text-left hover:bg-background"
            >
              <p className="font-medium">{r.reportCode}</p>
              <p className="text-sm text-textSecondary">
                {r.totalCount} rows
                {r.totalAmount != null ? ` · ${Number(r.totalAmount).toLocaleString()}` : ''}
              </p>
            </button>
          ))}
        </section>
      ) : null}

      {error ? (
        <p className="text-sm text-danger" role="alert">
          {error instanceof ApiError ? error.message : 'Failed to load report'}
        </p>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold">
              {mode === 'named'
                ? NAMED_REPORTS.find((r) => r.path === path)?.label
                : activeReport?.reportCode ?? 'Settlement report'}
            </h2>
            <p className="text-sm text-textSecondary">
              {activeReport?.totalCount ?? 0} rows
              {activeReport?.totalAmount != null
                ? ` · total ${Number(activeReport.totalAmount).toLocaleString()}`
                : ''}
            </p>
          </div>
          {rows.length > 0 ? (
            <button
              type="button"
              className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-background"
              onClick={() =>
                exportRowsToCsv(
                  `settlement-report-${mode === 'named' ? path : activeReport?.reportCode ?? 'summary'}.csv`,
                  rows as Array<Record<string, unknown>>,
                  columns
                )
              }
            >
              Export CSV
            </button>
          ) : null}
        </div>

        <div className="mt-4 overflow-x-auto">
          {loading ? (
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

function asReportList(data: SettlementReport | SettlementReport[] | undefined): SettlementReport[] {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (data.byStatus || data.pendingCount != null) {
    return [
      {
        reportCode: data.reportCode ?? 'SETTLEMENT_SUMMARY',
        rows: data.byStatus ?? [],
        totalCount: data.pendingCount ?? 0,
        totalAmount: data.completedTotal ?? null
      },
      {
        reportCode: 'by-provider',
        rows: data.byProvider ?? [],
        totalCount: data.completedCount ?? 0,
        totalAmount: data.completedTotal ?? null
      }
    ];
  }
  return [data];
}

function firstReport(data: SettlementReport | SettlementReport[] | undefined): SettlementReport | null {
  const list = asReportList(data);
  return list[0] ?? null;
}

function formatCell(value: unknown): string {
  if (value == null) return '—';
  if (typeof value === 'number') return value.toLocaleString();
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}
