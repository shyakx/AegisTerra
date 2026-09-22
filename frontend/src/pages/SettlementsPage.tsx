import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { settlementsApi, type Settlement } from '../api/settlements';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const STATUSES = [
  'PENDING',
  'UNDER_REVIEW',
  'APPROVED',
  'PROCESSING',
  'SENT',
  'CONFIRMED',
  'COMPLETED',
  'FAILED',
  'REJECTED',
  'CANCELLED',
  'RETRY_PENDING',
  'REVERSED',
  'CLOSED'
];

const SOURCE_MODULES = [
  'CLAIMS',
  'SUBSIDY',
  'PREMIUM_REFUND',
  'PARTNER_REIMBURSEMENT',
  'FARMER_INCENTIVE',
  'DISASTER_RELIEF',
  'MANUAL'
];

export default function SettlementsPage() {
  const { hasPermission, hasRole } = useAuth();
  const isFarmer = hasRole('FARMER');
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [sourceModule, setSourceModule] = useState('');
  const [providerCode, setProviderCode] = useState('');
  const [page, setPage] = useState(0);
  const [usePostSearch, setUsePostSearch] = useState(false);

  const providersQuery = useQuery({
    queryKey: ['payment-providers'],
    queryFn: () => settlementsApi.listPaymentProviders(),
    enabled: !isFarmer
  });

  const query = useQuery({
    queryKey: ['settlements', q, status, sourceModule, providerCode, page, usePostSearch],
    queryFn: () =>
      usePostSearch
        ? settlementsApi.search({
            q: q || undefined,
            status: status || undefined,
            sourceModule: sourceModule || undefined,
            providerCode: providerCode || undefined,
            page,
            size: 20,
            sort: 'createdAt,desc'
          })
        : settlementsApi.list({
            q: q || undefined,
            status: status || undefined,
            sourceModule: sourceModule || undefined,
            providerCode: providerCode || undefined,
            page,
            size: 20,
            sort: 'createdAt,desc'
          })
  });

  const rows = query.data?.content ?? [];
  const completedAmount = rows
    .filter((row) => row.status === 'COMPLETED')
    .reduce((total, row) => total + row.amount, 0);
  const pendingAmount = rows
    .filter((row) => ['PENDING', 'UNDER_REVIEW', 'APPROVED', 'PROCESSING', 'SENT', 'CONFIRMED'].includes(row.status))
    .reduce((total, row) => total + row.amount, 0);
  const statusLabel = (status: string) =>
    ({
      COMPLETED: 'Paid',
      PENDING: 'Pending review',
      UNDER_REVIEW: 'Under review',
      APPROVED: 'Approved for payment',
      PROCESSING: 'Processing',
      SENT: 'Sent to provider',
      CONFIRMED: 'Payment confirmed',
      FAILED: 'Payment failed',
      REVERSED: 'Reversed'
    })[status] ?? status.replace(/_/g, ' ');

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">{isFarmer ? 'My payouts' : 'Payouts'}</p>
          <h1 className="text-3xl font-semibold">{isFarmer ? 'My payouts' : 'Payouts'}</h1>
        </div>
        {!isFarmer ? <div className="flex flex-wrap gap-2">
          <Link
            to="/settlements/dashboard"
            className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface"
          >
            Dashboard
          </Link>
          {hasPermission('tasks:read') ? (
            <Link
              to="/tasks?subjectType=SETTLEMENT"
              className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface"
            >
              Settlement tasks
            </Link>
          ) : null}
          {hasPermission('settlements:approve') ? (
            <Link
              to="/settlements/finance"
              className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface"
            >
              Finance review
            </Link>
          ) : null}
          {hasPermission('reports:settlements') ? (
            <Link
              to="/settlements/reports"
              className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface"
            >
              Reports
            </Link>
          ) : null}
          {hasPermission('ledger:read') ? (
            <Link
              to="/ledger"
              className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface"
            >
              Ledger
            </Link>
          ) : null}
        </div> : null}
      </div>

      {isFarmer ? (
        <section className="grid gap-3 sm:grid-cols-3">
          <div className="rounded-2xl border border-border border-l-4 border-l-primary bg-surface p-4 shadow-sm">
            <p className="text-sm text-textSecondary">Records shown</p>
            <p className="mt-1 text-2xl font-semibold">{query.data?.totalElements ?? 0}</p>
            <p className="mt-1 text-xs text-textSecondary">Linked to your approved claims</p>
          </div>
          <div className="rounded-2xl border border-border border-l-4 border-l-emerald-500 bg-surface p-4 shadow-sm">
            <p className="text-sm text-textSecondary">Paid in this view</p>
            <p className="mt-1 text-2xl font-semibold">{completedAmount.toLocaleString()} RWF</p>
            <p className="mt-1 text-xs text-textSecondary">Completed payouts</p>
          </div>
          <div className="rounded-2xl border border-border border-l-4 border-l-amber-500 bg-surface p-4 shadow-sm">
            <p className="text-sm text-textSecondary">In progress</p>
            <p className="mt-1 text-2xl font-semibold">{pendingAmount.toLocaleString()} RWF</p>
            <p className="mt-1 text-xs text-textSecondary">Awaiting completion</p>
          </div>
        </section>
      ) : null}

      <EnterpriseTable<Settlement>
        title="Settlement portfolio"
        subtitle={isFarmer ? 'Payouts linked to your farmer profile and approved claims.' : 'Search by settlement number or source reference; filter by status, module, and provider'}
        rows={rows}
        loading={query.isLoading}
        error={
          query.error instanceof ApiError
            ? query.error.message
            : query.error
              ? 'Failed to load settlements'
              : null
        }
        emptyMessage="No settlements yet. Approved claims create settlements via the Settlement Platform."
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        searchPlaceholder="Settlement number…"
        filters={isFarmer ? null : (
          <div className="flex flex-wrap items-center gap-2">
            <select
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value);
              }}
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
              aria-label="Status filter"
            >
              <option value="">All statuses</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
            <select
              value={sourceModule}
              onChange={(e) => {
                setPage(0);
                setSourceModule(e.target.value);
              }}
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
              aria-label="Source module filter"
            >
              <option value="">All sources</option>
              {SOURCE_MODULES.map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
            <select
              value={providerCode}
              onChange={(e) => {
                setPage(0);
                setProviderCode(e.target.value);
              }}
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
              aria-label="Provider filter"
            >
              <option value="">All providers</option>
              {(providersQuery.data ?? []).map((p) => (
                <option key={p.providerCode} value={p.providerCode}>
                  {p.displayName}
                </option>
              ))}
            </select>
            <label className="flex items-center gap-2 text-sm text-textSecondary">
              <input
                type="checkbox"
                checked={usePostSearch}
                onChange={(e) => {
                  setPage(0);
                  setUsePostSearch(e.target.checked);
                }}
              />
              Advanced search
            </label>
          </div>
        )}
        page={page}
        totalPages={query.data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        columns={[
          {
            key: 'number',
            header: 'Settlement',
            sortValue: (r) => r.settlementNumber,
            render: (r) => (
              <Link to={`/settlements/${r.id}`} className="font-medium text-primary hover:underline">
                {r.settlementNumber}
              </Link>
            )
          },
          {
            key: 'source',
            header: 'Source',
            sortValue: (r) => r.sourceModule,
            render: (r) => (
              <span>
                {isFarmer ? 'Claim payout' : r.sourceModule}
                {r.sourceReference ? (
                  <span className="text-textSecondary"> · {r.sourceReference}</span>
                ) : null}
              </span>
            )
          },
          {
            key: 'amount',
            header: 'Amount',
            sortValue: (r) => r.amount,
            render: (r) => `${r.amount.toLocaleString()} ${r.currency}`
          },
          {
            key: 'provider',
            header: 'Provider',
            sortValue: (r) => r.providerCode,
            render: (r) => (isFarmer ? r.paymentMethod.replace(/_/g, ' ') : r.providerCode)
          },
          {
            key: 'status',
            header: 'Status',
            sortValue: (r) => r.status,
            render: (r) => (
              <span className="rounded-lg bg-background px-2 py-1 text-xs font-medium">{statusLabel(r.status)}</span>
            )
          }
        ]}
      />
    </div>
  );
}
