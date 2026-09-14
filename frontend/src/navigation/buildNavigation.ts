import type { LucideIcon } from 'lucide-react';
import {
  LayoutDashboard,
  Map,
  Users,
  Settings,
  Bell,
  CloudSun,
  TrendingUp
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
    eyebrow: 'Climate & yield intelligence',
    title: 'Plan ahead from past and current data'
  },
  INSURANCE_ADMIN: {
    eyebrow: 'Insurer planning',
    title: 'See climate risk and yield outlook before loss'
  },
  INSURANCE_OFFICER: {
    eyebrow: 'Insurer planning',
    title: 'Portfolio risk from climate and farms'
  },
  FI_OFFICER: {
    eyebrow: 'Lender planning',
    title: 'Borrower climate and yield exposure'
  },
  GOVERNMENT_ANALYST: {
    eyebrow: 'Government',
    title: 'National climate and production outlook'
  },
  DEVELOPMENT_PARTNER: {
    eyebrow: 'Development partner',
    title: 'Programme climate and yield intelligence'
  },
  AGGREGATOR: {
    eyebrow: 'Aggregator',
    title: 'Network farms and climate outlook'
  },
  FARMER: {
    eyebrow: 'My farm',
    title: 'What the climate means for my crops'
  },
  AUDITOR: {
    eyebrow: 'Assurance',
    title: 'Read-only climate and registry view'
  },
  SUPPORT: {
    eyebrow: 'Support',
    title: 'Help operators use planning tools'
  }
};

/** ADR-010: single intelligence-first catalog for all roles. */
function catalogForRole(role: string | undefined, farmerId?: string | null): NavGroup[] {
  if (role === 'FARMER') {
    return [
      {
        label: 'My workspace',
        items: [
          { label: 'Home', to: '/app', icon: LayoutDashboard },
          { label: 'Planning outlook', to: '/planning', icon: TrendingUp },
          { label: 'Alerts', to: '/climate-intel/alerts', icon: Bell, permission: 'climate-intel:read' }
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
      { label: 'Account', items: [{ label: 'Platform info', to: '/settings', icon: Settings }] }
    ];
  }

  return [
    {
      label: 'Workspace',
      items: [
        { label: 'Overview', to: '/app', icon: LayoutDashboard },
        { label: 'Planning outlook', to: '/planning', icon: TrendingUp }
      ]
    },
    {
      label: 'Registry',
      items: [
        { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
        { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' }
      ]
    },
    {
      label: 'Climate & risk',
      items: [
        { label: 'Climate data', to: '/climate', icon: CloudSun, permission: 'climate:read' },
        { label: 'Risk intelligence', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
        { label: 'Alerts', to: '/climate-intel/alerts', icon: Bell, permission: 'climate-intel:read' }
      ]
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
