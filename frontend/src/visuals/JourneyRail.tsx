import { Link, useLocation } from 'react-router-dom';

export const JOURNEY = [
  { id: 'satellite', label: 'Satellite', to: '/' },
  { id: 'signal', label: 'Signal', to: '/platform' },
  { id: 'devices', label: 'Devices', to: '/partners' },
  { id: 'sensors', label: 'Sensors', to: '/about' },
  { id: 'farms', label: 'Farms', to: '/about' },
  { id: 'insights', label: 'Insights', to: '/platform' },
  { id: 'action', label: 'Action', to: '/join' }
] as const;

function activeIndex(path: string) {
  if (path === '/') return 0;
  if (path.startsWith('/platform')) return 5;
  if (path.startsWith('/partners')) return 2;
  if (path.startsWith('/about')) return 4;
  if (path.startsWith('/join') || path.startsWith('/login')) return 6;
  return 0;
}

export function JourneyRail() {
  const { pathname } = useLocation();
  const active = activeIndex(pathname);
  const width = `${(active / (JOURNEY.length - 1)) * 100}%`;

  return (
    <div className="bg-white">
      <div className="mx-auto max-w-6xl overflow-x-auto px-4 py-3 sm:px-6">
        <p className="sr-only">System journey from satellite to action</p>
        <ol className="relative flex min-w-[640px] items-center justify-between gap-2">
          <span className="absolute left-4 right-4 top-2 h-px bg-border" aria-hidden />
          <span
            className="absolute left-4 top-2 h-px bg-primary transition-[width] duration-500 ease-out"
            style={{ width }}
            aria-hidden
          />
          {JOURNEY.map((step, index) => {
            const on = index <= active;
            return (
              <li key={step.id} className="relative z-10 flex flex-1 flex-col items-center">
                <Link
                  to={step.to}
                  className={`h-3 w-3 rounded-full ${on ? 'bg-primary' : 'bg-border'}`}
                  aria-current={index === active ? 'step' : undefined}
                />
                <span className={`mt-2 text-[11px] font-medium uppercase tracking-wide ${on ? 'text-primary' : 'text-textSecondary'}`}>
                  {step.label}
                </span>
              </li>
            );
          })}
        </ol>
      </div>
    </div>
  );
}
