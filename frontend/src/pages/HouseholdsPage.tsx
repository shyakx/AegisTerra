import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { agriApi, downloadCsv, type Household } from '../api/agriculture';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function HouseholdsPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('farmers:write');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [code, setCode] = useState('');
  const [headName, setHeadName] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ['households', q, page],
    queryFn: () => agriApi.searchHouseholds({ q, page, size: 20 })
  });

  async function handleCreate(event: React.FormEvent) {
    event.preventDefault();
    if (!canWrite) return;
    setFormError(null);
    try {
      await agriApi.createHousehold({ code, headName });
      setCode('');
      setHeadName('');
      await query.refetch();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to create household');
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-textSecondary">Agricultural core</p>
        <h1 className="text-3xl font-semibold">Households</h1>
      </div>

      {canWrite ? (
      <form onSubmit={handleCreate} className="grid gap-3 rounded-2xl border border-border bg-surface p-4 md:grid-cols-3 md:p-6">
        <label className="text-sm">
          Household code
          <input
            required
            value={code}
            onChange={(e) => setCode(e.target.value)}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
          />
        </label>
        <label className="text-sm">
          Head name
          <input
            value={headName}
            onChange={(e) => setHeadName(e.target.value)}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
          />
        </label>
        <div className="flex items-end">
          <button type="submit" className="w-full rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white">
            Create household
          </button>
        </div>
        {formError ? (
          <p className="md:col-span-3 text-sm text-red-700" role="alert">
            {formError}
          </p>
        ) : null}
      </form>
      ) : null}

      <EnterpriseTable<Household>
        title="Household list"
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : query.error ? 'Failed to load' : null}
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        page={page}
        totalPages={query.data?.totalPages ?? 1}
        onPageChange={setPage}
        onExport={() => {
          const rows = query.data?.content ?? [];
          const csv = ['code,headName,status', ...rows.map((r) => `${r.code},${r.headName ?? ''},${r.status}`)].join('\n');
          downloadCsv('households.csv', csv);
        }}
        getRowKey={(r) => r.id}
        columns={[
          { key: 'code', header: 'Code', sortValue: (r) => r.code, render: (r) => r.code },
          { key: 'head', header: 'Head', sortValue: (r) => r.headName ?? '', render: (r) => r.headName ?? '—' },
          { key: 'status', header: 'Status', sortValue: (r) => r.status, render: (r) => r.status }
        ]}
      />
    </div>
  );
}
