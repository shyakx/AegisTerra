import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { insuranceApi, type Policy } from '../api/insurance';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function PoliciesPage() {
  const { hasPermission, hasRole } = useAuth();
  const isFarmer = hasRole('FARMER');
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['policies', q, status, page],
    queryFn: () => insuranceApi.searchPolicies({ q, status, page, size: 20, sort: 'policyNumber,asc' })
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">{isFarmer ? 'Farmer portal' : 'Insurance core'}</p>
          <h1 className="text-3xl font-semibold">{isFarmer ? 'My policies' : 'Policies'}</h1>
        </div>
        {!isFarmer ? (
          <div className="flex flex-wrap gap-2">
            <Link to="/insurance/products" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
              Products
            </Link>
            <Link to="/insurance/calculator" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
              Premium calculator
            </Link>
            <Link to="/insurance/reports" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
              Reports
            </Link>
            {hasPermission('policies:write') ? (
              <Link
                to="/policies/issue"
                className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90"
              >
                Issue policy
              </Link>
            ) : null}
          </div>
        ) : null}
      </div>

      <EnterpriseTable<Policy>
        title={isFarmer ? 'My cover' : 'Policy portfolio'}
        subtitle={
          isFarmer
            ? 'Policies linked to your farmer record'
            : 'Search by policy number; filter by lifecycle status'
        }
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : query.error ? 'Failed to load policies' : null}
        emptyMessage="No policies yet. Start the issuance wizard."
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        searchPlaceholder="Policy number…"
        filters={
          <label className="text-sm">
            <span className="sr-only">Status</span>
            <select
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value);
              }}
              className="rounded-xl border border-border bg-background px-3 py-2"
            >
              <option value="">All statuses</option>
              {[
                'UNDER_REVIEW',
                'PREMIUM_PENDING',
                'ACTIVE',
                'SUSPENDED',
                'EXPIRED',
                'CANCELLED',
                'REJECTED'
              ].map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
        }
        page={page}
        totalPages={query.data?.totalPages ?? 1}
        onPageChange={setPage}
        onExport={() => void insuranceApi.exportPolicies({ q, status })}
        getRowKey={(row) => row.id}
        columns={[
          {
            key: 'number',
            header: 'Policy',
            sortValue: (r) => r.policyNumber,
            render: (r) => (
              <Link to={`/policies/${r.id}`} className="font-medium text-primary hover:underline">
                {r.policyNumber}
              </Link>
            )
          },
          {
            key: 'premium',
            header: 'Premium',
            sortValue: (r) => r.premiumAmount,
            render: (r) =>
              `${Number(r.premiumAmount).toLocaleString()} ${r.currency}`
          },
          {
            key: 'coverage',
            header: 'Coverage',
            sortValue: (r) => r.coverageAmount,
            render: (r) =>
              `${Number(r.coverageAmount).toLocaleString()} ${r.currency}`
          },
          {
            key: 'period',
            header: 'Period',
            sortValue: (r) => r.startDate,
            render: (r) => `${r.startDate} → ${r.endDate}`
          },
          { key: 'status', header: 'Status', sortValue: (r) => r.status, render: (r) => r.status }
        ]}
      />
    </div>
  );
}
