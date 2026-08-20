import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Activity,
  AlertTriangle,
  Bell,
  ClipboardList,
  CloudSun,
  MapPinned,
  ShieldCheck,
  Users,
  Wallet
} from 'lucide-react';
import { executiveApi } from '../api/executive';
import { climateApi } from '../api/climate';
import { climateIntelApi } from '../api/climateIntel';
import { agriApi } from '../api/agriculture';
import { claimsApi } from '../api/claims';
import { insuranceApi } from '../api/insurance';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { KpiCard } from '../components/KpiCard';
import { PageHeader, PageActionLink } from '../components/PageHeader';
import { StatusBadge } from '../components/StatusBadge';
import { roleDashboardMeta, roleQuickLinks, showsFocus } from '../navigation/roleDashboard';

const EXECUTIVE_PERMS = [
  'farmers:read',
  'policies:read',
  'claims:read',
  'settlements:read',
  'climate-intel:read',
  'climate:read'
] as const;

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
  const farmerPoliciesQuery = useQuery({
    queryKey: ['farmer-home-policies'],
    queryFn: () => insuranceApi.searchPolicies({ page: 0, size: 20 }),
    enabled: showsFocus(meta, 'farmer') && hasPermission('policies:read')
  });
  const farmerClaimsQuery = useQuery({
    queryKey: ['farmer-home-claims'],
    queryFn: () => claimsApi.search({ page: 0, size: 20 }),
    enabled: showsFocus(meta, 'farmer') && hasPermission('claims:read')
  });

  const o = overviewQuery.data;
  const gradeEntries = Object.entries(o?.climate?.farmsByRiskGrade ?? {});
  const stations = climateMapQuery.data?.features ?? [];
  const quickLinks = roleQuickLinks(role, user?.farmerId).filter((l) => !l.permission || hasPermission(l.permission));

  const headerActions = (
    <>
      {quickLinks.slice(0, 3).map((link, idx) => (
        <PageActionLink key={link.to} to={link.to} variant={idx === 0 ? 'primary' : 'secondary'}>
          {link.label}
        </PageActionLink>
      ))}
    </>
  );

  if (showsFocus(meta, 'farmer')) {
    const me = farmerMeQuery.data?.content?.[0];
    return (
      <div className="space-y-6">
        <PageHeader
          eyebrow={meta.eyebrow}
          title={meta.title}
          description={meta.description}
          actions={headerActions}
        />
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="My coverage">
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
            detail="Scoped to your account"
            icon={MapPinned}
            to="/farms"
          />
          <KpiCard
            label="My policies"
            value={fmt(farmerPoliciesQuery.data?.totalElements)}
            detail="Active cover"
            icon={ShieldCheck}
            to="/policies"
            tone="success"
          />
          <KpiCard
            label="My claims"
            value={fmt(farmerClaimsQuery.data?.totalElements)}
            detail="Loss events"
            icon={ClipboardList}
            to="/claims"
            tone="warning"
          />
        </section>
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Quick links</h2>
          <div className="mt-4 flex flex-wrap gap-2">
            {quickLinks.map((link) => (
              <PageActionLink key={link.to} to={link.to}>
                {link.label}
              </PageActionLink>
            ))}
          </div>
          <p className="mt-4 text-sm text-textSecondary">
            Signed in as {user?.displayName || user?.username}. Records below are scoped to your linked farmer
            {me?.farmerCode ? ` (${me.farmerCode})` : ''}.
          </p>
        </section>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader eyebrow={meta.eyebrow} title={meta.title} description={meta.description} actions={headerActions} />

      {!canExecutive ? (
        <p className="rounded-xl border border-border bg-surface px-4 py-3 text-sm text-textSecondary" role="status">
          Your role does not include executive overview data. Use the modules available in the navigation.
        </p>
      ) : null}

      {overviewQuery.isError ? (
        <p className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-warning" role="status">
          {overviewQuery.error instanceof ApiError
            ? `${overviewQuery.error.message} — showing available module links below.`
            : 'Executive overview API unavailable; use module dashboards.'}
        </p>
      ) : null}

      {(showsFocus(meta, 'portfolio') || showsFocus(meta, 'insurance') || showsFocus(meta, 'finance') || showsFocus(meta, 'support')) && (
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Role KPIs">
          {(showsFocus(meta, 'portfolio') || showsFocus(meta, 'support')) && hasPermission('farmers:read') ? (
            <KpiCard
              label="Farmers"
              value={fmt(o?.farmers?.total)}
              detail={`${fmt(o?.farmers?.active)} active`}
              icon={Users}
              to="/farmers"
              tone="info"
            />
          ) : null}
          {(showsFocus(meta, 'insurance') || showsFocus(meta, 'portfolio') || showsFocus(meta, 'support')) &&
          hasPermission('policies:read') ? (
            <KpiCard
              label="Active policies"
              value={fmt(o?.policies?.active ?? o?.policies?.total)}
              detail={`${fmt(o?.policies?.total)} total`}
              icon={ShieldCheck}
              to="/policies"
              tone="success"
            />
          ) : null}
          {(showsFocus(meta, 'insurance') || showsFocus(meta, 'finance') || showsFocus(meta, 'support')) &&
          hasPermission('claims:read') ? (
            <KpiCard
              label="Open claims"
              value={fmt(o?.claims?.open ?? o?.claims?.total)}
              detail={`${fmt(o?.claims?.total)} total`}
              icon={ClipboardList}
              to="/claims"
              tone="warning"
            />
          ) : null}
          {(showsFocus(meta, 'finance') || showsFocus(meta, 'portfolio')) && hasPermission('settlements:read') ? (
            <KpiCard
              label="Settlements pending"
              value={fmt(o?.settlements?.pending)}
              detail={
                o?.settlements?.pendingAmount != null
                  ? `${Number(o.settlements.pendingAmount).toLocaleString()} ${o.settlements.currency ?? 'RWF'}`
                  : `${fmt(o?.settlements?.completed)} completed`
              }
              icon={Wallet}
              to="/settlements/dashboard"
            />
          ) : null}
          {showsFocus(meta, 'support') && hasPermission('users:read') ? (
            <KpiCard label="User admin" value="Open" detail="Lookup & assistance" icon={Users} to="/users" />
          ) : null}
        </section>
      )}

      {(showsFocus(meta, 'ops') || showsFocus(meta, 'climate') || showsFocus(meta, 'portfolio')) && (
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Operations KPIs">
          {(showsFocus(meta, 'portfolio') || showsFocus(meta, 'ops')) && hasPermission('farms:read') ? (
            <KpiCard
              label="Farms"
              value={fmt(o?.farms?.total)}
              detail={`${fmt(o?.farms?.withBoundary)} with boundary`}
              icon={MapPinned}
              to="/farms"
            />
          ) : null}
          {showsFocus(meta, 'climate') && hasPermission('climate:read') ? (
            <KpiCard
              label="Climate stations"
              value={fmt(o?.climate?.stations)}
              detail={`${fmt(o?.climate?.recentObservations)} recent obs`}
              icon={CloudSun}
              to="/climate"
              tone="info"
            />
          ) : null}
          {showsFocus(meta, 'climate') &&
          (hasPermission('alerts:climate') || hasPermission('climate-intel:read')) ? (
            <KpiCard
              label="Open climate alerts"
              value={fmt(o?.climate?.openAlerts)}
              detail={`${fmt(o?.climate?.criticalAlerts)} critical`}
              icon={AlertTriangle}
              to="/climate-intel/alerts"
              tone="danger"
            />
          ) : null}
          {showsFocus(meta, 'ops') && hasPermission('tasks:read') ? (
            <KpiCard
              label="Pending tasks"
              value={fmt(o?.tasks?.pending)}
              detail={`${fmt(o?.notifications?.unread)} unread notifications`}
              icon={Activity}
              to="/tasks"
            />
          ) : null}
          {hasPermission('ledger:read') && showsFocus(meta, 'finance') ? (
            <KpiCard label="Ledger" value="Open" detail="Payment journal" icon={Wallet} to="/ledger" />
          ) : null}
        </section>
      )}

      <section className="rounded-2xl border border-border bg-surface p-6">
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
          <div className="overflow-hidden rounded-2xl border border-border bg-surface shadow-sm">
            <div className="flex items-center justify-between border-b border-border px-6 py-4">
              <div>
                <p className="text-sm text-textSecondary">Spatial posture</p>
                <h2 className="text-lg font-semibold">Stations & risk coverage</h2>
              </div>
              {hasPermission('farms:read') ? (
                <Link to="/gis" className="text-sm font-medium text-primary hover:underline">
                  Open GIS
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

          <div className="rounded-2xl border border-border bg-surface p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-textSecondary">Climate alerts</p>
                <h2 className="text-lg font-semibold">Open incidents</h2>
              </div>
              <Bell className="h-5 w-5 text-textSecondary" aria-hidden />
            </div>
            <ul className="mt-4 space-y-3">
              {(alertsQuery.data?.content ?? []).map((alert) => (
                <li key={alert.id} className="rounded-xl border border-border bg-background p-4">
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
