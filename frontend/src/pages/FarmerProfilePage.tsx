import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { agriApi } from '../api/agriculture';
import { insuranceApi } from '../api/insurance';
import { claimsApi } from '../api/claims';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function FarmerProfilePage() {
  const { id = '' } = useParams();
  const { hasPermission, hasRole } = useAuth();
  const farmerQuery = useQuery({
    queryKey: ['farmer', id],
    queryFn: () => agriApi.getFarmer(id),
    enabled: Boolean(id)
  });
  const farmsQuery = useQuery({
    queryKey: ['farms', 'by-farmer', id],
    queryFn: () => agriApi.searchFarms({ farmerId: id, size: 50 }),
    enabled: Boolean(id)
  });
  const policiesQuery = useQuery({
    queryKey: ['policies', 'by-farmer', id],
    queryFn: () => insuranceApi.searchPolicies({ farmerId: id, size: 20 }),
    enabled: Boolean(id) && hasPermission('policies:read')
  });
  const claimsQuery = useQuery({
    queryKey: ['claims', 'by-farmer', id],
    queryFn: () => claimsApi.search({ farmerId: id, size: 20 }),
    enabled: Boolean(id) && hasPermission('claims:read')
  });

  if (farmerQuery.isLoading) {
    return <p className="text-textSecondary">Loading farmer profile…</p>;
  }
  if (farmerQuery.error) {
    return (
      <p className="text-red-700" role="alert">
        {farmerQuery.error instanceof ApiError ? farmerQuery.error.message : 'Failed to load farmer'}
      </p>
    );
  }
  const farmer = farmerQuery.data!;

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-textSecondary">Farmer profile</p>
        <h1 className="text-3xl font-semibold">
          {farmer.firstName} {farmer.lastName}
        </h1>
        <p className="mt-1 text-textSecondary">
          {farmer.farmerCode} · {farmer.status}
          {hasRole('FARMER') ? ' · Your linked profile' : ''}
        </p>
      </div>

      <section className="grid gap-4 rounded-2xl border border-border bg-surface p-6 md:grid-cols-2">
        <div>
          <h2 className="text-lg font-semibold">Identity</h2>
          <dl className="mt-3 space-y-2 text-sm">
            <div>
              <dt className="text-textSecondary">National ID</dt>
              <dd>{farmer.nationalId}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Phone</dt>
              <dd>{farmer.phoneNumber}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Email</dt>
              <dd>{farmer.email ?? '—'}</dd>
            </div>
          </dl>
        </div>
        <div>
          <h2 className="text-lg font-semibold">Farms</h2>
          {farmsQuery.isLoading ? (
            <p className="mt-3 text-sm text-textSecondary">Loading farms…</p>
          ) : (farmsQuery.data?.content.length ?? 0) === 0 ? (
            <p className="mt-3 text-sm text-textSecondary">No farms linked yet.</p>
          ) : (
            <ul className="mt-3 space-y-2">
              {farmsQuery.data!.content.map((farm) => (
                <li key={farm.id}>
                  <Link to={`/farms/${farm.id}`} className="text-primary hover:underline">
                    {farm.farmName}
                  </Link>
                  <span className="ml-2 text-sm text-textSecondary">{farm.status}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="grid gap-4 rounded-2xl border border-border bg-surface p-6 md:grid-cols-2">
        {hasPermission('policies:read') ? (
          <div>
            <h2 className="text-lg font-semibold">Policies</h2>
            {policiesQuery.isLoading ? (
              <p className="mt-3 text-sm text-textSecondary">Loading policies…</p>
            ) : (policiesQuery.data?.content.length ?? 0) === 0 ? (
              <p className="mt-3 text-sm text-textSecondary">No policies linked yet.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {policiesQuery.data!.content.map((policy) => (
                  <li key={policy.id}>
                    <Link to={`/policies/${policy.id}`} className="text-primary hover:underline">
                      {policy.policyNumber}
                    </Link>
                    <span className="ml-2 text-sm text-textSecondary">{policy.status}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        ) : null}
        {hasPermission('claims:read') ? (
          <div>
            <h2 className="text-lg font-semibold">Claims</h2>
            {claimsQuery.isLoading ? (
              <p className="mt-3 text-sm text-textSecondary">Loading claims…</p>
            ) : (claimsQuery.data?.content.length ?? 0) === 0 ? (
              <p className="mt-3 text-sm text-textSecondary">No claims filed yet.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {claimsQuery.data!.content.map((claim) => (
                  <li key={claim.id}>
                    <Link to={`/claims/${claim.id}`} className="text-primary hover:underline">
                      {claim.claimNumber}
                    </Link>
                    <span className="ml-2 text-sm text-textSecondary">{claim.status}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        ) : null}
      </section>
    </div>
  );
}
