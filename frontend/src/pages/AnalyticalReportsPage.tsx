import { Link } from 'react-router-dom';
import { FileBarChart } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { PageHeader } from '../components/PageHeader';

export default function AnalyticalReportsPage() {
  const { hasPermission } = useAuth();

  const links = [
    {
      to: '/planning',
      label: 'Yield & climate outlook',
      note: 'Past → now → ahead with CSV export on the planning page',
      enabled: true
    },
    {
      to: '/climate-intel',
      label: 'National risk intelligence',
      note: 'District and AEZ risk heat for analytical briefings',
      enabled: hasPermission('climate-intel:read')
    },
    {
      to: '/climate',
      label: 'Climate data health',
      note: 'Station volume, imports, and provider status',
      enabled: hasPermission('climate:read')
    },
    {
      to: '/settlements/reports',
      label: 'Payout / settlement reports',
      note: 'Partner payout reporting when settlement ops are enabled',
      enabled: hasPermission('reports:settlements') || hasPermission('settlements:read')
    },
    {
      to: '/insurance/reports',
      label: 'Insurance portfolio reports',
      note: 'Policy portfolio analytics for partner insurers',
      enabled: hasPermission('policies:read')
    }
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Analytics"
        title="Analytical reports"
        description="Entry points to planning, climate risk, and partner operational reports."
      />
      <ul className="space-y-3">
        {links
          .filter((l) => l.enabled)
          .map((l) => (
            <li key={l.to}>
              <Link
                to={l.to}
                className="flex items-start gap-3 rounded-2xl border border-border bg-surface p-4 hover:border-primary/40"
              >
                <FileBarChart className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden />
                <span>
                  <span className="block font-semibold">{l.label}</span>
                  <span className="mt-1 block text-sm text-textSecondary">{l.note}</span>
                </span>
              </Link>
            </li>
          ))}
      </ul>
    </div>
  );
}
