import type { AuthUser } from '../api/auth';

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
  'decisions:act',
  'workflows:read'
];

const FARMER_PERMISSIONS = [
  'farmers:read',
  'farms:read',
  'policies:read',
  'claims:read',
  'claims:write',
  'notifications:read'
];

const INSURANCE_PERMISSIONS = [
  'farmers:read',
  'farms:read',
  'policies:read',
  'policies:write',
  'policies:approve',
  'claims:read',
  'claims:write',
  'claims:assess',
  'tasks:read',
  'tasks:act',
  'notifications:read',
  'notifications:write',
  'decisions:act',
  'workflows:read',
  'climate:read',
  'climate-intel:read'
];

const FI_PERMISSIONS = [
  'claims:read',
  'policies:read',
  'settlements:read',
  'settlements:approve',
  'settlements:process',
  'reports:settlements',
  'ledger:read',
  'tasks:read',
  'tasks:act',
  'notifications:read',
  'notifications:write',
  'decisions:act',
  'workflows:read'
];

export type OperatorAccount = {
  username: string;
  password: string;
  label: string;
  user: AuthUser;
};

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
    username: 'insurance.officer',
    password: 'Demo@1234!Aa',
    label: 'Insurance officer',
    user: user(
      'user-ins',
      'insurance.officer',
      'Insurance Officer',
      ['INSURANCE_OFFICER'],
      INSURANCE_PERMISSIONS
    )
  },
  {
    username: 'fi.officer',
    password: 'Demo@1234!Aa',
    label: 'Finance officer',
    user: user('user-fi', 'fi.officer', 'Financial Institution Officer', ['FI_OFFICER'], FI_PERMISSIONS)
  },
  {
    username: 'farmer.demo',
    password: 'Demo@1234!Aa',
    label: 'Farmer portal',
    user: user(
      'user-farmer',
      'farmer.demo',
      'Jean Niyonzima',
      ['FARMER'],
      FARMER_PERMISSIONS,
      'farmer-001'
    )
  }
];

export function page<T>(content: T[], pageNo = 0, size = 20) {
  return {
    content,
    page: pageNo,
    size,
    totalElements: content.length,
    totalPages: Math.max(1, Math.ceil(content.length / size) || 1)
  };
}

const kigaliRing = JSON.stringify({
  type: 'Polygon',
  coordinates: [
    [
      [30.061, -1.941],
      [30.068, -1.941],
      [30.068, -1.935],
      [30.061, -1.935],
      [30.061, -1.941]
    ]
  ]
});

export const farmers = [
  {
    id: 'farmer-001',
    farmerCode: 'FRM-2026-001',
    householdId: 'hh-001',
    firstName: 'Jean',
    lastName: 'Niyonzima',
    nationalId: '1199780123456784',
    phoneNumber: '0788000001',
    email: 'jean.niyonzima@aegisterra.rw',
    districtId: 'GASABO',
    villageId: 'KIMIRONKO',
    status: 'ACTIVE'
  },
  {
    id: 'farmer-002',
    farmerCode: 'FRM-2026-002',
    householdId: 'hh-002',
    firstName: 'Claudine',
    lastName: 'Uwase',
    nationalId: '1198680123456785',
    phoneNumber: '0788000002',
    email: 'claudine.uwase@aegisterra.rw',
    districtId: 'MUSANZE',
    villageId: 'MUHOZA',
    status: 'ACTIVE'
  },
  {
    id: 'farmer-003',
    farmerCode: 'FRM-2026-003',
    householdId: 'hh-003',
    firstName: 'Eric',
    lastName: 'Habimana',
    nationalId: '1199480123456786',
    phoneNumber: '0788000003',
    email: 'eric.habimana@aegisterra.rw',
    districtId: 'HUYE',
    villageId: 'NGORA',
    status: 'ACTIVE'
  }
];

export const households = [
  { id: 'hh-001', code: 'HH-2026-001', headName: 'Jean Niyonzima', status: 'ACTIVE' },
  { id: 'hh-002', code: 'HH-2026-002', headName: 'Claudine Uwase', status: 'ACTIVE' },
  { id: 'hh-003', code: 'HH-2026-003', headName: 'Eric Habimana', status: 'ACTIVE' }
];

