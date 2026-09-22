import { Link } from 'react-router-dom';
import { CloudSun, TrendingUp, LayoutDashboard } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { PageHeader } from '../components/PageHeader';

/**
 * Owner Climate hub: historical climate/yield, season forecast, risk intelligence.
 */
export default function ClimateHubPage() {
  const { hasPermission } = useAuth();

  const cards = [
    {
      to: '/climate',
      title: 'Historical weather & yield data',
      body: 'Stations, observations, imports, and crop history used for past learning.',
      icon: CloudSun,
      enabled: hasPermission('climate:read')
    },
    {
      to: '/planning',
      title: 'Forecasted weather of season',
      body: 'Past → now → ahead planning outlook for the coming season.',
      icon: TrendingUp,
      enabled: true
    },
    {
      to: '/climate-intel',
      title: 'Risk intelligence',
      body: 'Farm, district, AEZ, and national risk grades from climate data.',
      icon: LayoutDashboard,
      enabled: hasPermission('climate-intel:read')
    }
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Climate"
        title="Climate workspace"
        description="Historical weather and yield, seasonal forecast outlook, and risk intelligence in one place."
      />
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
