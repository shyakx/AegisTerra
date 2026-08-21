import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { guidanceApi } from '../api/guidance';
import { climateIntelApi } from '../api/climateIntel';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { StatusBadge } from '../components/StatusBadge';
import { PHOTOS } from '../media/photos';
import { WorkspaceBanner } from '../visuals/WorkspaceBanner';

export default function FarmerGuidancePage() {
  const { user, hasPermission } = useAuth();
  const recQuery = useQuery({
    queryKey: ['farmer-guidance', user?.farmerId],
    queryFn: () => guidanceApi.list(),
    enabled: Boolean(user?.farmerId) || hasPermission('farmers:read')
  });
  const alertQuery = useQuery({
    queryKey: ['farmer-guidance-alerts'],
    queryFn: () => climateIntelApi.listAlerts({ status: 'OPEN', page: 0, size: 5 }),
    enabled: hasPermission('climate-intel:read') || hasPermission('alerts:climate')
  });

  return (
    <div className="space-y-6">
      <WorkspaceBanner
        photo={PHOTOS.greenhouse}
        eyebrow="Climate-informed guidance"
        title="Alerts and recommendations"
        description="Weather alerts, risk notices, and agronomic recommendations generated from satellite and climate intelligence for your holdings."
      />

      {recQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {recQuery.error instanceof ApiError ? recQuery.error.message : 'Failed to load recommendations'}
        </p>
      ) : null}

      <section className="grid gap-4 lg:grid-cols-2">
        {(recQuery.data ?? []).map((item) => (
          <article key={item.id} className="rounded-2xl border border-border bg-surface p-6">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-lg font-semibold">{item.title}</h2>
              <StatusBadge status={item.severity} />
            </div>
            <p className="mt-3 text-sm text-textSecondary">{item.body}</p>
            <p className="mt-4 text-xs text-textSecondary">
              {item.kind} · {new Date(item.generatedAt).toLocaleString()}
            </p>
          </article>
        ))}
      </section>

      {(alertQuery.data?.content?.length ?? 0) > 0 ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Open climate alerts</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {alertQuery.data?.content.map((alert) => (
              <li key={alert.id} className="flex justify-between gap-3">
                <Link className="text-primary" to="/notifications">
                  {alert.alertNumber} · {alert.alertType}
                </Link>
                <StatusBadge status={alert.severity} />
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  );
}
