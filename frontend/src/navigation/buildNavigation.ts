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
  Landmark
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
  'AGGREGATOR',
  'FARMER',
  'AUDITOR',
  'SUPPORT'
] as const;

const HEADER_BY_ROLE: Record<string, { eyebrow: string; title: string }> = {
  SYSTEM_ADMIN: {
    eyebrow: 'Government-grade agricultural insurance',
    title: 'Operations Command Center'
  },
  INSURANCE_ADMIN: {
    eyebrow: 'Insurer administration',
    title: 'Insurance Operations'
  },
  INSURANCE_OFFICER: {
    eyebrow: 'Policy & claims operations',
    title: 'Insurance Workstation'
  },
  FI_OFFICER: {
    eyebrow: 'Settlement & payouts',
    title: 'Financial Operations'
  },
  GOVERNMENT_ANALYST: {
    eyebrow: 'National agricultural insurance',
    title: 'National Overview'
  },
  AGGREGATOR: {
    eyebrow: 'Farmer onboarding',
    title: 'Aggregator Workspace'
  },
  FARMER: {
    eyebrow: 'Farmer self-service',
    title: 'Farmer Portal'
  },
  AUDITOR: {
    eyebrow: 'Read-only assurance',
    title: 'Audit Workspace'
  },
  SUPPORT: {
    eyebrow: 'Operator assistance',
    title: 'Support Desk'
  }
};

