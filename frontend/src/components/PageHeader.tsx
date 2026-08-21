import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

type Props = {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
};

export function PageHeader({ eyebrow, title, description, actions }: Props) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div className="min-w-0">
        {eyebrow ? (
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-secondary">{eyebrow}</p>
        ) : null}
        <h1 className="font-display mt-1 text-3xl font-semibold tracking-tight text-textPrimary">{title}</h1>
        {description ? <p className="mt-3 max-w-3xl text-sm text-textSecondary">{description}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  );
}

export function PageActionLink({
  to,
  children,
  variant = 'secondary'
}: {
  to: string;
  children: ReactNode;
  variant?: 'primary' | 'secondary' | 'onPhoto' | 'ghost';
}) {
  const cls =
    variant === 'primary'
      ? 'at-btn rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white'
      : variant === 'onPhoto'
        ? 'at-btn rounded-full bg-white px-4 py-2 text-sm font-semibold text-primary'
        : variant === 'ghost'
          ? 'rounded-full px-4 py-2 text-sm font-semibold text-white'
          : 'at-btn rounded-full bg-surface px-4 py-2 text-sm font-semibold text-primary';
  return (
    <Link to={to} className={cls}>
      {children}
    </Link>
  );
}
