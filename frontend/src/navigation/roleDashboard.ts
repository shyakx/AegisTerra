export type RoleDashboardMeta = {
  eyebrow: string;
  title: string;
  description: string;
  focus: Array<'portfolio' | 'climate' | 'farmer' | 'support'>;
};

const META: Record<string, RoleDashboardMeta> = {
  SYSTEM_ADMIN: {
    eyebrow: 'Climate & yield intelligence',
    title: 'Plan ahead from shared data',
    description:
      'AegisTerra helps insurers and stakeholders see past climate and yield patterns, current risk, and what is likely coming — not run a full insurance company.',
    focus: ['climate', 'portfolio']
  },
  INSURANCE_ADMIN: {
    eyebrow: 'Insurer planning',
    title: 'Know what is coming before loss',
    description:
      'Use historical climate behaviour, current risk scores, and outlook views to design cover and manage portfolio exposure.',
    focus: ['climate', 'portfolio']
  },
  INSURANCE_OFFICER: {
    eyebrow: 'Insurer planning',
    title: 'Climate risk for your book',
    description: 'Monitor district and farm climate risk so unexpected harvest losses are less surprising.',
    focus: ['climate', 'portfolio']
  },
  FI_OFFICER: {
    eyebrow: 'Lender planning',
    title: 'Climate exposure on agricultural credit',
    description: 'See borrower farms, climate risk, and outlook before committing or monitoring credit.',
    focus: ['climate', 'portfolio']
  },
  GOVERNMENT_ANALYST: {
    eyebrow: 'Government',
    title: 'National climate and production outlook',
    description: 'Aggregated climate risk, AEZ views, and farmer registry for food-security planning.',
    focus: ['climate', 'portfolio']
  },
  DEVELOPMENT_PARTNER: {
    eyebrow: 'Development partner',
    title: 'Programme climate intelligence',
    description: 'Shared national climate and yield-oriented statistics for programme planning.',
    focus: ['climate', 'portfolio']
  },
  AGGREGATOR: {
    eyebrow: 'Aggregator',
    title: 'Network farms and climate outlook',
    description: 'Register farmers and farms, then follow climate risk across your network.',
    focus: ['portfolio', 'climate']
  },
  FARMER: {
    eyebrow: 'My farm',
    title: 'What is happening and what is coming',
    description: 'Your farms, guidance, and climate outlook so you can see risk early.',
    focus: ['farmer']
  },
  AUDITOR: {
    eyebrow: 'Assurance',
    title: 'Read-only planning view',
    description: 'Inspect registry and climate intelligence used for stakeholder planning.',
    focus: ['climate', 'portfolio']
  },
  SUPPORT: {
    eyebrow: 'Support',
    title: 'Help with planning tools',
    description: 'Assist users with registry, climate data, and outlook screens.',
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
      { to: farmerId ? `/farmers/${farmerId}` : '/farmers', label: 'My profile', permission: 'farmers:read' },
      { to: '/planning', label: 'Planning outlook' },
      { to: '/farms', label: 'My farms', permission: 'farms:read' },
      { to: '/guidance', label: 'Guidance', permission: 'notifications:read' }
    ];
  }
  return [
    { to: '/planning', label: 'Planning outlook' },
    { to: '/climate-intel', label: 'Risk intelligence', permission: 'climate-intel:read' },
    { to: '/climate', label: 'Climate data', permission: 'climate:read' },
    { to: '/farmers', label: 'Farmers', permission: 'farmers:read' },
    { to: '/farms', label: 'Farms', permission: 'farms:read' }
  ];
}
