import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { satelliteApi } from '../api/satellite';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { StatusBadge } from '../components/StatusBadge';
import { PHOTOS } from '../media/photos';
import { WorkspaceBanner } from '../visuals/WorkspaceBanner';

export default function SatelliteIntelligencePage() {
  const { hasPermission } = useAuth();
  const query = useQuery({
    queryKey: ['satellite-summary'],
    queryFn: () => satelliteApi.summary(),
    enabled: hasPermission('satellite:read') || hasPermission('climate-intel:read')
  });
  const d = query.data;

  return (
    <div className="space-y-6">
      <WorkspaceBanner
        photo={PHOTOS.satellite}
        eyebrow="Satellite intelligence"
        title="Remote sensing"
        description="Vegetation health, drought severity, flooding, and environmental stress from satellite imagery. Risk scores refresh when new scenes arrive."
      />

      {query.isError ? (
        <p className="text-sm text-danger" role="alert">
          {query.error instanceof ApiError ? query.error.message : 'Failed to load satellite intelligence'}
        </p>
      ) : null}

      <section className="grid gap-4 sm:grid-cols-3">
        <Stat label="Scenes this window" value={String(d?.sceneCount ?? '—')} />
        <Stat label="Stressed districts" value={String(d?.stressedDistricts ?? '—')} />
        <Stat label="High drought" value={String(d?.highDrought ?? '—')} />
      </section>

      <div className="overflow-x-auto rounded-2xl border border-border bg-surface">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border text-textSecondary">
            <tr>
              <th className="px-4 py-3 font-medium">District</th>
              <th className="px-4 py-3 font-medium">Captured</th>
              <th className="px-4 py-3 font-medium">NDVI</th>
              <th className="px-4 py-3 font-medium">Vegetation</th>
              <th className="px-4 py-3 font-medium">Drought</th>
              <th className="px-4 py-3 font-medium">Flood index</th>
            </tr>
          </thead>
          <tbody>
            {(d?.scenes ?? []).map((scene) => (
              <tr key={scene.id} className="border-b border-border/60">
                <td className="px-4 py-3 font-medium">
                  <Link className="text-primary" to={`/climate-intel/districts/${encodeURIComponent(scene.districtCode)}`}>
                    {scene.districtCode}
                  </Link>
                </td>
                <td className="px-4 py-3">{new Date(scene.capturedAt).toLocaleDateString()}</td>
                <td className="px-4 py-3">{scene.ndvi.toFixed(2)}</td>
                <td className="px-4 py-3">
                  <StatusBadge status={scene.vegetationHealth} />
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={scene.droughtSeverity} />
                </td>
                <td className="px-4 py-3">{scene.floodIndex.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-4">
      <p className="text-xs uppercase tracking-wide text-textSecondary">{label}</p>
      <p className="mt-2 text-2xl font-semibold">{value}</p>
    </div>
  );
}
