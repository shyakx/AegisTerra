import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { notificationsApi, type NotificationPreference } from '../api/notifications';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const DEFAULTS: NotificationPreference[] = [
  { channel: 'IN_APP', eventType: 'TaskCreated', enabled: true },
  { channel: 'IN_APP', eventType: 'TaskAssigned', enabled: true },
  { channel: 'IN_APP', eventType: 'DecisionRecorded', enabled: true },
  { channel: 'IN_APP', eventType: 'WorkflowCompleted', enabled: true },
  { channel: 'EMAIL', eventType: 'TaskAssigned', enabled: false }
];

export default function NotificationPreferencesPage() {
  const { hasPermission } = useAuth();
  const canWrite = hasPermission('notifications:write');
  const qc = useQueryClient();
  const [rows, setRows] = useState<NotificationPreference[]>(DEFAULTS);

  const query = useQuery({
    queryKey: ['notification-preferences'],
    queryFn: () => notificationsApi.preferences()
  });

  useEffect(() => {
    if (query.data && query.data.length > 0) {
      setRows(query.data);
    }
  }, [query.data]);

  const save = useMutation({
    mutationFn: () => notificationsApi.updatePreferences(rows),
    onSuccess: async (data) => {
      setRows(data);
      toast.success('Preferences saved');
      await qc.invalidateQueries({ queryKey: ['notification-preferences'] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Save failed')
  });

  function toggle(index: number) {
    if (!canWrite) return;
    setRows((prev) => prev.map((row, i) => (i === index ? { ...row, enabled: !row.enabled } : row)));
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <Link to="/notifications" className="text-sm text-primary hover:underline">
          ← Notification Center
        </Link>
        <h1 className="mt-2 text-3xl font-semibold">Notification preferences</h1>
        <p className="text-sm text-textSecondary">Control which event types reach each channel</p>
      </div>

      <ul className="space-y-3">
        {rows.map((row, index) => (
          <li
            key={`${row.channel}-${row.eventType}`}
            className="flex items-center justify-between rounded-2xl border border-border bg-surface px-4 py-3"
          >
            <div>
              <p className="font-medium">{row.eventType}</p>
              <p className="text-xs text-textSecondary">{row.channel}</p>
            </div>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={row.enabled}
                onChange={() => toggle(index)}
                disabled={!canWrite}
                aria-label={`Enable ${row.eventType} on ${row.channel}`}
              />
              Enabled
            </label>
          </li>
        ))}
      </ul>

      {canWrite ? (
        <button
          type="button"
          className="rounded-xl bg-primary px-4 py-2 text-sm text-white"
          onClick={() => save.mutate()}
          disabled={save.isPending}
        >
          Save preferences
        </button>
      ) : (
        <p className="text-sm text-textSecondary">Your role can view preferences but cannot change them.</p>
      )}
    </div>
  );
}
