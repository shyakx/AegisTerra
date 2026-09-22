import type { LucideIcon } from 'lucide-react';
import {
  LayoutDashboard,
  Map,
  Users,
  Settings,
  Bell,
  CloudSun,
  Building2,
  Landmark,
  Sprout,
  FileBarChart,
  Wallet,
  ScrollText,
  Brain,
  UserCircle
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
    eyebrow: 'System administrator',
    title: 'National climate, partners, and cover operations'
  },
  INSURANCE_ADMIN: {
    eyebrow: 'Insurance company',
    title: 'Zones, policies, and payout notices'
  },
  INSURANCE_OFFICER: {
    eyebrow: 'Authorized insurance officer',
    title: 'Zones, policies, and payout notices'
  },
  FI_OFFICER: {
    eyebrow: 'Authorized bank officer',
    title: 'Farmers, loans, policies, and payouts'
  },
  GOVERNMENT_ANALYST: {
    eyebrow: 'Government officer',
    title: 'Climate, zones, and analytical reports'
  },
  DEVELOPMENT_PARTNER: {
    eyebrow: 'Development partner',
    title: 'Programme climate and yield intelligence'
  },
  AGGREGATOR: {
    eyebrow: 'Authorized aggregator officer',
    title: 'Network farms and climate outlook'
  },
  FARMER: {
    eyebrow: 'Farmer',
    title: 'Status, payouts, and settings'
  },
  AUDITOR: {
    eyebrow: 'Auditor',
    title: 'Read-only climate and reports'
  },
  SUPPORT: {
    eyebrow: 'Support',
    title: 'Help operators use the platform'
  }
};

function settingsItem(): NavItem {
  return { label: 'Setting', to: '/settings', icon: Settings };
}

/** Owner admin sidebar (+ System intelligence). */
function adminCatalog(): NavGroup[] {
  return [
    {
      label: 'Workspace',
      items: [
        { label: 'Dashboard', to: '/app', icon: LayoutDashboard },
        { label: 'Climate', to: '/climate-hub', icon: CloudSun, permission: 'climate:read' },
        {
          label: 'System intelligence',
          to: '/system-intelligence',
          icon: Brain,
          permission: 'climate-intel:read'
        },
        {
          label: 'Agro-ecological zones',
          to: '/agroecological-zones',
          icon: Map,
          permission: 'farmers:read'
        }
      ]
    },
    {
      label: 'Partners',
      items: [
        {
          label: 'Financial institutions',
          to: '/directories/financial-institutions',
          icon: Landmark
        },
        {
          label: 'Insurance companies',
          to: '/directories/insurance-companies',
          icon: Building2
        },
        {
          label: 'Agricultural aggregators',
          to: '/directories/aggregators',
          icon: Sprout
        }
      ]
    },
    {
      label: 'Operations',
      items: [
        { label: 'Analytical reports', to: '/reports', icon: FileBarChart },
        { label: 'Payouts', to: '/payouts', icon: Wallet, permission: 'settlements:read' },
        { label: 'Policies', to: '/policies', icon: ScrollText, permission: 'policies:read' },
        { label: 'Users', to: '/users', icon: Users, permission: 'users:read' },
        settingsItem()
      ]
    }
  ];
}

function insuranceCatalog(): NavGroup[] {
  return [
    {
      label: 'Insurance workspace',
      items: [
        {
          label: 'Agro-ecological zones',
          to: '/agroecological-zones',
          icon: Map,
          permission: 'farmers:read'
        },
        {
          label: 'Payouts notification',
          to: '/payouts',
          icon: Bell,
          permission: 'settlements:read'
        },
        { label: 'Policy', to: '/policies', icon: ScrollText, permission: 'policies:read' },
        settingsItem()
      ]
    }
  ];
}

function bankCatalog(): NavGroup[] {
  return [
    {
      label: 'Bank workspace',
      items: [
        {
          label: 'Farmers & loans',
          to: '/lending',
          icon: Users,
          permission: 'loans:read'
        },
        {
          label: 'Payouts triggered',
          to: '/payouts',
          icon: Wallet,
          permission: 'settlements:read'
        },
        { label: 'Policy', to: '/policies', icon: ScrollText, permission: 'policies:read' },
        settingsItem()
      ]
    }
  ];
}

