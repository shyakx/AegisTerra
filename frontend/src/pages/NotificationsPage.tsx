import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { notificationsApi } from '../api/notifications';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function NotificationsPage() {
  const { hasPermission } = useAuth();
  const qc = useQueryClient();
  const query = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationsApi.list({ page: 0, size: 50 }),
    enabled: hasPermission('notifications:read')
  });

  const markRead = useMutation({
    mutationFn: (id: string) => notificationsApi.markRead(id),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['notifications'] });
      await qc.invalidateQueries({ queryKey: ['notifications-unread-count'] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed to mark read')
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl font-semibold">Notification Center</h1>
          <p className="text-sm text-textSecondary">In-app messages from the Communication Engine</p>
        </div>
        <Link to="/notification-preferences" className="text-sm text-primary hover:underline">
          Preferences
        </Link>
      </div>

      {query.isError ? (
        <p className="text-sm text-red-600">
          {query.error instanceof ApiError ? query.error.message : 'Failed to load notifications'}
        </p>
      ) : null}

      <ul className="space-y-3">
        {(query.data?.content ?? []).map((n) => {
          const unread = !n.readAt;
          return (
            <li
              key={n.id}
              className={`rounded-2xl border border-border bg-surface p-4 ${unread ? 'ring-1 ring-primary/30' : ''}`}
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <Link to={`/notifications/${n.id}`} className="text-lg font-semibold hover:underline">
                    {n.title}
                  </Link>
                  <p className="mt-1 text-sm text-textSecondary">{n.body}</p>
                  <p className="mt-2 text-xs text-textSecondary">
                    {n.eventType ?? 'Event'} · {new Date(n.createdAt).toLocaleString()}
                    {unread ? ' · Unread' : ''}
                  </p>
                </div>
                {unread && hasPermission('notifications:write') ? (
                  <button
                    type="button"
                    className="rounded-xl border border-border px-3 py-2 text-sm"
                    onClick={() => markRead.mutate(n.id)}
                  >
                    Mark read
                  </button>
                ) : null}
              </div>
            </li>
          );
        })}
      </ul>

      {(query.data?.content.length ?? 0) === 0 && !query.isLoading ? (
        <p className="text-sm text-textSecondary">No notifications yet.</p>
      ) : null}
    </div>
  );
}
