import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { useState } from 'react';
import { toast } from 'sonner';
import { agriApi } from '../api/agriculture';
import { ApiError } from '../api/client';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { useAuth } from '../auth/AuthContext';

export default function FarmPlotsPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('farms:write');
  const { id = '' } = useParams();
  const queryClient = useQueryClient();
  const [plotCode, setPlotCode] = useState('');
  const [name, setName] = useState('');
  const query = useQuery({ queryKey: ['plots', id], queryFn: () => agriApi.listPlots(id), enabled: Boolean(id) });

  const createMutation = useMutation({
    mutationFn: () => agriApi.createPlot({ farmId: id, plotCode, name, status: 'ACTIVE' }),
    onSuccess: async () => {
      setPlotCode('');
      setName('');
      toast.success('Plot created');
      await queryClient.invalidateQueries({ queryKey: ['plots', id] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed')
  });

  return (
    <div className="space-y-4">
      <h1 className="text-3xl font-semibold">Plot management</h1>
      {canWrite ? (
      <form
        className="grid gap-3 rounded-2xl border border-border bg-surface p-4 md:grid-cols-3"
        onSubmit={(e) => {
          e.preventDefault();
          createMutation.mutate();
        }}
      >
        <label className="text-sm">
          Plot code
          <input
            required
            value={plotCode}
            onChange={(e) => setPlotCode(e.target.value)}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
          />
        </label>
        <label className="text-sm">
          Name
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
          />
        </label>
        <div className="flex items-end">
          <button type="submit" className="w-full rounded-xl bg-primary px-4 py-2 text-sm text-white">
            Add plot
          </button>
        </div>
      </form>
      ) : null}
      <EnterpriseTable
        title="Plots"
        rows={query.data ?? []}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : null}
        getRowKey={(r) => r.id}
        columns={[
          { key: 'code', header: 'Code', sortValue: (r) => r.plotCode, render: (r) => r.plotCode },
          { key: 'name', header: 'Name', sortValue: (r) => r.name ?? '', render: (r) => r.name ?? '—' },
          { key: 'area', header: 'Area (ha)', sortValue: (r) => r.areaHa ?? 0, render: (r) => r.areaHa ?? '—' },
          { key: 'status', header: 'Status', sortValue: (r) => r.status, render: (r) => r.status }
        ]}
      />
    </div>
  );
}
