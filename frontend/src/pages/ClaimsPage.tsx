import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { claimsApi, type Claim } from '../api/claims';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const STATUSES = [
  'DRAFT',
  'SUBMITTED',
  'UNDER_VALIDATION',
  'INSPECTION',
  'ASSESSMENT',
  'PENDING_DECISION',
  'RETURNED_FOR_INFO',
  'APPROVED',
  'REJECTED',
  'PAYMENT_PENDING',
  'CANCELLED',
  'CLOSED'
];

export default function ClaimsPage() {
  const { hasPermission, hasRole } = useAuth();
  const isFarmer = hasRole('FARMER');
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [claimTypeCode, setClaimTypeCode] = useState('');
  const [page, setPage] = useState(0);

  const typesQuery = useQuery({
    queryKey: ['claim-types'],
    queryFn: () => claimsApi.listTypes()
  });

  const query = useQuery({
    queryKey: ['claims', q, status, claimTypeCode, page],
    queryFn: () => claimsApi.search({ q, status, claimTypeCode, page, size: 20 })
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">{isFarmer ? 'Farmer portal' : 'Claims management'}</p>
          <h1 className="text-3xl font-semibold">{isFarmer ? 'My claims' : 'Claims'}</h1>
        </div>
        <div className="flex flex-wrap gap-2">
          {!isFarmer && hasPermission('tasks:read') ? (
            <Link to="/tasks?subjectType=CLAIM" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
              Claim tasks
            </Link>
          ) : null}
          {hasPermission('claims:write') ? (
            <Link to="/claims/new" className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90">
              {isFarmer ? 'File a claim' : 'New claim'}
            </Link>
          ) : null}
        </div>
      </div>

      <EnterpriseTable<Claim>
        title={isFarmer ? 'My claims' : 'Claim portfolio'}
        subtitle={
          isFarmer
            ? 'Loss events linked to your policies — open a claim for status and evidence'
            : 'Search by number or description; filter by status and type'
        }
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : query.error ? 'Failed to load claims' : null}
        emptyMessage="No claims yet. File a claim against an active policy."
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        searchPlaceholder="Claim number…"
        filters={
          <div className="flex flex-wrap gap-2">
            <select
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value);
              }}
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
            >
              <option value="">All statuses</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
            <select
              value={claimTypeCode}
              onChange={(e) => {
                setPage(0);
                setClaimTypeCode(e.target.value);
              }}
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
            >
              <option value="">All types</option>
              {(typesQuery.data ?? []).map((t) => (
                <option key={t.code} value={t.code}>
                  {t.name}
                </option>
              ))}
            </select>
          </div>
        }
        page={page}
        totalPages={query.data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        columns={[
          {
            key: 'number',
            header: 'Claim',
            sortValue: (r) => r.claimNumber,
            render: (r) => (
              <Link to={`/claims/${r.id}`} className="font-medium text-primary hover:underline">
                {r.claimNumber}
              </Link>
            )
          },
          {
            key: 'type',
            header: 'Type',
            sortValue: (r) => r.claimTypeCode ?? '',
            render: (r) => r.claimTypeCode ?? '—'
          },
          {
            key: 'amount',
            header: 'Claimed',
            sortValue: (r) => r.claimedAmount,
            render: (r) => `${r.claimedAmount.toLocaleString()} ${r.currency}`
          },
          {
            key: 'incident',
            header: 'Incident',
            sortValue: (r) => r.incidentDate,
            render: (r) => r.incidentDate
          },
          {
            key: 'status',
            header: 'Status',
            sortValue: (r) => r.status,
            render: (r) => (
              <span className="rounded-lg bg-background px-2 py-1 text-xs font-medium">{r.status}</span>
            )
          }
        ]}
      />
    </div>
  );
}
