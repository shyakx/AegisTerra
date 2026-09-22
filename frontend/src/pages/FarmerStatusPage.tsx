import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { agriApi } from '../api/agriculture';
import { ApiError } from '../api/client';
import { notificationsApi } from '../api/notifications';
import { settlementsApi } from '../api/settlements';
import { useAuth } from '../auth/AuthContext';
import { PageHeader } from '../components/PageHeader';

export default function FarmerStatusPage() {
  const { user, hasPermission } = useAuth();
  const farmerId = user?.farmerId ?? null;

  const farmerQuery = useQuery({
    queryKey: ['farmer-status', farmerId],
    queryFn: () => agriApi.getFarmer(farmerId!),
    enabled: Boolean(farmerId) && hasPermission('farmers:read')
  });
  const farmsQuery = useQuery({
    queryKey: ['farmer-status-farms', farmerId],
    queryFn: () => agriApi.searchFarms({ farmerId: farmerId!, page: 0, size: 20 }),
    enabled: Boolean(farmerId) && hasPermission('farms:read')
  });
  const payoutsQuery = useQuery({
    queryKey: ['farmer-status-payouts', farmerId],
    queryFn: () => settlementsApi.list({ page: 0, size: 100, sort: 'createdAt,desc' }),
    enabled: hasPermission('settlements:read')
  });
  const notificationsQuery = useQuery({
    queryKey: ['farmer-status-notifications'],
    queryFn: () => notificationsApi.unread({ page: 0, size: 5 }),
    enabled: hasPermission('notifications:read')
  });

  if (!farmerId) {
    return (
      <div className="space-y-4">
        <PageHeader
          eyebrow="Farmer"
          title="Farmer status"
          description="No farmer record is linked to this login."
        />
        <Link to="/farmers" className="text-sm text-primary hover:underline">
          Open farmer registry
        </Link>
      </div>
    );
  }

  const farmer = farmerQuery.data;
  const farms = farmsQuery.data?.content ?? [];
  const payouts = payoutsQuery.data?.content ?? [];
  const unreadNotifications = notificationsQuery.data?.content ?? [];
  const pendingPayouts = payouts.filter((payout) => payout.status !== 'COMPLETED');

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Farmer"
        title="Farmer status"
        description="A clear view of your registration, land records, and payment progress."
      />

      {farmerQuery.error instanceof ApiError ? (
        <p className="text-sm text-danger" role="alert">
          {farmerQuery.error.message}
        </p>
      ) : null}

      <section className="overflow-hidden rounded-2xl border border-border bg-surface shadow-sm">
        <div className="border-b border-border bg-background/60 px-5 py-4">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary">Identity record</p>
          <h2 className="mt-1 text-lg font-semibold">Your farmer profile</h2>
        </div>
        <div className="p-5">
        {farmerQuery.isLoading ? (
          <div className="space-y-2" aria-label="Loading farmer profile">
            <div className="h-4 w-40 animate-pulse rounded bg-background" />
            <div className="h-4 w-56 animate-pulse rounded bg-background" />
          </div>
        ) : farmer ? (
          <dl className="mt-3 grid gap-2 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-textSecondary">Name</dt>
              <dd className="font-medium">
                {farmer.firstName} {farmer.lastName}
              </dd>
            </div>
            <div>
              <dt className="text-textSecondary">Status</dt>
              <dd className="mt-1 inline-flex rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-800">
                {(farmer.status ?? 'Pending').replace(/_/g, ' ')}
              </dd>
            </div>
            <div>
              <dt className="text-textSecondary">Phone</dt>
              <dd>{farmer.phoneNumber ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">National ID</dt>
              <dd>{farmer.nationalId ?? '—'}</dd>
            </div>
          </dl>
        ) : (
          <p className="mt-2 text-sm text-textSecondary">Farmer record not found.</p>
        )}
        <Link to={`/farmers/${farmerId}`} className="mt-4 inline-block text-sm text-primary hover:underline">
          Open full profile
        </Link>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-border border-l-4 border-l-primary bg-surface p-5 shadow-sm">
          <p className="text-sm text-textSecondary">Payouts</p>
          <p className="mt-1 text-2xl font-semibold">{payouts.length}</p>
          <p className="mt-1 text-xs text-textSecondary">
            {pendingPayouts.length ? `${pendingPayouts.length} still in progress` : 'No payouts awaiting completion'}
          </p>
          <Link to="/payouts" className="mt-3 inline-block text-sm font-medium text-primary hover:underline">
            Review my payouts
          </Link>
        </div>
        <div className="rounded-2xl border border-border border-l-4 border-l-emerald-500 bg-surface p-5 shadow-sm">
          <p className="text-sm text-textSecondary">Notifications</p>
          <p className="mt-1 text-2xl font-semibold">{unreadNotifications.length}</p>
          <p className="mt-1 text-xs text-textSecondary">Unread updates from the platform</p>
          <Link to="/settings" className="mt-3 inline-block text-sm font-medium text-primary hover:underline">
            Manage preferences
          </Link>
        </div>
        <div className="rounded-2xl border border-border border-l-4 border-l-amber-500 bg-surface p-5 shadow-sm">
          <p className="text-sm text-textSecondary">Your next step</p>
          <p className="mt-1 font-semibold">Keep your records current</p>
          <p className="mt-1 text-xs text-textSecondary">Check your farm, crops, and contact details before the next review.</p>
          <Link to="/farms" className="mt-3 inline-block text-sm font-medium text-primary hover:underline">
            Open my farms
          </Link>
        </div>
      </section>

      <section className="rounded-2xl border border-border bg-surface p-5 shadow-sm">
        <div className="flex items-center justify-between gap-2">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary">Land records</p>
            <h2 className="mt-1 text-lg font-semibold">Your farms <span className="text-textSecondary">({farms.length})</span></h2>
          </div>
          <Link to="/farms" className="text-sm text-primary hover:underline">
            All farms
          </Link>
        </div>
        <ul className="mt-4 grid gap-3 md:grid-cols-2 text-sm">
          {farms.map((f) => (
            <li key={f.id} className="flex items-center justify-between gap-3 rounded-xl border border-border bg-background/50 p-4">
              <div>
                <Link to={`/farms/${f.id}`} className="font-medium text-primary hover:underline">
                  {f.farmName}
                </Link>
                <p className="mt-1 text-xs text-textSecondary">{f.farmCode} · {f.farmSizeHa ?? '—'} ha</p>
              </div>
              <span className="shrink-0 rounded-full bg-background px-2.5 py-1 text-xs font-medium text-textSecondary">
                {(f.status ?? 'Unknown').replace(/_/g, ' ')}
              </span>
            </li>
          ))}
          {!farmsQuery.isLoading && farms.length === 0 ? (
            <li className="text-textSecondary">No farms linked yet.</li>
          ) : null}
        </ul>
      </section>

      <div className="flex flex-wrap gap-3">
        <Link to="/payouts" className="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white">
          View payouts
        </Link>
        <Link to="/settings" className="rounded-xl border border-border px-4 py-2 text-sm font-medium">
          Settings
        </Link>
      </div>
    </div>
  );
}
