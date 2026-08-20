import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { climateIntelApi } from '../api/climateIntel';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClimateAlertsPage() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState('OPEN');

  const alertsQuery = useQuery({
    queryKey: ['climate-intel-alerts', status],
    queryFn: () => climateIntelApi.listAlerts({ status: status || undefined, page: 0, size: 50 }),
    enabled: hasPermission('climate-intel:read') || hasPermission('alerts:climate')
  });

  const ackMutation = useMutation({
    mutationFn: (id: string) => climateIntelApi.acknowledgeAlert(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['climate-intel-alerts'] })
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Climate intelligence</p>
          <h1 className="text-3xl font-semibold">Alerts inbox</h1>
        </div>
        <Link
          to="/climate-intel"
          className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface"
        >
          National dashboard
        </Link>
      </div>

      <label className="block max-w-xs text-sm">
        <span className="mb-1 block text-textSecondary">Status</span>
        <select
          className="w-full rounded-xl border border-border px-3 py-2"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="">All</option>
          <option value="OPEN">OPEN</option>
          <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
          <option value="RESOLVED">RESOLVED</option>
          <option value="EXPIRED">EXPIRED</option>
        </select>
      </label>

      {alertsQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {alertsQuery.error instanceof ApiError ? alertsQuery.error.message : 'Failed to load alerts'}
        </p>
      ) : null}

      <ul className="space-y-3">
        {(alertsQuery.data?.content ?? []).map((alert) => (
          <li key={alert.id} className="rounded-2xl border border-border bg-surface p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-medium">
                  {alert.alertNumber} · {alert.alertType}
                </p>
                <p className="mt-1 text-sm text-textSecondary">
                  {alert.severity} · {alert.scopeType} {alert.scopeId ?? ''} · {alert.status}
                </p>
                <p className="mt-1 text-sm text-textSecondary">
                  Valid from {new Date(alert.validFrom).toLocaleString()}
                </p>
              </div>
              {(hasPermission('climate-intel:write') || hasPermission('alerts:climate')) &&
              alert.status === 'OPEN' ? (
                <button
                  type="button"
                  className="rounded-xl border border-border px-3 py-2 text-sm hover:bg-background"
                  disabled={ackMutation.isPending}
                  onClick={() => ackMutation.mutate(alert.id)}
                >
                  Acknowledge
                </button>
              ) : null}
            </div>
          </li>
        ))}
        {!alertsQuery.isLoading && (alertsQuery.data?.content?.length ?? 0) === 0 ? (
          <li className="text-sm text-textSecondary">No alerts</li>
        ) : null}
      </ul>
    </div>
  );
}
