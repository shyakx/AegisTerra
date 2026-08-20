import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { toast } from 'sonner';
import { agriApi, type Crop } from '../api/agriculture';
import { ApiError } from '../api/client';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { useAuth } from '../auth/AuthContext';

export default function CropsPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('farms:write');
  const queryClient = useQueryClient();
  const [q, setQ] = useState('');
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const query = useQuery({ queryKey: ['crops'], queryFn: () => agriApi.listCrops() });

  const createMutation = useMutation({
    mutationFn: () => agriApi.createCrop({ code, name, status: 'ACTIVE' }),
    onSuccess: async () => {
      setCode('');
      setName('');
      toast.success('Crop created');
      await queryClient.invalidateQueries({ queryKey: ['crops'] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed')
  });

  const rows = (query.data ?? []).filter(
    (c) => !q || c.name.toLowerCase().includes(q.toLowerCase()) || c.code.toLowerCase().includes(q.toLowerCase())
  );

  return (
    <div className="space-y-4">
      <h1 className="text-3xl font-semibold">Crops</h1>
      {canWrite ? (
      <form
        className="grid gap-3 rounded-2xl border border-border bg-surface p-4 md:grid-cols-3"
        onSubmit={(e) => {
          e.preventDefault();
          createMutation.mutate();
        }}
      >
        <label className="text-sm">
          Code
          <input required value={code} onChange={(e) => setCode(e.target.value)} className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2" />
        </label>
        <label className="text-sm">
          Name
          <input required value={name} onChange={(e) => setName(e.target.value)} className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2" />
        </label>
        <div className="flex items-end">
          <button type="submit" className="w-full rounded-xl bg-primary px-4 py-2 text-sm text-white">
            Add crop
          </button>
        </div>
      </form>
      ) : null}
      <EnterpriseTable<Crop>
        title="Crop catalog"
        rows={rows}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : null}
        searchValue={q}
        onSearchChange={setQ}
        getRowKey={(r) => r.id}
        columns={[
          { key: 'code', header: 'Code', sortValue: (r) => r.code, render: (r) => r.code },
          { key: 'name', header: 'Name', sortValue: (r) => r.name, render: (r) => r.name },
          { key: 'sci', header: 'Scientific', sortValue: (r) => r.scientificName ?? '', render: (r) => r.scientificName ?? '—' },
          { key: 'status', header: 'Status', sortValue: (r) => r.status, render: (r) => r.status }
        ]}
      />
    </div>
  );
}
