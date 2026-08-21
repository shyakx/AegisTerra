import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { ShieldCheck, UserCog } from 'lucide-react';
import { ApiError } from '../api/client';
import { usersApi } from '../api/users';
import { PageHeader } from '../components/PageHeader';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../auth/AuthContext';

export default function UsersPage() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    displayName: '',
    role: 'SUPPORT'
  });
  const [edit, setEdit] = useState({ email: '', displayName: '', status: 'ACTIVE', roles: ['SUPPORT'] });
  const [message, setMessage] = useState<string | null>(null);

  const usersQuery = useQuery({
    queryKey: ['users', q, status, page],
    queryFn: () => usersApi.search({ q, status: status || undefined, page, size: 20 }),
    enabled: hasPermission('users:read')
  });

  const rolesQuery = useQuery({
    queryKey: ['roles'],
    queryFn: () => usersApi.listRoles(),
    enabled: hasPermission('roles:read') || hasPermission('users:write')
  });

  const permissionsQuery = useQuery({
    queryKey: ['permissions'],
    queryFn: () => usersApi.listPermissions(),
    enabled: hasPermission('permissions:read')
  });

  const auditQuery = useQuery({
    queryKey: ['user-audit', selectedId, 'entries'],
    queryFn: () => usersApi.audit(selectedId!),
    enabled: !!selectedId && hasPermission('users:read')
  });

  const selected = useMemo(
    () => usersQuery.data?.content.find((u) => u.id === selectedId) ?? null,
    [usersQuery.data, selectedId]
  );
  const auditEntries = Array.isArray(auditQuery.data) ? auditQuery.data : [];

  const createMutation = useMutation({
    mutationFn: () =>
      usersApi.create({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        displayName: form.displayName.trim() || undefined,
        roles: [form.role]
      }),
    onSuccess: () => {
      setMessage('User created');
      setForm({ username: '', email: '', password: '', displayName: '', role: 'SUPPORT' });
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (err) => setMessage(err instanceof ApiError ? err.message : 'Create failed')
  });

  const updateMutation = useMutation({
    mutationFn: () => {
      if (!selectedId) throw new Error('No user selected');
      return usersApi.update(selectedId, {
        email: edit.email,
        displayName: edit.displayName,
        status: edit.status,
        roles: edit.roles
      });
    },
    onSuccess: () => {
      setMessage('User updated');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['user-audit', selectedId] });
    },
    onError: (err) => setMessage(err instanceof ApiError ? err.message : 'Update failed')
  });

  const toggleMutation = useMutation({
    mutationFn: async () => {
      if (!selected) throw new Error('No user selected');
      return selected.status === 'DISABLED'
        ? usersApi.enable(selected.id)
        : usersApi.disable(selected.id);
    },
    onSuccess: () => {
      setMessage('User status changed');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['user-audit', selectedId] });
    },
    onError: (err) => setMessage(err instanceof ApiError ? err.message : 'Status change failed')
  });

  function openUser(id: string) {
    const user = usersQuery.data?.content.find((u) => u.id === id);
    setSelectedId(id);
    if (user) {
      setEdit({
        email: user.email,
        displayName: user.displayName ?? '',
        status: user.status,
        roles: user.roles.length ? user.roles : [user.role]
      });
    }
  }

  const totalPages = usersQuery.data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Administration"
        title="Users and roles"
        description="Manage platform operators against live IAM — create, update, disable, and assign roles."
      />

      {message ? (
        <p className="rounded-xl border border-border bg-surface px-4 py-3 text-sm" role="status">
          {message}
        </p>
      ) : null}

      <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="space-y-4 rounded-2xl border border-border bg-surface p-6">
          <div className="flex flex-wrap gap-3">
            <input
              className="min-w-[12rem] flex-1 rounded-xl border border-border bg-background px-3 py-2 text-sm"
              placeholder="Search username, email, role…"
              value={q}
              onChange={(e) => {
                setPage(0);
                setQ(e.target.value);
              }}
            />
            <select
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value);
              }}
            >
              <option value="">All statuses</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="DISABLED">DISABLED</option>
              <option value="LOCKED">LOCKED</option>
            </select>
          </div>

          {usersQuery.isLoading ? (
            <p className="text-sm text-textSecondary">Loading users…</p>
          ) : usersQuery.isError ? (
            <p className="text-sm text-danger" role="alert">
              {usersQuery.error instanceof ApiError ? usersQuery.error.message : 'Failed to load users'}
            </p>
          ) : (
            <div className="space-y-2">
              {(usersQuery.data?.content ?? []).map((user) => (
                <button
                  key={user.id}
                  type="button"
                  onClick={() => openUser(user.id)}
                  className={`flex w-full items-center justify-between rounded-xl border px-4 py-3 text-left ${
                    selectedId === user.id ? 'border-primary bg-primary/5' : 'border-border bg-background'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="rounded-full bg-primary/10 p-2 text-primary">
                      <UserCog className="h-4 w-4" />
                    </div>
                    <div>
                      <p className="font-medium">{user.username}</p>
                      <p className="text-sm text-textSecondary">{user.email}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-textSecondary">{user.roles.join(', ')}</span>
                    <StatusBadge status={user.status} />
                  </div>
                </button>
              ))}
              {(usersQuery.data?.content.length ?? 0) === 0 ? (
                <p className="text-sm text-textSecondary">No users match filters.</p>
              ) : null}
            </div>
          )}

          <div className="flex items-center justify-between pt-2">
            <p className="text-xs text-textSecondary">
              {usersQuery.data?.totalElements ?? 0} users · page {page + 1}/{Math.max(totalPages, 1)}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                className="rounded-xl border border-border px-3 py-1.5 text-sm disabled:opacity-40"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </button>
              <button
                type="button"
                className="rounded-xl border border-border px-3 py-1.5 text-sm disabled:opacity-40"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </button>
            </div>
          </div>
        </div>

        <div className="space-y-4">
          {hasPermission('users:write') ? (
            <form
              className="space-y-3 rounded-2xl border border-border bg-surface p-6"
              onSubmit={(e) => {
                e.preventDefault();
                createMutation.mutate();
              }}
            >
              <div className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary" />
                <h3 className="font-semibold">Create user</h3>
              </div>
              <input
                required
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                placeholder="Username"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
              />
              <input
                required
                type="email"
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                placeholder="Email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
              <input
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                placeholder="Display name"
                value={form.displayName}
                onChange={(e) => setForm({ ...form, displayName: e.target.value })}
              />
              <input
                required
                type="password"
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                placeholder="Temporary password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />
              <select
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
              >
                {(rolesQuery.data ?? [{ code: 'SUPPORT', name: 'Support' }]).map((r) => (
                  <option key={r.code} value={r.code}>
                    {r.code} — {r.name}
                  </option>
                ))}
              </select>
              <button
                type="submit"
                className="w-full rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
                disabled={createMutation.isPending}
              >
                Create account
              </button>
            </form>
          ) : null}

          {selected ? (
            <div className="space-y-3 rounded-2xl border border-border bg-surface p-6">
              <h3 className="font-semibold">Edit {selected.username}</h3>
              <input
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                value={edit.email}
                onChange={(e) => setEdit({ ...edit, email: e.target.value })}
              />
              <input
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                value={edit.displayName}
                onChange={(e) => setEdit({ ...edit, displayName: e.target.value })}
              />
              <select
                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                value={edit.roles[0] ?? 'SUPPORT'}
                onChange={(e) => setEdit({ ...edit, roles: [e.target.value] })}
              >
                {(rolesQuery.data ?? []).map((r) => (
                  <option key={r.code} value={r.code}>
                    {r.code}
                  </option>
                ))}
              </select>
              {hasPermission('users:write') ? (
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
                    onClick={() => updateMutation.mutate()}
                  >
                    Save
                  </button>
                  <button
                    type="button"
                    className="rounded-xl border border-border px-4 py-2 text-sm"
                    onClick={() => toggleMutation.mutate()}
                  >
                    {selected.status === 'DISABLED' ? 'Enable' : 'Disable'}
                  </button>
                </div>
              ) : null}
              <div className="pt-2">
                <h4 className="text-sm font-semibold">Audit history</h4>
                {auditQuery.isLoading ? (
                  <p className="mt-2 text-xs text-textSecondary">Loading audit…</p>
                ) : (
                  <ul className="mt-2 max-h-40 space-y-1 overflow-y-auto text-xs text-textSecondary">
                    {auditEntries.map((a) => (
                      <li key={a.id}>
                        <span className="font-medium text-textPrimary">{a.action}</span>{' '}
                        · {new Date(a.createdAt).toLocaleString()}
                      </li>
                    ))}
                    {auditEntries.length === 0 ? <li>No audit events yet.</li> : null}
                  </ul>
                )}
              </div>
            </div>
          ) : null}

          {hasPermission('permissions:read') ? (
            <div className="rounded-2xl border border-border bg-surface p-6">
              <h3 className="font-semibold">Permission catalog</h3>
              <p className="mt-1 text-xs text-textSecondary">{permissionsQuery.data?.length ?? 0} permissions</p>
              <ul className="mt-3 max-h-48 space-y-1 overflow-y-auto text-xs text-textSecondary">
                {(permissionsQuery.data ?? []).slice(0, 40).map((p) => (
                  <li key={p.id}>
                    <span className="font-medium text-textPrimary">{p.code}</span> — {p.name}
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
