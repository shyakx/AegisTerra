import type { LucideIcon } from 'lucide-react';
import { Link } from 'react-router-dom';

type Props = {
  label: string;
  value: string | number;
  detail?: string;
  icon?: LucideIcon;
  to?: string;
  tone?: 'default' | 'success' | 'warning' | 'danger' | 'info';
};

const tones: Record<NonNullable<Props['tone']>, string> = {
  default: 'bg-primary text-white',
  success: 'bg-success text-white',
  warning: 'bg-warning text-white',
  danger: 'bg-danger text-white',
  info: 'bg-info text-white'
};

export function KpiCard({ label, value, detail, icon: Icon, to, tone = 'default' }: Props) {
  const body = (
    <div className="rounded-2xl bg-surface p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">{label}</p>
          <p className="mt-2 text-3xl font-semibold tabular-nums tracking-tight">{value}</p>
          {detail ? <p className="mt-2 text-sm text-textSecondary">{detail}</p> : null}
        </div>
        {Icon ? (
          <div className={`rounded-xl p-3 ${tones[tone]}`}>
            <Icon className="h-5 w-5" aria-hidden />
          </div>
        ) : null}
      </div>
    </div>
  );
  return to ? (
    <Link to={to} className="block focus:outline-none focus-visible:ring-2 focus-visible:ring-primary">
      {body}
    </Link>
  ) : (
    body
  );
}
