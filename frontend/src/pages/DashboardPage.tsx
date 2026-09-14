import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  Bell,
  CloudSun,
  MapPinned,
  TrendingUp,
  Users
} from 'lucide-react';
import { executiveApi } from '../api/executive';
import { climateApi } from '../api/climate';
import { climateIntelApi } from '../api/climateIntel';
import { agriApi } from '../api/agriculture';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { KpiCard } from '../components/KpiCard';
import { PageActionLink } from '../components/PageHeader';
import { StatusBadge } from '../components/StatusBadge';
import { roleDashboardMeta, roleQuickLinks, showsFocus } from '../navigation/roleDashboard';
import { PHOTOS } from '../media/photos';
import { WorkspaceBanner } from '../visuals/WorkspaceBanner';

const EXECUTIVE_PERMS = ['farmers:read', 'climate-intel:read', 'climate:read', 'farms:read'] as const;

export default function DashboardPage() {
  const { user, hasPermission, hasAnyPermission } = useAuth();
  const role = user?.roles?.[0];
  const meta = roleDashboardMeta(user?.roles);
  const canExecutive = hasAnyPermission(...EXECUTIVE_PERMS) && !showsFocus(meta, 'farmer');

  const overviewQuery = useQuery({
    queryKey: ['executive-overview', role],
    queryFn: () => executiveApi.overview(),
    enabled: canExecutive,
    retry: 1
  });

  const climateMapQuery = useQuery({
    queryKey: ['exec-map-stations'],
    queryFn: () => climateApi.mapStations(),
    enabled: canExecutive && hasPermission('climate:read') && showsFocus(meta, 'climate'),
    retry: false
  });

  const alertsQuery = useQuery({
    queryKey: ['exec-climate-alerts'],
    queryFn: () => climateIntelApi.listAlerts({ status: 'OPEN', page: 0, size: 5 }),
    enabled:
      canExecutive &&
      showsFocus(meta, 'climate') &&
      (hasPermission('climate-intel:read') || hasPermission('alerts:climate')),
    retry: false
  });

  const farmerMeQuery = useQuery({
    queryKey: ['farmer-home-profile'],
    queryFn: () => agriApi.searchFarmers({ page: 0, size: 1 }),
    enabled: showsFocus(meta, 'farmer') && hasPermission('farmers:read')
  });
  const farmerFarmsQuery = useQuery({
    queryKey: ['farmer-home-farms'],
    queryFn: () => agriApi.searchFarms({ page: 0, size: 20 }),
    enabled: showsFocus(meta, 'farmer') && hasPermission('farms:read')
  });

  const o = overviewQuery.data;
  const gradeEntries = Object.entries(o?.climate?.farmsByRiskGrade ?? {});
  const stations = climateMapQuery.data?.features ?? [];
  const quickLinks = roleQuickLinks(role, user?.farmerId).filter((l) => !l.permission || hasPermission(l.permission));

  const headerActions = (
    <>
      {quickLinks.slice(0, 3).map((link, idx) => (
        <PageActionLink key={link.to} to={link.to} variant={idx === 0 ? 'onPhoto' : 'ghost'}>
          {link.label}
        </PageActionLink>
      ))}
    </>
  );

  if (showsFocus(meta, 'farmer')) {
    const me = farmerMeQuery.data?.content?.[0];
    return (
      <div className="space-y-6">
        <WorkspaceBanner
          photo={PHOTOS.farmerField}
          eyebrow={meta.eyebrow}
          title={meta.title}
          description={meta.description}
          actions={headerActions}
        />
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3" aria-label="My farm">
          <KpiCard
            label="My profile"
            value={me ? `${me.firstName} ${me.lastName}` : '—'}
            detail={me?.farmerCode ?? 'Linking…'}
            icon={Users}
            to={me ? `/farmers/${me.id}` : '/farmers'}
            tone="info"
          />
          <KpiCard
            label="My farms"
            value={fmt(farmerFarmsQuery.data?.totalElements)}
            detail="Registered holdings"
            icon={MapPinned}
            to="/farms"
          />
          <KpiCard
            label="Planning outlook"
            value="Open"
            detail="Past → now → ahead"
            icon={TrendingUp}
            to="/planning"
            tone="success"
          />
        </section>
        <section className="rounded-2xl bg-surface p-6">
          <h2 className="text-lg font-semibold">Quick links</h2>
          <div className="mt-4 flex flex-wrap gap-2">
            {quickLinks.map((link) => (
              <PageActionLink key={link.to} to={link.to}>
                {link.label}
              </PageActionLink>
            ))}
          </div>
          <p className="mt-4 text-sm text-textSecondary">
            Signed in as {user?.displayName || user?.username}
            {me?.farmerCode ? ` · ${me.farmerCode}` : ''}.
          </p>
        </section>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <WorkspaceBanner
        photo={PHOTOS.fieldsAerial}
        eyebrow={meta.eyebrow}
        title={meta.title}
        description={meta.description}
        actions={headerActions}
      />

      {!canExecutive ? (
        <p className="rounded-xl border border-border bg-surface px-4 py-3 text-sm text-textSecondary" role="status">
          Your role does not include overview data. Use the planning and climate modules in the navigation.
        </p>
      ) : null}

      {overviewQuery.isError ? (
        <p className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-warning" role="status">
          {overviewQuery.error instanceof ApiError
            ? `${overviewQuery.error.message} — showing available module links below.`
            : 'Overview API unavailable; use climate and planning dashboards.'}
        </p>
      ) : null}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Planning KPIs">
        {hasPermission('farmers:read') ? (
          <KpiCard
            label="Farmers"
            value={fmt(o?.farmers?.total)}
            detail={`${fmt(o?.farmers?.active)} active`}
            icon={Users}
            to="/farmers"
            tone="info"
          />
        ) : null}
        {hasPermission('farms:read') ? (
          <KpiCard
            label="Farms"
            value={fmt(o?.farms?.total)}
            detail={`${fmt(o?.farms?.withBoundary)} with boundary`}
            icon={MapPinned}
            to="/farms"
          />
        ) : null}
        {hasPermission('climate:read') ? (
          <KpiCard
            label="Climate stations"
            value={fmt(o?.climate?.stations)}
            detail={`${fmt(o?.climate?.recentObservations)} recent obs`}
            icon={CloudSun}
            to="/climate"
            tone="info"
          />
        ) : null}
        {hasPermission('climate-intel:read') || hasPermission('alerts:climate') ? (
          <KpiCard
            label="Open climate alerts"
            value={fmt(o?.climate?.openAlerts)}
            detail={`${fmt(o?.climate?.criticalAlerts)} critical`}
            icon={AlertTriangle}
            to="/climate-intel/alerts"
            tone="danger"
          />
        ) : null}
        {hasPermission('climate-intel:read') ? (
          <KpiCard
            label="Planning outlook"
            value="Open"
            detail="Past → now → ahead"
            icon={TrendingUp}
            to="/planning"
            tone="success"
          />
        ) : null}
        {showsFocus(meta, 'support') && hasPermission('users:read') ? (
          <KpiCard label="User admin" value="Open" detail="Lookup & assistance" icon={Users} to="/users" />
        ) : null}
      </section>

      <section className="rounded-2xl bg-surface p-6">
        <h2 className="text-lg font-semibold">Workspace shortcuts</h2>
        <div className="mt-4 flex flex-wrap gap-2">
          {quickLinks.map((link) => (
            <PageActionLink key={link.to} to={link.to}>
              {link.label}
            </PageActionLink>
          ))}
        </div>
      </section>

      {showsFocus(meta, 'climate') && (hasPermission('climate:read') || hasPermission('climate-intel:read')) ? (
        <section className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
          <div className="overflow-hidden rounded-2xl bg-surface">
            <div className="flex items-center justify-between border-b border-border px-6 py-4">
              <div>
                <p className="text-sm text-textSecondary">Climate posture</p>
                <h2 className="text-lg font-semibold">Stations & risk coverage</h2>
              </div>
              {hasPermission('climate-intel:read') ? (
                <Link to="/climate-intel" className="text-sm font-medium text-primary hover:underline">
                  Risk intelligence
                </Link>
              ) : null}
            </div>
            <div
              className="relative min-h-[280px] p-6"
              style={{
                background:
                  'radial-gradient(circle at 18% 22%, rgba(16,185,129,0.16), transparent 42%), radial-gradient(circle at 82% 68%, rgba(14,116,144,0.14), transparent 40%), linear-gradient(165deg, #0f172a, #115e59 58%, #064e3b)'
              }}
            >
              <div
                className="pointer-events-none absolute inset-0 opacity-25"
                style={{
                  backgroundImage:
                    'linear-gradient(rgba(255,255,255,0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.08) 1px, transparent 1px)',
                  backgroundSize: '40px 40px'
                }}
              />
              <div className="relative z-10 grid gap-4 lg:grid-cols-2">
                <div className="rounded-xl border border-white/15 bg-black/25 p-4 text-emerald-50 backdrop-blur-sm">
                  <p className="text-sm font-semibold">Weather stations ({stations.length})</p>
                  <ul className="mt-3 max-h-44 space-y-2 overflow-auto text-sm">
                    {stations.slice(0, 8).map((f, idx) => {
                      const props = f.properties ?? {};
                      return (
                        <li key={String(props.id ?? idx)} className="flex justify-between gap-2">
                          <span>{String(props.code ?? props.name ?? 'Station')}</span>
                          <span className="text-emerald-100/70">{String(props.providerCode ?? '—')}</span>
                        </li>
                      );
                    })}
                    {!climateMapQuery.isLoading && stations.length === 0 ? (
                      <li className="text-emerald-100/70">No station geometries yet</li>
                    ) : null}
                  </ul>
                </div>
                <div className="rounded-xl border border-white/15 bg-black/25 p-4 text-emerald-50 backdrop-blur-sm">
                  <p className="text-sm font-semibold">Farms by risk grade</p>
                  <ul className="mt-3 space-y-2 text-sm">
                    {gradeEntries.map(([grade, count]) => (
                      <li key={grade} className="flex items-center justify-between gap-2">
                        <StatusBadge status={grade} />
                        <span className="font-semibold tabular-nums">{count}</span>
                      </li>
                    ))}
                    {!overviewQuery.isLoading && gradeEntries.length === 0 ? (
                      <li className="text-emerald-100/70">Run climate recalculation to populate grades</li>
                    ) : null}
                  </ul>
                </div>
              </div>
            </div>
          </div>

          <div className="rounded-2xl bg-surface p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-textSecondary">Climate alerts</p>
                <h2 className="text-lg font-semibold">Open incidents</h2>
              </div>
              <Bell className="h-5 w-5 text-textSecondary" aria-hidden />
            </div>
            <ul className="mt-4 space-y-3">
              {(alertsQuery.data?.content ?? []).map((alert) => (
                <li key={alert.id} className="rounded-xl bg-background p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-medium">{alert.alertNumber}</p>
                      <p className="mt-1 text-sm text-textSecondary">
                        {alert.alertType} · {alert.scopeType} {alert.scopeId ?? ''}
                      </p>
                    </div>
                    <StatusBadge status={alert.severity} />
                  </div>
                </li>
              ))}
              {!alertsQuery.isLoading && (alertsQuery.data?.content?.length ?? 0) === 0 ? (
                <li className="text-sm text-textSecondary">No open climate alerts</li>
              ) : null}
            </ul>
          </div>
        </section>
      ) : null}
    </div>
  );
}

function fmt(value: number | string | null | undefined) {
  if (value == null || value === '') return '—';
  if (typeof value === 'number') return value.toLocaleString();
  return String(value);
}
