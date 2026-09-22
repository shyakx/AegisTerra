import { Link } from 'react-router-dom';
import { Brain, TrendingUp, LayoutDashboard, Bell } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { PageHeader } from '../components/PageHeader';

/**
 * Admin System intelligence: planning brain + national risk + alerts (not a map desk).
 */
export default function SystemIntelligencePage() {
  const { hasPermission } = useAuth();

  const cards = [
    {
      to: '/planning',
      title: 'Planning outlook',
      body: 'Past climate and yield → current risk → ahead outlook for stakeholders.',
      icon: TrendingUp,
      enabled: true
    },
    {
      to: '/climate-intel',
      title: 'National & AEZ risk',
      body: 'Deterministic risk grades, district heat, and agroecological rollups.',
      icon: LayoutDashboard,
      enabled: hasPermission('climate-intel:read')
    },
    {
      to: '/climate-intel/alerts',
      title: 'Climate alerts',
      body: 'Open stress signals where payout risk may be rising.',
      icon: Bell,
      enabled: hasPermission('climate-intel:read') || hasPermission('alerts:climate')
    }
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="System intelligence"
        title="Intelligence plane"
        description="Cross-cutting climate and yield intelligence that powers planning across partners — separate from raw climate data ops."
      />
      <div className="flex items-start gap-3 rounded-2xl border border-border bg-surface p-4">
        <Brain className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden />
        <p className="text-sm text-textSecondary">
          Includes the experimental maize ML spike panel on Planning outlook. Rule-based outlook remains the product
          default.
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        {cards
          .filter((c) => c.enabled)
          .map((card) => (
            <Link
              key={card.to}
              to={card.to}
              className="rounded-2xl border border-border bg-surface p-5 shadow-sm transition hover:border-primary/40"
            >
              <card.icon className="h-6 w-6 text-primary" aria-hidden />
              <h2 className="mt-3 text-lg font-semibold">{card.title}</h2>
              <p className="mt-2 text-sm text-textSecondary">{card.body}</p>
            </Link>
          ))}
      </div>
    </div>
  );
}
