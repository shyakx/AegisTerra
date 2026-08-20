import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { tasksApi, type WorkflowTask } from '../api/tasks';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';

type Mode = 'inbox' | 'my';

export default function TasksPage() {
  const [mode, setMode] = useState<Mode>('my');
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [subjectType, setSubjectType] = useState('');
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['tasks', mode, q, status, subjectType, page],
    queryFn: () =>
      mode === 'my'
        ? tasksApi.my({ q, status, subjectType, page, size: 20, sort: 'createdAt,desc' })
        : tasksApi.search({ q, status, subjectType, page, size: 20, sort: 'createdAt,desc' })
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Workflow engine</p>
          <h1 className="text-3xl font-semibold">Tasks</h1>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => {
              setMode('my');
              setPage(0);
            }}
            className={`rounded-xl px-4 py-2 text-sm ${mode === 'my' ? 'bg-primary text-white' : 'border border-border'}`}
          >
            My tasks
          </button>
          <button
            type="button"
            onClick={() => {
              setMode('inbox');
              setPage(0);
            }}
            className={`rounded-xl px-4 py-2 text-sm ${mode === 'inbox' ? 'bg-primary text-white' : 'border border-border'}`}
          >
            All inbox
          </button>
        </div>
      </div>

      <EnterpriseTable<WorkflowTask>
        title={mode === 'my' ? 'Assigned to me & role pools' : 'Operations inbox'}
        subtitle="Generic workflow tasks — no domain-specific screens"
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        error={query.error instanceof ApiError ? query.error.message : query.error ? 'Failed to load tasks' : null}
        emptyMessage="No tasks match these filters."
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        searchPlaceholder="Title or subject type…"
        filters={
          <div className="flex flex-wrap gap-2">
            <select
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value);
              }}
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
            >
              <option value="">All statuses</option>
              {['PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED'].map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
            <input
              value={subjectType}
              onChange={(e) => {
                setPage(0);
                setSubjectType(e.target.value.toUpperCase());
              }}
              placeholder="Subject type"
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
            />
          </div>
        }
        page={page}
        totalPages={query.data?.totalPages ?? 1}
        onPageChange={setPage}
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
          { key: 'type', header: 'Type', sortValue: (r) => r.taskType, render: (r) => r.taskType },
          {
            key: 'subject',
            header: 'Subject',
            sortValue: (r) => r.subjectType,
            render: (r) => `${r.subjectType}`
          },
          { key: 'step', header: 'Step', sortValue: (r) => r.stepCode, render: (r) => r.stepCode },
          { key: 'status', header: 'Status', sortValue: (r) => r.status, render: (r) => r.status },
          {
            key: 'assignee',
            header: 'Assignee',
            sortValue: (r) => r.assigneeRoleCode ?? r.assigneeUserId ?? '',
            render: (r) => r.assigneeRoleCode ?? r.assigneeUserId ?? '—'
          }
        ]}
      />
    </div>
  );
}
