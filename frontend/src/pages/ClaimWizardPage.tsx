import { useMutation, useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { claimsApi } from '../api/claims';
import { insuranceApi } from '../api/insurance';
import { ApiError } from '../api/client';

export default function ClaimWizardPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [step, setStep] = useState(1);
  const [policyId, setPolicyId] = useState(params.get('policyId') ?? '');
  const [claimTypeCode, setClaimTypeCode] = useState('MANUAL');
  const [incidentDate, setIncidentDate] = useState(new Date().toISOString().slice(0, 10));
  const [description, setDescription] = useState('');
  const [causeOfLoss, setCauseOfLoss] = useState('');
  const [claimedAmount, setClaimedAmount] = useState('100000');
  const [claimId, setClaimId] = useState<string | null>(null);
  const [evidenceTitle, setEvidenceTitle] = useState('Claim form');
  const [evidenceUri, setEvidenceUri] = useState('file://claim-form.pdf');

  const typesQuery = useQuery({ queryKey: ['claim-types'], queryFn: () => claimsApi.listTypes() });
  const policiesQuery = useQuery({
    queryKey: ['policies-active-for-claims'],
    queryFn: () => insuranceApi.searchPolicies({ status: 'ACTIVE', page: 0, size: 50 })
  });

  const payload = useMemo(
    () => ({
      policyId,
      claimTypeCode,
      incidentDate,
      description,
      claimedAmount: Number(claimedAmount),
      causeOfLoss: causeOfLoss || undefined
    }),
    [policyId, claimTypeCode, incidentDate, description, claimedAmount, causeOfLoss]
  );

  const createIntake = useMutation({
    mutationFn: () => claimsApi.create(payload),
    onSuccess: (claim) => {
      setClaimId(claim.id);
      setStep(3);
      toast.success(`Draft ${claim.claimNumber} created`);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed to create claim')
  });

  const addEvidence = useMutation({
    mutationFn: () =>
      claimsApi.addEvidence(claimId!, {
        documentType: 'CLAIM_FORM',
        title: evidenceTitle,
        storageUri: evidenceUri,
        source: 'WIZARD'
      }),
    onSuccess: () => {
      setStep(4);
      toast.success('Evidence recorded');
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed to add evidence')
  });

  const submit = useMutation({
    mutationFn: () => claimsApi.submit(claimId!),
    onSuccess: (claim) => {
      toast.success('Claim submitted');
      navigate(`/claims/${claim.id}`);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Submit failed')
  });

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <p className="text-sm text-textSecondary">Claims intake</p>
        <h1 className="text-3xl font-semibold">New claim</h1>
        <p className="mt-1 text-sm text-textSecondary">Step {step} of 4 — policy, loss, evidence, submit</p>
      </div>

      {step === 1 ? (
        <section className="space-y-4 rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Policy</h2>
          <label className="block text-sm">
            Active policy
            <select
              value={policyId}
              onChange={(e) => setPolicyId(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            >
              <option value="">Select policy…</option>
              {(policiesQuery.data?.content ?? []).map((p) => (
                <option key={p.id} value={p.id}>
                  {p.policyNumber} ({p.currency} {p.coverageAmount.toLocaleString()})
                </option>
              ))}
            </select>
          </label>
          <label className="block text-sm">
            Claim type
            <select
              value={claimTypeCode}
              onChange={(e) => setClaimTypeCode(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            >
              {(typesQuery.data ?? []).map((t) => (
                <option key={t.code} value={t.code}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            disabled={!policyId}
            onClick={() => setStep(2)}
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
          >
            Continue
          </button>
        </section>
      ) : null}

      {step === 2 ? (
        <section className="space-y-4 rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Loss details</h2>
          <label className="block text-sm">
            Incident date
            <input
              type="date"
              value={incidentDate}
              onChange={(e) => setIncidentDate(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
          <label className="block text-sm">
            Cause of loss
            <input
              value={causeOfLoss}
              onChange={(e) => setCauseOfLoss(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              placeholder="Drought, flood, hail…"
            />
          </label>
          <label className="block text-sm">
            Description
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              rows={4}
            />
          </label>
          <label className="block text-sm">
            Claimed amount
            <input
              type="number"
              value={claimedAmount}
              onChange={(e) => setClaimedAmount(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
          <div className="flex gap-2">
            <button type="button" onClick={() => setStep(1)} className="rounded-xl border border-border px-4 py-2 text-sm">
              Back
            </button>
            <button
              type="button"
              disabled={description.trim().length < 5 || createIntake.isPending}
              onClick={() => createIntake.mutate()}
              className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              Save draft claim
            </button>
          </div>
        </section>
      ) : null}

      {step === 3 && claimId ? (
        <section className="space-y-4 rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Evidence</h2>
          <p className="text-sm text-textSecondary">
            Metadata only for v1 — attach required document types for this claim type (e.g. CLAIM_FORM).
          </p>
          <label className="block text-sm">
            Title
            <input
              value={evidenceTitle}
              onChange={(e) => setEvidenceTitle(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
          <label className="block text-sm">
            Storage URI
            <input
              value={evidenceUri}
              onChange={(e) => setEvidenceUri(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
          <button
            type="button"
            disabled={addEvidence.isPending}
            onClick={() => addEvidence.mutate()}
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
          >
            Add CLAIM_FORM evidence
          </button>
        </section>
      ) : null}

      {step === 4 && claimId ? (
        <section className="space-y-4 rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Review & submit</h2>
          <dl className="grid gap-2 text-sm md:grid-cols-2">
            <div>
              <dt className="text-textSecondary">Policy</dt>
              <dd className="font-medium">{policyId}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Type</dt>
              <dd className="font-medium">{claimTypeCode}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Amount</dt>
              <dd className="font-medium">{Number(claimedAmount).toLocaleString()}</dd>
            </div>
            <div>
              <dt className="text-textSecondary">Incident</dt>
              <dd className="font-medium">{incidentDate}</dd>
            </div>
          </dl>
          <div className="flex gap-2">
            <Link to={`/claims/${claimId}`} className="rounded-xl border border-border px-4 py-2 text-sm">
              Open draft
            </Link>
            <button
              type="button"
              disabled={submit.isPending}
              onClick={() => submit.mutate()}
              className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
            >
              Submit claim
            </button>
          </div>
        </section>
      ) : null}
    </div>
  );
}