function catalogForRole(role: string | undefined, farmerId?: string | null): NavGroup[] {
  switch (role) {
    case 'INSURANCE_ADMIN':
      return [
        {
          label: 'Operations',
          items: [
            { label: 'Overview', to: '/', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Registry',
          items: [
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
            { label: 'GIS', to: '/gis', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Insurance',
          items: [
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Products', to: '/insurance/products', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Premium calc', to: '/insurance/calculator', icon: FileText, permission: 'policies:read' },
            { label: 'Ins. reports', to: '/insurance/reports', icon: FileText, permission: 'policies:read' },
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' }
          ]
        },
        {
          label: 'Climate',
          items: [
            { label: 'Climate data', to: '/climate', icon: CloudSun, permission: 'climate:read' },
            { label: 'Climate intel', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' }
          ]
        },
        {
          label: 'Account',
          items: [{ label: 'Platform info', to: '/settings', icon: Settings }]
        }
      ];
    case 'INSURANCE_OFFICER':
      return [
        {
          label: 'My Work',
          items: [
            { label: 'Overview', to: '/', icon: LayoutDashboard },
            { label: 'My Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Insurance',
          items: [
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Premium calc', to: '/insurance/calculator', icon: FileText, permission: 'policies:read' },
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' },
            { label: 'Ins. reports', to: '/insurance/reports', icon: FileText, permission: 'policies:read' }
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
          label: 'Account',
          items: [{ label: 'Platform info', to: '/settings', icon: Settings }]
        }
      ];
    case 'FI_OFFICER':
      return [
        {
          label: 'My Work',
          items: [
            { label: 'Overview', to: '/', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Financial Operations',
          items: [
            { label: 'Settlements', to: '/settlements', icon: Wallet, permission: 'settlements:read' },
            { label: 'Settle. dash', to: '/settlements/dashboard', icon: LayoutDashboard, permission: 'settlements:read' },
            { label: 'Finance review', to: '/settlements/finance', icon: ClipboardList, permission: 'settlements:approve' },
            { label: 'Settle. reports', to: '/settlements/reports', icon: FileText, permission: 'reports:settlements' },
            { label: 'Ledger', to: '/ledger', icon: Landmark, permission: 'ledger:read' },
            { label: 'Pay providers', to: '/payment-providers', icon: Settings, permission: 'settlements:read' }
          ]
        },
        {
          label: 'Supporting reads',
          items: [
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' },
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' }
          ]
        },
        {
          label: 'Account',
          items: [{ label: 'Platform info', to: '/settings', icon: Settings }]
        }
      ];
    case 'GOVERNMENT_ANALYST':
      return [
        {
          label: 'National Overview',
          items: [
            { label: 'Executive', to: '/', icon: LayoutDashboard },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Agriculture Insights',
          items: [
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
            { label: 'GIS', to: '/gis', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Insurance Insights',
          items: [
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' },
            { label: 'Ins. reports', to: '/insurance/reports', icon: FileText, permission: 'policies:read' }
          ]
        },
        {
          label: 'Climate Intelligence',
          items: [
            { label: 'Climate data', to: '/climate', icon: CloudSun, permission: 'climate:read' },
            { label: 'National risk', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
            { label: 'Alerts', to: '/climate-intel/alerts', icon: Bell, permission: 'climate-intel:read' }
          ]
        },
        {
          label: 'Financial Insights',
          items: [
            { label: 'Settlements', to: '/settlements', icon: Wallet, permission: 'settlements:read' },
            { label: 'Settle. reports', to: '/settlements/reports', icon: FileText, permission: 'reports:settlements' },
            { label: 'Ledger', to: '/ledger', icon: Landmark, permission: 'ledger:read' }
          ]
        },
        {
          label: 'Account',
          items: [{ label: 'Platform info', to: '/settings', icon: Settings }]
        }
      ];
    case 'AGGREGATOR':
      return [
        {
          label: 'My Work',
          items: [
            { label: 'Overview', to: '/', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Onboarding',
          items: [
            { label: 'Households', to: '/households', icon: Users, permission: 'farmers:read' },
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
            { label: 'GIS', to: '/gis', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Cover context',
          items: [{ label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' }]
        },
        {
          label: 'Account',
          items: [{ label: 'Platform info', to: '/settings', icon: Settings }]
        }
      ];
    case 'FARMER':
      return [
        {
          label: 'My Workspace',
          items: [
            { label: 'Home', to: '/', icon: LayoutDashboard },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'My Farm',
          items: [
            { label: 'My profile', to: farmerId ? `/farmers/${farmerId}` : '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'My farms', to: '/farms', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'My Cover',
          items: [
            { label: 'My policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'My claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' }
          ]
        },
        {
          label: 'Account',
          items: [{ label: 'Platform info', to: '/settings', icon: Settings }]
        }
      ];
    case 'AUDITOR':
      return [
        {
          label: 'Audit Desk',
          items: [
            { label: 'Overview', to: '/', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Agriculture',
          items: [
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
            { label: 'GIS', to: '/gis', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Insurance',
          items: [
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' },
            { label: 'Ins. reports', to: '/insurance/reports', icon: FileText, permission: 'policies:read' }
          ]
        },
        {
          label: 'Finance & Ledger',
          items: [
            { label: 'Settlements', to: '/settlements', icon: Wallet, permission: 'settlements:read' },
            { label: 'Settle. reports', to: '/settlements/reports', icon: FileText, permission: 'reports:settlements' },
            { label: 'Ledger', to: '/ledger', icon: Landmark, permission: 'ledger:read' }
          ]
        },
        {
          label: 'Climate',
          items: [
            { label: 'Climate data', to: '/climate', icon: CloudSun, permission: 'climate:read' },
            { label: 'Climate intel', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
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
    case 'SUPPORT':
      return [
        {
          label: 'Support Desk',
          items: [
            { label: 'Overview', to: '/', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'User assistance',
          items: [{ label: 'Users', to: '/users', icon: Users, permission: 'users:read' }]
        },
        {
          label: 'Registry lookup',
          items: [
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Case lookup',
          items: [
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' }
          ]
        },
        {
          label: 'Account',
          items: [{ label: 'Platform info', to: '/settings', icon: Settings }]
        }
      ];
    case 'SYSTEM_ADMIN':
    default:
      return [
        {
          label: 'Operations Command Center',
          items: [
            { label: 'Overview', to: '/', icon: LayoutDashboard },
            { label: 'Tasks', to: '/tasks', icon: ClipboardList, permission: 'tasks:read' },
            { label: 'Notifications', to: '/notifications', icon: Bell, permission: 'notifications:read' }
          ]
        },
        {
          label: 'Agriculture',
          items: [
            { label: 'Households', to: '/households', icon: Users, permission: 'farmers:read' },
            { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
            { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
            { label: 'Crops', to: '/crops', icon: FileText, permission: 'farms:read' },
            { label: 'Seasons', to: '/seasons', icon: FileText, permission: 'farms:read' },
            { label: 'GIS', to: '/gis', icon: Map, permission: 'farms:read' }
          ]
        },
        {
          label: 'Insurance & Claims',
          items: [
            { label: 'Policies', to: '/policies', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Products', to: '/insurance/products', icon: ShieldCheck, permission: 'policies:read' },
            { label: 'Premium calc', to: '/insurance/calculator', icon: FileText, permission: 'policies:read' },
            { label: 'Ins. reports', to: '/insurance/reports', icon: FileText, permission: 'policies:read' },
            { label: 'Claims', to: '/claims', icon: ClipboardList, permission: 'claims:read' }
          ]
        },
        {
          label: 'Settlement',
          items: [
            { label: 'Settlements', to: '/settlements', icon: Wallet, permission: 'settlements:read' },
            { label: 'Settle. dash', to: '/settlements/dashboard', icon: LayoutDashboard, permission: 'settlements:read' },
            { label: 'Finance review', to: '/settlements/finance', icon: ClipboardList, permission: 'settlements:approve' },
            { label: 'Settle. reports', to: '/settlements/reports', icon: FileText, permission: 'reports:settlements' },
            { label: 'Ledger', to: '/ledger', icon: Landmark, permission: 'ledger:read' },
            { label: 'Pay providers', to: '/payment-providers', icon: Settings, permission: 'settlements:read' }
          ]
        },
        {
          label: 'Climate',
          items: [
            { label: 'Climate data', to: '/climate', icon: CloudSun, permission: 'climate:read' },
            { label: 'Climate intel', to: '/climate-intel', icon: LayoutDashboard, permission: 'climate-intel:read' },
            { label: 'Climate alerts', to: '/climate-intel/alerts', icon: Bell, permission: 'climate-intel:read' }
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
