import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { insuranceApi, type Policy } from '../api/insurance';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { PHOTOS } from '../media/photos';
import { WorkspaceBanner } from '../visuals/WorkspaceBanner';

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
      <WorkspaceBanner
        photo={PHOTOS.operator}
        eyebrow={isFarmer ? 'My records' : 'Insurance core'}
        title={isFarmer ? 'My policies' : 'Policies'}
        description={
          isFarmer
            ? 'Cover issued by partner insurers against your registered farms.'
            : 'Partner products administered on climate-verified farm records. AegisTerra does not underwrite.'
        }
        actions={
          !isFarmer ? (
            <>
              {hasPermission('policies:write') ? (
                <Link to="/policies/issue" className="at-btn rounded-full bg-white px-4 py-2 text-sm font-semibold text-primary">
                  Issue policy
                </Link>
              ) : null}
              <Link to="/insurance/products" className="rounded-full px-4 py-2 text-sm font-semibold text-white">
                Products
              </Link>
              <Link to="/insurance/calculator" className="rounded-full px-4 py-2 text-sm font-semibold text-white">
                Calculator
              </Link>
              <Link to="/insurance/reports" className="rounded-full px-4 py-2 text-sm font-semibold text-white">
                Reports
              </Link>
            </>
          ) : undefined
        }
      />

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
