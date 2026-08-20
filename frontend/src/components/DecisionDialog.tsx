import { useMemo, useState } from 'react';
import type { DecisionType } from '../api/decisions';

type Props = {
  open: boolean;
  types: DecisionType[];
  busy?: boolean;
  onClose: () => void;
  onSubmit: (payload: { decisionTypeCode: string; comment?: string; targetUserId?: string }) => void;
};

export default function DecisionDialog({ open, types, busy, onClose, onSubmit }: Props) {
  const [code, setCode] = useState('');
  const [comment, setComment] = useState('');
  const [targetUserId, setTargetUserId] = useState('');

  const selected = useMemo(() => types.find((t) => t.code === code), [types, code]);

  if (!open) {
    return null;
  }

  const commentOk = !selected?.requiresComment || comment.trim().length > 0;
  const targetOk = !selected?.requiresTargetUser || targetUserId.trim().length > 0;
  const canSubmit = Boolean(code) && commentOk && targetOk && !busy;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-4 sm:items-center"
      role="dialog"
      aria-modal="true"
      aria-labelledby="decision-dialog-title"
    >
      <div className="w-full max-w-lg rounded-2xl border border-border bg-surface p-6 shadow-lg">
        <h2 id="decision-dialog-title" className="text-xl font-semibold">
          Record decision
        </h2>
        <p className="mt-1 text-sm text-textSecondary">
          Choose a configurable decision type. Routing follows the workflow definition.
        </p>

        <label className="mt-4 block text-sm">
          Decision type
          <select
            value={code}
            onChange={(e) => setCode(e.target.value)}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            aria-required="true"
          >
            <option value="">Select…</option>
            {types.map((t) => (
              <option key={t.code} value={t.code}>
                {t.name} ({t.code})
              </option>
            ))}
          </select>
        </label>

        {selected?.description ? (
          <p className="mt-2 text-xs text-textSecondary">{selected.description}</p>
        ) : null}

        <label className="mt-3 block text-sm">
          Comment{selected?.requiresComment ? ' (required)' : ''}
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            rows={3}
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            aria-required={selected?.requiresComment}
          />
        </label>

        {selected?.requiresTargetUser ? (
          <label className="mt-3 block text-sm">
            Target user id (required)
            <input
              value={targetUserId}
              onChange={(e) => setTargetUserId(e.target.value)}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 font-mono text-sm"
              placeholder="UUID"
              aria-required="true"
            />
          </label>
        ) : null}

        <div className="mt-6 flex flex-wrap justify-end gap-2">
          <button
            type="button"
            className="rounded-xl border border-border px-3 py-2 text-sm"
            onClick={onClose}
            disabled={busy}
          >
            Cancel
          </button>
          <button
            type="button"
            className="rounded-xl bg-primary px-3 py-2 text-sm text-white disabled:opacity-50"
            disabled={!canSubmit}
            onClick={() =>
              onSubmit({
                decisionTypeCode: code,
                comment: comment.trim() || undefined,
                targetUserId: targetUserId.trim() || undefined
              })
            }
          >
            {busy ? 'Submitting…' : 'Submit decision'}
          </button>
        </div>
      </div>
    </div>
  );
}
