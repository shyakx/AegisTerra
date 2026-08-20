import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { toast } from 'sonner';
import { agriApi } from '../api/agriculture';
import { insuranceApi, type PremiumQuote } from '../api/insurance';
import { ApiError } from '../api/client';

export default function PremiumCalculatorPage() {
  const [farmerId, setFarmerId] = useState('');
  const [farmId, setFarmId] = useState('');
  const [productId, setProductId] = useState('');
  const [packageId, setPackageId] = useState('');
  const [areaHa, setAreaHa] = useState('2.5');
  const [riskZoneCode, setRiskZoneCode] = useState('MEDIUM');
  const [quote, setQuote] = useState<PremiumQuote | null>(null);

  const productsQuery = useQuery({
    queryKey: ['insurance-products'],
    queryFn: () => insuranceApi.listProducts()
  });
  const packagesQuery = useQuery({
    queryKey: ['coverage-packages', productId],
    queryFn: () => insuranceApi.listPackages(productId),
    enabled: Boolean(productId)
  });
  const farmersQuery = useQuery({
    queryKey: ['farmers-calc'],
    queryFn: () => agriApi.searchFarmers({ page: 0, size: 20 })
  });
  const farmsQuery = useQuery({
    queryKey: ['farms-calc', farmerId],
    queryFn: () => agriApi.searchFarms({ farmerId, page: 0, size: 50 }),
    enabled: Boolean(farmerId)
  });

  const quoteMutation = useMutation({
    mutationFn: () =>
      insuranceApi.quote({
        productId,
        coveragePackageId: packageId,
        farmerId,
        farmId,
        areaHa: Number(areaHa),
        riskZoneCode
      }),
    onSuccess: (data) => {
      setQuote(data);
      toast.success('Quote calculated');
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Quote failed')
  });

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <p className="text-sm text-textSecondary">Insurance core</p>
        <h1 className="text-3xl font-semibold">Premium calculator</h1>
      </div>

      <section className="space-y-4 rounded-2xl border border-border bg-surface p-6">
        <label className="block text-sm">
          Farmer
          <select
            value={farmerId}
            onChange={(e) => {
              setFarmerId(e.target.value);
              setFarmId('');
            }}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
          >
            <option value="">Select farmer</option>
            {(farmersQuery.data?.content ?? []).map((f) => (
              <option key={f.id} value={f.id}>
                {f.firstName} {f.lastName}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm">
          Farm
          <select
            value={farmId}
            onChange={(e) => setFarmId(e.target.value)}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
          >
            <option value="">Select farm</option>
            {(farmsQuery.data?.content ?? []).map((f) => (
              <option key={f.id} value={f.id}>
                {f.farmName}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm">
          Product
          <select
            value={productId}
            onChange={(e) => {
              setProductId(e.target.value);
              setPackageId('');
            }}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
          >
            <option value="">Select product</option>
            {(productsQuery.data ?? []).map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm">
          Coverage package
          <select
            value={packageId}
            onChange={(e) => setPackageId(e.target.value)}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
          >
            <option value="">Select package</option>
            {(packagesQuery.data ?? []).map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </label>
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="block text-sm">
            Area (ha)
            <input
              value={areaHa}
              onChange={(e) => setAreaHa(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
          <label className="block text-sm">
            Risk zone
            <select
              value={riskZoneCode}
              onChange={(e) => setRiskZoneCode(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            >
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
            </select>
          </label>
        </div>
        <button
          type="button"
          className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
          disabled={!farmerId || !farmId || !productId || !packageId || quoteMutation.isPending}
          onClick={() => quoteMutation.mutate()}
        >
          {quoteMutation.isPending ? 'Calculating…' : 'Calculate'}
        </button>
      </section>

      {quote ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Quote result</h2>
          <p className="mt-2 text-sm">
            Net{' '}
            <strong>
              {Number(quote.netAmount).toLocaleString()} {quote.currency}
            </strong>{' '}
            · Coverage{' '}
            <strong>
              {Number(quote.coverageAmount).toLocaleString()} {quote.currency}
            </strong>
          </p>
          <p className="mt-1 text-xs text-textSecondary">Expires {new Date(quote.expiresAt).toLocaleString()}</p>
          <pre className="mt-4 max-h-64 overflow-auto rounded-xl bg-background p-3 text-xs">{quote.breakdownJson}</pre>
        </section>
      ) : null}
    </div>
  );
}
