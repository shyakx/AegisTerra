import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { agriApi } from '../api/agriculture';
import { ApiError } from '../api/client';
import { geographyApi } from '../api/geography';

type Payload = {
  household?: { code?: string; headName?: string; id?: string; skip?: boolean };
  farmer: {
    firstName: string;
    lastName: string;
    nationalId: string;
    phoneNumber: string;
    email?: string;
  };
  identityVerified: boolean;
  farm: { farmName: string; farmSizeHa?: number; farmCode?: string; districtId?: string };
  boundary: { geoJson: string | null; skip?: boolean };
  plots: Array<{ plotCode: string; name?: string }>;
  cropSeasons: Array<{ cropId: string; seasonId: string; plantedAreaHa?: number }>;
};

const STEPS = [
  'Household',
  'Primary farmer',
  'Identity',
  'Farm',
  'Boundary',
  'Plots',
  'Crops',
  'Review'
];

const emptyPayload = (): Payload => ({
  household: { skip: true },
  farmer: { firstName: '', lastName: '', nationalId: '', phoneNumber: '', email: '' },
  identityVerified: false,
  farm: { farmName: '', farmSizeHa: undefined },
  boundary: { geoJson: null, skip: true },
  plots: [],
  cropSeasons: []
});

export default function FarmerRegistrationPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const draftIdParam = params.get('draftId');
  const [draftId, setDraftId] = useState<string | null>(draftIdParam);
  const [step, setStep] = useState(1);
  const [payload, setPayload] = useState<Payload>(emptyPayload);
  const [provinceId, setProvinceId] = useState('');

  const cropsQuery = useQuery({ queryKey: ['crops'], queryFn: () => agriApi.listCrops() });
  const seasonsQuery = useQuery({ queryKey: ['seasons'], queryFn: () => agriApi.listSeasons() });
  const provincesQuery = useQuery({ queryKey: ['provinces'], queryFn: () => geographyApi.listProvinces() });
  const districtsQuery = useQuery({
    queryKey: ['districts', provinceId],
    queryFn: () => geographyApi.listDistricts(provinceId),
    enabled: Boolean(provinceId)
  });
  const districtQuery = useQuery({
    queryKey: ['district', payload.farm.districtId],
    queryFn: () => geographyApi.getDistrict(payload.farm.districtId!),
    enabled: Boolean(payload.farm.districtId)
  });

  useEffect(() => {
    if (!draftIdParam) return;
    void agriApi.getDraft(draftIdParam).then((draft) => {
      setDraftId(draft.id);
      setStep(draft.currentStep);
      setPayload(JSON.parse(draft.payloadJson) as Payload);
    });
  }, [draftIdParam]);

  useEffect(() => {
    const derivedProvinceId = districtQuery.data?.provinceId;
    if (derivedProvinceId && !provinceId) {
      setProvinceId(derivedProvinceId);
    }
  }, [districtQuery.data, provinceId]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = { currentStep: step, payloadJson: JSON.stringify(payload) };
      if (draftId) return agriApi.updateDraft(draftId, body);
      return agriApi.createDraft(body);
    },
    onSuccess: (draft) => {
      setDraftId(draft.id);
      toast.success('Draft saved');
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed to save draft')
  });

  const submitMutation = useMutation({
    mutationFn: async () => {
      let id = draftId;
      if (!id) {
        const created = await agriApi.createDraft({ currentStep: 8, payloadJson: JSON.stringify(payload) });
        id = created.id;
        setDraftId(id);
      } else {
        await agriApi.updateDraft(id, { currentStep: 8, payloadJson: JSON.stringify(payload) });
      }
      return agriApi.submitDraft(id);
    },
    onSuccess: (result) => {
      toast.success('Registration submitted');
      navigate(`/farmers/${result.farmerId}`);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Submit failed')
  });

  const canNext = useMemo(() => {
    if (step === 2) {
      const f = payload.farmer;
      return Boolean(f.firstName && f.lastName && f.nationalId && f.phoneNumber);
    }
    if (step === 3) return payload.identityVerified;
    if (step === 4) return Boolean(payload.farm.farmName && payload.farm.districtId);
    if (step === 5) return true;
    return true;
  }, [step, payload]);

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-textSecondary">Guided onboarding</p>
        <h1 className="text-3xl font-semibold">Farmer registration</h1>
      </div>

      <ol className="grid gap-2 md:grid-cols-4" aria-label="Registration steps">
        {STEPS.map((label, index) => {
          const n = index + 1;
          const active = n === step;
          return (
            <li
              key={label}
              className={`rounded-xl border px-3 py-2 text-sm ${active ? 'border-primary bg-primary/5 font-medium' : 'border-border'}`}
            >
              {n}. {label}
            </li>
          );
        })}
      </ol>

      <section className="rounded-2xl border border-border bg-surface p-4 md:p-6">
        {step === 1 && (
          <div className="space-y-3">
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={Boolean(payload.household?.skip)}
                onChange={(e) =>
                  setPayload((p) => ({ ...p, household: { ...p.household, skip: e.target.checked } }))
                }
              />
              Skip household (single-farmer)
            </label>
            {!payload.household?.skip ? (
              <div className="grid gap-3 md:grid-cols-2">
                <label className="text-sm">
                  Household code
                  <input
                    className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                    value={payload.household?.code ?? ''}
                    onChange={(e) =>
                      setPayload((p) => ({ ...p, household: { ...p.household, skip: false, code: e.target.value } }))
                    }
                  />
                </label>
                <label className="text-sm">
                  Head name
                  <input
                    className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                    value={payload.household?.headName ?? ''}
                    onChange={(e) =>
                      setPayload((p) => ({
                        ...p,
                        household: { ...p.household, skip: false, headName: e.target.value }
                      }))
                    }
                  />
                </label>
              </div>
            ) : null}
          </div>
        )}

        {step === 2 && (
          <div className="grid gap-3 md:grid-cols-2">
            {(
              [
                ['firstName', 'First name'],
                ['lastName', 'Last name'],
                ['nationalId', 'National ID'],
                ['phoneNumber', 'Phone'],
                ['email', 'Email']
              ] as const
            ).map(([key, label]) => (
              <label key={key} className="text-sm">
                {label}
                <input
                  required={key !== 'email'}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                  value={payload.farmer[key] ?? ''}
                  onChange={(e) =>
                    setPayload((p) => ({ ...p, farmer: { ...p.farmer, [key]: e.target.value } }))
                  }
                />
              </label>
            ))}
          </div>
        )}

        {step === 3 && (
          <label className="flex items-start gap-3 text-sm">
            <input
              type="checkbox"
              checked={payload.identityVerified}
              onChange={(e) => setPayload((p) => ({ ...p, identityVerified: e.target.checked }))}
            />
            <span>
              I confirm national ID <strong>{payload.farmer.nationalId || '—'}</strong> was verified against available
              records (manual / field verification for Phase 4).
            </span>
          </label>
        )}

        {step === 4 && (
          <div className="space-y-4">
            <div className="grid gap-3 md:grid-cols-2">
              <label className="text-sm">
                Farm name
                <input
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                  value={payload.farm.farmName}
                  onChange={(e) => setPayload((p) => ({ ...p, farm: { ...p.farm, farmName: e.target.value } }))}
                />
              </label>
              <label className="text-sm">
                Estimated size (ha)
                <input
                  type="number"
                  min={0}
                  step="0.01"
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                  value={payload.farm.farmSizeHa ?? ''}
                  onChange={(e) =>
                    setPayload((p) => ({
                      ...p,
                      farm: { ...p.farm, farmSizeHa: e.target.value ? Number(e.target.value) : undefined }
                    }))
                  }
                />
              </label>
              <label className="text-sm">
                Province
                <select
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                  value={provinceId}
                  onChange={(e) => {
                    const nextProvinceId = e.target.value;
                    setProvinceId(nextProvinceId);
                    setPayload((p) => ({ ...p, farm: { ...p.farm, districtId: undefined } }));
                  }}
                >
                  <option value="">Select province</option>
                  {(provincesQuery.data ?? []).map((province) => (
                    <option key={province.id} value={province.id}>
                      {province.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="text-sm">
                District
                <select
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 disabled:opacity-40"
                  value={payload.farm.districtId ?? ''}
                  disabled={!provinceId}
                  onChange={(e) =>
                    setPayload((p) => ({
                      ...p,
                      farm: { ...p.farm, districtId: e.target.value || undefined }
                    }))
                  }
                >
                  <option value="">Select district</option>
                  {(districtsQuery.data ?? []).map((district) => (
                    <option key={district.id} value={district.id}>
                      {district.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            {payload.farm.districtId ? (
              <div className="grid gap-3 rounded-xl border border-border bg-background p-3 md:grid-cols-2">
                <label className="text-sm">
                  Agroecological Zone
                  <input
                    readOnly
                    className="mt-1 w-full rounded-xl border border-border bg-surface px-3 py-2 text-textSecondary"
                    value={districtQuery.data?.agroecologicalZoneName ?? '—'}
                  />
                </label>
                <label className="text-sm">
                  Agroecological Sub-zone
                  <input
                    readOnly
                    className="mt-1 w-full rounded-xl border border-border bg-surface px-3 py-2 text-textSecondary"
                    value={
                      districtQuery.data?.agroecologicalSubzoneCode
                        ? `${districtQuery.data.agroecologicalSubzoneCode} — ${districtQuery.data.agroecologicalSubzoneName ?? ''}`.trim()
                        : '—'
                    }
                  />
                </label>
              </div>
            ) : (
              <p className="text-sm text-textSecondary">Select a province and district to derive agroecological classification.</p>
            )}
          </div>
        )}

        {step === 5 && (
          <div className="space-y-3">
            <p className="text-sm text-textSecondary">
              Live map / polygon drawing is frozen with the GIS desk. District and agroecological zone already place
              the farm for climate and yield planning.
            </p>
            <p className="text-sm font-medium text-textPrimary">Boundary polygon: skipped</p>
          </div>
        )}

        {step === 6 && (
          <div className="space-y-3">
            <button
              type="button"
              className="rounded-xl border border-border px-3 py-2 text-sm"
              onClick={() =>
                setPayload((p) => ({
                  ...p,
                  plots: [...p.plots, { plotCode: `P${p.plots.length + 1}`, name: '' }]
                }))
              }
            >
              Add plot
            </button>
            {payload.plots.length === 0 ? <p className="text-sm text-textSecondary">Optional — add zero or more plots.</p> : null}
            {payload.plots.map((plot, index) => (
              <div key={index} className="grid gap-3 md:grid-cols-2">
                <label className="text-sm">
                  Plot code
                  <input
                    className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                    value={plot.plotCode}
                    onChange={(e) =>
                      setPayload((p) => {
                        const plots = [...p.plots];
                        plots[index] = { ...plots[index], plotCode: e.target.value };
                        return { ...p, plots };
                      })
                    }
                  />
                </label>
                <label className="text-sm">
                  Name
                  <input
                    className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                    value={plot.name ?? ''}
                    onChange={(e) =>
                      setPayload((p) => {
                        const plots = [...p.plots];
                        plots[index] = { ...plots[index], name: e.target.value };
                        return { ...p, plots };
                      })
                    }
                  />
                </label>
              </div>
            ))}
          </div>
        )}

        {step === 7 && (
          <div className="space-y-3">
            {(cropsQuery.isError || seasonsQuery.isError) && (
              <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
                Could not load crops/seasons. Check you are logged in and the API is running.
              </p>
            )}
            {!cropsQuery.isLoading && (cropsQuery.data?.length ?? 0) === 0 && (
              <p className="text-sm text-textSecondary">
                No crops available. Create some under <strong>Crops</strong> first.
              </p>
            )}
            {!seasonsQuery.isLoading && (seasonsQuery.data?.length ?? 0) === 0 && (
              <p className="text-sm text-textSecondary">
                No seasons available. Create some under <strong>Seasons</strong> first (or restart the backend after
                the season seed migration).
              </p>
            )}
            <button
              type="button"
              className="rounded-xl border border-border px-3 py-2 text-sm disabled:opacity-40"
              disabled={(cropsQuery.data?.length ?? 0) === 0 || (seasonsQuery.data?.length ?? 0) === 0}
              onClick={() =>
                setPayload((p) => ({
                  ...p,
                  cropSeasons: [
                    ...p.cropSeasons,
                    {
                      cropId: cropsQuery.data?.[0]?.id ?? '',
                      seasonId: seasonsQuery.data?.[0]?.id ?? ''
                    }
                  ]
                }))
              }
            >
              Add crop season
            </button>
            {payload.cropSeasons.map((cs, index) => (
              <div key={index} className="grid gap-3 md:grid-cols-3">
                <label className="text-sm">
                  Crop
                  <select
                    className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                    value={cs.cropId}
                    onChange={(e) =>
                      setPayload((p) => {
                        const cropSeasons = [...p.cropSeasons];
                        cropSeasons[index] = { ...cropSeasons[index], cropId: e.target.value };
                        return { ...p, cropSeasons };
                      })
                    }
                  >
                    <option value="">Select crop</option>
                    {(cropsQuery.data ?? []).map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-sm">
                  Season
                  <select
                    className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                    value={cs.seasonId}
                    onChange={(e) =>
                      setPayload((p) => {
                        const cropSeasons = [...p.cropSeasons];
                        cropSeasons[index] = { ...cropSeasons[index], seasonId: e.target.value };
                        return { ...p, cropSeasons };
                      })
                    }
                  >
                    <option value="">Select season</option>
                    {(seasonsQuery.data ?? []).map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-sm">
                  Planted area (ha)
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                    value={cs.plantedAreaHa ?? ''}
                    onChange={(e) =>
                      setPayload((p) => {
                        const cropSeasons = [...p.cropSeasons];
                        cropSeasons[index] = {
                          ...cropSeasons[index],
                          plantedAreaHa: e.target.value ? Number(e.target.value) : undefined
                        };
                        return { ...p, cropSeasons };
                      })
                    }
                  />
                </label>
              </div>
            ))}
          </div>
        )}

        {step === 8 && (
          <div className="space-y-2 text-sm">
            <p>
              <strong>Farmer:</strong> {payload.farmer.firstName} {payload.farmer.lastName} ({payload.farmer.nationalId})
            </p>
            <p>
              <strong>Farm:</strong> {payload.farm.farmName}
            </p>
            <p>
              <strong>Province:</strong> {districtQuery.data?.provinceName ?? '—'}
            </p>
            <p>
              <strong>District:</strong> {districtQuery.data?.name ?? '—'}
            </p>
            <p>
              <strong>Agroecological Zone:</strong> {districtQuery.data?.agroecologicalZoneName ?? '—'}
            </p>
            <p>
              <strong>Agroecological Sub-zone:</strong>{' '}
              {districtQuery.data?.agroecologicalSubzoneCode
                ? `${districtQuery.data.agroecologicalSubzoneCode} — ${districtQuery.data.agroecologicalSubzoneName ?? ''}`.trim()
                : '—'}
            </p>
            <p>
              <strong>Boundary:</strong> Skipped
            </p>
            <p>
              <strong>Plots:</strong> {payload.plots.length}
            </p>
            <p>
              <strong>Crop seasons:</strong> {payload.cropSeasons.length}
            </p>
          </div>
        )}
      </section>

      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          disabled={step === 1}
          onClick={() => setStep((s) => Math.max(1, s - 1))}
          className="rounded-xl border border-border px-4 py-2 text-sm disabled:opacity-40"
        >
          Back
        </button>
        <button
          type="button"
          onClick={() => saveMutation.mutate()}
          className="rounded-xl border border-border px-4 py-2 text-sm"
        >
          Save draft
        </button>
        {step < 8 ? (
          <button
            type="button"
            disabled={!canNext}
            onClick={() => setStep((s) => Math.min(8, s + 1))}
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
          >
            Continue
          </button>
        ) : (
          <button
            type="button"
            disabled={!canNext || submitMutation.isPending}
            onClick={() => submitMutation.mutate()}
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
          >
            Submit registration
          </button>
        )}
      </div>
    </div>
  );
}
