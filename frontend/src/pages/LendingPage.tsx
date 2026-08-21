import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { lendingApi, type AgriculturalLoan } from '../api/lending';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';
import { StatusBadge } from '../components/StatusBadge';
import { PHOTOS } from '../media/photos';
import { WorkspaceBanner } from '../visuals/WorkspaceBanner';

export default function LendingPage() {
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['loans', q, status, page],
    queryFn: () => lendingApi.search({ q, status: status || undefined, page, size: 20 })
  });

  return (
    <div className="space-y-4">
      <WorkspaceBanner
        photo={PHOTOS.harvestAerial}
        eyebrow="Agricultural lending"
        title="Insured loans"
        description="Banks use farmer profiles, crop condition, and climate risk to approve and monitor agricultural credit. Insurance is embedded in the loan."
      />

      <EnterpriseTable<AgriculturalLoan>
        title="Loan book"
        subtitle="Climate-informed agricultural credit"
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : query.error ? 'Failed to load loans' : null}
        emptyMessage="No loan applications in this window."
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        searchPlaceholder="Loan number, borrower, farmer code…"
        filters={
          <select
            value={status}
            onChange={(e) => {
              setPage(0);
              setStatus(e.target.value);
            }}
            className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
          >
            <option value="">All statuses</option>
            <option value="UNDER_REVIEW">Under review</option>
            <option value="APPROVED">Approved</option>
            <option value="ACTIVE">Active</option>
            <option value="IN_ARREARS">In arrears</option>
            <option value="CLOSED">Closed</option>
          </select>
        }
        page={page}
        totalPages={query.data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        columns={[
          {
            key: 'loanNumber',
            header: 'Loan',
            render: (row) => (
              <Link className="font-medium text-primary" to={`/lending/${row.id}`}>
                {row.loanNumber}
              </Link>
            ),
            sortValue: (row) => row.loanNumber
          },
          {
            key: 'borrower',
            header: 'Borrower',
            render: (row) => (
              <span>
                {row.borrowerName}
                <span className="block text-xs text-textSecondary">{row.farmerCode}</span>
              </span>
            ),
            sortValue: (row) => row.borrowerName
          },
          {
            key: 'principal',
            header: 'Principal',
            render: (row) => `${row.principal.toLocaleString()} ${row.currency}`,
            sortValue: (row) => row.principal
          },
          {
            key: 'embed',
            header: 'Insurance embed',
            render: (row) => `${row.insuranceEmbedPct.toFixed(1)}%`
          },
          {
            key: 'risk',
            header: 'Repayment risk',
            render: (row) => <StatusBadge status={row.repaymentRisk} />
          },
          {
            key: 'crop',
            header: 'Crop condition',
            render: (row) => <StatusBadge status={row.cropCondition} />
          },
          {
            key: 'status',
            header: 'Status',
            render: (row) => <StatusBadge status={row.status} />
          }
        ]}
      />
    </div>
  );
}
