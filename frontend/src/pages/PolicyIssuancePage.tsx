import { useMutation, useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { agriApi } from '../api/agriculture';
import { insuranceApi, type PremiumQuote } from '../api/insurance';
import { ApiError } from '../api/client';

const STEPS = ['Farmer & farm', 'Product', 'Coverage', 'Premium', 'Review'];

type FormState = {
  farmerId: string;
  farmId: string;
  productId: string;
  policyTypeId: string;
  coveragePackageId: string;
  cropId: string;
  seasonId: string;
  areaHa: string;
  riskZoneCode: string;
  startDate: string;
  endDate: string;
  quote: PremiumQuote | null;
};

function todayPlus(days: number) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}

export default function PolicyIssuancePage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [farmerQ, setFarmerQ] = useState('');
  const [form, setForm] = useState<FormState>({
    farmerId: '',
    farmId: '',
    productId: '',
    policyTypeId: '',
    coveragePackageId: '',
    cropId: '',
    seasonId: '',
    areaHa: '',
    riskZoneCode: 'MEDIUM',
    startDate: todayPlus(0),
    endDate: todayPlus(180),
    quote: null
  });

  const farmersQuery = useQuery({
    queryKey: ['farmers-pick', farmerQ],
    queryFn: () => agriApi.searchFarmers({ q: farmerQ, page: 0, size: 10 }),
    enabled: step === 0
  });
  const farmsQuery = useQuery({
    queryKey: ['farms-pick', form.farmerId],
    queryFn: () => agriApi.searchFarms({ farmerId: form.farmerId, page: 0, size: 50 }),
    enabled: Boolean(form.farmerId)
  });
  const productsQuery = useQuery({
    queryKey: ['insurance-products'],
    queryFn: () => insuranceApi.listProducts()
  });
  const typesQuery = useQuery({
    queryKey: ['policy-types'],
    queryFn: () => insuranceApi.listPolicyTypes()
  });
  const packagesQuery = useQuery({
    queryKey: ['coverage-packages', form.productId],
    queryFn: () => insuranceApi.listPackages(form.productId),
    enabled: Boolean(form.productId)
  });
  const cropsQuery = useQuery({ queryKey: ['crops'], queryFn: () => agriApi.listCrops() });
  const seasonsQuery = useQuery({ queryKey: ['seasons'], queryFn: () => agriApi.listSeasons() });

  const selectedFarm = useMemo(
    () => farmsQuery.data?.content.find((f) => f.id === form.farmId),
    [farmsQuery.data, form.farmId]
  );

  const quoteMutation = useMutation({
    mutationFn: () =>
      insuranceApi.quote({
        productId: form.productId,
        coveragePackageId: form.coveragePackageId,
        farmerId: form.farmerId,
        farmId: form.farmId,
        cropId: form.cropId || null,
        seasonId: form.seasonId || null,
        areaHa: form.areaHa ? Number(form.areaHa) : selectedFarm?.farmSizeHa ?? null,
        riskZoneCode: form.riskZoneCode
      }),
    onSuccess: (quote) => {
      setForm((f) => ({ ...f, quote }));
      toast.success('Premium quoted');
      setStep(4);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Quote failed')
  });

  const submitMutation = useMutation({
    mutationFn: () => {
      if (!form.quote) throw new Error('Quote required');
      return insuranceApi.submitPolicy({
        farmerId: form.farmerId,
        farmId: form.farmId,
        productId: form.productId,
        policyTypeId: form.policyTypeId,
        coveragePackageId: form.coveragePackageId,
        premiumQuoteId: form.quote.id,
        cropId: form.cropId || null,
        seasonId: form.seasonId || null,
        startDate: form.startDate,
        endDate: form.endDate,
        reason: 'issuance-wizard'
      });
    },
    onSuccess: (policy) => {
      toast.success('Policy submitted for underwriting');
      navigate(`/policies/${policy.id}`);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Submit failed')
  });

  const canNext =
    (step === 0 && form.farmerId && form.farmId) ||
    (step === 1 && form.productId && form.policyTypeId) ||
    (step === 2 && form.coveragePackageId) ||
    step === 3 ||
    step === 4;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <p className="text-sm text-textSecondary">Insurance core</p>
        <h1 className="text-3xl font-semibold">Policy issuance</h1>
      </div>

      <ol className="flex flex-wrap gap-2">
        {STEPS.map((label, index) => (
          <li
            key={label}
            className={`rounded-full px-3 py-1 text-sm ${
              index === step ? 'bg-primary text-white' : index < step ? 'bg-primary/15 text-primary' : 'bg-surface text-textSecondary'
            }`}
          >
            {index + 1}. {label}
          </li>
        ))}
      </ol>

      <section className="rounded-2xl border border-border bg-surface p-6">
        {step === 0 ? (
          <div className="space-y-4">
            <label className="block text-sm">
              Search farmer
              <input
                value={farmerQ}
                onChange={(e) => setFarmerQ(e.target.value)}
                className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                placeholder="Name, national ID, phone…"
              />
            </label>
            <div className="max-h-48 space-y-2 overflow-auto">
              {(farmersQuery.data?.content ?? []).map((f) => (
                <button
                  key={f.id}
                  type="button"
                  onClick={() => setForm((s) => ({ ...s, farmerId: f.id, farmId: '' }))}
                  className={`block w-full rounded-xl border px-3 py-2 text-left text-sm ${
                    form.farmerId === f.id ? 'border-primary bg-primary/5' : 'border-border'
                  }`}
                >
                  {f.firstName} {f.lastName} · {f.nationalId}
                </button>
              ))}
            </div>
            {form.farmerId ? (
              <label className="block text-sm">
                Farm
                <select
                  value={form.farmId}
                  onChange={(e) => setForm((s) => ({ ...s, farmId: e.target.value }))}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                >
                  <option value="">Select farm</option>
                  {(farmsQuery.data?.content ?? []).map((farm) => (
                    <option key={farm.id} value={farm.id}>
                      {farm.farmName} ({farm.farmSizeHa ?? '—'} ha)
                    </option>
                  ))}
                </select>
              </label>
            ) : null}
          </div>
        ) : null}

        {step === 1 ? (
          <div className="space-y-4">
            <label className="block text-sm">
              Product
              <select
                value={form.productId}
                onChange={(e) =>
                  setForm((s) => ({
                    ...s,
                    productId: e.target.value,
                    policyTypeId: '',
                    coveragePackageId: '',
                    quote: null
                  }))
                }
                className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              >
                <option value="">Select product</option>
                {(productsQuery.data ?? []).map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.code} — {p.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-sm">
              Policy type
              <select
                value={form.policyTypeId}
                onChange={(e) => setForm((s) => ({ ...s, policyTypeId: e.target.value }))}
                className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              >
                <option value="">Select type</option>
                {(typesQuery.data ?? [])
                  .filter((t) => !form.productId || t.productId === form.productId)
                  .map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.code} — {t.name}
                    </option>
                  ))}
              </select>
            </label>
          </div>
        ) : null}

        {step === 2 ? (
          <div className="space-y-4">
            <label className="block text-sm">
              Coverage package
              <select
                value={form.coveragePackageId}
                onChange={(e) => setForm((s) => ({ ...s, coveragePackageId: e.target.value, quote: null }))}
                className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              >
                <option value="">Select package</option>
                {(packagesQuery.data ?? []).map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.code} — {p.name}
                    {p.coverageLevelPct != null ? ` (${p.coverageLevelPct}%)` : ''}
                  </option>
                ))}
              </select>
            </label>
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block text-sm">
                Crop (optional)
                <select
                  value={form.cropId}
                  onChange={(e) => setForm((s) => ({ ...s, cropId: e.target.value }))}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                >
                  <option value="">None</option>
                  {(cropsQuery.data ?? []).map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.code} — {c.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-sm">
                Season (optional)
                <select
                  value={form.seasonId}
                  onChange={(e) => setForm((s) => ({ ...s, seasonId: e.target.value }))}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                >
                  <option value="">None</option>
                  {(seasonsQuery.data ?? []).map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.code} — {s.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </div>
        ) : null}

        {step === 3 ? (
          <div className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block text-sm">
                Area (ha)
                <input
                  value={form.areaHa}
                  onChange={(e) => setForm((s) => ({ ...s, areaHa: e.target.value }))}
                  placeholder={selectedFarm?.farmSizeHa != null ? String(selectedFarm.farmSizeHa) : undefined}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                />
              </label>
              <label className="block text-sm">
                Risk zone
                <select
                  value={form.riskZoneCode}
                  onChange={(e) => setForm((s) => ({ ...s, riskZoneCode: e.target.value }))}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                >
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                </select>
              </label>
              <label className="block text-sm">
                Start date
                <input
                  type="date"
                  value={form.startDate}
                  onChange={(e) => setForm((s) => ({ ...s, startDate: e.target.value }))}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                />
              </label>
              <label className="block text-sm">
                End date
                <input
                  type="date"
                  value={form.endDate}
                  onChange={(e) => setForm((s) => ({ ...s, endDate: e.target.value }))}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                />
              </label>
            </div>
            <button
              type="button"
              className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
              disabled={quoteMutation.isPending}
              onClick={() => quoteMutation.mutate()}
            >
              {quoteMutation.isPending ? 'Calculating…' : 'Calculate premium'}
            </button>
          </div>
        ) : null}

        {step === 4 ? (
          <div className="space-y-4 text-sm">
            {form.quote ? (
              <>
                <p>
                  Net premium:{' '}
                  <strong>
                    {Number(form.quote.netAmount).toLocaleString()} {form.quote.currency}
                  </strong>
                </p>
                <p>
                  Coverage:{' '}
                  <strong>
                    {Number(form.quote.coverageAmount).toLocaleString()} {form.quote.currency}
                  </strong>
                </p>
                <pre className="max-h-40 overflow-auto rounded-xl bg-background p-3 text-xs">
                  {form.quote.breakdownJson}
                </pre>
                <button
                  type="button"
                  className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
                  disabled={submitMutation.isPending}
                  onClick={() => submitMutation.mutate()}
                >
                  {submitMutation.isPending ? 'Submitting…' : 'Submit for underwriting'}
                </button>
              </>
            ) : (
              <p className="text-textSecondary">Calculate a premium quote before submitting.</p>
            )}
          </div>
        ) : null}
      </section>

      <div className="flex justify-between">
        <button
          type="button"
          className="rounded-xl border border-border px-4 py-2 text-sm"
          disabled={step === 0}
          onClick={() => setStep((s) => Math.max(0, s - 1))}
        >
          Back
        </button>
        {step < 3 ? (
          <button
            type="button"
            className="rounded-xl bg-primary px-4 py-2 text-sm text-white disabled:opacity-40"
            disabled={!canNext}
            onClick={() => setStep((s) => s + 1)}
          >
            Next
          </button>
        ) : null}
      </div>
    </div>
  );
}
