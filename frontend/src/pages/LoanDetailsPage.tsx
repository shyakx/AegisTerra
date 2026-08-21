import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { type ReactNode } from 'react';
import { lendingApi } from '../api/lending';
import { ApiError } from '../api/client';
import { StatusBadge } from '../components/StatusBadge';

export default function LoanDetailsPage() {
  const { id = '' } = useParams();
  const query = useQuery({
    queryKey: ['loan', id],
    queryFn: () => lendingApi.get(id),
    enabled: Boolean(id)
  });
  const loan = query.data;

  if (query.isError) {
    return (
      <p className="text-sm text-danger" role="alert">
        {query.error instanceof ApiError ? query.error.message : 'Failed to load loan'}
      </p>
    );
  }

  if (!loan) {
    return <p className="text-sm text-textSecondary">Loading loan…</p>;
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-textSecondary">Agricultural lending</p>
        <h1 className="text-3xl font-semibold">{loan.loanNumber}</h1>
        <p className="mt-1 text-sm text-textSecondary">
          Climate risk assessment informs approval, monitoring, and whether the embedded policy stays active for the
          loan period.
        </p>
      </div>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Field label="Status" value={<StatusBadge status={loan.status} />} />
        <Field label="Repayment risk" value={<StatusBadge status={loan.repaymentRisk} />} />
        <Field label="Crop condition" value={<StatusBadge status={loan.cropCondition} />} />
        <Field label="Insurance embed" value={`${loan.insuranceEmbedPct.toFixed(1)}%`} />
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Borrower</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <Row label="Name" value={loan.borrowerName} />
            <Row
              label="Farmer"
              value={
                <Link className="text-primary" to={`/farmers/${loan.farmerId}`}>
                  {loan.farmerCode}
                </Link>
              }
            />
            <Row
              label="Farm"
              value={
                <Link className="text-primary" to={`/farms/${loan.farmId}`}>
                  Open farm
                </Link>
              }
            />
            <Row label="District" value={loan.districtId} />
            <Row label="Lender" value={loan.lenderName} />
          </dl>
        </div>
        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Credit & cover</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <Row label="Purpose" value={loan.purpose} />
            <Row label="Principal" value={`${loan.principal.toLocaleString()} ${loan.currency}`} />
            <Row
              label="Linked policy"
              value={
                loan.policyId ? (
                  <Link className="text-primary" to={`/policies/${loan.policyId}`}>
                    Open policy
                  </Link>
                ) : (
                  'Not yet issued'
                )
              }
            />
            <Row
              label="Climate intelligence"
              value={
                <Link className="text-primary" to={`/climate-intel/farms/${loan.farmId}`}>
                  Farm risk profile
                </Link>
              }
            />
            <Row label="Maturity" value={new Date(loan.maturityAt).toLocaleDateString()} />
          </dl>
        </div>
      </section>
    </div>
  );
}

function Field({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-4">
      <p className="text-xs uppercase tracking-wide text-textSecondary">{label}</p>
      <div className="mt-2 text-sm font-medium">{value}</div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-textSecondary">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}
