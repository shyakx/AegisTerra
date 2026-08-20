import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useState } from 'react';
import { toast } from 'sonner';
import { claimsApi } from '../api/claims';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function ClaimInspectionPage() {
  const { id = '' } = useParams();
  const { hasPermission } = useAuth();
  const qc = useQueryClient();
  const [notes, setNotes] = useState('');
  const [findingsJson, setFindingsJson] = useState('{"severity":"visible"}');

  const listQuery = useQuery({
    queryKey: ['claim-inspections', id],
    queryFn: () => claimsApi.listInspections(id),
    enabled: Boolean(id)
  });

  const create = useMutation({
    mutationFn: (complete: boolean) =>
      claimsApi.createInspection(id, {
        notes,
        findingsJson,
        complete
      }),
    onSuccess: () => {
      toast.success('Inspection saved');
      void qc.invalidateQueries({ queryKey: ['claim-inspections', id] });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Inspection failed')
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Field / remote inspection</p>
          <h1 className="text-3xl font-semibold">Inspection</h1>
        </div>
        <Link to={`/claims/${id}`} className="rounded-xl border border-border px-4 py-2 text-sm">
          Back to claim
        </Link>
      </div>

      {hasPermission('claims:assess') ? (
        <section className="space-y-3 rounded-2xl border border-border bg-surface p-4">
          <label className="block text-sm">
            Findings JSON
            <textarea
              value={findingsJson}
              onChange={(e) => setFindingsJson(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              rows={3}
            />
          </label>
          <label className="block text-sm">
            Notes
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              rows={2}
            />
          </label>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => create.mutate(false)}
              className="rounded-xl border border-border px-4 py-2 text-sm"
            >
              Schedule
            </button>
            <button
              type="button"
              onClick={() => create.mutate(true)}
              className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
            >
              Complete inspection
            </button>
          </div>
        </section>
      ) : null}

      <ul className="space-y-2">
        {(listQuery.data ?? []).map((item) => (
          <li key={item.id} className="rounded-2xl border border-border bg-surface p-4 text-sm">
            <p className="font-medium">{item.status}</p>
            <p className="text-textSecondary">{item.notes ?? 'No notes'}</p>
            {item.findingsJson ? <pre className="mt-2 overflow-auto text-xs">{item.findingsJson}</pre> : null}
          </li>
        ))}
      </ul>
    </div>
  );
}
