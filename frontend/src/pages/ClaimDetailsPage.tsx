import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useState } from 'react';
import { toast } from 'sonner';
import { claimsApi } from '../api/claims';
import { tasksApi } from '../api/tasks';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import WorkflowTimeline from '../components/WorkflowTimeline';

export default function ClaimDetailsPage() {
  const { id = '' } = useParams();
  const { hasPermission } = useAuth();
  const qc = useQueryClient();
  const [cancelReason, setCancelReason] = useState('');

  const claimQuery = useQuery({
    queryKey: ['claim', id],
    queryFn: () => claimsApi.get(id),
    enabled: Boolean(id)
  });
  const timelineQuery = useQuery({
    queryKey: ['claim-timeline', id],
    queryFn: () => claimsApi.timeline(id),
    enabled: Boolean(id)
  });
  const tasksQuery = useQuery({
    queryKey: ['claim-tasks', id],
    queryFn: () => tasksApi.search({ subjectType: 'CLAIM', page: 0, size: 50 }),
    enabled: Boolean(id) && hasPermission('tasks:read')
  });
  const workflowQuery = useQuery({
    queryKey: ['claim-workflow', claimQuery.data?.workflowInstanceId],
    queryFn: () => claimsApi.getWorkflowInstance(claimQuery.data!.workflowInstanceId!),
    enabled: Boolean(claimQuery.data?.workflowInstanceId) && hasPermission('workflows:read')
  });

  const cancel = useMutation({
    mutationFn: () => claimsApi.cancel(id, cancelReason || 'withdrawn'),
    onSuccess: () => {
      toast.success('Claim cancelled');
      void qc.invalidateQueries({ queryKey: ['claim', id] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Cancel failed')
  });

  const submit = useMutation({
    mutationFn: () => claimsApi.submit(id),
    onSuccess: () => {
      toast.success('Claim submitted');
      void qc.invalidateQueries({ queryKey: ['claim', id] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Submit failed')
  });

  const claim = claimQuery.data;
  const relatedTasks = (tasksQuery.data?.content ?? []).filter((t) => t.subjectId === id);

  if (claimQuery.isLoading) {
    return <p className="text-sm text-textSecondary">Loading claim…</p>;
  }
  if (!claim) {
    return <p className="text-sm text-danger">Claim not found</p>;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Claim detail</p>
          <h1 className="text-3xl font-semibold">{claim.claimNumber}</h1>
          <p className="mt-1 text-sm text-textSecondary">
            {claim.claimTypeCode} · {claim.status}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to={`/claims/${id}/evidence`} className="rounded-xl border border-border px-4 py-2 text-sm">
            Evidence
          </Link>
          {hasPermission('claims:assess') ? (
            <>
              <Link to={`/claims/${id}/inspection`} className="rounded-xl border border-border px-4 py-2 text-sm">
                Inspection
              </Link>
              <Link to={`/claims/${id}/assessment`} className="rounded-xl border border-border px-4 py-2 text-sm">
                Assessment
              </Link>
            </>
          ) : null}
          {hasPermission('tasks:read') ? (
            <Link to="/tasks?subjectType=CLAIM" className="rounded-xl border border-border px-4 py-2 text-sm">
              Task inbox
            </Link>
          ) : null}
        </div>
      </div>

      <section className="grid gap-4 rounded-2xl border border-border bg-surface p-6 md:grid-cols-3">
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Claimed</p>
          <p className="text-xl font-semibold">
            {claim.claimedAmount.toLocaleString()} {claim.currency}
          </p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Assessed</p>
          <p className="text-xl font-semibold">
            {claim.assessedAmount != null ? `${claim.assessedAmount.toLocaleString()} ${claim.currency}` : '—'}
          </p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Approved</p>
          <p className="text-xl font-semibold">
            {claim.approvedAmount != null ? `${claim.approvedAmount.toLocaleString()} ${claim.currency}` : '—'}
          </p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Policy</p>
          <Link to={`/policies/${claim.policyId}`} className="font-medium text-primary hover:underline">
            {claim.policyId.slice(0, 8)}…
          </Link>
        </div>
        {claim.farmerId && hasPermission('farmers:read') ? (
          <div>
            <p className="text-xs uppercase tracking-wide text-textSecondary">Farmer</p>
            <Link to={`/farmers/${claim.farmerId}`} className="font-medium text-primary hover:underline">
              Open profile
            </Link>
          </div>
        ) : null}
        {claim.farmId && hasPermission('farms:read') ? (
          <div>
            <p className="text-xs uppercase tracking-wide text-textSecondary">Farm</p>
            <Link to={`/farms/${claim.farmId}`} className="font-medium text-primary hover:underline">
              Open farm
            </Link>
          </div>
        ) : null}
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Incident</p>
          <p className="font-medium">{claim.incidentDate}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Workflow</p>
          <p className="font-medium">{claim.workflowDefinitionCode ?? '—'}</p>
        </div>
        <div className="md:col-span-3">
          <p className="text-xs uppercase tracking-wide text-textSecondary">Description</p>
          <p className="mt-1 text-sm">{claim.description}</p>
        </div>
      </section>

      {hasPermission('claims:write') && (claim.status === 'DRAFT' || claim.status === 'RETURNED_FOR_INFO') ? (
        <section className="rounded-2xl border border-border bg-surface p-4">
          <button
            type="button"
            onClick={() => submit.mutate()}
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
          >
            Submit claim
          </button>
        </section>
      ) : null}

      {hasPermission('claims:write') && !['APPROVED', 'PAYMENT_PENDING', 'SETTLED', 'CLOSED'].includes(claim.status) ? (
        <section className="flex flex-wrap items-end gap-2 rounded-2xl border border-border bg-surface p-4">
          <label className="grow text-sm">
            Cancel reason
            <input
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
          <button
            type="button"
            onClick={() => cancel.mutate()}
            className="rounded-xl border border-danger px-4 py-2 text-sm text-danger"
          >
            Cancel claim
          </button>
        </section>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Open tasks</h2>
        <ul className="mt-3 space-y-2">
          {relatedTasks.length === 0 ? (
            <li className="text-sm text-textSecondary">No open tasks linked to this claim.</li>
          ) : (
            relatedTasks.map((task) => (
              <li key={task.id} className="flex items-center justify-between rounded-xl border border-border px-3 py-2 text-sm">
                <span>
                  {task.title} · {task.stepCode} · {task.status}
                </span>
                <Link to={`/tasks/${task.id}`} className="text-primary hover:underline">
                  Open
                </Link>
              </li>
            ))
          )}
        </ul>
      </section>

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Status history</h2>
        <ul className="mt-3 space-y-2">
          {(timelineQuery.data ?? []).map((entry, idx) => (
            <li key={`${entry.occurredAt}-${idx}`} className="rounded-xl border border-border px-3 py-2 text-sm">
              <span className="font-medium">
                {entry.fromStatus ?? '—'} → {entry.toStatus}
              </span>
              <span className="text-textSecondary"> · {new Date(entry.occurredAt).toLocaleString()}</span>
              {entry.reason ? <p className="text-textSecondary">{entry.reason}</p> : null}
            </li>
          ))}
        </ul>
      </section>

      {claim.workflowInstanceId ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="mb-3 text-lg font-semibold">Workflow timeline</h2>
          <WorkflowTimeline events={workflowQuery.data?.events ?? []} />
        </section>
      ) : null}
    </div>
  );
}
