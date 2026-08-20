import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { useMemo } from 'react';
import { settlementsApi } from '../api/settlements';
import { tasksApi, type WorkflowTask } from '../api/tasks';
import { ApiError } from '../api/client';
import { EnterpriseTable } from '../components/EnterpriseTable';

export default function SettlementFinanceReviewPage() {
  const [params] = useSearchParams();
  const settlementId = params.get('settlementId') ?? '';

  const settlementQuery = useQuery({
    queryKey: ['settlement-finance', settlementId],
    queryFn: () => settlementsApi.get(settlementId),
    enabled: Boolean(settlementId)
  });

  const tasksQuery = useQuery({
    queryKey: ['settlement-finance-tasks', settlementId],
    queryFn: () =>
      tasksApi.search({
        subjectType: 'SETTLEMENT',
        status: 'PENDING,ASSIGNED,IN_PROGRESS',
        page: 0,
        size: 50,
        sort: 'createdAt,desc'
      })
  });

  const queue = useMemo(() => {
    const all = tasksQuery.data?.content ?? [];
    const financeish = all.filter(
      (t) =>
        t.stepCode?.includes('FINANCE') ||
        t.stepCode?.includes('APPROVAL') ||
        t.taskType === 'VERIFICATION' ||
        t.taskType === 'APPROVAL'
    );
    if (settlementId) {
      return financeish.filter((t) => t.subjectId === settlementId);
    }
    return financeish.length > 0 ? financeish : all;
  }, [tasksQuery.data?.content, settlementId]);

  const snapshot = useMemo(() => {
    const json = settlementQuery.data?.financialSnapshotJson;
    if (!json) return null;
    try {
      const parsed = JSON.parse(json) as unknown;
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
        ? (parsed as Record<string, unknown>)
        : null;
    } catch {
      return null;
    }
  }, [settlementQuery.data?.financialSnapshotJson]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Financial settlement</p>
          <h1 className="text-3xl font-semibold">Finance review</h1>
          <p className="mt-1 text-sm text-textSecondary">
            Review settlement snapshots and advance finance / approval tasks via the shared task inbox.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/settlements" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
            Settlements
          </Link>
          <Link
            to="/tasks?subjectType=SETTLEMENT"
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90"
          >
            Open task inbox
          </Link>
        </div>
      </div>

      {settlementId ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          {settlementQuery.isLoading ? (
            <p className="text-sm text-textSecondary">Loading settlement…</p>
          ) : settlementQuery.isError ? (
            <p className="text-sm text-danger" role="alert">
              {settlementQuery.error instanceof ApiError
                ? settlementQuery.error.message
                : 'Failed to load settlement'}
            </p>
          ) : settlementQuery.data ? (
            <>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-semibold">{settlementQuery.data.settlementNumber}</h2>
                  <p className="text-sm text-textSecondary">
                    {settlementQuery.data.status} · {settlementQuery.data.providerCode}
                  </p>
                </div>
                <Link
                  to={`/settlements/${settlementQuery.data.id}`}
                  className="text-sm text-primary hover:underline"
                >
                  Full detail
                </Link>
              </div>
              <div className="mt-4 grid gap-3 sm:grid-cols-3 text-sm">
                <div>
                  <p className="text-xs uppercase tracking-wide text-textSecondary">Amount</p>
                  <p className="font-semibold">
                    {settlementQuery.data.amount.toLocaleString()} {settlementQuery.data.currency}
                  </p>
                </div>
                <div>
                  <p className="text-xs uppercase tracking-wide text-textSecondary">Beneficiary</p>
                  <p className="font-medium">{settlementQuery.data.beneficiaryName ?? '—'}</p>
                </div>
                <div>
                  <p className="text-xs uppercase tracking-wide text-textSecondary">Source</p>
                  <p className="font-medium">
                    {settlementQuery.data.sourceModule} ·{' '}
                    {settlementQuery.data.sourceReference ?? settlementQuery.data.sourceRecordId}
                  </p>
                </div>
              </div>
              {snapshot ? (
                <dl className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3 text-sm">
                  {Object.entries(snapshot)
                    .slice(0, 9)
                    .map(([key, value]) => (
                      <div key={key} className="rounded-xl border border-border px-3 py-2">
                        <dt className="text-xs text-textSecondary">{key}</dt>
                        <dd className="font-medium break-all">
                          {value == null
                            ? '—'
                            : typeof value === 'object'
                              ? JSON.stringify(value)
                              : String(value)}
                        </dd>
                      </div>
                    ))}
                </dl>
              ) : null}
            </>
          ) : null}
        </section>
      ) : (
        <section className="rounded-2xl border border-border bg-surface p-4 text-sm text-textSecondary">
          Select a settlement from the list or open a finance task to deep-link a snapshot here.
        </section>
      )}

      <EnterpriseTable<WorkflowTask>
        title="Finance & approval queue"
        subtitle="Tasks for SETTLEMENT subjects — decisions are recorded on the task detail screen"
        rows={queue}
        loading={tasksQuery.isLoading}
        error={
          tasksQuery.error instanceof ApiError
            ? tasksQuery.error.message
            : tasksQuery.error
              ? 'Failed to load tasks'
              : null
        }
        emptyMessage="No finance review tasks in the current queue."
        getRowKey={(row) => row.id}
        columns={[
          {
            key: 'title',
            header: 'Task',
            sortValue: (r) => r.title,
            render: (r) => (
              <Link to={`/tasks/${r.id}`} className="font-medium text-primary hover:underline">
                {r.title}
              </Link>
            )
          },
          {
            key: 'step',
            header: 'Step',
            sortValue: (r) => r.stepCode ?? '',
            render: (r) => r.stepCode ?? '—'
          },
          {
            key: 'subject',
            header: 'Settlement',
            render: (r) =>
              r.subjectId ? (
                <Link to={`/settlements/${r.subjectId}`} className="text-primary hover:underline">
                  {r.subjectId.slice(0, 8)}…
                </Link>
              ) : (
                '—'
              )
          },
          {
            key: 'status',
            header: 'Status',
            sortValue: (r) => r.status,
            render: (r) => (
              <span className="rounded-lg bg-background px-2 py-1 text-xs font-medium">{r.status}</span>
            )
          }
        ]}
      />
    </div>
  );
}
