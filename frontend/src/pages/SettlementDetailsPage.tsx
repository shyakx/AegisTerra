import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useMemo, useState } from 'react';
import { toast } from 'sonner';
import { settlementsApi } from '../api/settlements';
import { tasksApi } from '../api/tasks';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import WorkflowTimeline from '../components/WorkflowTimeline';

export default function SettlementDetailsPage() {
  const { id = '' } = useParams();
  const { hasPermission } = useAuth();
  const qc = useQueryClient();
  const [providerReference, setProviderReference] = useState('');
  const [confirmNotes, setConfirmNotes] = useState('');

  const settlementQuery = useQuery({
    queryKey: ['settlement', id],
    queryFn: () => settlementsApi.get(id),
    enabled: Boolean(id)
  });
  const timelineQuery = useQuery({
    queryKey: ['settlement-timeline', id],
    queryFn: () => settlementsApi.timeline(id),
    enabled: Boolean(id)
  });
  const ledgerQuery = useQuery({
    queryKey: ['settlement-ledger', id],
    queryFn: () => settlementsApi.settlementLedger(id),
    enabled: Boolean(id) && hasPermission('ledger:read')
  });
  const tasksQuery = useQuery({
    queryKey: ['settlement-tasks', id],
    queryFn: () => tasksApi.search({ subjectType: 'SETTLEMENT', page: 0, size: 50 }),
    enabled: Boolean(id) && hasPermission('tasks:read')
  });
  const workflowQuery = useQuery({
    queryKey: ['settlement-workflow', settlementQuery.data?.workflowInstanceId],
    queryFn: () => settlementsApi.getWorkflowInstance(settlementQuery.data!.workflowInstanceId!),
    enabled: Boolean(settlementQuery.data?.workflowInstanceId) && hasPermission('workflows:read')
  });

  const confirm = useMutation({
    mutationFn: () =>
      settlementsApi.manualConfirm(id, {
        providerReference: providerReference.trim(),
        notes: confirmNotes.trim() || undefined
      }),
    onSuccess: () => {
      toast.success('Manual payment confirmed');
      void qc.invalidateQueries({ queryKey: ['settlement', id] });
      void qc.invalidateQueries({ queryKey: ['settlement-timeline', id] });
      void qc.invalidateQueries({ queryKey: ['settlement-ledger', id] });
      setProviderReference('');
      setConfirmNotes('');
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Confirm failed')
  });

  const settlement = settlementQuery.data;
  const relatedTasks = (tasksQuery.data?.content ?? []).filter((t) => t.subjectId === id);

  const snapshot = useMemo(() => parseSnapshot(settlement?.financialSnapshotJson), [settlement?.financialSnapshotJson]);

  if (settlementQuery.isLoading) {
    return <p className="text-sm text-textSecondary">Loading settlement…</p>;
  }
  if (!settlement) {
    return <p className="text-sm text-danger">Settlement not found</p>;
  }

  const canManualConfirm =
    hasPermission('settlements:process') &&
    settlement.providerCode === 'MANUAL' &&
    ['APPROVED', 'PROCESSING', 'SENT'].includes(settlement.status);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link to="/settlements" className="text-sm text-primary hover:underline">
            ← Settlements
          </Link>
          <h1 className="mt-2 text-3xl font-semibold">{settlement.settlementNumber}</h1>
          <p className="mt-1 text-sm text-textSecondary">
            {settlement.sourceModule} · {settlement.status}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {hasPermission('settlements:approve') ? (
            <Link
              to={`/settlements/finance?settlementId=${id}`}
              className="rounded-xl border border-border px-4 py-2 text-sm"
            >
              Finance review
            </Link>
          ) : null}
          {hasPermission('tasks:read') ? (
            <Link to="/tasks?subjectType=SETTLEMENT" className="rounded-xl border border-border px-4 py-2 text-sm">
              Task inbox
            </Link>
          ) : null}
          {hasPermission('ledger:read') ? (
            <Link to="/ledger" className="rounded-xl border border-border px-4 py-2 text-sm">
              Ledger
            </Link>
          ) : null}
        </div>
      </div>

      <section className="grid gap-4 rounded-2xl border border-border bg-surface p-6 md:grid-cols-3">
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Amount</p>
          <p className="text-xl font-semibold">
            {settlement.amount.toLocaleString()} {settlement.currency}
          </p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Provider</p>
          <p className="text-xl font-semibold">{settlement.providerCode}</p>
          <p className="text-sm text-textSecondary">{settlement.paymentMethod}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Beneficiary</p>
          <p className="font-medium">{settlement.beneficiaryName ?? '—'}</p>
          <p className="text-sm text-textSecondary">{settlement.beneficiaryAccount ?? '—'}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Source</p>
          <p className="font-medium">{settlement.sourceReference ?? settlement.sourceRecordId}</p>
          {settlement.sourceModule === 'CLAIMS' ? (
            <Link to={`/claims/${settlement.sourceRecordId}`} className="text-sm text-primary hover:underline">
              Open claim
            </Link>
          ) : null}
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Workflow</p>
          <p className="font-medium">{settlement.workflowDefinitionCode ?? '—'}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-textSecondary">Provider reference</p>
          <p className="font-medium">{settlement.providerReference ?? '—'}</p>
        </div>
        {settlement.failureReason ? (
          <div className="md:col-span-3">
            <p className="text-xs uppercase tracking-wide text-textSecondary">Failure reason</p>
            <p className="mt-1 text-sm text-danger">{settlement.failureReason}</p>
          </div>
        ) : null}
      </section>

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Financial snapshot</h2>
        <p className="mt-1 text-sm text-textSecondary">Frozen at settlement intake — immutable after approval.</p>
        {snapshot ? (
          <dl className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3 text-sm">
            {Object.entries(snapshot).map(([key, value]) => (
              <div key={key} className="rounded-xl border border-border px-3 py-2">
                <dt className="text-xs uppercase tracking-wide text-textSecondary">{key}</dt>
                <dd className="mt-1 font-medium break-all">{formatSnapshotValue(value)}</dd>
              </div>
            ))}
          </dl>
        ) : (
          <pre className="mt-3 overflow-x-auto rounded-xl border border-border bg-background p-3 text-xs">
            {settlement.financialSnapshotJson || '—'}
          </pre>
        )}
      </section>

      {canManualConfirm ? (
        <section className="rounded-2xl border border-border bg-surface p-4">
          <h2 className="text-lg font-semibold">Manual settlement confirm</h2>
          <p className="mt-1 text-sm text-textSecondary">
            Record external payment reference for ManualSettlementProvider.
          </p>
          <div className="mt-3 flex flex-wrap items-end gap-2">
            <label className="grow text-sm min-w-[12rem]">
              Provider reference
              <input
                value={providerReference}
                onChange={(e) => setProviderReference(e.target.value)}
                className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                required
                aria-required
              />
            </label>
            <label className="grow text-sm min-w-[12rem]">
              Notes
              <input
                value={confirmNotes}
                onChange={(e) => setConfirmNotes(e.target.value)}
                className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              />
            </label>
            <button
              type="button"
              disabled={!providerReference.trim() || confirm.isPending}
              onClick={() => confirm.mutate()}
              className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              Confirm payment
            </button>
          </div>
        </section>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Open tasks</h2>
        <ul className="mt-3 space-y-2">
          {relatedTasks.length === 0 ? (
            <li className="text-sm text-textSecondary">No open tasks linked to this settlement.</li>
          ) : (
            relatedTasks.map((task) => (
              <li
                key={task.id}
                className="flex items-center justify-between rounded-xl border border-border px-3 py-2 text-sm"
              >
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
          {!timelineQuery.isLoading && (timelineQuery.data ?? []).length === 0 ? (
            <li className="text-sm text-textSecondary">No status history yet.</li>
          ) : null}
        </ul>
      </section>

      {hasPermission('ledger:read') ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Ledger entries</h2>
          {ledgerQuery.isLoading ? (
            <p className="mt-3 text-sm text-textSecondary">Loading ledger…</p>
          ) : (ledgerQuery.data ?? []).length === 0 ? (
            <p className="mt-3 text-sm text-textSecondary">No ledger entries posted yet.</p>
          ) : (
            <div className="mt-3 overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-border text-textSecondary">
                    <th className="px-3 py-2 font-medium">#</th>
                    <th className="px-3 py-2 font-medium">Type</th>
                    <th className="px-3 py-2 font-medium">Account</th>
                    <th className="px-3 py-2 font-medium">Amount</th>
                    <th className="px-3 py-2 font-medium">Posted</th>
                  </tr>
                </thead>
                <tbody>
                  {(ledgerQuery.data ?? []).map((entry) => (
                    <tr key={entry.id} className="border-b border-border/60">
                      <td className="px-3 py-2">{entry.entryNo}</td>
                      <td className="px-3 py-2">{entry.entryType}</td>
                      <td className="px-3 py-2">{entry.accountCode}</td>
                      <td className="px-3 py-2">
                        {entry.amount.toLocaleString()} {entry.currency}
                      </td>
                      <td className="px-3 py-2">{new Date(entry.postedAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      ) : null}

      {settlement.workflowInstanceId ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="mb-3 text-lg font-semibold">Workflow timeline</h2>
          <WorkflowTimeline events={workflowQuery.data?.events ?? []} />
        </section>
      ) : null}
    </div>
  );
}

function parseSnapshot(json: string | undefined): Record<string, unknown> | null {
  if (!json) return null;
  try {
    const parsed = JSON.parse(json) as unknown;
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed as Record<string, unknown>;
    }
    return null;
  } catch {
    return null;
  }
}

function formatSnapshotValue(value: unknown): string {
  if (value == null) return '—';
  if (typeof value === 'number') return value.toLocaleString();
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}
