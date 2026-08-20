import { useQuery } from '@tanstack/react-query';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useState } from 'react';
import { settlementsApi, type LedgerEntry, type LedgerTransaction } from '../api/settlements';
import { EnterpriseTable } from '../components/EnterpriseTable';
import { ApiError } from '../api/client';

export default function LedgerViewerPage() {
  const { id: routeId } = useParams();
  const [searchParams] = useSearchParams();
  const detailId = routeId || searchParams.get('id') || '';
  const [q, setQ] = useState('');
  const [entryType, setEntryType] = useState('');
  const [settlementId, setSettlementId] = useState(searchParams.get('settlementId') ?? '');
  const [page, setPage] = useState(0);

  const listQuery = useQuery({
    queryKey: ['ledger', q, entryType, settlementId, page],
    queryFn: () =>
      settlementsApi.listLedger({
        q: q || undefined,
        entryType: entryType || undefined,
        settlementId: settlementId || undefined,
        page,
        size: 20,
        sort: 'postedAt,desc'
      }),
    enabled: !detailId
  });

  const detailQuery = useQuery({
    queryKey: ['ledger-detail', detailId],
    queryFn: () => settlementsApi.getLedger(detailId),
    enabled: Boolean(detailId)
  });

  if (detailId) {
    return (
      <div className="space-y-6">
        <div>
          <Link to="/ledger" className="text-sm text-primary hover:underline">
            ← Ledger
          </Link>
          <h1 className="mt-2 text-3xl font-semibold">Ledger entry</h1>
          <p className="text-sm text-textSecondary">{detailId}</p>
        </div>

        {detailQuery.isLoading ? (
          <p className="text-sm text-textSecondary">Loading…</p>
        ) : detailQuery.isError ? (
          <p className="text-sm text-danger" role="alert">
            {detailQuery.error instanceof ApiError ? detailQuery.error.message : 'Failed to load ledger entry'}
          </p>
        ) : detailQuery.data ? (
          <LedgerDetail data={detailQuery.data} />
        ) : (
          <p className="text-sm text-danger">Not found</p>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Financial settlement</p>
          <h1 className="text-3xl font-semibold">Ledger</h1>
          <p className="mt-1 text-sm text-textSecondary">Immutable debit / credit postings for settlements.</p>
        </div>
        <Link to="/settlements" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          Settlements
        </Link>
      </div>

      <EnterpriseTable<LedgerEntry>
        title="Ledger entries"
        subtitle="Append-only accounting lines — amounts are never updated in place"
        rows={listQuery.data?.content ?? []}
        loading={listQuery.isLoading}
        error={
          listQuery.error instanceof ApiError
            ? listQuery.error.message
            : listQuery.error
              ? 'Failed to load ledger'
              : null
        }
        emptyMessage="No ledger entries yet."
        searchValue={q}
        onSearchChange={(value) => {
          setPage(0);
          setQ(value);
        }}
        searchPlaceholder="Account or narration…"
        filters={
          <div className="flex flex-wrap gap-2">
            <select
              value={entryType}
              onChange={(e) => {
                setPage(0);
                setEntryType(e.target.value);
              }}
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
              aria-label="Entry type"
            >
              <option value="">All types</option>
              {['DEBIT', 'CREDIT', 'ADJUSTMENT', 'REVERSAL', 'REFUND'].map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            <input
              value={settlementId}
              onChange={(e) => {
                setPage(0);
                setSettlementId(e.target.value.trim());
              }}
              placeholder="Settlement ID"
              className="rounded-xl border border-border bg-background px-3 py-2 text-sm"
              aria-label="Settlement ID filter"
            />
          </div>
        }
        page={page}
        totalPages={listQuery.data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        columns={[
          {
            key: 'entry',
            header: 'Entry',
            sortValue: (r) => r.entryNo,
            render: (r) => (
              <Link to={`/ledger/${r.id}`} className="font-medium text-primary hover:underline">
                #{r.entryNo}
              </Link>
            )
          },
          {
            key: 'type',
            header: 'Type',
            sortValue: (r) => r.entryType,
            render: (r) => r.entryType
          },
          {
            key: 'account',
            header: 'Account',
            sortValue: (r) => r.accountCode,
            render: (r) => r.accountCode
          },
          {
            key: 'amount',
            header: 'Amount',
            sortValue: (r) => r.amount,
            render: (r) => `${r.amount.toLocaleString()} ${r.currency}`
          },
          {
            key: 'settlement',
            header: 'Settlement',
            render: (r) =>
              r.settlementId ? (
                <Link to={`/settlements/${r.settlementId}`} className="text-primary hover:underline">
                  {r.settlementId.slice(0, 8)}…
                </Link>
              ) : (
                '—'
              )
          },
          {
            key: 'posted',
            header: 'Posted',
            sortValue: (r) => r.postedAt,
            render: (r) => new Date(r.postedAt).toLocaleString()
          }
        ]}
      />
    </div>
  );
}

function LedgerDetail({ data }: { data: LedgerEntry | LedgerTransaction }) {
  const entries = 'entries' in data && Array.isArray(data.entries) ? data.entries : null;
  const fields = Object.entries(data as unknown as Record<string, unknown>).filter(
    ([key]) => key !== 'entries'
  );

  return (
    <div className="space-y-4">
      <section className="grid gap-3 rounded-2xl border border-border bg-surface p-6 sm:grid-cols-2 lg:grid-cols-3 text-sm">
        {fields.map(([key, value]) => (
            <div key={key}>
              <p className="text-xs uppercase tracking-wide text-textSecondary">{key}</p>
              <p className="mt-1 font-medium break-all">
                {value == null
                  ? '—'
                  : typeof value === 'object'
                    ? JSON.stringify(value)
                    : String(value)}
              </p>
            </div>
          ))}
      </section>
      {entries ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Lines</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {entries.map((e) => (
              <li key={e.id} className="rounded-xl border border-border px-3 py-2">
                #{e.entryNo} · {e.entryType} · {e.accountCode} · {e.amount.toLocaleString()} {e.currency}
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  );
}
