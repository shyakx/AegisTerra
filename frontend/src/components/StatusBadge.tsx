const STATUS_TONES: Record<string, string> = {
  ACTIVE: 'bg-emerald-50 text-success',
  OPEN: 'bg-amber-50 text-warning',
  PENDING: 'bg-amber-50 text-warning',
  UNDER_REVIEW: 'bg-sky-50 text-info',
  APPROVED: 'bg-emerald-50 text-success',
  COMPLETED: 'bg-emerald-50 text-success',
  CLOSED: 'bg-emerald-50 text-success',
  SUCCEEDED: 'bg-emerald-50 text-success',
  FAILED: 'bg-red-50 text-danger',
  REJECTED: 'bg-red-50 text-danger',
  CANCELLED: 'bg-slate-100 text-textSecondary',
  DRAFT: 'bg-slate-100 text-textSecondary',
  CRITICAL: 'bg-red-50 text-danger',
  WARNING: 'bg-amber-50 text-warning',
  WATCH: 'bg-sky-50 text-info',
  HIGH: 'bg-amber-50 text-warning',
  EXTREME: 'bg-red-50 text-danger',
  MODERATE: 'bg-sky-50 text-info',
  LOW: 'bg-emerald-50 text-success',
  ACKNOWLEDGED: 'bg-sky-50 text-info',
  RESOLVED: 'bg-emerald-50 text-success',
  PROCESSING: 'bg-sky-50 text-info',
  PARTIAL: 'bg-amber-50 text-warning',
  ENABLED: 'bg-emerald-50 text-success',
  STUB: 'bg-slate-100 text-textSecondary'
};

export function StatusBadge({ status }: { status: string | null | undefined }) {
  if (!status) {
    return <span className="text-sm text-textSecondary">—</span>;
  }
  const tone = STATUS_TONES[status.toUpperCase()] ?? 'bg-slate-100 text-textSecondary';
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${tone}`}>{status}</span>
  );
}
