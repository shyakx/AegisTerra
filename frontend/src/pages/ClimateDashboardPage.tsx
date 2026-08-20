import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { climateApi } from '../api/climate';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClimateDashboardPage() {
  const { hasPermission } = useAuth();

  const dashboardQuery = useQuery({
    queryKey: ['climate-dashboard'],
    queryFn: () => climateApi.dashboard(),
    enabled: hasPermission('climate:read')
  });

  const jobsQuery = useQuery({
    queryKey: ['climate-dashboard-jobs'],
    queryFn: () => climateApi.listImportJobs({ page: 0, size: 5 }),
    enabled: hasPermission('climate:read')
  });

  const providersQuery = useQuery({
    queryKey: ['climate-dashboard-providers'],
    queryFn: () => climateApi.listProviders(),
    enabled: hasPermission('climate:read')
  });

  const d = dashboardQuery.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Climate data platform</p>
          <h1 className="text-3xl font-semibold">Climate dashboard</h1>
          <p className="mt-1 text-sm text-textSecondary">
            Observation volume, import health, provider status, and quality grades.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/climate/stations" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
            Stations
          </Link>
          <Link to="/climate/observations" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
            Observations
          </Link>
          <Link to="/climate/import-jobs" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
            Import jobs
          </Link>
          <Link to="/climate/map" className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90">
            Map
          </Link>
        </div>
      </div>

      {dashboardQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {dashboardQuery.error instanceof ApiError ? dashboardQuery.error.message : 'Failed to load dashboard'}
        </p>
      ) : null}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Climate summary">
        <StatCard label="Stations" value={String(d?.stationCount ?? '—')} detail="Registered" />
        <StatCard
          label="Providers"
          value={String(d?.enabledProviderCount ?? '—')}
          detail={`${d?.providerCount ?? 0} total`}
        />
        <StatCard
          label="Recent observations"
          value={String(d?.observationCountRecent ?? '—')}
          detail="Trusted window"
        />
        <StatCard
          label="Open imports"
          value={String(d?.openImportJobs ?? '—')}
          detail={`Quality ${d?.latestQualityGrade ?? '—'}`}
        />
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Providers</h2>
          <ul className="mt-4 space-y-2">
            {(providersQuery.data ?? []).slice(0, 8).map((p) => (
              <li key={p.id} className="flex items-center justify-between text-sm">
                <span>{p.displayName}</span>
                <span className={p.enabled ? 'text-success' : 'text-textSecondary'}>
                  {p.enabled ? 'Enabled' : 'Stub'}
                </span>
              </li>
            ))}
            {providersQuery.isLoading ? <li className="text-sm text-textSecondary">Loading…</li> : null}
          </ul>
        </div>
        <div className="rounded-2xl border border-border bg-surface p-6">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Recent import jobs</h2>
            <Link to="/climate/import-jobs" className="text-sm text-primary">
              View all
            </Link>
          </div>
          <ul className="mt-4 space-y-2">
            {(jobsQuery.data?.content ?? []).map((job) => (
              <li key={job.id} className="flex items-center justify-between text-sm">
                <span className="font-medium">{job.jobNumber}</span>
                <span>
                  {job.status} · {job.rowsAccepted}/{job.rowsRead}
                </span>
              </li>
            ))}
            {!jobsQuery.isLoading && (jobsQuery.data?.content?.length ?? 0) === 0 ? (
              <li className="text-sm text-textSecondary">No import jobs yet</li>
            ) : null}
          </ul>
        </div>
      </section>
    </div>
  );
}

function StatCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-5">
      <p className="text-sm text-textSecondary">{label}</p>
      <p className="mt-2 text-3xl font-semibold">{value}</p>
      <p className="mt-1 text-sm text-textSecondary">{detail}</p>
    </div>
  );
}
