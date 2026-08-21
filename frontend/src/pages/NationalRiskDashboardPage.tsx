import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { climateIntelApi } from '../api/climateIntel';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { PHOTOS } from '../media/photos';
import { WorkspaceBanner } from '../visuals/WorkspaceBanner';

export default function NationalRiskDashboardPage() {
  const { hasPermission } = useAuth();

  const dashQuery = useQuery({
    queryKey: ['climate-intel-national'],
    queryFn: () => climateIntelApi.nationalDashboard(),
    enabled: hasPermission('climate-intel:read')
  });

  const d = dashQuery.data;
  const gradeEntries = Object.entries(d?.farmsByGrade ?? {});

  return (
    <div className="space-y-6">
      <WorkspaceBanner
        photo={PHOTOS.earthNight}
        eyebrow="Climate intelligence"
        title="National risk dashboard"
        description="Deterministic risk grades, alerts, and district heat from Climate Data aggregates."
        actions={
          <>
            <Link to="/climate" className="at-btn rounded-full bg-white px-4 py-2 text-sm font-semibold text-primary">
              Climate data
            </Link>
            <Link to="/climate-intel/alerts" className="rounded-full px-4 py-2 text-sm font-semibold text-white">
              Alerts
            </Link>
            <Link to="/climate-intel/jobs" className="rounded-full px-4 py-2 text-sm font-semibold text-white">
              Recalc jobs
            </Link>
          </>
        }
      />

      {dashQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {dashQuery.error instanceof ApiError ? dashQuery.error.message : 'Failed to load dashboard'}
        </p>
      ) : null}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Stat label="Open alerts" value={String(d?.openAlerts ?? '—')} />
        <Stat label="Critical alerts" value={String(d?.openCriticalAlerts ?? '—')} />
        <Stat
          label="Data coverage"
          value={d?.dataCoveragePct != null ? `${d.dataCoveragePct}%` : '—'}
        />
        <Stat label="Districts ranked" value={String(d?.districtHeat?.length ?? '—')} />
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Farms by risk grade</h2>
          <ul className="mt-4 space-y-2">
            {gradeEntries.map(([grade, count]) => (
              <li key={grade} className="flex justify-between text-sm">
                <span>{grade}</span>
                <span className="font-medium">{count}</span>
              </li>
            ))}
            {!dashQuery.isLoading && gradeEntries.length === 0 ? (
              <li className="text-sm text-textSecondary">No scores yet — run a recalculation job</li>
            ) : null}
          </ul>
        </div>
        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">District heat</h2>
          <ul className="mt-4 space-y-2">
            {(d?.districtHeat ?? []).slice(0, 12).map((row) => (
              <li key={row.districtCode} className="flex justify-between text-sm">
                <Link className="text-primary" to={`/climate-intel/districts/${encodeURIComponent(row.districtCode)}`}>
                  {row.districtCode}
                </Link>
                <span>
                  {row.grade ?? '—'} · {row.meanScore != null ? row.meanScore.toFixed(1) : '—'}
                </span>
              </li>
            ))}
            {!dashQuery.isLoading && (d?.districtHeat?.length ?? 0) === 0 ? (
              <li className="text-sm text-textSecondary">No district rollups</li>
            ) : null}
          </ul>
        </div>
      </section>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-5">
      <p className="text-sm text-textSecondary">{label}</p>
      <p className="mt-2 text-3xl font-semibold">{value}</p>
    </div>
  );
}
