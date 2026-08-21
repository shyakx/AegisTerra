import type { LucideIcon } from 'lucide-react';
import {
  LayoutDashboard,
  ShieldCheck,
  Map,
  FileText,
  Users,
  Settings,
  Bell,
  CloudSun,
  Wallet,
  ClipboardList,
  Landmark,
  Satellite
} from 'lucide-react';

export type NavItem = {
  label: string;
  to: string;
  icon: LucideIcon;
  permission?: string;
};

export type NavGroup = {
  label: string;
  items: NavItem[];
};

type AuthHelpers = {
  hasPermission: (permission: string) => boolean;
  hasRole: (role: string) => boolean;
  hasAnyPermission?: (...permissions: string[]) => boolean;
  farmerId?: string | null;
};

const ROLE_ORDER = [
  'SYSTEM_ADMIN',
  'INSURANCE_ADMIN',
  'INSURANCE_OFFICER',
  'FI_OFFICER',
  'GOVERNMENT_ANALYST',
  'DEVELOPMENT_PARTNER',
  'AGGREGATOR',
  'FARMER',
  'AUDITOR',
  'SUPPORT'
] as const;

const HEADER_BY_ROLE: Record<string, { eyebrow: string; title: string }> = {
  SYSTEM_ADMIN: {
    eyebrow: 'Climate risk intelligence',
    title: 'Agricultural risk backbone'
  },
  INSURANCE_ADMIN: {
    eyebrow: 'Partner insurer',
    title: 'Product design and portfolio'
  },
  INSURANCE_OFFICER: {
    eyebrow: 'Partner insurer',
    title: 'Policy and claims administration'
  },
  FI_OFFICER: {
    eyebrow: 'Financial institution',
    title: 'Agricultural lending'
  },
  GOVERNMENT_ANALYST: {
    eyebrow: 'Government',
    title: 'National risk and food security'
  },
  DEVELOPMENT_PARTNER: {
    eyebrow: 'Development partner',
    title: 'Programme intelligence'
  },
  AGGREGATOR: {
    eyebrow: 'Aggregator network',
    title: 'Farmer onboarding and insured inputs'
  },
  FARMER: {
    eyebrow: 'Farmer',
    title: 'My farm and coverage'
  },
  AUDITOR: {
    eyebrow: 'Assurance',
    title: 'Audit workspace'
  },
  SUPPORT: {
    eyebrow: 'Operator assistance',
    title: 'Support desk'
  }
};

