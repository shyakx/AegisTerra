import { useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { AGGREGATORS, INSURERS, LENDERS, type NamedPartner } from '../site/content';
import { PageHeader } from '../components/PageHeader';

type DirectoryKind = 'financial-institutions' | 'insurance-companies' | 'aggregators';

const META: Record<
  DirectoryKind,
  { eyebrow: string; title: string; description: string; partners: NamedPartner[] }
> = {
  'financial-institutions': {
    eyebrow: 'Partners',
    title: 'Financial institutions',
    description: 'Banks and lenders AegisTerra works with for climate-informed agricultural credit.',
    partners: LENDERS
  },
  'insurance-companies': {
    eyebrow: 'Partners',
    title: 'Insurance companies',
    description: 'Insurers that plan cover from shared climate and yield intelligence.',
    partners: INSURERS
  },
  aggregators: {
    eyebrow: 'Partners',
    title: 'Agricultural aggregators',
    description: 'Seed and fertilizer companies and aggregators serving farmer networks.',
    partners: AGGREGATORS
  }
};

export default function PartnerDirectoryPage() {
  const { kind = 'insurance-companies' } = useParams<{ kind: DirectoryKind }>();
  const meta = useMemo(() => META[kind as DirectoryKind] ?? META['insurance-companies'], [kind]);

  return (
    <div className="space-y-6">
      <PageHeader eyebrow={meta.eyebrow} title={meta.title} description={meta.description} />
      <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {meta.partners.map((p) => (
          <li
            key={p.name}
            className="flex items-center gap-3 rounded-2xl border border-border bg-surface p-4"
          >
            {p.logo ? (
              <div
                className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${
                  p.logoOnDark ? 'bg-slate-900' : 'bg-background'
                }`}
              >
                <img src={p.logo} alt="" className="max-h-8 max-w-[2.5rem] object-contain" />
              </div>
            ) : (
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-sm font-semibold text-primary">
                {p.initials ?? p.name.slice(0, 2).toUpperCase()}
              </div>
            )}
            <div>
              <p className="font-medium">{p.name}</p>
              <p className="text-xs text-textSecondary">Active partner</p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
