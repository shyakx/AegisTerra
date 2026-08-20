import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { climateApi } from '../api/climate';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClimateObservationsPage() {
  const { hasPermission } = useAuth();
  const defaultTo = useMemo(() => new Date().toISOString(), []);
  const defaultFrom = useMemo(
    () => new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
    []
  );
  const [from, setFrom] = useState(defaultFrom.slice(0, 16));
  const [to, setTo] = useState(defaultTo.slice(0, 16));
  const [variableCode, setVariableCode] = useState('');
  const [stationId, setStationId] = useState('');

  const stationsQuery = useQuery({
    queryKey: ['climate-obs-stations'],
    queryFn: () => climateApi.listStations({ page: 0, size: 100 }),
    enabled: hasPermission('climate:read')
  });

  const obsQuery = useQuery({
    queryKey: ['climate-observations', from, to, variableCode, stationId],
    queryFn: () =>
      climateApi.listObservations({
        from: new Date(from).toISOString(),
        to: new Date(to).toISOString(),
        variableCode: variableCode || undefined,
        stationId: stationId || undefined,
        page: 0,
        size: 100
      }),
    enabled: hasPermission('climate:read') && Boolean(from) && Boolean(to)
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Climate data</p>
          <h1 className="text-3xl font-semibold">Observation viewer</h1>
          <p className="mt-1 text-sm text-textSecondary">Time window is required for all queries.</p>
        </div>
        <Link to="/climate" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          Dashboard
        </Link>
      </div>

      <div className="grid gap-3 rounded-2xl border border-border bg-surface p-4 md:grid-cols-4">
        <label className="text-sm">
          <span className="mb-1 block text-textSecondary">From</span>
          <input
            type="datetime-local"
            className="w-full rounded-xl border border-border px-3 py-2"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
        </label>
        <label className="text-sm">
          <span className="mb-1 block text-textSecondary">To</span>
          <input
            type="datetime-local"
            className="w-full rounded-xl border border-border px-3 py-2"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </label>
        <label className="text-sm">
          <span className="mb-1 block text-textSecondary">Station</span>
          <select
            className="w-full rounded-xl border border-border px-3 py-2"
            value={stationId}
            onChange={(e) => setStationId(e.target.value)}
          >
            <option value="">All</option>
            {(stationsQuery.data?.content ?? []).map((s) => (
              <option key={s.id} value={s.id}>
                {s.code} — {s.name}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm">
          <span className="mb-1 block text-textSecondary">Variable</span>
          <select
            className="w-full rounded-xl border border-border px-3 py-2"
            value={variableCode}
            onChange={(e) => setVariableCode(e.target.value)}
          >
            <option value="">All</option>
            <option value="TEMP_C">TEMP_C</option>
            <option value="RAIN_MM">RAIN_MM</option>
            <option value="HUMIDITY_PCT">HUMIDITY_PCT</option>
            <option value="WIND_MS">WIND_MS</option>
          </select>
        </label>
      </div>

      {obsQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {obsQuery.error instanceof ApiError ? obsQuery.error.message : 'Failed to load observations'}
        </p>
      ) : null}

      <div className="overflow-x-auto rounded-2xl border border-border bg-surface">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border text-textSecondary">
            <tr>
              <th className="px-4 py-3 font-medium">Observed</th>
              <th className="px-4 py-3 font-medium">Station</th>
              <th className="px-4 py-3 font-medium">Variable</th>
              <th className="px-4 py-3 font-medium">Value</th>
              <th className="px-4 py-3 font-medium">Quality</th>
              <th className="px-4 py-3 font-medium">Provider</th>
            </tr>
          </thead>
          <tbody>
            {(obsQuery.data?.content ?? []).map((o) => (
              <tr key={o.id} className="border-b border-border/60">
                <td className="px-4 py-3">{new Date(o.observedAt).toLocaleString()}</td>
                <td className="px-4 py-3">
                  <Link className="text-primary" to={`/climate/stations/${o.stationId}`}>
                    {o.stationId.slice(0, 8)}…
                  </Link>
                </td>
                <td className="px-4 py-3">{o.variableCode ?? '—'}</td>
                <td className="px-4 py-3">
                  {o.value ?? o.rainfallMm ?? o.temperatureC ?? '—'} {o.unit ?? ''}
                </td>
                <td className="px-4 py-3">{o.qualityFlag ?? '—'}</td>
                <td className="px-4 py-3">{o.providerCode ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!obsQuery.isLoading && (obsQuery.data?.content?.length ?? 0) === 0 ? (
          <p className="p-4 text-sm text-textSecondary">No observations for filters</p>
        ) : null}
      </div>
    </div>
  );
}
