import { useMemo, useState } from 'react';
import { Download, Loader2, Search } from 'lucide-react';

type Column<T> = {
  key: string;
  header: string;
  render: (row: T) => React.ReactNode;
  sortValue?: (row: T) => string | number;
};

type Props<T> = {
  title: string;
  subtitle?: string;
  rows: T[];
  columns: Column<T>[];
  loading?: boolean;
  error?: string | null;
  emptyMessage?: string;
  searchPlaceholder?: string;
  onSearchChange?: (value: string) => void;
  searchValue?: string;
  filters?: React.ReactNode;
  actions?: React.ReactNode;
  page?: number;
  totalPages?: number;
  onPageChange?: (page: number) => void;
  onExport?: () => void;
  getRowKey: (row: T) => string;
};

export function EnterpriseTable<T>({
  title,
  subtitle,
  rows,
  columns,
  loading,
  error,
  emptyMessage = 'No records found.',
  searchPlaceholder = 'Search…',
  onSearchChange,
  searchValue,
  filters,
  actions,
  page = 0,
  totalPages = 1,
  onPageChange,
  onExport,
  getRowKey
}: Props<T>) {
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  const sorted = useMemo(() => {
    if (!sortKey) return rows;
    const col = columns.find((c) => c.key === sortKey);
    if (!col?.sortValue) return rows;
    const copy = [...rows];
    copy.sort((a, b) => {
      const av = col.sortValue!(a);
      const bv = col.sortValue!(b);
      if (av < bv) return sortDir === 'asc' ? -1 : 1;
      if (av > bv) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });
    return copy;
  }, [rows, columns, sortKey, sortDir]);

  function toggleSort(key: string) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  }

  return (
    <section className="space-y-4 rounded-2xl border border-border bg-surface p-4 shadow-sm md:p-6" aria-label={title}>
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          {subtitle ? <p className="text-sm text-textSecondary">{subtitle}</p> : null}
          <h2 className="text-2xl font-semibold">{title}</h2>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {actions}
          {onExport ? (
            <button
              type="button"
              onClick={onExport}
              className="inline-flex items-center gap-2 rounded-xl border border-border px-3 py-2 text-sm font-medium hover:bg-background"
            >
              <Download className="h-4 w-4" aria-hidden />
              Export CSV
            </button>
          ) : null}
        </div>
      </div>

      <div className="flex flex-col gap-3 md:flex-row md:items-center">
        {onSearchChange ? (
          <label className="relative block min-w-[220px] flex-1 text-sm">
            <span className="sr-only">{searchPlaceholder}</span>
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-textSecondary" />
            <input
              value={searchValue ?? ''}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder={searchPlaceholder}
              className="w-full rounded-xl border border-border bg-background py-2 pl-9 pr-3 outline-none focus:ring-2 focus:ring-primary/30"
            />
          </label>
        ) : null}
        {filters}
      </div>

      {loading ? (
        <div className="flex items-center gap-2 py-12 text-textSecondary" role="status">
          <Loader2 className="h-5 w-5 animate-spin" />
          Loading…
        </div>
      ) : error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700" role="alert">
          {error}
        </div>
      ) : sorted.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border p-10 text-center text-textSecondary">{emptyMessage}</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-textSecondary">
                {columns.map((col) => (
                  <th key={col.key} scope="col" className="px-3 py-3 font-medium">
                    {col.sortValue ? (
                      <button type="button" className="hover:text-primary" onClick={() => toggleSort(col.key)}>
                        {col.header}
                        {sortKey === col.key ? (sortDir === 'asc' ? ' ↑' : ' ↓') : ''}
                      </button>
                    ) : (
                      col.header
                    )}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {sorted.map((row) => (
                <tr key={getRowKey(row)} className="border-b border-border/70 hover:bg-background/80">
                  {columns.map((col) => (
                    <td key={col.key} className="px-3 py-3 align-top">
                      {col.render(row)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {onPageChange && totalPages > 1 ? (
        <div className="flex items-center justify-between gap-3 text-sm">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => onPageChange(page - 1)}
            className="rounded-lg border border-border px-3 py-1.5 disabled:opacity-40"
          >
            Previous
          </button>
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => onPageChange(page + 1)}
            className="rounded-lg border border-border px-3 py-1.5 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      ) : null}
    </section>
  );
}
