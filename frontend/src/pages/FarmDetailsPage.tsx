import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { agriApi } from '../api/agriculture';
import { ApiError } from '../api/client';
import BoundaryMapEditor from '../components/BoundaryMapEditor';
import { useAuth } from '../auth/AuthContext';

export default function FarmDetailsPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('farms:write');
  const { id = '' } = useParams();
  const farmQuery = useQuery({ queryKey: ['farm', id], queryFn: () => agriApi.getFarm(id), enabled: Boolean(id) });
  const plotsQuery = useQuery({ queryKey: ['plots', id], queryFn: () => agriApi.listPlots(id), enabled: Boolean(id) });
  const boundariesQuery = useQuery({
    queryKey: ['boundaries', id],
    queryFn: () => agriApi.listBoundaries(id),
    enabled: Boolean(id)
  });

  if (farmQuery.isLoading) return <p>Loading farm…</p>;
  if (farmQuery.error) {
    return <p role="alert">{farmQuery.error instanceof ApiError ? farmQuery.error.message : 'Failed to load farm'}</p>;
  }
  const farm = farmQuery.data!;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Farm details</p>
          <h1 className="text-3xl font-semibold">{farm.farmName}</h1>
          <p className="text-textSecondary">
            {farm.farmCode} · {farm.status}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            to={`/farms/${id}/boundary`}
            className={`rounded-xl px-3 py-2 text-sm ${canWrite ? 'bg-primary text-white' : 'border border-border'}`}
          >
            {canWrite ? 'Boundary editor' : 'View boundary'}
          </Link>
          <Link to={`/farms/${id}/plots`} className="rounded-xl border border-border px-3 py-2 text-sm">
            Plots
          </Link>
          <Link to={`/farms/${id}/crop-history`} className="rounded-xl border border-border px-3 py-2 text-sm">
            Crop history
          </Link>
          {hasPermission('policies:read') ? (
            <Link to="/policies" className="rounded-xl border border-border px-3 py-2 text-sm">
              Policies
            </Link>
          ) : null}
          {hasPermission('claims:read') ? (
            <Link to="/claims" className="rounded-xl border border-border px-3 py-2 text-sm">
              Claims
            </Link>
          ) : null}
          {hasPermission('climate-intel:read') ? (
            <Link
              to={`/climate-intel/farms/${id}`}
              className="rounded-xl border border-border px-3 py-2 text-sm"
            >
              Climate risk
            </Link>
          ) : null}
        </div>
      </div>

      <section className="grid gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-border bg-surface p-4">
          <h2 className="font-semibold">Summary</h2>
          <dl className="mt-3 space-y-2 text-sm">
            <div>
              <dt className="text-textSecondary">Size (ha)</dt>
              <dd>{farm.farmSizeHa ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Crop type</dt>
              <dd>{farm.cropType ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Active boundaries</dt>
              <dd>{boundariesQuery.data?.filter((b) => b.status === 'ACTIVE').length ?? 0}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Plots</dt>
              <dd>{plotsQuery.data?.length ?? 0}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Farmer</dt>
              <dd>
                <Link to={`/farmers/${farm.farmerId}`} className="text-primary hover:underline">
                  Open farmer profile
                </Link>
              </dd>
            </div>
          </dl>
        </div>
        <div className="rounded-2xl border border-border bg-surface p-4">
          <h2 className="font-semibold">Quick boundary check</h2>
          <p className="mt-2 text-sm text-textSecondary">Open the full editor to draw or edit polygons.</p>
          {boundariesQuery.data?.[0]?.geoJson ? (
            <BoundaryMapEditor
              initialGeoJson={boundariesQuery.data[0].geoJson}
              readOnly
              onChange={() => undefined}
            />
          ) : (
            <p className="mt-4 text-sm text-textSecondary">No boundary stored yet.</p>
          )}
        </div>
      </section>
    </div>
  );
}
