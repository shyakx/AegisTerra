import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { agriApi } from '../api/agriculture';
import { ApiError } from '../api/client';
import { geographyApi } from '../api/geography';
import { useAuth } from '../auth/AuthContext';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export default function FarmDetailsPage() {
  const { hasPermission } = useAuth();
  const { id = '' } = useParams();
  const farmQuery = useQuery({ queryKey: ['farm', id], queryFn: () => agriApi.getFarm(id), enabled: Boolean(id) });
  const plotsQuery = useQuery({ queryKey: ['plots', id], queryFn: () => agriApi.listPlots(id), enabled: Boolean(id) });
  const boundariesQuery = useQuery({
    queryKey: ['boundaries', id],
    queryFn: () => agriApi.listBoundaries(id),
    enabled: Boolean(id)
  });
  const catalogDistrictId = farmQuery.data?.districtId && UUID_RE.test(farmQuery.data.districtId)
    ? farmQuery.data.districtId
    : null;
  const districtQuery = useQuery({
    queryKey: ['district', catalogDistrictId],
    queryFn: () => geographyApi.getDistrict(catalogDistrictId!),
    enabled: Boolean(catalogDistrictId)
  });

  if (farmQuery.isLoading) return <p>Loading farm…</p>;
  if (farmQuery.error) {
    return <p role="alert">{farmQuery.error instanceof ApiError ? farmQuery.error.message : 'Failed to load farm'}</p>;
  }
  const farm = farmQuery.data!;
  const activeBoundaries = boundariesQuery.data?.filter((b) => b.status === 'ACTIVE').length ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Farm details</p>
          <h1 className="text-3xl font-semibold">{farm.farmName}</h1>
          <p className="text-textSecondary">
            {farm.farmCode} · {farm.status}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to={`/farms/${id}/plots`} className="rounded-xl border border-border px-3 py-2 text-sm">
            Plots
          </Link>
          <Link to={`/farms/${id}/crop-history`} className="rounded-xl border border-border px-3 py-2 text-sm">
            Crop history
          </Link>
          {hasPermission('climate-intel:read') ? (
            <>
              <Link
                to={`/climate-intel/farms/${id}`}
                className="rounded-xl border border-border px-3 py-2 text-sm"
              >
                Climate risk
              </Link>
              <Link to="/planning" className="rounded-xl bg-primary px-3 py-2 text-sm text-white">
                Planning outlook
              </Link>
            </>
          ) : null}
        </div>
      </div>

      <section className="grid gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-border bg-surface p-4">
          <h2 className="font-semibold">Summary</h2>
          <dl className="mt-3 space-y-2 text-sm">
            <div>
              <dt className="text-textSecondary">Size (ha)</dt>
              <dd>{farm.farmSizeHa ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Crop type</dt>
              <dd>{farm.cropType ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Province</dt>
              <dd>{districtQuery.data?.provinceName ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">District</dt>
              <dd>{districtQuery.data?.name ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Agroecological Zone</dt>
              <dd>{districtQuery.data?.agroecologicalZoneName ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Agroecological Sub-zone</dt>
              <dd>
                {districtQuery.data?.agroecologicalSubzoneCode
                  ? `${districtQuery.data.agroecologicalSubzoneCode} — ${districtQuery.data.agroecologicalSubzoneName ?? ''}`.trim()
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-textSecondary">Active boundaries</dt>
              <dd>{activeBoundaries}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Plots</dt>
              <dd>{plotsQuery.data?.length ?? 0}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Farmer</dt>
              <dd>
                <Link to={`/farmers/${farm.farmerId}`} className="text-primary hover:underline">
                  Open farmer profile
                </Link>
              </dd>
            </div>
          </dl>
        </div>
        <div className="rounded-2xl border border-border bg-surface p-4">
          <h2 className="font-semibold">Location context</h2>
          <p className="mt-2 text-sm text-textSecondary">
            Place is driven by province, district, and agroecological zone — not a live map desk. Planning and
            climate risk use that registry context.
          </p>
          <dl className="mt-4 space-y-2 text-sm">
            <div>
              <dt className="text-textSecondary">Boundary on file</dt>
              <dd>{activeBoundaries > 0 ? 'Yes' : 'Not recorded yet'}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">District code</dt>
              <dd>{districtQuery.data?.code ?? '—'}</dd>
            </div>
          </dl>
        </div>
      </section>
    </div>
  );
}
