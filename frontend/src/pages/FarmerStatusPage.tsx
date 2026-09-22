import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { agriApi } from '../api/agriculture';
import { ApiError } from '../api/client';
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

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Farmer"
        title="Farmer status"
        description="Your registration status, farms, and next actions."
      />

      {farmerQuery.error instanceof ApiError ? (
        <p className="text-sm text-danger" role="alert">
          {farmerQuery.error.message}
        </p>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-5">
        <h2 className="font-semibold">Profile</h2>
        {farmerQuery.isLoading ? (
          <p className="mt-2 text-sm text-textSecondary">Loading…</p>
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
              <dd className="font-medium">{farmer.status ?? '—'}</dd>
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
      </section>

      <section className="rounded-2xl border border-border bg-surface p-5">
        <div className="flex items-center justify-between gap-2">
          <h2 className="font-semibold">Farms ({farms.length})</h2>
          <Link to="/farms" className="text-sm text-primary hover:underline">
            All farms
          </Link>
        </div>
        <ul className="mt-3 space-y-2 text-sm">
          {farms.map((f) => (
            <li key={f.id} className="flex justify-between gap-2 border-t border-border pt-2">
              <Link to={`/farms/${f.id}`} className="font-medium text-primary hover:underline">
                {f.farmName}
              </Link>
              <span className="text-textSecondary">{f.status}</span>
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
          Setting
        </Link>
      </div>
    </div>
  );
}
