/** Browser-side CSV download for report/table rows. Production-safe client utility. */
export function exportRowsToCsv(
  filename: string,
  rows: Array<Record<string, unknown>>,
  columns?: string[]
): void {
  if (!rows.length) {
    return;
  }
  const cols = columns ?? Object.keys(rows[0]);
  const escape = (value: unknown) => {
    const raw = value == null ? '' : String(value);
    if (/[",\n\r]/.test(raw)) {
      return `"${raw.replace(/"/g, '""')}"`;
    }
    return raw;
  };
  const lines = [cols.join(',')].concat(rows.map((row) => cols.map((c) => escape(row[c])).join(',')));
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename.endsWith('.csv') ? filename : `${filename}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
