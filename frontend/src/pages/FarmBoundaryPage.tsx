import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { useState } from 'react';
import { toast } from 'sonner';
import { agriApi } from '../api/agriculture';
import { ApiError } from '../api/client';
import BoundaryMapEditor from '../components/BoundaryMapEditor';
import { useAuth } from '../auth/AuthContext';

export default function FarmBoundaryPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('farms:write');
  const { id = '' } = useParams();
  const queryClient = useQueryClient();
  const [geoJson, setGeoJson] = useState<string | null>(null);
  const [valid, setValid] = useState(false);
  const boundariesQuery = useQuery({
    queryKey: ['boundaries', id],
    queryFn: () => agriApi.listBoundaries(id),
    enabled: Boolean(id)
  });

  const saveMutation = useMutation({
    mutationFn: () =>
      agriApi.saveBoundary({
        farmId: id,
        geoJson,
        source: 'MAPLIBRE',
        status: 'ACTIVE',
        reason: 'boundary-editor'
      }),
    onSuccess: async () => {
      toast.success('Boundary saved');
      await queryClient.invalidateQueries({ queryKey: ['boundaries', id] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Save failed')
  });

  const existing = boundariesQuery.data?.[0]?.geoJson ?? null;

  return (
    <div className="space-y-4">
      <div>
        <p className="text-sm text-textSecondary">Spatial editing</p>
        <h1 className="text-3xl font-semibold">Farm boundary</h1>
      </div>
      <BoundaryMapEditor
        initialGeoJson={existing}
        readOnly={!canWrite}
        onChange={(value, _area, isValid) => {
          setGeoJson(value);
          setValid(isValid);
        }}
      />
      {canWrite ? (
        <button
          type="button"
          disabled={!valid || !geoJson || saveMutation.isPending}
          onClick={() => saveMutation.mutate()}
          className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
        >
          Save & activate boundary
        </button>
      ) : (
        <p className="text-sm text-textSecondary">Read-only boundary view for your role.</p>
      )}
    </div>
  );
}