export const farms = [
  {
    id: 'farm-001',
    farmerId: 'farmer-001',
    farmCode: 'FARM-2026-001',
    farmName: 'Kimironko maize plot',
    farmSizeHa: 1.8,
    cropType: 'Maize',
    status: 'ACTIVE'
  },
  {
    id: 'farm-002',
    farmerId: 'farmer-002',
    farmCode: 'FARM-2026-002',
    farmName: 'Muhoza highland farm',
    farmSizeHa: 2.4,
    cropType: 'Potato',
    status: 'ACTIVE'
  },
  {
    id: 'farm-003',
    farmerId: 'farmer-003',
    farmCode: 'FARM-2026-003',
    farmName: 'Huye southern plot',
    farmSizeHa: 1.2,
    cropType: 'Beans',
    status: 'ACTIVE'
  }
];

export const policies = [
  {
    id: 'policy-001',
    policyNumber: 'POL-2026-001',
    farmerId: 'farmer-001',
    farmId: 'farm-001',
    policyTypeId: 'ptype-maize',
    productId: 'prod-001',
    coveragePackageId: 'pkg-001',
    coverageAmount: 850000,
    premiumAmount: 42000,
    currency: 'RWF',
    startDate: '2026-03-01',
    endDate: '2026-08-31',
    status: 'ACTIVE',
    transitionReason: null
  },
  {
    id: 'policy-002',
    policyNumber: 'POL-2026-002',
    farmerId: 'farmer-002',
    farmId: 'farm-002',
    policyTypeId: 'ptype-potato',
    productId: 'prod-001',
    coveragePackageId: 'pkg-001',
    coverageAmount: 1200000,
    premiumAmount: 61000,
    currency: 'RWF',
    startDate: '2026-03-01',
    endDate: '2026-08-31',
    status: 'ACTIVE',
    transitionReason: null
  }
];

export const claims = [
  {
    id: 'claim-001',
    claimNumber: 'CLM-2026-001',
    policyId: 'policy-001',
    claimTypeCode: 'DROUGHT',
    farmerId: 'farmer-001',
    farmId: 'farm-001',
    seasonId: 'season-a',
    cropId: 'crop-maize',
    causeOfLoss: 'Prolonged dry spell',
    description: 'Maize yield loss after 28 consecutive dry days in Gasabo.',
    claimedAmount: 620000,
    assessedAmount: 540000,
    approvedAmount: 540000,
    currency: 'RWF',
    incidentDate: '2026-06-12',
    status: 'APPROVED',
    coverageSnapshotJson: null,
    financialSnapshotJson: null,
    workflowInstanceId: 'wf-claim-001',
    workflowDefinitionCode: 'CLAIM_STANDARD',
    fraudTier: 'LOW',
    fraudScore: 0.12,
    submittedAt: '2026-06-14T08:00:00Z',
    closedAt: null,
    reasonCode: null,
    correlationId: 'claim-001'
  },
  {
    id: 'claim-002',
    claimNumber: 'CLM-2026-002',
    policyId: 'policy-002',
    claimTypeCode: 'FLOOD',
    farmerId: 'farmer-002',
    farmId: 'farm-002',
    seasonId: 'season-a',
    cropId: 'crop-potato',
    causeOfLoss: 'Flooding',
    description: 'Plot inundation after heavy rainfall in Musanze.',
    claimedAmount: 410000,
    assessedAmount: null,
    approvedAmount: null,
    currency: 'RWF',
    incidentDate: '2026-07-02',
    status: 'SUBMITTED',
    coverageSnapshotJson: null,
    financialSnapshotJson: null,
    workflowInstanceId: 'wf-claim-002',
    workflowDefinitionCode: 'CLAIM_STANDARD',
    fraudTier: null,
    fraudScore: null,
    submittedAt: '2026-07-03T10:00:00Z',
    closedAt: null,
    reasonCode: null,
    correlationId: 'claim-002'
  }
];

