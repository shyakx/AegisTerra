export type RoleDashboardMeta = {
  eyebrow: string;
  title: string;
  description: string;
  /** Which KPI bands to emphasize on the shared overview surface */
  focus: Array<'portfolio' | 'insurance' | 'finance' | 'climate' | 'ops' | 'support' | 'farmer'>;
};

const META: Record<string, RoleDashboardMeta> = {
  SYSTEM_ADMIN: {
    eyebrow: 'Operations Command Center',
    title: 'Executive overview',
    description: 'National portfolio, claims, settlements, and climate risk posture.',
    focus: ['portfolio', 'insurance', 'finance', 'climate', 'ops']
  },
  INSURANCE_ADMIN: {
    eyebrow: 'Insurance administration',
    title: 'Insurance portfolio',
    description: 'Policies, claims, registry coverage, and operational task posture for the insurer.',
    focus: ['portfolio', 'insurance', 'ops']
  },
  INSURANCE_OFFICER: {
    eyebrow: 'Insurance workstation',
    title: 'Operational insurance desk',
    description: 'Focus on policies, claims intake, and your assigned workflow tasks.',
    focus: ['insurance', 'ops']
  },
  FI_OFFICER: {
    eyebrow: 'Financial operations',
    title: 'Settlement desk',
    description: 'Pending payouts, ledger posture, and finance review work.',
    focus: ['finance', 'ops']
  },
  GOVERNMENT_ANALYST: {
    eyebrow: 'National oversight',
    title: 'Analytics & climate intelligence',
    description: 'Read-only national view of agriculture, insurance, settlements, and climate risk.',
    focus: ['portfolio', 'insurance', 'finance', 'climate']
  },
  AGGREGATOR: {
    eyebrow: 'Aggregator workspace',
    title: 'Farmer portfolio',
    description: 'Onboarding and agricultural registry operations for your cohort.',
    focus: ['portfolio', 'ops']
  },
  FARMER: {
    eyebrow: 'Farmer portal',
    title: 'My workspace',
    description: 'Your profile, farms, policies, and claims — scoped to your linked farmer record.',
    focus: ['farmer']
  },
  AUDITOR: {
    eyebrow: 'Audit workspace',
    title: 'Oversight overview',
    description: 'Read-only operational posture across registry, insurance, finance, and climate.',
    focus: ['portfolio', 'insurance', 'finance', 'climate', 'ops']
  },
  SUPPORT: {
    eyebrow: 'Support desk',
    title: 'Assistance overview',
    description: 'Lookup and case lookup for farmers, policies, claims, and users.',
    focus: ['support', 'ops']
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
  switch (role) {
    case 'INSURANCE_ADMIN':
      return [
        { to: '/policies', label: 'Policies', permission: 'policies:read' },
        { to: '/claims', label: 'Claims', permission: 'claims:read' },
        { to: '/insurance/products', label: 'Products', permission: 'policies:read' },
        { to: '/tasks', label: 'Tasks', permission: 'tasks:read' },
        { to: '/farmers', label: 'Farmers', permission: 'farmers:read' }
      ];
    case 'INSURANCE_OFFICER':
      return [
        { to: '/claims', label: 'Claims', permission: 'claims:read' },
        { to: '/policies', label: 'Policies', permission: 'policies:read' },
        { to: '/tasks', label: 'My tasks', permission: 'tasks:read' },
        { to: '/claims/new', label: 'New claim', permission: 'claims:write' },
        { to: '/policies/issue', label: 'Issue policy', permission: 'policies:write' }
      ];
    case 'FI_OFFICER':
      return [
        { to: '/settlements', label: 'Settlements', permission: 'settlements:read' },
        { to: '/settlements/finance', label: 'Finance review', permission: 'settlements:approve' },
        { to: '/ledger', label: 'Ledger', permission: 'ledger:read' },
        { to: '/settlements/reports', label: 'Reports', permission: 'reports:settlements' },
        { to: '/tasks', label: 'Tasks', permission: 'tasks:read' }
      ];
    case 'GOVERNMENT_ANALYST':
      return [
        { to: '/climate-intel', label: 'National risk', permission: 'climate-intel:read' },
        { to: '/climate-intel/alerts', label: 'Alerts', permission: 'climate-intel:read' },
        { to: '/gis', label: 'GIS', permission: 'farms:read' },
        { to: '/insurance/reports', label: 'Ins. reports', permission: 'policies:read' },
        { to: '/settlements/reports', label: 'Settle. reports', permission: 'reports:settlements' }
      ];
    case 'AGGREGATOR':
      return [
        { to: '/farmers/register', label: 'Register farmer', permission: 'farmers:write' },
        { to: '/farmers', label: 'Farmers', permission: 'farmers:read' },
        { to: '/farms', label: 'Farms', permission: 'farms:read' },
        { to: '/gis', label: 'GIS', permission: 'farms:read' },
        { to: '/tasks', label: 'Tasks', permission: 'tasks:read' }
      ];
    case 'FARMER':
      return [
        { to: farmerId ? `/farmers/${farmerId}` : '/farmers', label: 'My profile', permission: 'farmers:read' },
        { to: '/farms', label: 'My farms', permission: 'farms:read' },
        { to: '/policies', label: 'My policies', permission: 'policies:read' },
        { to: '/claims', label: 'My claims', permission: 'claims:read' },
        { to: '/notifications', label: 'Notifications', permission: 'notifications:read' }
      ];
    case 'AUDITOR':
      return [
        { to: '/claims', label: 'Claims', permission: 'claims:read' },
        { to: '/settlements', label: 'Settlements', permission: 'settlements:read' },
        { to: '/ledger', label: 'Ledger', permission: 'ledger:read' },
        { to: '/users', label: 'Users', permission: 'users:read' },
        { to: '/climate-intel', label: 'Climate intel', permission: 'climate-intel:read' }
      ];
    case 'SUPPORT':
      return [
        { to: '/users', label: 'Users', permission: 'users:read' },
        { to: '/farmers', label: 'Farmers', permission: 'farmers:read' },
        { to: '/policies', label: 'Policies', permission: 'policies:read' },
        { to: '/claims', label: 'Claims', permission: 'claims:read' },
        { to: '/tasks', label: 'Tasks', permission: 'tasks:read' }
      ];
    case 'SYSTEM_ADMIN':
    default:
      return [
        { to: '/farmers', label: 'Farmers', permission: 'farmers:read' },
        { to: '/policies', label: 'Policies', permission: 'policies:read' },
        { to: '/claims', label: 'Claims', permission: 'claims:read' },
        { to: '/settlements', label: 'Settlements', permission: 'settlements:read' },
        { to: '/climate-intel', label: 'Climate intel', permission: 'climate-intel:read' },
        { to: '/users', label: 'Users', permission: 'users:read' }
      ];
  }
}
