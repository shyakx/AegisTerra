import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useState } from 'react';
import { toast } from 'sonner';
import { claimsApi } from '../api/claims';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClaimAssessmentPage() {
  const { id = '' } = useParams();
  const { hasPermission } = useAuth();
  const qc = useQueryClient();
  const [recommendedAmount, setRecommendedAmount] = useState('');
  const [notes, setNotes] = useState('');
  const [methods, setMethods] = useState('["DOCUMENT_REVIEW","FIELD_INSPECTION"]');

  const claimQuery = useQuery({
    queryKey: ['claim', id],
    queryFn: () => claimsApi.get(id),
    enabled: Boolean(id)
  });
  const listQuery = useQuery({
    queryKey: ['claim-assessments', id],
    queryFn: () => claimsApi.listAssessments(id),
    enabled: Boolean(id)
  });

  const create = useMutation({
    mutationFn: () =>
      claimsApi.createAssessment(id, {
        methodsJson: methods,
        recommendedAmount: Number(recommendedAmount),
        notes,
        accepted: true
      }),
    onSuccess: () => {
      toast.success('Assessment recorded');
      void qc.invalidateQueries({ queryKey: ['claim-assessments', id] });
      void qc.invalidateQueries({ queryKey: ['claim', id] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Assessment failed')
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Loss quantification</p>
          <h1 className="text-3xl font-semibold">Assessment</h1>
          {claimQuery.data ? (
            <p className="text-sm text-textSecondary">
              Claimed {claimQuery.data.claimedAmount.toLocaleString()} {claimQuery.data.currency}
            </p>
          ) : null}
        </div>
        <Link to={`/claims/${id}`} className="rounded-xl border border-border px-4 py-2 text-sm">
          Back to claim
        </Link>
      </div>

      {hasPermission('claims:assess') ? (
        <section className="space-y-3 rounded-2xl border border-border bg-surface p-4">
          <label className="block text-sm">
            Methods JSON
            <input
              value={methods}
              onChange={(e) => setMethods(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
          <label className="block text-sm">
            Recommended amount
            <input
              type="number"
              value={recommendedAmount}
              onChange={(e) => setRecommendedAmount(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
          <label className="block text-sm">
            Notes
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              rows={3}
            />
          </label>
          <button
            type="button"
            disabled={!recommendedAmount || create.isPending}
            onClick={() => create.mutate()}
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
          >
            Record assessment
          </button>
        </section>
      ) : null}

      <ul className="space-y-2">
        {(listQuery.data ?? []).map((a) => (
          <li key={a.id} className="rounded-2xl border border-border bg-surface p-4 text-sm">
            <p className="font-medium">
              {a.recommendedAmount.toLocaleString()} {a.currency}
              {a.accepted ? ' · accepted' : ''}
            </p>
            <p className="text-textSecondary">{a.methodsJson}</p>
            {a.notes ? <p>{a.notes}</p> : null}
          </li>
        ))}
      </ul>
    </div>
  );
}
