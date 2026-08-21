import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { aggregatorApi, type InsuredInputSale } from '../api/guidance';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';
import { StatusBadge } from '../components/StatusBadge';
import { PHOTOS } from '../media/photos';
import { WorkspaceBanner } from '../visuals/WorkspaceBanner';

export default function InsuredInputsPage() {
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: ['insured-inputs', q, page],
    queryFn: () => aggregatorApi.listSales({ q, page, size: 20 })
  });

  return (
    <div className="space-y-4">
      <WorkspaceBanner
        photo={PHOTOS.wheatClose}
        eyebrow="Aggregator network"
        title="Insured inputs"
        description="Seed and fertilizer sales carry an embedded insurance premium. The farmer who buys the bag buys an insured product; aggregators remain the onboarding channel."
      />

      <EnterpriseTable<InsuredInputSale>
        title="Input sales"
        subtitle="Premium embedded at point of sale"
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : query.error ? 'Failed to load sales' : null}
        emptyMessage="No insured input sales recorded."
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        searchPlaceholder="Receipt, farmer, product…"
        page={page}
        totalPages={query.data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        columns={[
          {
            key: 'receipt',
            header: 'Receipt',
            render: (row) => <span className="font-medium">{row.receiptNumber}</span>,
            sortValue: (row) => row.receiptNumber
          },
          {
            key: 'farmer',
            header: 'Farmer',
            render: (row) => (
              <Link className="text-primary" to={`/farmers/${row.farmerId}`}>
                {row.farmerName}
                <span className="block text-xs text-textSecondary">{row.farmerCode}</span>
              </Link>
            )
          },
          {
            key: 'product',
            header: 'Product',
            render: (row) => (
              <span>
                {row.productName}
                <span className="block text-xs text-textSecondary">{row.productType}</span>
              </span>
            )
          },
          {
            key: 'premium',
            header: 'Embedded premium',
            render: (row) => `${row.premiumEmbedded.toLocaleString()} ${row.currency}`
          },
          {
            key: 'sold',
            header: 'Sold',
            render: (row) => new Date(row.soldAt).toLocaleDateString()
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
