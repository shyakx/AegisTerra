import type { AuthUser } from '../api/auth';
import {
  claims,
  farms,
  farmers,
  households,
  notifications,
  policies,
  tasks
} from './seed';

export {
  alerts,
  claims,
  crops,
  datasets,
  farms,
  farmers,
  households,
  importJobs,
  inputSales,
  kigaliBoundary,
  ledgerEntries,
  loans,
  notifications,
  observations,
  packages,
  policies,
  policyTypes,
  products,
  recommendations,
  riskMap,
  satelliteScenes,
  settlements,
  stationMap,
  stations,
  tasks
} from './seed';

export const ALL_PERMISSIONS = [
  'users:read',
  'users:write',
  'roles:read',
  'permissions:read',
  'farmers:read',
  'farmers:write',
  'farms:read',
  'farms:write',
  'policies:read',
  'policies:write',
  'policies:approve',
  'claims:read',
  'claims:write',
  'claims:assess',
  'settlements:read',
  'settlements:write',
  'settlements:approve',
  'settlements:process',
  'reports:settlements',
  'ledger:read',
  'tasks:read',
  'tasks:act',
  'notifications:read',
  'notifications:write',
  'climate:read',
  'climate:write',
  'climate:import',
  'climate-intel:read',
  'climate-intel:write',
  'climate-intel:admin',
  'alerts:climate',
  'satellite:read',
  'loans:read',
  'loans:write',
  'inputs:read',
  'inputs:write',
  'decisions:act',
  'workflows:read'
];

const READ_NOTIFY = ['notifications:read', 'notifications:write'] as const;

const FARMER_PERMISSIONS = [
  'farmers:read',
  'farms:read',
  'policies:read',
  'claims:read',
  'claims:write',
  ...READ_NOTIFY
];

const INSURANCE_ADMIN_PERMISSIONS = [
  'farmers:read',
  'farmers:write',
  'farms:read',
  'farms:write',
  'policies:read',
  'policies:write',
  'policies:approve',
  'claims:read',
  'claims:write',
  'claims:assess',
  'tasks:read',
  'tasks:act',
  'decisions:act',
  'workflows:read',
  'climate:read',
  'climate-intel:read',
  'alerts:climate',
  'satellite:read',
  ...READ_NOTIFY
];

const INSURANCE_OFFICER_PERMISSIONS = [
  'farmers:read',
  'farms:read',
  'policies:read',
  'policies:write',
  'claims:read',
  'claims:write',
  'claims:assess',
  'tasks:read',
  'tasks:act',
  'decisions:act',
  'workflows:read',
  'climate-intel:read',
  'satellite:read',
  ...READ_NOTIFY
];

const FI_PERMISSIONS = [
  'farmers:read',
  'farms:read',
  'claims:read',
  'policies:read',
  'loans:read',
  'loans:write',
  'settlements:read',
  'settlements:write',
  'settlements:approve',
  'settlements:process',
  'reports:settlements',
  'ledger:read',
  'tasks:read',
  'tasks:act',
  'decisions:act',
  'workflows:read',
  'climate:read',
  'climate-intel:read',
  'alerts:climate',
  'satellite:read',
  ...READ_NOTIFY
];

const GOV_PERMISSIONS = [
  'farmers:read',
  'farms:read',
  'policies:read',
  'claims:read',
  'settlements:read',
  'reports:settlements',
  'ledger:read',
  'climate:read',
  'climate-intel:read',
  'alerts:climate',
  'satellite:read',
  'loans:read',
  ...READ_NOTIFY
];

const AGGREGATOR_PERMISSIONS = [
  'farmers:read',
  'farmers:write',
  'farms:read',
  'farms:write',
  'policies:read',
  'inputs:read',
  'inputs:write',
  'tasks:read',
  'tasks:act',
  ...READ_NOTIFY
];

const PARTNER_PERMISSIONS = [
  'farmers:read',
  'farms:read',
  'policies:read',
  'claims:read',
  'settlements:read',
  'reports:settlements',
  'loans:read',
  'climate:read',
  'climate-intel:read',
  'alerts:climate',
  'satellite:read',
  ...READ_NOTIFY
];

const AUDITOR_PERMISSIONS = [
  'users:read',
  'farmers:read',
  'farms:read',
  'policies:read',
  'claims:read',
  'settlements:read',
  'reports:settlements',
  'ledger:read',
  'tasks:read',
  'climate:read',
  'climate-intel:read',
  'alerts:climate',
  'satellite:read',
  'loans:read',
  'workflows:read',
  ...READ_NOTIFY
];

const SUPPORT_PERMISSIONS = [
  'users:read',
  'farmers:read',
  'farms:read',
  'policies:read',
  'claims:read',
  'tasks:read',
  'tasks:act',
  ...READ_NOTIFY
];

export type OperatorAccount = {
  username: string;
  password: string;
  label: string;
  user: AuthUser;
};

const OPERATOR_PASSWORD = 'Aegis@2026!Aa';

function user(
  id: string,
  username: string,
  displayName: string,
  roles: string[],
  permissions: string[],
  farmerId?: string
): AuthUser {
  return {
    id,
    username,
    email: `${username}@aegisterra.rw`,
    displayName,
    roles,
    permissions,
    mustChangePassword: false,
    farmerId: farmerId ?? null
  };
}

