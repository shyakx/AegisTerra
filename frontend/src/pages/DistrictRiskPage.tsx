import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { climateIntelApi } from '../api/climateIntel';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function DistrictRiskPage() {
  const { code = '' } = useParams();
  const { hasPermission } = useAuth();

  const profileQuery = useQuery({
    queryKey: ['district-risk', code],
    queryFn: () => climateIntelApi.districtRiskProfile(code),
    enabled: Boolean(code) && hasPermission('climate-intel:read'),
    retry: false
  });

  const p = profileQuery.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">District risk</p>
          <h1 className="text-3xl font-semibold">{code}</h1>
        </div>
        <Link to="/climate-intel" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          National dashboard
        </Link>
      </div>

      {profileQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {profileQuery.error instanceof ApiError ? profileQuery.error.message : 'Failed to load district profile'}
        </p>
      ) : null}

      {p ? (
        <>
          <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Stat label="Grade" value={p.grade ?? '—'} />
            <Stat label="Mean score" value={p.meanScore != null ? p.meanScore.toFixed(1) : '—'} />
            <Stat label="P90 score" value={p.p90Score != null ? p.p90Score.toFixed(1) : '—'} />
            <Stat label="Farms / alerts" value={`${p.farmCount} / ${p.openAlertCount}`} />
          </section>
          {p.metricsJson ? (
            <pre className="overflow-auto rounded-2xl border border-border bg-surface p-4 text-xs">
              {pretty(p.metricsJson)}
            </pre>
          ) : null}
        </>
      ) : !profileQuery.isError ? (
        <p className="text-sm text-textSecondary">Loading…</p>
      ) : null}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-5">
      <p className="text-sm text-textSecondary">{label}</p>
      <p className="mt-2 text-2xl font-semibold">{value}</p>
    </div>
  );
}

function pretty(raw: string) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}
