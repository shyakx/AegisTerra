import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { settlementsApi, type Settlement } from '../api/settlements';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const PENDING_STATUSES = new Set([
  'PENDING',
  'UNDER_REVIEW',
  'APPROVED',
  'PROCESSING',
  'SENT',
  'RETRY_PENDING'
]);

export default function SettlementDashboardPage() {
  const { hasPermission } = useAuth();

  const listQuery = useQuery({
    queryKey: ['settlements-dashboard'],
    queryFn: () => settlementsApi.list({ page: 0, size: 100, sort: 'createdAt,desc' })
  });

  const reportsQuery = useQuery({
    queryKey: ['settlements-dashboard-reports'],
    queryFn: () => settlementsApi.reports(),
    enabled: hasPermission('reports:settlements') || hasPermission('settlements:read')
  });

  const rows = listQuery.data?.content ?? [];
  const pending = rows.filter((s) => PENDING_STATUSES.has(s.status));
  const failed = rows.filter((s) => s.status === 'FAILED');
  const completed = rows.filter((s) => s.status === 'COMPLETED' || s.status === 'CLOSED');
  const pendingAmount = pending.reduce((sum, s) => sum + (s.amount ?? 0), 0);
  const completedAmount = completed.reduce((sum, s) => sum + (s.amount ?? 0), 0);
  const currency = rows[0]?.currency ?? 'RWF';

  const reportSummary = normalizeReports(reportsQuery.data);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Financial settlement</p>
          <h1 className="text-3xl font-semibold">Settlement dashboard</h1>
          <p className="mt-1 text-sm text-textSecondary">
            Portfolio overview for indemnity and outbound payment settlements.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/settlements" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
            All settlements
          </Link>
          <Link
            to="/tasks?subjectType=SETTLEMENT"
            className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface"
          >
            Settlement tasks
          </Link>
          {hasPermission('settlements:approve') ? (
            <Link
              to="/settlements/finance"
              className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90"
            >
              Finance review
            </Link>
          ) : null}
        </div>
      </div>

      {listQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {listQuery.error instanceof ApiError ? listQuery.error.message : 'Failed to load settlements'}
        </p>
      ) : null}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Settlement summary">
        <StatCard
          label="Pending"
          value={String(pending.length)}
          detail={`${pendingAmount.toLocaleString()} ${currency}`}
        />
        <StatCard
          label="Completed"
          value={String(completed.length)}
          detail={`${completedAmount.toLocaleString()} ${currency}`}
        />
        <StatCard label="Failed" value={String(failed.length)} detail="Require retry or close" />
        <StatCard
          label="Loaded rows"
          value={String(listQuery.data?.totalElements ?? rows.length)}
          detail={listQuery.isLoading ? 'Loading…' : 'From current page window'}
        />
      </section>

      {reportSummary.length > 0 ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Report highlights</h2>
          <ul className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {reportSummary.map((item) => (
              <li key={item.reportCode} className="rounded-xl border border-border px-3 py-3 text-sm">
                <p className="font-medium">{item.reportCode}</p>
                <p className="text-textSecondary">
                  {item.totalCount} rows
                  {item.totalAmount != null ? ` · ${Number(item.totalAmount).toLocaleString()}` : ''}
                </p>
              </li>
            ))}
          </ul>
          {hasPermission('reports:settlements') ? (
            <Link to="/settlements/reports" className="mt-4 inline-block text-sm text-primary hover:underline">
              Open settlement reports
            </Link>
          ) : null}
        </section>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-lg font-semibold">Recent settlements</h2>
          <Link to="/settlements" className="text-sm text-primary hover:underline">
            View all
          </Link>
        </div>
        {listQuery.isLoading ? (
          <p className="mt-3 text-sm text-textSecondary">Loading…</p>
        ) : rows.length === 0 ? (
          <p className="mt-3 text-sm text-textSecondary">No settlements yet.</p>
        ) : (
          <ul className="mt-4 divide-y divide-border">
            {rows.slice(0, 8).map((s) => (
              <RecentRow key={s.id} settlement={s} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function StatCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-5">
      <p className="text-sm text-textSecondary">{label}</p>
      <p className="mt-2 text-2xl font-semibold">{value}</p>
      <p className="mt-1 text-sm text-textSecondary">{detail}</p>
    </div>
  );
}

function RecentRow({ settlement }: { settlement: Settlement }) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-2 py-3 text-sm">
      <div>
        <Link to={`/settlements/${settlement.id}`} className="font-medium text-primary hover:underline">
          {settlement.settlementNumber}
        </Link>
        <p className="text-textSecondary">
          {settlement.sourceModule} · {settlement.providerCode}
        </p>
      </div>
      <div className="text-right">
        <p className="font-medium">
          {settlement.amount.toLocaleString()} {settlement.currency}
        </p>
        <p className="text-xs text-textSecondary">{settlement.status}</p>
      </div>
    </li>
  );
}

function normalizeReports(data: unknown): Array<{
  reportCode: string;
  totalCount: number;
  totalAmount: number | null;
}> {
  if (!data) return [];
  if (Array.isArray(data)) {
    return data.map((r) => ({
      reportCode: String((r as { reportCode?: string }).reportCode ?? 'report'),
      totalCount: Number((r as { totalCount?: number }).totalCount ?? 0),
      totalAmount: (r as { totalAmount?: number | null }).totalAmount ?? null
    }));
  }
  if (typeof data === 'object' && data !== null && 'reportCode' in data) {
    const r = data as { reportCode: string; totalCount?: number; totalAmount?: number | null };
    return [{ reportCode: r.reportCode, totalCount: r.totalCount ?? 0, totalAmount: r.totalAmount ?? null }];
  }
  return [];
}
