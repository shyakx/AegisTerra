import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { settlementsApi, type PaymentProvider } from '../api/settlements';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';

export default function PaymentProvidersPage() {
  const query = useQuery({
    queryKey: ['payment-providers'],
    queryFn: () => settlementsApi.listPaymentProviders()
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Financial settlement</p>
          <h1 className="text-3xl font-semibold">Payment providers</h1>
          <p className="mt-1 text-sm text-textSecondary">
            Registered disbursement channels for settlement payouts.
          </p>
        </div>
        <Link to="/settlements" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          Settlements
        </Link>
      </div>

      <EnterpriseTable<PaymentProvider>
        title="Provider registry"
        subtitle="Configured channels for bank transfer, mobile money, and treasury payouts"
        rows={query.data ?? []}
        loading={query.isLoading}
        error={
          query.error instanceof ApiError
            ? query.error.message
            : query.error
              ? 'Failed to load payment providers'
              : null
        }
        emptyMessage="No payment providers configured."
        getRowKey={(row) => row.id}
        columns={[
          {
            key: 'code',
            header: 'Code',
            sortValue: (r) => r.providerCode,
            render: (r) => <span className="font-medium">{r.providerCode}</span>
          },
          {
            key: 'name',
            header: 'Display name',
            sortValue: (r) => r.displayName,
            render: (r) => r.displayName
          },
          {
            key: 'method',
            header: 'Method',
            sortValue: (r) => r.paymentMethod,
            render: (r) => r.paymentMethod
          },
          {
            key: 'enabled',
            header: 'Enabled',
            sortValue: (r) => (r.enabled ? 1 : 0),
            render: (r) => (
              <span className="rounded-lg bg-background px-2 py-1 text-xs font-medium">
                {r.enabled ? 'Yes' : 'No'}
              </span>
            )
          },
          {
            key: 'status',
            header: 'Status',
            sortValue: (r) => r.status,
            render: (r) => r.status
          }
        ]}
      />

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Manual provider</h2>
        <p className="mt-2 text-sm text-textSecondary">
          Finance operators confirm outbound payments with an external reference on the settlement
          detail page. Bank transfer is the active disbursement channel; additional providers can be
          enabled from this registry.
        </p>
        {(query.data ?? [])
          .filter((p) => p.providerCode === 'MANUAL')
          .map((p) => (
            <div key={p.id} className="mt-4 rounded-xl border border-border px-4 py-3 text-sm">
              <p className="font-medium">{p.displayName}</p>
              <p className="text-textSecondary">
                {p.enabled ? 'Enabled' : 'Disabled'} · {p.paymentMethod}
              </p>
              {p.configJson ? (
                <pre className="mt-2 overflow-x-auto rounded-lg bg-background p-3 text-xs">{p.configJson}</pre>
              ) : null}
            </div>
          ))}
      </section>
    </div>
  );
}
