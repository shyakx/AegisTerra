import { useQuery } from '@tanstack/react-query';
import { Link, Navigate } from 'react-router-dom';
import { useState } from 'react';
import { agriApi, downloadCsv, type Farmer } from '../api/agriculture';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function FarmersPage() {
  const { hasPermission, hasRole, user } = useAuth();
  const isFarmer = hasRole('FARMER');
  const linkedFarmerId = user?.farmerId;
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['farmers', q, status, page],
    queryFn: () => agriApi.searchFarmers({ q, status, page, size: 20, sort: 'lastName,asc' })
  });

  if (isFarmer && linkedFarmerId && !q && !status) {
    return <Navigate to={`/farmers/${linkedFarmerId}`} replace />;
  }

  async function handleExport() {
    const csv = await agriApi.exportFarmers({ q, status });
    downloadCsv('farmers.csv', csv);
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">{isFarmer ? 'Farmer portal' : 'Agricultural core'}</p>
          <h1 className="text-3xl font-semibold">{isFarmer ? 'My profile' : 'Farmers'}</h1>
        </div>
        {hasPermission('farmers:write') ? (
          <Link
            to="/farmers/register"
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90"
          >
            Register farmer
          </Link>
        ) : null}
      </div>

      <EnterpriseTable<Farmer>
        title={isFarmer ? 'My farmer record' : 'Farmer registry'}
        subtitle={
          isFarmer
            ? 'Your linked farmer profile (subject-scoped)'
            : 'Search by name, national ID, phone, or farmer code'
        }
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : query.error ? 'Failed to load farmers' : null}
        emptyMessage="No farmers yet. Start the registration wizard."
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        searchPlaceholder="National ID, phone, name, farmer code…"
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
              <option value="PENDING_VERIFICATION">Pending verification</option>
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </label>
        }
        page={page}
        totalPages={query.data?.totalPages ?? 1}
        onPageChange={setPage}
        onExport={() => void handleExport()}
        getRowKey={(row) => row.id}
        columns={[
          {
            key: 'name',
            header: 'Name',
            sortValue: (r) => `${r.lastName} ${r.firstName}`,
            render: (r) => (
              <Link to={`/farmers/${r.id}`} className="font-medium text-primary hover:underline">
                {r.firstName} {r.lastName}
              </Link>
            )
          },
          { key: 'code', header: 'Code', sortValue: (r) => r.farmerCode ?? '', render: (r) => r.farmerCode ?? '—' },
          { key: 'nid', header: 'National ID', sortValue: (r) => r.nationalId, render: (r) => r.nationalId },
          { key: 'phone', header: 'Phone', sortValue: (r) => r.phoneNumber, render: (r) => r.phoneNumber },
          { key: 'status', header: 'Status', sortValue: (r) => r.status, render: (r) => r.status }
        ]}
      />
    </div>
  );
}