export const OPERATORS: OperatorAccount[] = [
  {
    username: 'admin',
    password: 'Admin@1234!Aa',
    label: 'System administrator',
    user: user('user-admin', 'admin', 'System Administrator', ['SYSTEM_ADMIN'], ALL_PERMISSIONS)
  },
  {
    username: 'insurance.admin',
    password: OPERATOR_PASSWORD,
    label: 'Insurance administrator',
    user: user(
      'user-ins-admin',
      'insurance.admin',
      'Insurance Administrator',
      ['INSURANCE_ADMIN'],
      INSURANCE_ADMIN_PERMISSIONS
    )
  },
  {
    username: 'insurance.officer',
    password: OPERATOR_PASSWORD,
    label: 'Insurance officer',
    user: user(
      'user-ins',
      'insurance.officer',
      'Insurance Officer',
      ['INSURANCE_OFFICER'],
      INSURANCE_OFFICER_PERMISSIONS
    )
  },
  {
    username: 'fi.officer',
    password: OPERATOR_PASSWORD,
    label: 'Lender',
    user: user('user-fi', 'fi.officer', 'Financial Institution Officer', ['FI_OFFICER'], FI_PERMISSIONS)
  },
  {
    username: 'gov.analyst',
    password: OPERATOR_PASSWORD,
    label: 'Government analyst',
    user: user('user-gov', 'gov.analyst', 'Government Analyst', ['GOVERNMENT_ANALYST'], GOV_PERMISSIONS)
  },
  {
    username: 'partner',
    password: OPERATOR_PASSWORD,
    label: 'Development partner',
    user: user('user-partner', 'partner', 'Development Partner', ['DEVELOPMENT_PARTNER'], PARTNER_PERMISSIONS)
  },
  {
    username: 'aggregator',
    password: OPERATOR_PASSWORD,
    label: 'Aggregator',
    user: user('user-agg', 'aggregator', 'Aggregator Officer', ['AGGREGATOR'], AGGREGATOR_PERMISSIONS)
  },
  {
    username: 'farmer1',
    password: OPERATOR_PASSWORD,
    label: 'Farmer',
    user: user('user-farmer', 'farmer1', 'Jean Niyonzima', ['FARMER'], FARMER_PERMISSIONS, 'farmer-001')
  },
  {
    username: 'auditor',
    password: OPERATOR_PASSWORD,
    label: 'Auditor',
    user: user('user-aud', 'auditor', 'Auditor', ['AUDITOR'], AUDITOR_PERMISSIONS)
  },
  {
    username: 'support',
    password: OPERATOR_PASSWORD,
    label: 'Support',
    user: user('user-sup', 'support', 'Support Agent', ['SUPPORT'], SUPPORT_PERMISSIONS)
  }
];

export function page<T>(content: T[], pageNo = 0, size = 20) {
  const start = Math.max(0, pageNo) * size;
  return {
    content: content.slice(start, start + size),
    page: pageNo,
    size,
    totalElements: content.length,
    totalPages: Math.max(1, Math.ceil(content.length / size) || 1)
  };
}

export function hasPermission(user: AuthUser, permission: string) {
  return user.permissions.includes(permission);
}

export function isFarmerUser(user: AuthUser) {
  return user.roles.includes('FARMER');
}

export function scopedFarmers(user: AuthUser) {
  if (!isFarmerUser(user)) {
    return farmers;
  }
  return farmers.filter((f) => f.id === user.farmerId);
}

export function scopedFarms(user: AuthUser) {
  if (!isFarmerUser(user)) {
    return farms;
  }
  return farms.filter((f) => f.farmerId === user.farmerId);
}

export function scopedPolicies(user: AuthUser) {
  if (!isFarmerUser(user)) {
    return policies;
  }
  return policies.filter((p) => p.farmerId === user.farmerId);
}

export function scopedClaims(user: AuthUser) {
  if (!isFarmerUser(user)) {
    return claims;
  }
  return claims.filter((c) => c.farmerId === user.farmerId);
}

export function scopedHouseholds(user: AuthUser) {
  if (!isFarmerUser(user)) {
    return households;
  }
  const ids = new Set(scopedFarmers(user).map((f) => f.householdId));
  return households.filter((h) => ids.has(h.id));
}

const BROAD_TASK_ROLES = ['SYSTEM_ADMIN', 'INSURANCE_ADMIN', 'AUDITOR', 'SUPPORT'];

export function scopedTasks(user: AuthUser) {
  if (user.roles.some((role) => BROAD_TASK_ROLES.includes(role))) {
    return tasks;
  }
  return tasks.filter(
    (task) => user.roles.includes(task.assigneeRoleCode) || task.assigneeUserId === user.id
  );
}

export function scopedNotifications(user: AuthUser) {
  return notifications.filter((item) => {
    if (item.audienceUsernames?.length) {
      return item.audienceUsernames.includes(user.username);
    }
    if (item.audienceRoles?.length) {
      return item.audienceRoles.some((role) => user.roles.includes(role));
    }
    if (isFarmerUser(user)) {
      const farmIds = new Set(scopedFarms(user).map((f) => f.id));
      const policyIds = new Set(scopedPolicies(user).map((p) => p.id));
      const claimIds = new Set(scopedClaims(user).map((c) => c.id));
      return (
        item.subjectId === user.farmerId ||
        farmIds.has(item.subjectId) ||
        policyIds.has(item.subjectId) ||
        claimIds.has(item.subjectId)
      );
    }
    return true;
  });
}
