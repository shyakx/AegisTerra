export type RoleDashboardMeta = {
  eyebrow: string;
  title: string;
  description: string;
  focus: Array<'portfolio' | 'climate' | 'farmer' | 'support'>;
};

const META: Record<string, RoleDashboardMeta> = {
  SYSTEM_ADMIN: {
    eyebrow: 'System administrator',
    title: 'National overview',
    description:
      'Climate hub, system intelligence, partner directories, policies, and payouts for platform administration.',
    focus: ['climate', 'portfolio']
  },
  INSURANCE_ADMIN: {
    eyebrow: 'Insurance company',
    title: 'Zones, policy, and payout notices',
    description: 'Agroecological context plus policy and payout notification for insurer staff.',
    focus: ['climate', 'portfolio']
  },
  INSURANCE_OFFICER: {
    eyebrow: 'Authorized insurance officer',
    title: 'Zones, policy, and payout notices',
    description: 'Work AEZ context, policies, and payout notifications.',
    focus: ['climate', 'portfolio']
  },
  FI_OFFICER: {
    eyebrow: 'Authorized bank officer',
    title: 'Farmers, loans, and payouts',
    description: 'See registered farmers with loans, triggered payouts, and related policy.',
    focus: ['climate', 'portfolio']
  },
  GOVERNMENT_ANALYST: {
    eyebrow: 'Government officer',
    title: 'Climate and analytical reports',
    description: 'National climate, AEZ, and analytical report entry points.',
    focus: ['climate', 'portfolio']
  },
  DEVELOPMENT_PARTNER: {
    eyebrow: 'Development partner',
    title: 'Programme climate intelligence',
    description: 'Shared national climate and yield-oriented statistics for programme planning.',
    focus: ['climate', 'portfolio']
  },
  AGGREGATOR: {
    eyebrow: 'Authorized aggregator officer',
    title: 'Network farms and climate',
    description: 'Register and follow farmer networks with climate and AEZ context.',
    focus: ['portfolio', 'climate']
  },
  FARMER: {
    eyebrow: 'Farmer',
    title: 'Status and payouts',
    description: 'Your farmer status, farms, and payout records.',
    focus: ['farmer']
  },
  AUDITOR: {
    eyebrow: 'Auditor',
    title: 'Read-only assurance view',
    description: 'Inspect climate, zones, reports, policies, and payouts.',
    focus: ['climate', 'portfolio']
  },
  SUPPORT: {
    eyebrow: 'Support',
    title: 'Help with platform access',
    description: 'Assist users with registry and account screens.',
    focus: ['support', 'portfolio']
  }
};

export function roleDashboardMeta(roles: string[] | undefined): RoleDashboardMeta {
  const role = roles?.[0];
  if (role && META[role]) {
    return META[role];
  }
  return META.SYSTEM_ADMIN;
}

export function showsFocus(meta: RoleDashboardMeta, band: RoleDashboardMeta['focus'][number]): boolean {
  return meta.focus.includes(band);
}

export type QuickLink = { to: string; label: string; permission?: string };

export function roleQuickLinks(role: string | undefined, farmerId?: string | null): QuickLink[] {
  if (role === 'FARMER') {
    return [
      { to: '/farmer-status', label: 'Farmer status', permission: 'farmers:read' },
      { to: '/payouts', label: 'Payouts', permission: 'settlements:read' },
      { to: '/settings', label: 'Setting' }
    ];
  }
  if (role === 'INSURANCE_OFFICER' || role === 'INSURANCE_ADMIN') {
    return [
      { to: '/agroecological-zones', label: 'Agro-ecological zones', permission: 'farmers:read' },
      { to: '/payouts', label: 'Payouts notification', permission: 'settlements:read' },
      { to: '/policies', label: 'Policy', permission: 'policies:read' },
      { to: '/settings', label: 'Setting' }
    ];
  }
  if (role === 'FI_OFFICER') {
    return [
      { to: '/lending', label: 'Farmers & loans', permission: 'loans:read' },
      { to: '/payouts', label: 'Payouts triggered', permission: 'settlements:read' },
      { to: '/policies', label: 'Policy', permission: 'policies:read' },
      { to: '/settings', label: 'Setting' }
    ];
  }
  if (role === 'SYSTEM_ADMIN') {
    return [
      { to: '/climate-hub', label: 'Climate', permission: 'climate:read' },
      { to: '/system-intelligence', label: 'System intelligence', permission: 'climate-intel:read' },
      { to: '/agroecological-zones', label: 'Agro-ecological zones', permission: 'farmers:read' },
      { to: '/reports', label: 'Analytical reports' },
      { to: '/payouts', label: 'Payouts', permission: 'settlements:read' },
      { to: '/policies', label: 'Policies', permission: 'policies:read' }
    ];
  }
  return [
    { to: '/climate-hub', label: 'Climate', permission: 'climate:read' },
    { to: '/agroecological-zones', label: 'Agro-ecological zones', permission: 'farmers:read' },
    { to: '/reports', label: 'Analytical reports' },
    { to: farmerId ? `/farmers/${farmerId}` : '/farmers', label: 'Farmers', permission: 'farmers:read' },
    { to: '/settings', label: 'Setting' }
  ];
}
