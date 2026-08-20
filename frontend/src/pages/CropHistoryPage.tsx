import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { useState } from 'react';
import { toast } from 'sonner';
import { agriApi } from '../api/agriculture';
import { ApiError } from '../api/client';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { useAuth } from '../auth/AuthContext';

export default function CropHistoryPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('farms:write');
  const { id = '' } = useParams();
  const queryClient = useQueryClient();
  const historyQuery = useQuery({
    queryKey: ['crop-seasons', id],
    queryFn: () => agriApi.listCropSeasons(id),
    enabled: Boolean(id)
  });
  const cropsQuery = useQuery({ queryKey: ['crops'], queryFn: () => agriApi.listCrops(), enabled: canWrite });
  const seasonsQuery = useQuery({ queryKey: ['seasons'], queryFn: () => agriApi.listSeasons(), enabled: canWrite });
  const [cropId, setCropId] = useState('');
  const [seasonId, setSeasonId] = useState('');

  const createMutation = useMutation({
    mutationFn: () => agriApi.createCropSeason({ farmId: id, cropId, seasonId, status: 'PLANNED' }),
    onSuccess: async () => {
      toast.success('Crop season added');
      await queryClient.invalidateQueries({ queryKey: ['crop-seasons', id] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed')
  });

  return (
    <div className="space-y-4">
      <h1 className="text-3xl font-semibold">Crop history</h1>
      {canWrite ? (
        <form
          className="grid gap-3 rounded-2xl border border-border bg-surface p-4 md:grid-cols-3"
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
        >
          <label className="text-sm">
            Crop
            <select
              required
              value={cropId}
              onChange={(e) => setCropId(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            >
              <option value="">Select</option>
              {(cropsQuery.data ?? []).map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm">
            Season
            <select
              required
              value={seasonId}
              onChange={(e) => setSeasonId(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            >
              <option value="">Select</option>
              {(seasonsQuery.data ?? []).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <div className="flex items-end">
            <button type="submit" className="w-full rounded-xl bg-primary px-4 py-2 text-sm text-white">
              Add record
            </button>
          </div>
        </form>
      ) : null}
      <EnterpriseTable
        title="Crop seasons"
        rows={historyQuery.data ?? []}
        loading={historyQuery.isLoading}
        error={historyQuery.error instanceof ApiError ? historyQuery.error.message : null}
        getRowKey={(r) => r.id}
        columns={[
          { key: 'crop', header: 'Crop ID', sortValue: (r) => r.cropId, render: (r) => r.cropId.slice(0, 8) },
          { key: 'season', header: 'Season ID', sortValue: (r) => r.seasonId, render: (r) => r.seasonId.slice(0, 8) },
          { key: 'area', header: 'Area', sortValue: (r) => r.plantedAreaHa ?? 0, render: (r) => r.plantedAreaHa ?? '—' },
          { key: 'status', header: 'Status', sortValue: (r) => r.status, render: (r) => r.status }
        ]}
      />
    </div>
  );
}