function farmerCatalog(): NavGroup[] {
  return [
    {
      label: 'My workspace',
      items: [
        {
          label: 'Farmer status',
          to: '/farmer-status',
          icon: UserCircle,
          permission: 'farmers:read'
        },
        { label: 'Payouts', to: '/payouts', icon: Wallet, permission: 'settlements:read' },
        { label: 'Settings', to: '/settings', icon: Settings }
      ]
    }
  ];
}

function aggregatorCatalog(): NavGroup[] {
  return [
    {
      label: 'Aggregator workspace',
      items: [
        { label: 'Dashboard', to: '/app', icon: LayoutDashboard },
        { label: 'Climate', to: '/climate-hub', icon: CloudSun, permission: 'climate:read' },
        {
          label: 'Agro-ecological zones',
          to: '/agroecological-zones',
          icon: Map,
          permission: 'farmers:read'
        },
        { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
        { label: 'Farms', to: '/farms', icon: Map, permission: 'farms:read' },
        settingsItem()
      ]
    }
  ];
}

function governmentCatalog(): NavGroup[] {
  return [
    {
      label: 'Government workspace',
      items: [
        { label: 'Dashboard', to: '/app', icon: LayoutDashboard },
        { label: 'Climate', to: '/climate-hub', icon: CloudSun, permission: 'climate:read' },
        {
          label: 'Agro-ecological zones',
          to: '/agroecological-zones',
          icon: Map,
          permission: 'farmers:read'
        },
        { label: 'Analytical reports', to: '/reports', icon: FileBarChart },
        settingsItem()
      ]
    }
  ];
}

function auditorCatalog(): NavGroup[] {
  return [
    {
      label: 'Assurance',
      items: [
        { label: 'Dashboard', to: '/app', icon: LayoutDashboard },
        { label: 'Climate', to: '/climate-hub', icon: CloudSun, permission: 'climate:read' },
        {
          label: 'Agro-ecological zones',
          to: '/agroecological-zones',
          icon: Map,
          permission: 'farmers:read'
        },
        { label: 'Analytical reports', to: '/reports', icon: FileBarChart },
        { label: 'Policies', to: '/policies', icon: ScrollText, permission: 'policies:read' },
        { label: 'Payouts', to: '/payouts', icon: Wallet, permission: 'settlements:read' },
        settingsItem()
      ]
    }
  ];
}

function partnerCatalog(): NavGroup[] {
  return governmentCatalog();
}

function supportCatalog(): NavGroup[] {
  return [
    {
      label: 'Support',
      items: [
        { label: 'Dashboard', to: '/app', icon: LayoutDashboard },
        { label: 'Users', to: '/users', icon: Users, permission: 'users:read' },
        { label: 'Farmers', to: '/farmers', icon: Users, permission: 'farmers:read' },
        settingsItem()
      ]
    }
  ];
}

function catalogForRole(role: string | undefined): NavGroup[] {
  switch (role) {
    case 'SYSTEM_ADMIN':
      return adminCatalog();
    case 'INSURANCE_ADMIN':
    case 'INSURANCE_OFFICER':
      return insuranceCatalog();
    case 'FI_OFFICER':
      return bankCatalog();
    case 'FARMER':
      return farmerCatalog();
    case 'AGGREGATOR':
      return aggregatorCatalog();
    case 'GOVERNMENT_ANALYST':
      return governmentCatalog();
    case 'AUDITOR':
      return auditorCatalog();
    case 'DEVELOPMENT_PARTNER':
      return partnerCatalog();
    case 'SUPPORT':
      return supportCatalog();
    default:
      return adminCatalog();
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

export function buildNavigation({ hasPermission, hasRole }: AuthHelpers): NavGroup[] {
  const role = ROLE_ORDER.find((code) => hasRole(code));
  return catalogForRole(role)
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => !item.permission || hasPermission(item.permission))
    }))
    .filter((group) => group.items.length > 0);
}
