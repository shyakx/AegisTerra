import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { climateApi } from '../api/climate';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

function defaultWindow() {
  const to = new Date();
  const from = new Date(to.getTime() - 30 * 24 * 60 * 60 * 1000);
  return { from: from.toISOString(), to: to.toISOString() };
}

export default function ClimateStationDetailPage() {
  const { id = '' } = useParams();
  const { hasPermission } = useAuth();
  const window = defaultWindow();

  const stationQuery = useQuery({
    queryKey: ['climate-station', id],
    queryFn: () => climateApi.getStation(id),
    enabled: Boolean(id) && hasPermission('climate:read')
  });

  const obsQuery = useQuery({
    queryKey: ['climate-station-obs', id, window.from, window.to],
    queryFn: () =>
      climateApi.listObservations({
        stationId: id,
        from: window.from,
        to: window.to,
        page: 0,
        size: 50
      }),
    enabled: Boolean(id) && hasPermission('climate:read')
  });

  const station = stationQuery.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Weather station</p>
          <h1 className="text-3xl font-semibold">{station?.name ?? 'Station'}</h1>
          <p className="mt-1 text-sm text-textSecondary">{station?.code}</p>
        </div>
        <Link to="/climate/stations" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          All stations
        </Link>
      </div>

      {stationQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {stationQuery.error instanceof ApiError ? stationQuery.error.message : 'Failed to load station'}
        </p>
      ) : null}

      {station ? (
        <section className="grid gap-4 rounded-2xl border border-border bg-surface p-6 sm:grid-cols-2 lg:grid-cols-4">
          <Meta label="Provider" value={station.providerCode ?? '—'} />
          <Meta label="District" value={station.districtCode ?? '—'} />
          <Meta
            label="Coordinates"
            value={
              station.longitude != null && station.latitude != null
                ? `${station.latitude.toFixed(4)}, ${station.longitude.toFixed(4)}`
                : '—'
            }
          />
          <Meta label="Elevation (m)" value={station.elevationM != null ? String(station.elevationM) : '—'} />
        </section>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Recent observations (30d)</h2>
        <div className="mt-4 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-border text-textSecondary">
              <tr>
                <th className="px-3 py-2 font-medium">Observed</th>
                <th className="px-3 py-2 font-medium">Variable</th>
                <th className="px-3 py-2 font-medium">Value</th>
                <th className="px-3 py-2 font-medium">Quality</th>
              </tr>
            </thead>
            <tbody>
              {(obsQuery.data?.content ?? []).map((o) => (
                <tr key={o.id} className="border-b border-border/60">
                  <td className="px-3 py-2">{new Date(o.observedAt).toLocaleString()}</td>
                  <td className="px-3 py-2">{o.variableCode ?? '—'}</td>
                  <td className="px-3 py-2">
                    {o.value ?? o.rainfallMm ?? o.temperatureC ?? '—'} {o.unit ?? ''}
                  </td>
                  <td className="px-3 py-2">{o.qualityFlag ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {!obsQuery.isLoading && (obsQuery.data?.content?.length ?? 0) === 0 ? (
            <p className="mt-3 text-sm text-textSecondary">No observations in window</p>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-sm text-textSecondary">{label}</p>
      <p className="mt-1 font-medium">{value}</p>
    </div>
  );
}
