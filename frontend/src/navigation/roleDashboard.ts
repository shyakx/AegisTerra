export type RoleDashboardMeta = {
  eyebrow: string;
  title: string;
  description: string;
  focus: Array<'portfolio' | 'insurance' | 'finance' | 'climate' | 'ops' | 'support' | 'farmer' | 'lending'>;
};

const META: Record<string, RoleDashboardMeta> = {
  SYSTEM_ADMIN: {
    eyebrow: 'Climate risk intelligence',
    title: 'National risk backbone',
    description:
      'AegisTerra does not sell insurance. It fuses climate, satellite, and farm data so insurers, lenders, aggregators, and government share one risk picture.',
    focus: ['climate', 'portfolio', 'insurance', 'lending', 'ops']
  },
  INSURANCE_ADMIN: {
    eyebrow: 'Partner insurer',
    title: 'Product design from climate risk',
    description: 'Use localized climate risk reports to design products, price premiums, and administer policies.',
    focus: ['climate', 'insurance', 'portfolio', 'ops']
  },
  INSURANCE_OFFICER: {
    eyebrow: 'Partner insurer',
    title: 'Policy and claims administration',
    description: 'Administer partner policies and claims against climate-verified farm records.',
    focus: ['insurance', 'climate', 'ops']
  },
  FI_OFFICER: {
    eyebrow: 'Financial institution',
    title: 'Agricultural lending',
    description: 'Insured loans, borrower crop condition, climate exposure, and payout status.',
    focus: ['lending', 'climate', 'finance', 'ops']
  },
  GOVERNMENT_ANALYST: {
    eyebrow: 'Government',
    title: 'Food security and climate risk',
    description: 'Aggregated coverage, productivity, climate risk, and disaster-response intelligence.',
    focus: ['climate', 'portfolio', 'insurance']
  },
  DEVELOPMENT_PARTNER: {
    eyebrow: 'Development partner',
    title: 'Programme intelligence',
    description: 'Aggregated agricultural and climate statistics for project monitoring and investment planning.',
    focus: ['climate', 'portfolio']
  },
  AGGREGATOR: {
    eyebrow: 'Aggregator',
    title: 'Farmer networks and insured inputs',
    description: 'Register farmers and sell seed or fertilizer with an embedded insurance premium.',
    focus: ['portfolio', 'ops']
  },
  FARMER: {
    eyebrow: 'My farm',
    title: 'Coverage and guidance',
    description: 'Your profile, farms, partner cover, weather alerts, and recommendations.',
    focus: ['farmer']
  },
  AUDITOR: {
    eyebrow: 'Assurance',
    title: 'Oversight',
    description: 'Read-only view across farmer data, partner insurance, lending, and climate intelligence.',
    focus: ['portfolio', 'insurance', 'finance', 'climate', 'lending']
  },
  SUPPORT: {
    eyebrow: 'Support',
    title: 'Assistance overview',
    description: 'Lookup for operators, farmers, policies, and claims.',
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
        { to: '/climate-intel', label: 'Climate risk reports', permission: 'climate-intel:read' },
        { to: '/insurance/products', label: 'Products', permission: 'policies:read' },
        { to: '/policies', label: 'Policies', permission: 'policies:read' },
        { to: '/claims', label: 'Claims', permission: 'claims:read' }
      ];
    case 'INSURANCE_OFFICER':
      return [
        { to: '/climate-intel', label: 'Climate risk', permission: 'climate-intel:read' },
        { to: '/claims', label: 'Claims', permission: 'claims:read' },
        { to: '/policies', label: 'Policies', permission: 'policies:read' }
      ];
    case 'FI_OFFICER':
      return [
        { to: '/lending', label: 'Insured loans', permission: 'loans:read' },
        { to: '/climate-intel', label: 'Climate risk', permission: 'climate-intel:read' },
        { to: '/farmers', label: 'Borrowers', permission: 'farmers:read' },
        { to: '/settlements', label: 'Payout status', permission: 'settlements:read' }
      ];
    case 'GOVERNMENT_ANALYST':
    case 'DEVELOPMENT_PARTNER':
      return [
        { to: '/climate-intel', label: 'National risk', permission: 'climate-intel:read' },
        { to: '/satellite', label: 'Satellite', permission: 'satellite:read' },
        { to: '/climate-intel/alerts', label: 'Alerts', permission: 'climate-intel:read' },
        { to: '/gis', label: 'GIS', permission: 'farms:read' }
      ];
    case 'AGGREGATOR':
      return [
        { to: '/farmers/register', label: 'Register farmer', permission: 'farmers:write' },
        { to: '/inputs', label: 'Insured inputs', permission: 'inputs:read' },
        { to: '/farmers', label: 'Farmers', permission: 'farmers:read' }
      ];
    case 'FARMER':
      return [
        { to: farmerId ? `/farmers/${farmerId}` : '/farmers', label: 'My profile', permission: 'farmers:read' },
        { to: '/guidance', label: 'Guidance', permission: 'notifications:read' },
        { to: '/farms', label: 'My farms', permission: 'farms:read' },
        { to: '/policies', label: 'My policies', permission: 'policies:read' }
      ];
    case 'AUDITOR':
      return [
        { to: '/climate-intel', label: 'Climate risk', permission: 'climate-intel:read' },
        { to: '/lending', label: 'Loans', permission: 'loans:read' },
        { to: '/claims', label: 'Claims', permission: 'claims:read' }
      ];
    case 'SUPPORT':
      return [
        { to: '/users', label: 'Users', permission: 'users:read' },
        { to: '/farmers', label: 'Farmers', permission: 'farmers:read' },
        { to: '/policies', label: 'Policies', permission: 'policies:read' }
      ];
    case 'SYSTEM_ADMIN':
    default:
      return [
        { to: '/climate-intel', label: 'Risk intelligence', permission: 'climate-intel:read' },
        { to: '/satellite', label: 'Satellite', permission: 'satellite:read' },
        { to: '/lending', label: 'Lending', permission: 'loans:read' },
        { to: '/farmers', label: 'Farmers', permission: 'farmers:read' },
        { to: '/policies', label: 'Policies', permission: 'policies:read' }
      ];
  }
}
