import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useState } from 'react';
import { toast } from 'sonner';
import { insuranceApi } from '../api/insurance';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function PolicyDetailsPage() {
  const { id = '' } = useParams();
  const { hasPermission } = useAuth();
  const qc = useQueryClient();
  const [reason, setReason] = useState('');

  const policyQuery = useQuery({
    queryKey: ['policy', id],
    queryFn: () => insuranceApi.getPolicy(id),
    enabled: Boolean(id)
  });
  const docsQuery = useQuery({
    queryKey: ['policy-docs', id],
    queryFn: () => insuranceApi.listDocuments(id),
    enabled: Boolean(id)
  });

  const action = useMutation({
    mutationFn: async (kind: string) => {
      switch (kind) {
        case 'approve':
          return insuranceApi.approve(id, reason || 'approved');
        case 'reject':
          return insuranceApi.reject(id, reason || 'rejected');
        case 'paid':
          return insuranceApi.markPaid(id, reason || 'paid');
        case 'suspend':
          return insuranceApi.suspend(id, reason || 'suspended');
        case 'reinstate':
          return insuranceApi.reinstate(id, reason || 'reinstated');
        case 'cancel':
          return insuranceApi.cancel(id, reason || 'cancelled');
        case 'renew':
          return insuranceApi.renew(id);
        case 'regen':
          return insuranceApi.regenerateDocuments(id, 'POLICY_CERTIFICATE');
        default:
          throw new Error('Unknown action');
      }
    },
    onSuccess: async () => {
      toast.success('Policy updated');
      await qc.invalidateQueries({ queryKey: ['policy', id] });
      await qc.invalidateQueries({ queryKey: ['policy-docs', id] });
      await qc.invalidateQueries({ queryKey: ['policies'] });
      setReason('');
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Action failed')
  });

  const policy = policyQuery.data;
  const status = policy?.status;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link to="/policies" className="text-sm text-primary hover:underline">
            ← Policies
          </Link>
          <h1 className="mt-2 text-3xl font-semibold">{policy?.policyNumber ?? 'Policy'}</h1>
          <p className="text-sm text-textSecondary">{status ?? 'Loading…'}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {policy && hasPermission('claims:write') && policy.status === 'ACTIVE' ? (
            <Link
              to={`/claims/new?policyId=${policy.id}`}
              className="rounded-xl border border-border px-3 py-2 text-sm hover:bg-background"
            >
              File claim
            </Link>
          ) : null}
          {policy && hasPermission('claims:read') ? (
            <Link
              to="/claims"
              className="rounded-xl border border-border px-3 py-2 text-sm hover:bg-background"
            >
              Claims
            </Link>
          ) : null}
        </div>
      </div>

      {policyQuery.isError ? (
        <p className="text-sm text-red-600">
          {policyQuery.error instanceof ApiError ? policyQuery.error.message : 'Failed to load policy'}
        </p>
      ) : null}

      {policy ? (
        <section className="grid gap-6 lg:grid-cols-2">
          <div className="space-y-3 rounded-2xl border border-border bg-surface p-6">
            <h2 className="text-lg font-semibold">Coverage</h2>
            <dl className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-textSecondary">Premium</dt>
                <dd className="font-medium">
                  {Number(policy.premiumAmount).toLocaleString()} {policy.currency}
                </dd>
              </div>
              <div>
                <dt className="text-textSecondary">Sum insured</dt>
                <dd className="font-medium">
                  {Number(policy.coverageAmount).toLocaleString()} {policy.currency}
                </dd>
              </div>
              <div>
                <dt className="text-textSecondary">Period</dt>
                <dd className="font-medium">
                  {policy.startDate} → {policy.endDate}
                </dd>
              </div>
              <div>
                <dt className="text-textSecondary">Last reason</dt>
                <dd className="font-medium">{policy.transitionReason ?? '—'}</dd>
              </div>
              {hasPermission('farmers:read') ? (
                <div>
                  <dt className="text-textSecondary">Farmer</dt>
                  <dd className="font-medium">
                    <Link to={`/farmers/${policy.farmerId}`} className="text-primary hover:underline">
                      Open profile
                    </Link>
                  </dd>
                </div>
              ) : null}
              {hasPermission('farms:read') ? (
                <div>
                  <dt className="text-textSecondary">Farm</dt>
                  <dd className="font-medium">
                    <Link to={`/farms/${policy.farmId}`} className="text-primary hover:underline">
                      Open farm
                    </Link>
                  </dd>
                </div>
              ) : null}
            </dl>
          </div>

          {hasPermission('policies:write') || hasPermission('policies:approve') ? (
            <div className="space-y-3 rounded-2xl border border-border bg-surface p-6">
              <h2 className="text-lg font-semibold">Lifecycle actions</h2>
              <label className="block text-sm">
                Reason
                <input
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                  placeholder="Required for reject / suspend / cancel"
                />
              </label>
              <div className="flex flex-wrap gap-2">
                {status === 'UNDER_REVIEW' && hasPermission('policies:approve') ? (
                  <>
                    <button
                      type="button"
                      className="rounded-xl bg-primary px-3 py-2 text-sm text-white"
                      onClick={() => action.mutate('approve')}
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      className="rounded-xl border border-border px-3 py-2 text-sm"
                      onClick={() => action.mutate('reject')}
                    >
                      Reject
                    </button>
                  </>
                ) : null}
                {status === 'PREMIUM_PENDING' && hasPermission('policies:write') ? (
                  <button
                    type="button"
                    className="rounded-xl bg-primary px-3 py-2 text-sm text-white"
                    onClick={() => action.mutate('paid')}
                  >
                    Mark premium paid
                  </button>
                ) : null}
                {status === 'ACTIVE' && hasPermission('policies:write') ? (
                  <>
                    <button
                      type="button"
                      className="rounded-xl border border-border px-3 py-2 text-sm"
                      onClick={() => action.mutate('suspend')}
                    >
                      Suspend
                    </button>
                    <button
                      type="button"
                      className="rounded-xl border border-border px-3 py-2 text-sm"
                      onClick={() => action.mutate('renew')}
                    >
                      Renew
                    </button>
                  </>
                ) : null}
                {status === 'SUSPENDED' && hasPermission('policies:write') ? (
                  <button
                    type="button"
                    className="rounded-xl bg-primary px-3 py-2 text-sm text-white"
                    onClick={() => action.mutate('reinstate')}
                  >
                    Reinstate
                  </button>
                ) : null}
                {status && !['CANCELLED', 'REJECTED', 'EXPIRED'].includes(status) && hasPermission('policies:write') ? (
                  <button
                    type="button"
                    className="rounded-xl border border-red-200 px-3 py-2 text-sm text-red-700"
                    onClick={() => action.mutate('cancel')}
                  >
                    Cancel
                  </button>
                ) : null}
              </div>
            </div>
          ) : null}
        </section>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">Documents</h2>
          {hasPermission('policies:write') ? (
            <button
              type="button"
              className="rounded-xl border border-border px-3 py-2 text-sm"
              onClick={() => action.mutate('regen')}
            >
              Regenerate
            </button>
          ) : null}
        </div>
        <div className="mt-4 space-y-3">
          {(docsQuery.data ?? []).map((doc) => (
            <article key={doc.id} className="rounded-xl border border-border bg-background p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="font-medium">
                  {doc.documentType} · v{doc.versionNo}
                </p>
                <p className="text-sm text-textSecondary">{doc.signatureStatus}</p>
              </div>
              <pre className="mt-3 max-h-48 overflow-auto whitespace-pre-wrap text-xs text-textSecondary">
                {doc.contentText}
              </pre>
              {doc.qrPayload ? <p className="mt-2 text-xs text-textSecondary">QR: {doc.qrPayload}</p> : null}
            </article>
          ))}
          {!docsQuery.isLoading && (docsQuery.data?.length ?? 0) === 0 ? (
            <p className="text-sm text-textSecondary">Documents appear after activation.</p>
          ) : null}
        </div>
      </section>
    </div>
  );
}
