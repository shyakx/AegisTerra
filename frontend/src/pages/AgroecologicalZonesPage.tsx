import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { geographyApi } from '../api/geography';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { PageHeader } from '../components/PageHeader';
import { AEZ_FARMER_REGISTRY } from '../demo/aezFarmerRegistry';

const ZONE_OPTIONS = ['ZONE A', 'ZONE B', 'ZONE C', 'ZONE D', 'ZONE E'] as const;

export default function AgroecologicalZonesPage() {
  const { hasPermission } = useAuth();
  const enabled = hasPermission('farmers:read');
  const [zone, setZone] = useState<string>('');
  const [subzoneCode, setSubzoneCode] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const pageSize = 25;

  const zonesQuery = useQuery({
    queryKey: ['aez-zones'],
    queryFn: () => geographyApi.listAgroecologicalZones(),
    enabled
  });
  const subzonesQuery = useQuery({
    queryKey: ['aez-subzones'],
    queryFn: () => geographyApi.listAgroecologicalSubzones(),
    enabled
  });

  const subzoneOptions = useMemo(() => {
    const map = new Map<string, string>();
    for (const row of AEZ_FARMER_REGISTRY) {
      if (zone && row.mainZone !== zone) continue;
      map.set(row.subzoneCode, row.subzone);
    }
    return [...map.entries()].sort((a, b) => a[0].localeCompare(b[0]));
  }, [zone]);

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    return AEZ_FARMER_REGISTRY.filter((row) => {
      if (zone && row.mainZone !== zone) return false;
      if (subzoneCode && row.subzoneCode !== subzoneCode) return false;
      if (!needle) return true;
      return (
        row.farmerId.toLowerCase().includes(needle) ||
        row.farmerName.toLowerCase().includes(needle) ||
        row.district.toLowerCase().includes(needle) ||
        row.sector.toLowerCase().includes(needle) ||
        row.cell.toLowerCase().includes(needle) ||
        row.crop.toLowerCase().includes(needle) ||
        row.phoneNumber.toLowerCase().includes(needle)
      );
    });
  }, [zone, subzoneCode, q]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const pageRows = filtered.slice(page * pageSize, page * pageSize + pageSize);

  const summary = useMemo(() => {
    const byZone = new Map<string, number>();
    for (const row of AEZ_FARMER_REGISTRY) {
      byZone.set(row.mainZone, (byZone.get(row.mainZone) ?? 0) + 1);
    }
    return byZone;
  }, []);

  if (!enabled) {
    return <p className="text-sm text-textSecondary">You do not have access to agroecological zones.</p>;
  }

  const geoError =
    zonesQuery.error instanceof ApiError
      ? zonesQuery.error.message
      : subzonesQuery.error instanceof ApiError
        ? subzonesQuery.error.message
        : null;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Geography"
        title="Agro-ecological zones"
        description="Owner registry of farmers by main zone and sub-zone, with district, sector, cell, crop, and field size."
      />

      <section className="grid gap-3 sm:grid-cols-5">
        {ZONE_OPTIONS.map((z) => (
          <button
            key={z}
            type="button"
            onClick={() => {
              setZone((prev) => (prev === z ? '' : z));
              setSubzoneCode('');
              setPage(0);
            }}
            className={`rounded-2xl border px-3 py-3 text-left text-sm ${
              zone === z ? 'border-primary bg-primary/5' : 'border-border bg-surface'
            }`}
          >
            <p className="font-semibold">{z}</p>
            <p className="mt-1 text-textSecondary">{summary.get(z) ?? 0} farmers</p>
          </button>
        ))}
      </section>

      {geoError ? (
        <p className="text-xs text-textSecondary">Catalog API note: {geoError}</p>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-4">
        <div className="flex flex-wrap items-end gap-3">
          <label className="text-sm">
            Sub-zone
            <select
              className="mt-1 block min-w-[220px] rounded-xl border border-border bg-background px-3 py-2"
              value={subzoneCode}
              onChange={(e) => {
                setSubzoneCode(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All sub-zones</option>
              {subzoneOptions.map(([code, label]) => (
                <option key={code} value={code}>
                  {code} — {label}
                </option>
              ))}
            </select>
          </label>
          <label className="min-w-[200px] flex-1 text-sm">
            Search
            <input
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              placeholder="Farmer ID, name, district, crop…"
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(0);
              }}
            />
          </label>
          <p className="pb-2 text-sm text-textSecondary">
            Showing {pageRows.length} of {filtered.length} (registry {AEZ_FARMER_REGISTRY.length})
          </p>
        </div>

        <div className="mt-4 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="text-textSecondary">
              <tr>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">Farmer ID</th>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">Main Zone</th>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">Sub-zone</th>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">Farmer Name</th>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">Phone</th>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">Crop</th>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">Field (ha)</th>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">District</th>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">Sector</th>
                <th className="whitespace-nowrap py-2 pr-3 font-medium">Cell</th>
                <th className="whitespace-nowrap py-2 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {pageRows.map((row) => (
                <tr key={row.farmerId} className="border-t border-border">
                  <td className="py-2 pr-3 font-medium">{row.farmerId}</td>
                  <td className="py-2 pr-3">{row.mainZone}</td>
                  <td className="max-w-[220px] py-2 pr-3">{row.subzone}</td>
                  <td className="py-2 pr-3">{row.farmerName}</td>
                  <td className="whitespace-nowrap py-2 pr-3">{row.phoneNumber}</td>
                  <td className="py-2 pr-3">{row.crop}</td>
                  <td className="py-2 pr-3">{row.fieldSizeHa.toFixed(2)}</td>
                  <td className="py-2 pr-3">{row.district}</td>
                  <td className="py-2 pr-3">{row.sector}</td>
                  <td className="py-2 pr-3">{row.cell}</td>
                  <td className="py-2">{row.registrationStatus}</td>
                </tr>
              ))}
              {pageRows.length === 0 ? (
                <tr>
                  <td colSpan={11} className="py-6 text-center text-textSecondary">
                    No farmers match these filters.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>

        <div className="mt-4 flex items-center justify-between gap-3">
          <button
            type="button"
            className="rounded-xl border border-border px-3 py-1.5 text-sm disabled:opacity-40"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Previous
          </button>
          <p className="text-sm text-textSecondary">
            Page {page + 1} / {totalPages}
          </p>
          <button
            type="button"
            className="rounded-xl border border-border px-3 py-1.5 text-sm disabled:opacity-40"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      </section>
    </div>
  );
}