export const settlements = [
  {
    id: 'set-001',
    settlementNumber: 'SET-2026-001',
    sourceModule: 'CLAIMS',
    sourceRecordId: 'claim-001',
    sourceReference: 'CLM-2026-001',
    amount: 540000,
    currency: 'RWF',
    exchangeRate: 1,
    paymentMethod: 'MOBILE_MONEY',
    providerCode: 'MTN_MOMO',
    providerReference: 'MOMO-88421',
    beneficiaryName: 'Jean Niyonzima',
    beneficiaryAccount: '0788000001',
    financialSnapshotJson: '{}',
    workflowInstanceId: 'wf-set-001',
    workflowDefinitionCode: 'SETTLEMENT_STANDARD',
    correlationId: 'settlement-801',
    reasonCode: null,
    failureReason: null,
    submittedAt: '2026-07-20T09:00:00Z',
    completedAt: '2026-07-21T11:30:00Z',
    createdAt: '2026-07-20T09:00:00Z',
    status: 'COMPLETED'
  },
  {
    id: 'set-002',
    settlementNumber: 'SET-2026-002',
    sourceModule: 'CLAIMS',
    sourceRecordId: 'claim-002',
    sourceReference: 'CLM-2026-002',
    amount: 410000,
    currency: 'RWF',
    exchangeRate: 1,
    paymentMethod: 'BANK_TRANSFER',
    providerCode: 'BNR_RTGS',
    providerReference: null,
    beneficiaryName: 'Claudine Uwase',
    beneficiaryAccount: '400-221190',
    financialSnapshotJson: '{}',
    workflowInstanceId: 'wf-set-002',
    workflowDefinitionCode: 'SETTLEMENT_STANDARD',
    correlationId: 'settlement-802',
    reasonCode: null,
    failureReason: null,
    submittedAt: '2026-08-01T09:00:00Z',
    completedAt: null,
    createdAt: '2026-08-01T09:00:00Z',
    status: 'PENDING'
  }
];

export const stations = [
  {
    id: 'station-kgl',
    code: 'KGL-01',
    name: 'Kigali Central Station',
    elevationM: 1490,
    providerCode: 'MANUAL',
    externalStationId: 'KGL-01',
    districtCode: 'GASABO',
    longitude: 30.0616,
    latitude: -1.9441,
    status: 'ACTIVE'
  },
  {
    id: 'station-mus',
    code: 'MUS-01',
    name: 'Musanze Highlands Station',
    elevationM: 1850,
    providerCode: 'MANUAL',
    externalStationId: 'MUS-01',
    districtCode: 'MUSANZE',
    longitude: 29.634,
    latitude: -1.498,
    status: 'ACTIVE'
  }
];

export const alerts = [
  {
    id: 'alert-001',
    alertNumber: 'ALT-2026-001',
    alertType: 'DROUGHT',
    severity: 'HIGH',
    scopeType: 'FARM',
    scopeId: 'farm-001',
    validFrom: '2026-06-10T00:00:00Z',
    validTo: '2026-06-30T00:00:00Z',
    ruleVersion: '1.0.0',
    evidenceJson: '{"dryDays":28}',
    status: 'OPEN',
    createdAt: '2026-06-10T06:00:00Z'
  }
];

export const tasks = [
  {
    id: 'task-001',
    instanceId: 'wf-claim-002',
    stepCode: 'VALIDATION',
    taskType: 'VERIFICATION',
    title: 'Validate flood claim intake',
    description: 'Re-validate flood claim CLM-2026-002 after returned evidence checklist.',
    subjectType: 'CLAIM',
    subjectId: 'claim-002',
    assigneeUserId: null,
    assigneeRoleCode: 'INSURANCE_OFFICER',
    priority: 90,
    dueAt: '2026-08-22T00:00:00Z',
    status: 'PENDING',
    outcome: null,
    completedAt: null,
    createdAt: '2026-08-18T08:00:00Z',
    comments: [],
    timeline: []
  },
  {
    id: 'task-002',
    instanceId: 'wf-set-002',
    stepCode: 'FINANCE_REVIEW',
    taskType: 'REVIEW',
    title: 'Finance review for pending settlement',
    description: 'Finance review for pending settlement SET-2026-002 before approval.',
    subjectType: 'SETTLEMENT',
    subjectId: 'set-002',
    assigneeUserId: null,
    assigneeRoleCode: 'FI_OFFICER',
    priority: 80,
    dueAt: '2026-08-23T00:00:00Z',
    status: 'PENDING',
    outcome: null,
    completedAt: null,
    createdAt: '2026-08-18T09:00:00Z',
    comments: [],
    timeline: []
  }
];

