import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useState } from 'react';
import { toast } from 'sonner';
import { claimsApi } from '../api/claims';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClaimEvidencePage() {
  const { id = '' } = useParams();
  const { hasPermission } = useAuth();
  const qc = useQueryClient();
  const [documentType, setDocumentType] = useState('CLAIM_FORM');
  const [title, setTitle] = useState('');
  const [storageUri, setStorageUri] = useState('');

  const evidenceQuery = useQuery({
    queryKey: ['claim-evidence', id],
    queryFn: () => claimsApi.listEvidence(id),
    enabled: Boolean(id)
  });

  const add = useMutation({
    mutationFn: () =>
      claimsApi.addEvidence(id, {
        documentType,
        title: title || documentType,
        storageUri: storageUri || undefined,
        source: 'MANUAL'
      }),
    onSuccess: () => {
      toast.success('Evidence added');
      setTitle('');
      setStorageUri('');
      void qc.invalidateQueries({ queryKey: ['claim-evidence', id] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Failed to add evidence')
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Evidence package</p>
          <h1 className="text-3xl font-semibold">Claim evidence</h1>
        </div>
        <Link to={`/claims/${id}`} className="rounded-xl border border-border px-4 py-2 text-sm">
          Back to claim
        </Link>
      </div>

      {hasPermission('claims:write') ? (
        <section className="grid gap-3 rounded-2xl border border-border bg-surface p-4 md:grid-cols-4">
          <select
            value={documentType}
            onChange={(e) => setDocumentType(e.target.value)}
            className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
          >
            {['CLAIM_FORM', 'PHOTO', 'THIRD_PARTY', 'REPORT'].map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Title"
            className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
          />
          <input
            value={storageUri}
            onChange={(e) => setStorageUri(e.target.value)}
            placeholder="Storage URI"
            className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
          />
          <button
            type="button"
            onClick={() => add.mutate()}
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
          >
            Add evidence
          </button>
        </section>
      ) : null}

      <ul className="space-y-2">
        {(evidenceQuery.data ?? []).map((item) => (
          <li key={item.id} className="rounded-2xl border border-border bg-surface p-4">
            <p className="font-medium">
              {item.documentType} · {item.title}
            </p>
            <p className="text-sm text-textSecondary">{item.storageUri ?? 'No URI'} · {item.source}</p>
          </li>
        ))}
        {(evidenceQuery.data ?? []).length === 0 ? (
          <li className="text-sm text-textSecondary">No evidence yet.</li>
        ) : null}
      </ul>
    </div>
  );
}
