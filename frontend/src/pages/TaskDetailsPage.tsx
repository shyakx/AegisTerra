import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useState } from 'react';
import { toast } from 'sonner';
import { decisionsApi } from '../api/decisions';
import { tasksApi } from '../api/tasks';
import { ApiError } from '../api/client';
import DecisionDialog from '../components/DecisionDialog';
import DecisionTimeline from '../components/DecisionTimeline';
import WorkflowTimeline from '../components/WorkflowTimeline';
import { useAuth } from '../auth/AuthContext';

export default function TaskDetailsPage() {
  const { id = '' } = useParams();
  const { hasPermission } = useAuth();
  const qc = useQueryClient();
  const [comment, setComment] = useState('');
  const [advanceAction, setAdvanceAction] = useState('');
  const [decisionOpen, setDecisionOpen] = useState(false);

  const query = useQuery({
    queryKey: ['task', id],
    queryFn: () => tasksApi.get(id),
    enabled: Boolean(id)
  });

  const decisionsQuery = useQuery({
    queryKey: ['task-decisions', id],
    queryFn: () => decisionsApi.history(id),
    enabled: Boolean(id)
  });

  const typesQuery = useQuery({
    queryKey: ['decision-types'],
    queryFn: () => decisionsApi.types(),
    enabled: decisionOpen
  });

  const action = useMutation({
    mutationFn: async (kind: string) => {
      switch (kind) {
        case 'claim':
          return tasksApi.claim(id);
        case 'complete':
          return tasksApi.complete(id, {
            outcome: 'COMPLETED',
            advanceAction: advanceAction || undefined
          });
        case 'cancel':
          return tasksApi.cancel(id, 'Cancelled from UI');
        case 'comment':
          return tasksApi.comment(id, comment);
        default:
          throw new Error('Unknown action');
      }
    },
    onSuccess: async () => {
      toast.success('Task updated');
      setComment('');
      await qc.invalidateQueries({ queryKey: ['task', id] });
      await qc.invalidateQueries({ queryKey: ['tasks'] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Action failed')
  });

  const decide = useMutation({
    mutationFn: (body: { decisionTypeCode: string; comment?: string; targetUserId?: string }) =>
      decisionsApi.decide(id, body),
    onSuccess: async () => {
      toast.success('Decision recorded');
      setDecisionOpen(false);
      await qc.invalidateQueries({ queryKey: ['task', id] });
      await qc.invalidateQueries({ queryKey: ['task-decisions', id] });
      await qc.invalidateQueries({ queryKey: ['tasks'] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Decision failed')
  });

  const task = query.data;
  const open = task && !['COMPLETED', 'CANCELLED', 'REJECTED', 'EXPIRED'].includes(task.status);
  const canDecide = hasPermission('decisions:act') || hasPermission('tasks:act');

  return (
    <div className="space-y-6">
      <div>
        <Link to="/tasks" className="text-sm text-primary hover:underline">
          ← Tasks
        </Link>
        <h1 className="mt-2 text-3xl font-semibold">{task?.title ?? 'Task'}</h1>
        <p className="text-sm text-textSecondary">
          {task?.status ?? 'Loading…'} · {task?.taskType} · {task?.subjectType}
        </p>
      </div>

      {query.isError ? (
        <p className="text-sm text-red-600">
          {query.error instanceof ApiError ? query.error.message : 'Failed to load task'}
        </p>
      ) : null}

      {task ? (
        <section className="grid gap-6 lg:grid-cols-2">
          <div className="space-y-3 rounded-2xl border border-border bg-surface p-6">
            <h2 className="text-lg font-semibold">Details</h2>
            <dl className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-textSecondary">Step</dt>
                <dd className="font-medium">{task.stepCode}</dd>
              </div>
              <div>
                <dt className="text-textSecondary">Role / user</dt>
                <dd className="font-medium">{task.assigneeRoleCode ?? task.assigneeUserId ?? '—'}</dd>
              </div>
              <div className="col-span-2">
                <dt className="text-textSecondary">Description</dt>
                <dd className="font-medium">{task.description ?? '—'}</dd>
              </div>
            </dl>

            {canDecide && open ? (
              <div className="mt-4 space-y-3 border-t border-border pt-4">
                {(task.status === 'PENDING' || task.status === 'ASSIGNED') && (
                  <button
                    type="button"
                    className="rounded-xl bg-primary px-3 py-2 text-sm text-white"
                    onClick={() => action.mutate('claim')}
                  >
                    Claim
                  </button>
                )}
                <button
                  type="button"
                  className="rounded-xl bg-primary px-3 py-2 text-sm text-white"
                  onClick={() => setDecisionOpen(true)}
                >
                  Record decision
                </button>
                <label className="block text-sm">
                  Advance workflow action (optional complete)
                  <input
                    value={advanceAction}
                    onChange={(e) => setAdvanceAction(e.target.value.toUpperCase())}
                    className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                    placeholder="e.g. APPROVE"
                  />
                </label>
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    className="rounded-xl border border-border px-3 py-2 text-sm"
                    onClick={() => action.mutate('complete')}
                  >
                    Complete
                  </button>
                  <button
                    type="button"
                    className="rounded-xl border border-border px-3 py-2 text-sm"
                    onClick={() => action.mutate('cancel')}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : null}
          </div>

          <div className="rounded-2xl border border-border bg-surface p-6">
            <h2 className="text-lg font-semibold">Workflow timeline</h2>
            <div className="mt-4">
              <WorkflowTimeline events={task.timeline ?? []} />
            </div>
          </div>
        </section>
      ) : null}

      {task ? (
        <section className="grid gap-6 lg:grid-cols-2">
          <div className="rounded-2xl border border-border bg-surface p-6">
            <h2 className="text-lg font-semibold">Decision history</h2>
            <div className="mt-4">
              <DecisionTimeline decisions={decisionsQuery.data ?? []} />
            </div>
          </div>

          <div className="rounded-2xl border border-border bg-surface p-6">
            <h2 className="text-lg font-semibold">Assignment history</h2>
            <ul className="mt-3 space-y-2 text-sm">
              {(task.assignments ?? []).map((a) => (
                <li key={a.id} className="rounded-xl border border-border bg-background px-3 py-2">
                  <span className="font-medium">{a.action}</span>
                  {a.toRoleCode ? ` → role ${a.toRoleCode}` : ''}
                  {a.toUserId ? ` → user ${a.toUserId.slice(0, 8)}…` : ''}
                  <span className="block text-xs text-textSecondary">{new Date(a.occurredAt).toLocaleString()}</span>
                </li>
              ))}
              {(task.assignments?.length ?? 0) === 0 ? <li className="text-textSecondary">No assignments yet.</li> : null}
            </ul>
          </div>
        </section>
      ) : null}

      {task ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Comments</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {(task.comments ?? []).map((c) => (
              <li key={c.id} className="rounded-xl border border-border bg-background px-3 py-2">
                {c.body}
                <span className="block text-xs text-textSecondary">{new Date(c.createdAt).toLocaleString()}</span>
              </li>
            ))}
          </ul>
          {hasPermission('tasks:act') ? (
            <div className="mt-3 flex gap-2">
              <input
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                className="flex-1 rounded-xl border border-border bg-background px-3 py-2 text-sm"
                placeholder="Add comment"
              />
              <button
                type="button"
                className="rounded-xl border border-border px-3 py-2 text-sm"
                disabled={!comment.trim()}
                onClick={() => action.mutate('comment')}
              >
                Post
              </button>
            </div>
          ) : null}
        </section>
      ) : null}

      <DecisionDialog
        open={decisionOpen}
        types={typesQuery.data ?? []}
        busy={decide.isPending}
        onClose={() => setDecisionOpen(false)}
        onSubmit={(body) => decide.mutate(body)}
      />
    </div>
  );
}
