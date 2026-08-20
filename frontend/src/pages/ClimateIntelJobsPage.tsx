import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { climateIntelApi, type ClimateIntelJob } from '../api/climateIntel';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClimateIntelJobsPage() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const [farmId, setFarmId] = useState('');
  const [districtCode, setDistrictCode] = useState('');
  const [lastJob, setLastJob] = useState<ClimateIntelJob | null>(null);

  const recalcMutation = useMutation({
    mutationFn: () =>
      climateIntelApi.recalculate({
        farmId: farmId || undefined,
        districtCode: districtCode || undefined
      }),
    onSuccess: (job) => {
      setLastJob(job);
      queryClient.invalidateQueries({ queryKey: ['climate-intel-national'] });
    }
  });

  if (!hasPermission('climate-intel:admin') && !hasPermission('climate-intel:write')) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-semibold">Recalculation jobs</h1>
        <p className="text-sm text-textSecondary">Admin or write permission required.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Climate intelligence</p>
          <h1 className="text-3xl font-semibold">Recalculation jobs</h1>
          <p className="mt-1 text-sm text-textSecondary">
            On-demand deterministic risk recalculation from Climate Data only.
          </p>
        </div>
        <Link to="/climate-intel" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          National dashboard
        </Link>
      </div>

      <form
        className="space-y-3 rounded-2xl border border-border bg-surface p-6"
        onSubmit={(e) => {
          e.preventDefault();
          recalcMutation.mutate();
        }}
      >
        <label className="block max-w-lg text-sm">
          <span className="mb-1 block text-textSecondary">Farm ID (optional)</span>
          <input
            className="w-full rounded-xl border border-border px-3 py-2"
            value={farmId}
            onChange={(e) => setFarmId(e.target.value)}
            placeholder="UUID"
          />
        </label>
        <label className="block max-w-lg text-sm">
          <span className="mb-1 block text-textSecondary">District code (optional)</span>
          <input
            className="w-full rounded-xl border border-border px-3 py-2"
            value={districtCode}
            onChange={(e) => setDistrictCode(e.target.value)}
          />
        </label>
        {recalcMutation.isError ? (
          <p className="text-sm text-danger" role="alert">
            {recalcMutation.error instanceof ApiError ? recalcMutation.error.message : 'Job failed'}
          </p>
        ) : null}
        <button
          type="submit"
          disabled={recalcMutation.isPending}
          className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90"
        >
          {recalcMutation.isPending ? 'Running…' : 'Start recalculation'}
        </button>
      </form>

      {lastJob ? (
        <section className="rounded-2xl border border-border bg-surface p-6 text-sm">
          <h2 className="text-lg font-semibold">Last job</h2>
          <p className="mt-2">{lastJob.jobNumber}</p>
          <p className="text-textSecondary">
            {lastJob.status} · subjects {lastJob.subjectsProcessed}
            {lastJob.errorSummary ? ` · ${lastJob.errorSummary}` : ''}
          </p>
          {farmId ? (
            <Link className="mt-3 inline-block text-primary" to={`/climate-intel/farms/${farmId}`}>
              Open farm intelligence
            </Link>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
