import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { agriApi, downloadCsv, type Farm } from '../api/agriculture';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { PHOTOS } from '../media/photos';
import { WorkspaceBanner } from '../visuals/WorkspaceBanner';

export default function FarmsPage() {
  const { hasRole } = useAuth();
  const isFarmer = hasRole('FARMER');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: ['farms', q, page],
    queryFn: () => agriApi.searchFarms({ q, page, size: 20 })
  });

  return (
    <div className="space-y-4">
      <WorkspaceBanner
        photo={PHOTOS.cropsClose}
        eyebrow={isFarmer ? 'My records' : 'Agricultural core'}
        title={isFarmer ? 'My farms' : 'Farms'}
        description={
          isFarmer
            ? 'Holdings linked to your farmer profile.'
            : 'Plots, boundaries, and crop records on the same current as climate risk.'
        }
      />
      <EnterpriseTable<Farm>
        title={isFarmer ? 'My farms' : 'Farm registry'}
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : query.error ? 'Failed to load farms' : null}
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        page={page}
        totalPages={query.data?.totalPages ?? 1}
        onPageChange={setPage}
        onExport={async () => downloadCsv('farms.csv', await agriApi.exportFarms({ q }))}
        getRowKey={(r) => r.id}
        columns={[
          {
            key: 'name',
            header: 'Farm',
            sortValue: (r) => r.farmName,
            render: (r) => (
              <Link to={`/farms/${r.id}`} className="font-medium text-primary hover:underline">
                {r.farmName}
              </Link>
            )
          },
          { key: 'code', header: 'Code', sortValue: (r) => r.farmCode, render: (r) => r.farmCode },
          { key: 'size', header: 'Size (ha)', sortValue: (r) => r.farmSizeHa ?? 0, render: (r) => r.farmSizeHa ?? '—' },
          { key: 'status', header: 'Status', sortValue: (r) => r.status, render: (r) => r.status }
        ]}
      />
    </div>
  );
}