export const notifications = [
  {
    id: 'ntf-001',
    channel: 'IN_APP',
    title: 'Drought alert for farm FARM-2026-001',
    body: 'Climate Intelligence opened alert ALT-2026-001 for farm FARM-2026-001.',
    eventType: 'CLIMATE_ALERT',
    subjectType: 'FARM',
    subjectId: 'farm-001',
    correlationId: null,
    templateCode: null,
    sentAt: '2026-08-18T07:00:00Z',
    readAt: null,
    status: 'SENT',
    createdAt: '2026-08-18T07:00:00Z'
  },
  {
    id: 'ntf-002',
    channel: 'IN_APP',
    title: 'Claim submitted for validation',
    body: 'Claim CLM-2026-001 was submitted and awaits inspection and assessment.',
    eventType: 'CLAIM',
    subjectType: 'CLAIM',
    subjectId: 'claim-001',
    correlationId: null,
    templateCode: null,
    sentAt: '2026-08-18T07:10:00Z',
    readAt: null,
    status: 'SENT',
    createdAt: '2026-08-18T07:10:00Z'
  }
];

export const ledgerEntries = [
  {
    id: 'le-001',
    ledgerTransactionId: 'lt-001',
    settlementId: 'set-001',
    entryNo: 1,
    entryType: 'DEBIT',
    accountCode: 'PAYABLE.CLAIMS',
    amount: 540000,
    currency: 'RWF',
    narration: 'Debit payable for SET-2026-001',
    postedAt: '2026-07-21T11:30:00Z',
    status: 'POSTED'
  },
  {
    id: 'le-002',
    ledgerTransactionId: 'lt-001',
    settlementId: 'set-001',
    entryNo: 2,
    entryType: 'CREDIT',
    accountCode: 'CASH.MOBILE',
    amount: 540000,
    currency: 'RWF',
    narration: 'Credit cash for SET-2026-001',
    postedAt: '2026-07-21T11:30:00Z',
    status: 'POSTED'
  }
];

export const products = [
  {
    id: 'prod-001',
    code: 'MAIZE-INDEX-2026',
    name: 'National maize weather index',
    description: 'Seasonal drought and excess-rain cover for maize.',
    pricingStrategyCode: 'AREA_YIELD',
    status: 'ACTIVE'
  }
];

export const packages = [
  {
    id: 'pkg-001',
    productId: 'prod-001',
    policyTypeId: 'ptype-maize',
    code: 'STD-80',
    name: 'Standard 80% coverage',
    coverageLevelPct: 80,
    maxSumInsured: 1500000,
    status: 'ACTIVE'
  }
];

export const kigaliBoundary = kigaliRing;

export function stationMap() {
  return {
    type: 'FeatureCollection',
    features: stations.map((s) => ({
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [s.longitude, s.latitude] },
      properties: { id: s.id, code: s.code, name: s.name, status: s.status }
    }))
  };
}

export function riskMap() {
  return {
    type: 'FeatureCollection',
    features: farms.map((f, i) => ({
      type: 'Feature',
      geometry: {
        type: 'Point',
        coordinates: i === 0 ? [30.064, -1.938] : i === 1 ? [29.64, -1.5] : [29.74, -2.6]
      },
      properties: { farmId: f.id, farmCode: f.farmCode, grade: i === 0 ? 'HIGH' : 'MODERATE' }
    }))
  };
}
