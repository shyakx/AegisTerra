import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { notificationsApi } from '../api/notifications';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function NotificationDetailPage() {
  const { id = '' } = useParams();
  const { hasPermission } = useAuth();
  const qc = useQueryClient();

  const query = useQuery({
    queryKey: ['notification', id],
    queryFn: () => notificationsApi.get(id),
    enabled: Boolean(id)
  });

  const markRead = useMutation({
    mutationFn: () => notificationsApi.markRead(id),
    onSuccess: async () => {
      toast.success('Marked as read');
      await qc.invalidateQueries({ queryKey: ['notification', id] });
      await qc.invalidateQueries({ queryKey: ['notifications'] });
      await qc.invalidateQueries({ queryKey: ['notifications-unread-count'] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed')
  });

  const n = query.data;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Link to="/notifications" className="text-sm text-primary hover:underline">
        ← Notification Center
      </Link>
      <h1 className="text-3xl font-semibold">{n?.title ?? 'Notification'}</h1>
      {query.isError ? (
        <p className="text-sm text-red-600">
          {query.error instanceof ApiError ? query.error.message : 'Failed to load'}
        </p>
      ) : null}
      {n ? (
        <article className="space-y-4 rounded-2xl border border-border bg-surface p-6">
          <p className="whitespace-pre-wrap text-sm">{n.body}</p>
          <dl className="grid gap-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-textSecondary">Event</dt>
              <dd className="font-medium">{n.eventType ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Channel</dt>
              <dd className="font-medium">{n.channel}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Subject</dt>
              <dd className="font-medium">
                {n.subjectType ?? '—'} {n.subjectId ? n.subjectId.slice(0, 8) + '…' : ''}
              </dd>
            </div>
            <div>
              <dt className="text-textSecondary">Received</dt>
              <dd className="font-medium">{new Date(n.createdAt).toLocaleString()}</dd>
            </div>
          </dl>
          {!n.readAt && hasPermission('notifications:write') ? (
            <button
              type="button"
              className="rounded-xl bg-primary px-3 py-2 text-sm text-white"
              onClick={() => markRead.mutate()}
            >
              Mark as read
            </button>
          ) : null}
        </article>
      ) : null}
    </div>
  );
}
