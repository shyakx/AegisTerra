import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { CloudRain, MapPinned, Radio, Upload } from 'lucide-react';
import { climateApi, type ClimateImportJob } from '../api/climate';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { KpiCard } from '../components/KpiCard';
import { StatusBadge } from '../components/StatusBadge';
import { PHOTOS } from '../media/photos';
import { WorkspaceBanner } from '../visuals/WorkspaceBanner';

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
  const jobs = jobsQuery.data?.content ?? [];
  const providers = (providersQuery.data ?? []).slice(0, 8);

  return (
    <div className="space-y-6">
      <WorkspaceBanner
        photo={PHOTOS.groundStation}
        eyebrow="Climate data platform"
        title="Climate dashboard"
        description="Observation volume, import health, provider status, and quality grades."
        actions={
          <>
            <Link to="/climate/map" className="at-btn rounded-full bg-white px-4 py-2 text-sm font-semibold text-primary">
              Map
            </Link>
            <Link to="/climate/stations" className="rounded-full px-4 py-2 text-sm font-semibold text-white">
              Stations
            </Link>
            <Link to="/climate/observations" className="rounded-full px-4 py-2 text-sm font-semibold text-white">
              Observations
            </Link>
            <Link to="/climate/import-jobs" className="rounded-full px-4 py-2 text-sm font-semibold text-white">
              Import jobs
            </Link>
          </>
        }
      />

      {dashboardQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {dashboardQuery.error instanceof ApiError ? dashboardQuery.error.message : 'Failed to load dashboard'}
        </p>
      ) : null}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Climate summary">
        <KpiCard
          label="Stations"
          value={d?.stationCount ?? '—'}
          detail="Registered"
          icon={MapPinned}
          to="/climate/stations"
        />
        <KpiCard
          label="Providers"
          value={d?.enabledProviderCount ?? '—'}
          detail={`${d?.providerCount ?? 0} total`}
          icon={Radio}
          tone="info"
        />
        <KpiCard
          label="Recent observations"
          value={d?.observationCountRecent ?? '—'}
          detail="Trusted window"
          icon={CloudRain}
          to="/climate/observations"
          tone="success"
        />
        <KpiCard
          label="Open imports"
          value={d?.openImportJobs ?? '—'}
          detail={`Quality ${d?.latestQualityGrade ?? '—'}`}
          icon={Upload}
          to="/climate/import-jobs"
          tone={d?.openImportJobs ? 'warning' : 'default'}
        />
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl bg-surface p-6">
          <h2 className="text-lg font-semibold">Providers</h2>
          <p className="mt-1 text-sm text-textSecondary">Feeds that land observations into the trusted window.</p>
          <ul className="mt-5 space-y-3">
            {providers.map((p) => (
              <li key={p.id} className="flex items-center justify-between gap-3 rounded-xl bg-background px-4 py-3">
                <div>
                  <p className="text-sm font-medium">{p.displayName}</p>
                  <p className="mt-0.5 text-xs text-textSecondary">{p.code}</p>
                </div>
                <StatusBadge status={p.enabled ? 'ENABLED' : 'STUB'} />
              </li>
            ))}
            {providersQuery.isLoading ? <li className="text-sm text-textSecondary">Loading…</li> : null}
            {!providersQuery.isLoading && providers.length === 0 ? (
              <li className="text-sm text-textSecondary">No providers configured.</li>
            ) : null}
          </ul>
        </div>

        <div className="rounded-2xl bg-surface p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold">Recent import jobs</h2>
              <p className="mt-1 text-sm text-textSecondary">Accepted rows versus rows read on the latest runs.</p>
            </div>
            <Link to="/climate/import-jobs" className="text-sm font-medium text-primary">
              View all
            </Link>
          </div>
          <ul className="mt-5 space-y-3">
            {jobs.map((job) => (
              <li key={job.id}>
                <ImportJobRow job={job} />
              </li>
            ))}
            {jobsQuery.isLoading ? <li className="text-sm text-textSecondary">Loading…</li> : null}
            {!jobsQuery.isLoading && jobs.length === 0 ? (
              <li className="rounded-xl bg-background px-4 py-6 text-sm text-textSecondary">No import jobs yet.</li>
            ) : null}
          </ul>
        </div>
      </section>
    </div>
  );
}

function ImportJobRow({ job }: { job: ClimateImportJob }) {
  const read = job.rowsRead || 0;
  const accepted = job.rowsAccepted || 0;
  const rejected = job.rowsRejected || Math.max(0, read - accepted);
  const pct = read > 0 ? Math.min(100, Math.round((accepted / read) * 100)) : 0;

  return (
    <div className="rounded-xl bg-background px-4 py-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="font-mono text-sm font-semibold tracking-tight">{job.jobNumber}</p>
          <p className="mt-1 text-xs text-textSecondary">
            {job.jobType.replaceAll('_', ' ')} · {job.providerCode}
          </p>
        </div>
        <StatusBadge status={job.status} />
      </div>
      <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-white">
        <div
          className={`h-full rounded-full ${rejected > 0 ? 'bg-warning' : 'bg-primary'}`}
          style={{ width: `${pct}%` }}
        />
      </div>
      <p className="mt-2 text-xs text-textSecondary">
        <span className="font-medium text-textPrimary">{accepted}</span> accepted
        {read ? ` of ${read}` : ''}
        {rejected > 0 ? ` · ${rejected} rejected` : ''}
      </p>
    </div>
  );
}
