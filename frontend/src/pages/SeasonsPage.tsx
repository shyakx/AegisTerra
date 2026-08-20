import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { toast } from 'sonner';
import { agriApi, type Season } from '../api/agriculture';
import { ApiError } from '../api/client';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { useAuth } from '../auth/AuthContext';

export default function SeasonsPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('farms:write');
  const queryClient = useQueryClient();
  const [q, setQ] = useState('');
  const [form, setForm] = useState({ code: '', name: '', startDate: '', endDate: '' });
  const query = useQuery({ queryKey: ['seasons'], queryFn: () => agriApi.listSeasons() });

  const createMutation = useMutation({
    mutationFn: () => agriApi.createSeason({ ...form, status: 'PLANNED' }),
    onSuccess: async () => {
      setForm({ code: '', name: '', startDate: '', endDate: '' });
      toast.success('Season created');
      await queryClient.invalidateQueries({ queryKey: ['seasons'] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed')
  });

  const rows = (query.data ?? []).filter(
    (s) => !q || s.name.toLowerCase().includes(q.toLowerCase()) || s.code.toLowerCase().includes(q.toLowerCase())
  );

  return (
    <div className="space-y-4">
      <h1 className="text-3xl font-semibold">Seasons</h1>
      {canWrite ? (
      <form
        className="grid gap-3 rounded-2xl border border-border bg-surface p-4 md:grid-cols-4"
        onSubmit={(e) => {
          e.preventDefault();
          createMutation.mutate();
        }}
      >
        {(['code', 'name', 'startDate', 'endDate'] as const).map((key) => (
          <label key={key} className="text-sm capitalize">
            {key.replace(/([A-Z])/g, ' $1')}
            <input
              required
              type={key.includes('Date') ? 'date' : 'text'}
              value={form[key]}
              onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
        ))}
        <div className="md:col-span-4">
          <button type="submit" className="rounded-xl bg-primary px-4 py-2 text-sm text-white">
            Add season
          </button>
        </div>
      </form>
      ) : null}
      <EnterpriseTable<Season>
        title="Season calendar"
        rows={rows}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : null}
        searchValue={q}
        onSearchChange={setQ}
        getRowKey={(r) => r.id}
        columns={[
          { key: 'code', header: 'Code', sortValue: (r) => r.code, render: (r) => r.code },
          { key: 'name', header: 'Name', sortValue: (r) => r.name, render: (r) => r.name },
          { key: 'start', header: 'Start', sortValue: (r) => r.startDate, render: (r) => r.startDate },
          { key: 'end', header: 'End', sortValue: (r) => r.endDate, render: (r) => r.endDate },
          { key: 'status', header: 'Status', sortValue: (r) => r.status, render: (r) => r.status }
        ]}
      />
    </div>
  );
}
