import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { climateApi } from '../api/climate';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClimateMapPage() {
  const { hasPermission } = useAuth();

  const stationsQuery = useQuery({
    queryKey: ['climate-map-stations'],
    queryFn: () => climateApi.mapStations(),
    enabled: hasPermission('climate:read')
  });

  const footprintsQuery = useQuery({
    queryKey: ['climate-map-footprints'],
    queryFn: () => climateApi.mapFootprints(),
    enabled: hasPermission('climate:read')
  });

  const stations = stationsQuery.data?.features ?? [];
  const footprints = footprintsQuery.data?.features ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Climate data</p>
          <h1 className="text-3xl font-semibold">Map viewer</h1>
          <p className="mt-1 text-sm text-textSecondary">
            Station points and satellite footprints as GeoJSON layers.
          </p>
        </div>
        <Link to="/climate" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          Dashboard
        </Link>
      </div>

      {(stationsQuery.isError || footprintsQuery.isError) && (
        <p className="text-sm text-danger" role="alert">
          {stationsQuery.error instanceof ApiError
            ? stationsQuery.error.message
            : footprintsQuery.error instanceof ApiError
              ? footprintsQuery.error.message
              : 'Failed to load map layers'}
        </p>
      )}

      <section
        className="relative min-h-[420px] overflow-hidden rounded-2xl border border-border"
        style={{
          background:
            'radial-gradient(circle at 20% 20%, rgba(16,185,129,0.18), transparent 40%), radial-gradient(circle at 80% 70%, rgba(14,116,144,0.2), transparent 45%), linear-gradient(160deg, #0f172a, #134e4a 55%, #064e3b)'
        }}
      >
        <div className="absolute inset-0 opacity-30" style={{
          backgroundImage:
            'linear-gradient(rgba(255,255,255,0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.08) 1px, transparent 1px)',
          backgroundSize: '48px 48px'
        }} />
        <div className="relative z-10 grid gap-4 p-6 lg:grid-cols-2">
          <div className="rounded-xl border border-white/15 bg-black/25 p-4 text-emerald-50 backdrop-blur-sm">
            <h2 className="font-semibold">Stations ({stations.length})</h2>
            <ul className="mt-3 max-h-72 space-y-2 overflow-auto text-sm">
              {stations.map((f, idx) => {
                const props = f.properties ?? {};
                const coords = Array.isArray((f.geometry as { coordinates?: number[] })?.coordinates)
                  ? (f.geometry as { coordinates: number[] }).coordinates
                  : null;
                return (
                  <li key={String(props.id ?? idx)} className="flex justify-between gap-2">
                    <span>{String(props.code ?? props.name ?? 'Station')}</span>
                    <span className="text-emerald-100/70">
                      {coords ? `${coords[1]?.toFixed?.(3)}, ${coords[0]?.toFixed?.(3)}` : '—'}
                    </span>
                  </li>
                );
              })}
              {!stationsQuery.isLoading && stations.length === 0 ? (
                <li className="text-emerald-100/70">No station geometries</li>
              ) : null}
            </ul>
          </div>
          <div className="rounded-xl border border-white/15 bg-black/25 p-4 text-emerald-50 backdrop-blur-sm">
            <h2 className="font-semibold">Footprints ({footprints.length})</h2>
            <ul className="mt-3 max-h-72 space-y-2 overflow-auto text-sm">
              {footprints.map((f, idx) => {
                const props = f.properties ?? {};
                return (
                  <li key={String(props.id ?? idx)} className="flex justify-between gap-2">
                    <span>{String(props.productId ?? 'Scene')}</span>
                    <span className="text-emerald-100/70">{String(props.providerCode ?? '—')}</span>
                  </li>
                );
              })}
              {!footprintsQuery.isLoading && footprints.length === 0 ? (
                <li className="text-emerald-100/70">No footprints</li>
              ) : null}
            </ul>
          </div>
        </div>
      </section>
    </div>
  );
}