function catalogForRole(role: string | undefined, farmerId?: string | null): NavGroup[] {
  switch (role) {
    case 'INSURANCE_ADMIN':
      return [
        {
          label: 'Workspace',
          items: [
            { label: 'Overview', to: '/app', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Risk intelligence',
          items: [
            { label: 'Climate risk reports', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
            { label: 'Satellite', to: '/satellite', icon: Satellite, permission: 'satellite:read' },
            { label: 'Climate data', to: '/climate', icon: CloudSun, permission: 'climate:read' }
          ]
        },
        {
          label: 'Policy administration',
          items: [
            { label: 'Products', to: '/insurance/products', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Premium pricing', to: '/insurance/calculator', icon: FileText, permission: 'policies:read' },
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Reports', to: '/insurance/reports', icon: FileText, permission: 'policies:read' }
          ]
        },
        {
          label: 'Claims and payouts',
          items: [
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' }
          ]
        },
        {
          label: 'Farmer management',
          items: [
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
            { label: 'GIS', to: '/gis', icon: Map, permission: 'farms:read' }
          ]
        },
        { label: 'Account', items: [{ label: 'Platform info', to: '/settings', icon: Settings }] }
      ];
    case 'INSURANCE_OFFICER':
      return [
        {
          label: 'Workspace',
          items: [
            { label: 'Overview', to: '/app', icon: LayoutDashboard },
            { label: 'My tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Risk intelligence',
          items: [
            { label: 'Climate risk reports', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
            { label: 'Satellite', to: '/satellite', icon: Satellite, permission: 'satellite:read' }
          ]
        },
        {
          label: 'Policy administration',
          items: [
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Premium pricing', to: '/insurance/calculator', icon: FileText, permission: 'policies:read' },
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' }
          ]
        },
        {
          label: 'Farmer management',
          items: [
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' }
          ]
        },
        { label: 'Account', items: [{ label: 'Platform info', to: '/settings', icon: Settings }] }
      ];
    case 'FI_OFFICER':
      return [
        {
          label: 'Workspace',
          items: [
            { label: 'Overview', to: '/app', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Agricultural lending',
          items: [
            { label: 'Insured loans', to: '/lending', icon: Landmark, permission: 'loans:read' },
            { label: 'Borrowers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Crop performance', to: '/farms', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Risk intelligence',
          items: [
            { label: 'Climate risk', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
            { label: 'Satellite', to: '/satellite', icon: Satellite, permission: 'satellite:read' }
          ]
        },
        {
          label: 'Payout status',
          items: [
            { label: 'Payouts', to: '/settlements', icon: Wallet, permission: 'settlements:read' },
            { label: 'Linked policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' }
          ]
        },
        { label: 'Account', items: [{ label: 'Platform info', to: '/settings', icon: Settings }] }
      ];
    case 'GOVERNMENT_ANALYST':
    case 'DEVELOPMENT_PARTNER':
      return [
        {
          label: 'National intelligence',
          items: [
            { label: 'Overview', to: '/app', icon: LayoutDashboard },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Climate and satellite',
          items: [
            { label: 'Climate risk', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
            { label: 'Satellite', to: '/satellite', icon: Satellite, permission: 'satellite:read' },
            { label: 'Alerts', to: '/climate-intel/alerts', icon: Bell, permission: 'climate-intel:read' },
            { label: 'Climate data', to: '/climate', icon: CloudSun, permission: 'climate:read' }
          ]
        },
        {
          label: 'Coverage and production',
          items: [
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
            { label: 'GIS', to: '/gis', icon: Map, permission: 'farms:read' },
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Reports', to: '/insurance/reports', icon: FileText, permission: 'policies:read' }
          ]
        },
        { label: 'Account', items: [{ label: 'Platform info', to: '/settings', icon: Settings }] }
      ];
    case 'AGGREGATOR':
      return [
        {
          label: 'Workspace',
          items: [
            { label: 'Overview', to: '/app', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Farmer management',
          items: [
            { label: 'Register farmer', to: '/farmers/register', icon: Users, permission: 'farmers:write' },
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
            { label: 'GIS', to: '/gis', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Insured inputs',
          items: [{ label: 'Seed and fertilizer', to: '/inputs', icon: ShieldCheck, permission: 'inputs:read' }]
        },
        { label: 'Account', items: [{ label: 'Platform info', to: '/settings', icon: Settings }] }
      ];
    case 'FARMER':
      return [
        {
          label: 'My workspace',
          items: [
            { label: 'Home', to: '/app', icon: LayoutDashboard },
            { label: 'Guidance', to: '/guidance', icon: Bell, permission: 'notifications:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'My farm',
          items: [
            {
              label: 'My profile',
              to: farmerId ? `/farmers/${farmerId}` : '/farmers',
              icon: Users,
              permission: 'farmers:read'
            },
            { label: 'My farms', to: '/farms', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Cover from partners',
          items: [
            { label: 'My policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'My claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' }
          ]
        },
        { label: 'Account', items: [{ label: 'Platform info', to: '/settings', icon: Settings }] }
      ];
    case 'AUDITOR':
      return [
        {
          label: 'Assurance',
          items: [
            { label: 'Overview', to: '/app', icon: LayoutDashboard },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Capabilities',
          items: [
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' },
            { label: 'Payouts', to: '/settlements', icon: Wallet, permission: 'settlements:read' },
            { label: 'Loans', to: '/lending', icon: Landmark, permission: 'loans:read' },
            { label: 'Climate risk', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
            { label: 'Satellite', to: '/satellite', icon: Satellite, permission: 'satellite:read' }
          ]
        },
        { label: 'Account', items: [{ label: 'Users', to: '/users', icon: Users, permission: 'users:read' }, { label: 'Platform info', to: '/settings', icon: Settings }] }
      ];
    case 'SUPPORT':
      return [
        {
          label: 'Support desk',
          items: [
            { label: 'Overview', to: '/app', icon: LayoutDashboard },
            { label: 'Users', to: '/users', icon: Users, permission: 'users:read' },
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' }
          ]
        },
        { label: 'Account', items: [{ label: 'Platform info', to: '/settings', icon: Settings }] }
      ];
    case 'SYSTEM_ADMIN':
    default:
      return [
        {
          label: 'Workspace',
          items: [
            { label: 'Overview', to: '/app', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Farmer management',
          items: [
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
            { label: 'GIS', to: '/gis', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Climate intelligence',
          items: [
            { label: 'Climate data', to: '/climate', icon: CloudSun, permission: 'climate:read' },
            { label: 'Risk intelligence', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
            { label: 'Alerts', to: '/climate-intel/alerts', icon: Bell, permission: 'climate-intel:read' }
          ]
        },
        {
          label: 'Satellite intelligence',
          items: [{ label: 'Remote sensing', to: '/satellite', icon: Satellite, permission: 'satellite:read' }]
        },
        {
          label: 'Policy administration',
          items: [
            { label: 'Products', to: '/insurance/products', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Premium pricing', to: '/insurance/calculator', icon: FileText, permission: 'policies:read' },
            { label: 'Reports', to: '/insurance/reports', icon: FileText, permission: 'policies:read' }
          ]
        },
        {
          label: 'Claims and payouts',
          items: [
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' },
            { label: 'Payouts', to: '/settlements', icon: Wallet, permission: 'settlements:read' }
          ]
        },
        {
          label: 'Agricultural lending',
          items: [{ label: 'Insured loans', to: '/lending', icon: Landmark, permission: 'loans:read' }]
        },
        {
          label: 'Administration',
          items: [
            { label: 'Users', to: '/users', icon: Users, permission: 'users:read' },
            { label: 'Platform info', to: '/settings', icon: Settings }
          ]
        }
      ];
  }
}

export function primaryRole(roles: string[] | undefined): string | undefined {
  return roles?.[0];
}

export function portalHeader(roles: string[] | undefined): { eyebrow: string; title: string } {
  const role = primaryRole(roles);
  if (role && HEADER_BY_ROLE[role]) {
    return HEADER_BY_ROLE[role];
  }
  return HEADER_BY_ROLE.SYSTEM_ADMIN;
}

export function buildNavigation({ hasPermission, hasRole, farmerId }: AuthHelpers): NavGroup[] {
  const role = ROLE_ORDER.find((code) => hasRole(code));
  return catalogForRole(role, farmerId)
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => !item.permission || hasPermission(item.permission))
    }))
    .filter((group) => group.items.length > 0);
}
