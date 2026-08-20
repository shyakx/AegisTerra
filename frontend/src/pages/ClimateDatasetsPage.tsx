import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { climateApi } from '../api/climate';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClimateDatasetsPage() {
  const { hasPermission } = useAuth();

  const datasetsQuery = useQuery({
    queryKey: ['climate-datasets'],
    queryFn: () => climateApi.listDatasets({ page: 0, size: 50 }),
    enabled: hasPermission('climate:read')
  });

  const rastersQuery = useQuery({
    queryKey: ['climate-rasters'],
    queryFn: () => climateApi.listRasters({ page: 0, size: 20 }),
    enabled: hasPermission('climate:read')
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Climate data</p>
          <h1 className="text-3xl font-semibold">Dataset browser</h1>
        </div>
        <Link to="/climate" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          Dashboard
        </Link>
      </div>

      {datasetsQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {datasetsQuery.error instanceof ApiError ? datasetsQuery.error.message : 'Failed to load datasets'}
        </p>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Datasets</h2>
        <ul className="mt-4 divide-y divide-border">
          {(datasetsQuery.data?.content ?? []).map((d) => (
            <li key={d.id} className="flex flex-wrap items-center justify-between gap-2 py-3 text-sm">
              <div>
                <p className="font-medium">{d.name}</p>
                <p className="text-textSecondary">
                  {d.code} · {d.datasetType} · {d.providerCode ?? '—'}
                </p>
              </div>
              <span>{d.status}</span>
            </li>
          ))}
          {!datasetsQuery.isLoading && (datasetsQuery.data?.content?.length ?? 0) === 0 ? (
            <li className="py-3 text-sm text-textSecondary">No datasets published yet</li>
          ) : null}
        </ul>
      </section>

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Raster catalog</h2>
        <ul className="mt-4 divide-y divide-border">
          {(rastersQuery.data?.content ?? []).map((r) => (
            <li key={r.id} className="flex flex-wrap items-center justify-between gap-2 py-3 text-sm">
              <div>
                <p className="font-medium">{r.name}</p>
                <p className="text-textSecondary">
                  {r.code} · {r.variableCode ?? '—'} · {r.format ?? '—'}
                </p>
              </div>
              <span className="truncate text-textSecondary">{r.storageUri ?? '—'}</span>
            </li>
          ))}
          {!rastersQuery.isLoading && (rastersQuery.data?.content?.length ?? 0) === 0 ? (
            <li className="py-3 text-sm text-textSecondary">No raster layers cataloged</li>
          ) : null}
        </ul>
      </section>
    </div>
  );
}
