import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { climateApi } from '../api/climate';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClimateStationsPage() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const [q, setQ] = useState('');
  const [form, setForm] = useState({
    code: '',
    name: '',
    longitude: '30.1',
    latitude: '-1.95',
    districtCode: '',
    providerCode: 'MANUAL'
  });

  const stationsQuery = useQuery({
    queryKey: ['climate-stations', q],
    queryFn: () => climateApi.listStations({ q: q || undefined, page: 0, size: 50 }),
    enabled: hasPermission('climate:read')
  });

  const createMutation = useMutation({
    mutationFn: () =>
      climateApi.createStation({
        code: form.code.trim(),
        name: form.name.trim(),
        longitude: Number(form.longitude),
        latitude: Number(form.latitude),
        districtCode: form.districtCode || undefined,
        providerCode: form.providerCode || undefined
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['climate-stations'] });
      setForm((current) => ({ ...current, code: '', name: '' }));
    }
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Climate data</p>
          <h1 className="text-3xl font-semibold">Weather stations</h1>
        </div>
        <Link to="/climate" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          Dashboard
        </Link>
      </div>

      <div className="flex gap-2">
        <input
          className="w-full max-w-md rounded-xl border border-border bg-surface px-3 py-2 text-sm"
          placeholder="Search code or name"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      {stationsQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {stationsQuery.error instanceof ApiError ? stationsQuery.error.message : 'Failed to load stations'}
        </p>
      ) : null}

      <div className="overflow-x-auto rounded-2xl border border-border bg-surface">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border text-textSecondary">
            <tr>
              <th className="px-4 py-3 font-medium">Code</th>
              <th className="px-4 py-3 font-medium">Name</th>
              <th className="px-4 py-3 font-medium">District</th>
              <th className="px-4 py-3 font-medium">Provider</th>
              <th className="px-4 py-3 font-medium">Status</th>
            </tr>
          </thead>
          <tbody>
            {(stationsQuery.data?.content ?? []).map((s) => (
              <tr key={s.id} className="border-b border-border/60">
                <td className="px-4 py-3">
                  <Link className="font-medium text-primary" to={`/climate/stations/${s.id}`}>
                    {s.code}
                  </Link>
                </td>
                <td className="px-4 py-3">{s.name}</td>
                <td className="px-4 py-3">{s.districtCode ?? '—'}</td>
                <td className="px-4 py-3">{s.providerCode ?? '—'}</td>
                <td className="px-4 py-3">{s.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {hasPermission('climate:write') ? (
        <form
          className="grid gap-3 rounded-2xl border border-border bg-surface p-6 md:grid-cols-3"
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
        >
          <h2 className="md:col-span-3 text-lg font-semibold">Register station</h2>
          {(['code', 'name', 'longitude', 'latitude', 'districtCode', 'providerCode'] as const).map((field) => (
            <label key={field} className="text-sm">
              <span className="mb-1 block text-textSecondary">{field}</span>
              <input
                className="w-full rounded-xl border border-border px-3 py-2"
                value={form[field]}
                onChange={(e) => setForm((c) => ({ ...c, [field]: e.target.value }))}
                required={field === 'code' || field === 'name' || field === 'longitude' || field === 'latitude'}
              />
            </label>
          ))}
          {createMutation.isError ? (
            <p className="md:col-span-3 text-sm text-danger" role="alert">
              {createMutation.error instanceof ApiError ? createMutation.error.message : 'Create failed'}
            </p>
          ) : null}
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90 md:col-span-3 md:w-fit"
          >
            {createMutation.isPending ? 'Saving…' : 'Create station'}
          </button>
        </form>
      ) : null}
    </div>
  );
}
