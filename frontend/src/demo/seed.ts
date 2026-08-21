function pad(n: number) {
  return String(n).padStart(3, '0');
}

const DISTRICTS = [
  { code: 'GASABO', village: 'KIMIRONKO', name: 'Kigali Central', lon: 30.0616, lat: -1.9441, elev: 1490 },
  { code: 'MUSANZE', village: 'MUHOZA', name: 'Musanze Highlands', lon: 29.634, lat: -1.498, elev: 1850 },
  { code: 'HUYE', village: 'NGORA', name: 'Huye Southern', lon: 29.739, lat: -2.596, elev: 1768 },
  { code: 'NYAGATARE', village: 'NYAGATARE', name: 'Nyagatare Plains', lon: 30.326, lat: -1.298, elev: 1350 },
  { code: 'RUBAVU', village: 'GISENYI', name: 'Rubavu Lakeside', lon: 29.256, lat: -1.702, elev: 1550 },
  { code: 'KAYONZA', village: 'MUKARANGE', name: 'Kayonza East', lon: 30.507, lat: -1.93, elev: 1480 },
  { code: 'RUHANGO', village: 'BYIMANA', name: 'Ruhango Central', lon: 29.78, lat: -2.21, elev: 1620 },
  { code: 'BUGESERA', village: 'NYAMATA', name: 'Bugesera Lowlands', lon: 30.084, lat: -2.15, elev: 1400 },
  { code: 'RWAMAGANA', village: 'RWAMAGANA', name: 'Rwamagana Plateau', lon: 30.435, lat: -1.949, elev: 1520 },
  { code: 'GICUMBI', village: 'BYUMBA', name: 'Gicumbi North', lon: 30.064, lat: -1.576, elev: 1880 },
  { code: 'RUSIZI', village: 'KAMEMBE', name: 'Rusizi Valley', lon: 28.907, lat: -2.483, elev: 1460 },
  { code: 'NYANZA', village: 'BUSASAMANA', name: 'Nyanza South', lon: 29.751, lat: -2.352, elev: 1700 }
] as const;

const CROPS = [
  { name: 'Maize', typeId: 'ptype-maize', cropId: 'crop-maize' },
  { name: 'Potato', typeId: 'ptype-potato', cropId: 'crop-potato' },
  { name: 'Beans', typeId: 'ptype-beans', cropId: 'crop-beans' },
  { name: 'Rice', typeId: 'ptype-rice', cropId: 'crop-rice' },
  { name: 'Coffee', typeId: 'ptype-coffee', cropId: 'crop-coffee' },
  { name: 'Banana', typeId: 'ptype-banana', cropId: 'crop-banana' },
  { name: 'Sorghum', typeId: 'ptype-sorghum', cropId: 'crop-sorghum' },
  { name: 'Tea', typeId: 'ptype-tea', cropId: 'crop-tea' }
] as const;

const NAMED_FARMERS: [string, string][] = [
  ['Jean', 'Niyonzima'],
  ['Claudine', 'Uwase'],
  ['Eric', 'Habimana'],
  ['Aline', 'Mukamana'],
  ['Bosco', 'Ndayisaba'],
  ['Grace', 'Ingabire'],
  ['Patrick', 'Hakizimana'],
  ['Diane', 'Uwimana']
];

const FIRST = [
  'Emmanuel',
  'Immaculee',
  'Theogene',
  'Jeanne',
  'Pacifique',
  'Solange',
  'Fabrice',
  'Chantal',
  'Innocent',
  'Vestine',
  'Olivier',
  'Alice',
  'Samuel',
  'Beatrice',
  'Jacques',
  'Delphine'
];

const LAST = [
  'Bizimana',
  'Mukeshimana',
  'Nsengimana',
  'Uwimana',
  'Nkurunziza',
  'Iradukunda',
  'Mugisha',
  'Nyirahabimana',
  'Habyarimana',
  'Mukamuganga',
  'Twagirimana',
  'Uwera'
];

const POLICY_STATUSES = [
  'ACTIVE',
  'ACTIVE',
  'ACTIVE',
  'UNDER_REVIEW',
  'ACTIVE',
  'DRAFT',
  'ACTIVE',
  'EXPIRED',
  'ACTIVE',
  'PREMIUM_PENDING'
];

const CLAIM_STATUSES = [
  'APPROVED',
  'SUBMITTED',
  'PAYMENT_PENDING',
  'CLOSED',
  'UNDER_ASSESSMENT',
  'RETURNED_FOR_INFO',
  'APPROVED',
  'REJECTED'
];

const CLAIM_TYPES = [
  { code: 'DROUGHT', cause: 'Prolonged dry spell', crop: 'crop-maize' },
  { code: 'FLOOD', cause: 'Plot inundation', crop: 'crop-potato' },
  { code: 'HAIL', cause: 'Hailstorm', crop: 'crop-beans' },
  { code: 'DROUGHT', cause: 'Irrigation deficit', crop: 'crop-rice' },
  { code: 'PEST', cause: 'Fall armyworm outbreak', crop: 'crop-maize' },
  { code: 'EXCESS_RAIN', cause: 'Consecutive wet days', crop: 'crop-coffee' }
];

const FARMER_COUNT = 60;
const POLICY_COUNT = 45;
const CLAIM_INDEXES = [1, 2, 3, 4, 6, 8, 9, 11, 12, 14, 15, 17, 18, 20, 22, 24, 25, 28, 30, 33, 36, 40];

function nameFor(n: number): [string, string] {
  if (n <= NAMED_FARMERS.length) {
    return NAMED_FARMERS[n - 1];
  }
  const i = n - 1;
  return [FIRST[i % FIRST.length], LAST[(i * 3) % LAST.length]];
}

function districtFor(n: number) {
  return DISTRICTS[(n - 1) % DISTRICTS.length];
}

function cropFor(n: number) {
  return CROPS[(n - 1) % CROPS.length];
}

export const kigaliBoundary = JSON.stringify({
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

export const farmers = Array.from({ length: FARMER_COUNT }, (_, i) => {
  const n = i + 1;
  const [first, last] = nameFor(n);
  const district = districtFor(n);
  const statuses = n % 17 === 0 ? 'SUSPENDED' : n % 13 === 0 ? 'PENDING_VERIFICATION' : 'ACTIVE';
  return {
    id: `farmer-${pad(n)}`,
    farmerCode: `FRM-2026-${pad(n)}`,
    householdId: `hh-${pad(n)}`,
    firstName: first,
    lastName: last,
    nationalId: `1199${String(780123456000 + n).slice(-12)}`,
    phoneNumber: `0788${String(100000 + n).slice(-6)}`,
    email: `${first.toLowerCase()}.${last.toLowerCase()}${n}@aegisterra.rw`,
    districtId: district.code,
    villageId: district.village,
    status: statuses
  };
});

export const households = farmers.map((f, i) => ({
  id: f.householdId,
  code: `HH-2026-${pad(i + 1)}`,
  headName: `${f.firstName} ${f.lastName}`,
  districtId: f.districtId,
  status: f.status
}));

export const farms = farmers.map((f, i) => {
  const n = i + 1;
  const crop = cropFor(n);
  const district = districtFor(n);
  return {
    id: `farm-${pad(n)}`,
    farmerId: f.id,
    farmCode: `FARM-2026-${pad(n)}`,
    farmName: `${district.village.charAt(0) + district.village.slice(1).toLowerCase()} ${crop.name.toLowerCase()} holding`,
    farmSizeHa: Number((1.1 + (n % 9) * 0.35).toFixed(2)),
    cropType: crop.name,
    districtId: district.code,
    longitude: Number((district.lon + ((n % 5) - 2) * 0.018).toFixed(4)),
    latitude: Number((district.lat + ((Math.floor(n / 5) % 5) - 2) * 0.012).toFixed(4)),
    status: f.status === 'SUSPENDED' ? 'SUSPENDED' : 'ACTIVE'
  };
});

export const policies = Array.from({ length: POLICY_COUNT }, (_, i) => {
  const n = i + 1;
  const crop = cropFor(n);
  const status = POLICY_STATUSES[(n - 1) % POLICY_STATUSES.length];
  return {
    id: `policy-${pad(n)}`,
    policyNumber: `POL-2026-${pad(n)}`,
    farmerId: `farmer-${pad(n)}`,
    farmId: `farm-${pad(n)}`,
    policyTypeId: crop.typeId,
    productId: n % 4 === 0 ? 'prod-002' : 'prod-001',
    coveragePackageId: n % 4 === 0 ? 'pkg-002' : 'pkg-001',
    coverageAmount: 380000 + n * 28000,
    premiumAmount: 18000 + n * 1400,
    currency: 'RWF',
    startDate: '2026-03-01',
    endDate: '2026-08-31',
    status,
    transitionReason: null
  };
});

export const claims = CLAIM_INDEXES.filter((n) => n <= POLICY_COUNT).map((n, i) => {
  const kind = CLAIM_TYPES[i % CLAIM_TYPES.length];
  const status = CLAIM_STATUSES[i % CLAIM_STATUSES.length];
  const claimed = 210000 + n * 18000;
  const assessed = status === 'SUBMITTED' || status === 'RETURNED_FOR_INFO' ? null : Math.round(claimed * 0.82);
  return {
    id: `claim-${pad(n)}`,
    claimNumber: `CLM-2026-${pad(n)}`,
    policyId: `policy-${pad(n)}`,
    claimTypeCode: kind.code,
    farmerId: `farmer-${pad(n)}`,
    farmId: `farm-${pad(n)}`,
    seasonId: 'season-a',
    cropId: kind.crop,
    causeOfLoss: kind.cause,
    description: `${kind.cause} reported on farm FARM-2026-${pad(n)} in ${districtFor(n).code}.`,
    claimedAmount: claimed,
    assessedAmount: assessed,
    approvedAmount: status === 'APPROVED' || status === 'PAYMENT_PENDING' || status === 'CLOSED' ? assessed : null,
    currency: 'RWF',
    incidentDate: `2026-${String(5 + (i % 3)).padStart(2, '0')}-${String(8 + (i % 20)).padStart(2, '0')}`,
    status,
    coverageSnapshotJson: null,
    financialSnapshotJson: null,
    workflowInstanceId: `wf-claim-${pad(n)}`,
    workflowDefinitionCode: 'CLAIM_STANDARD',
    fraudTier: status === 'REJECTED' ? 'HIGH' : 'LOW',
    fraudScore: status === 'REJECTED' ? 0.71 : 0.08 + (i % 5) * 0.03,
    submittedAt: `2026-0${5 + (i % 3)}-${String(10 + (i % 18)).padStart(2, '0')}T08:00:00Z`,
    closedAt: status === 'CLOSED' || status === 'REJECTED' ? `2026-07-${String(8 + (i % 18)).padStart(2, '0')}T16:00:00Z` : null,
    reasonCode: status === 'REJECTED' ? 'INSUFFICIENT_EVIDENCE' : null,
    correlationId: `claim-${pad(n)}`
  };
});

const SETTLEMENT_CLAIMS = claims.filter((c) =>
  ['APPROVED', 'PAYMENT_PENDING', 'CLOSED'].includes(c.status)
);

export const settlements = SETTLEMENT_CLAIMS.map((claim, i) => {
  const farmer = farmers.find((f) => f.id === claim.farmerId);
  const status = claim.status === 'CLOSED' ? 'COMPLETED' : i % 4 === 1 ? 'PENDING' : i % 4 === 2 ? 'APPROVED' : 'COMPLETED';
  const method = i % 3 === 1 ? 'BANK_TRANSFER' : i % 3 === 2 ? 'MOBILE_MONEY' : 'MOBILE_MONEY';
  const provider = method === 'BANK_TRANSFER' ? 'BNR_RTGS' : i % 2 === 0 ? 'MTN_MOMO' : 'AIRTEL_MONEY';
  const amount = claim.approvedAmount ?? claim.claimedAmount;
  return {
    id: `set-${pad(i + 1)}`,
    settlementNumber: `SET-2026-${pad(i + 1)}`,
    sourceModule: 'CLAIMS',
    sourceRecordId: claim.id,
    sourceReference: claim.claimNumber,
    amount,
    currency: 'RWF',
    exchangeRate: 1,
    paymentMethod: method,
    providerCode: provider,
    providerReference: status === 'COMPLETED' ? `${provider}-${88000 + i}` : null,
    beneficiaryName: farmer ? `${farmer.firstName} ${farmer.lastName}` : 'Beneficiary',
    beneficiaryAccount: farmer?.phoneNumber ?? '0788000000',
    financialSnapshotJson: '{}',
    workflowInstanceId: `wf-set-${pad(i + 1)}`,
    workflowDefinitionCode: 'SETTLEMENT_STANDARD',
    correlationId: `settlement-${800 + i}`,
    reasonCode: null,
    failureReason: null,
    submittedAt: `2026-07-${String(10 + (i % 18)).padStart(2, '0')}T09:00:00Z`,
    completedAt: status === 'COMPLETED' ? `2026-07-${String(12 + (i % 16)).padStart(2, '0')}T11:30:00Z` : null,
    createdAt: `2026-07-${String(10 + (i % 18)).padStart(2, '0')}T09:00:00Z`,
    status
  };
});

export const stations = DISTRICTS.map((d, i) => {
    const codes = [
      'KGL-01',
      'MUS-01',
      'HUE-01',
      'NYG-01',
      'RBV-01',
      'KYZ-01',
      'RHG-01',
      'BGS-01',
      'RWG-01',
      'GCM-01',
      'RSZ-01',
      'NYZ-01'
    ];
    const code = codes[i] ?? `${d.code.slice(0, 3)}-01`;
  return {
    id: `station-${d.code.toLowerCase()}`,
    code,
    name: `${d.name} Station`,
    elevationM: d.elev,
    providerCode: 'MANUAL',
    externalStationId: code,
    districtCode: d.code,
    longitude: d.lon,
    latitude: d.lat,
    status: i === 10 ? 'MAINTENANCE' : 'ACTIVE'
  };
});

const ALERT_TYPES = ['DROUGHT', 'FLOOD', 'HAIL', 'HEAT', 'EXCESS_RAIN'] as const;
const ALERT_SEVERITY = ['HIGH', 'MODERATE', 'CRITICAL', 'LOW'] as const;
const ALERT_STATUS = ['OPEN', 'OPEN', 'ACKNOWLEDGED', 'CLOSED'] as const;

export const alerts = Array.from({ length: 20 }, (_, i) => {
  const n = (i % FARMER_COUNT) + 1;
  return {
    id: `alert-${pad(i + 1)}`,
    alertNumber: `ALT-2026-${pad(i + 1)}`,
    alertType: ALERT_TYPES[i % ALERT_TYPES.length],
    severity: ALERT_SEVERITY[i % ALERT_SEVERITY.length],
    scopeType: i % 5 === 0 ? 'DISTRICT' : 'FARM',
    scopeId: i % 5 === 0 ? districtFor(n).code : `farm-${pad(n)}`,
    validFrom: `2026-0${5 + (i % 3)}-${String(4 + (i % 20)).padStart(2, '0')}T00:00:00Z`,
    validTo: `2026-0${6 + (i % 2)}-${String(10 + (i % 18)).padStart(2, '0')}T00:00:00Z`,
    ruleVersion: '1.0.0',
    evidenceJson: `{"indicator":${12 + i}}`,
    status: ALERT_STATUS[i % ALERT_STATUS.length],
    createdAt: `2026-0${5 + (i % 3)}-${String(4 + (i % 20)).padStart(2, '0')}T06:00:00Z`
  };
});

export const tasks = [
  ...claims
    .filter((c) => ['SUBMITTED', 'UNDER_ASSESSMENT', 'RETURNED_FOR_INFO'].includes(c.status))
    .map((c, i) => ({
      id: `task-claim-${c.id}`,
      instanceId: c.workflowInstanceId,
      stepCode: c.status === 'UNDER_ASSESSMENT' ? 'ASSESSMENT' : 'VALIDATION',
      taskType: c.status === 'UNDER_ASSESSMENT' ? 'ASSESSMENT' : 'VERIFICATION',
      title: `Review claim ${c.claimNumber}`,
      description: `${c.causeOfLoss} — ${c.description}`,
      subjectType: 'CLAIM',
      subjectId: c.id,
      assigneeUserId: null,
      assigneeRoleCode: 'INSURANCE_OFFICER',
      priority: 70 + (i % 25),
      dueAt: `2026-08-${String(20 + (i % 8)).padStart(2, '0')}T00:00:00Z`,
      status: i % 4 === 0 ? 'IN_PROGRESS' : 'PENDING',
      outcome: null,
      completedAt: null,
      createdAt: '2026-08-18T08:00:00Z',
      comments: [],
      timeline: []
    })),
  ...settlements
    .filter((s) => s.status === 'PENDING' || s.status === 'APPROVED')
    .map((s, i) => ({
      id: `task-set-${s.id}`,
      instanceId: s.workflowInstanceId,
      stepCode: s.status === 'APPROVED' ? 'DISBURSE' : 'FINANCE_REVIEW',
      taskType: s.status === 'APPROVED' ? 'PROCESS' : 'REVIEW',
      title: `${s.status === 'APPROVED' ? 'Disburse' : 'Review'} settlement ${s.settlementNumber}`,
      description: `Finance action required for ${s.sourceReference} (${s.amount.toLocaleString()} RWF).`,
      subjectType: 'SETTLEMENT',
      subjectId: s.id,
      assigneeUserId: null,
      assigneeRoleCode: 'FI_OFFICER',
      priority: 75 + (i % 20),
      dueAt: `2026-08-${String(21 + (i % 6)).padStart(2, '0')}T00:00:00Z`,
      status: 'PENDING',
      outcome: null,
      completedAt: null,
      createdAt: '2026-08-18T09:00:00Z',
      comments: [],
      timeline: []
    })),
  ...policies
    .filter((p) => p.status === 'UNDER_REVIEW')
    .slice(0, 4)
    .map((p, i) => ({
      id: `task-pol-${p.id}`,
      instanceId: `wf-${p.id}`,
      stepCode: 'UNDERWRITE',
      taskType: 'REVIEW',
      title: `Underwrite policy ${p.policyNumber}`,
      description: `Coverage review for ${p.policyNumber}.`,
      subjectType: 'POLICY',
      subjectId: p.id,
      assigneeUserId: null,
      assigneeRoleCode: 'INSURANCE_ADMIN',
      priority: 60 + i,
      dueAt: `2026-08-2${5 + i}T00:00:00Z`,
      status: 'PENDING',
      outcome: null,
      completedAt: null,
      createdAt: '2026-08-17T08:00:00Z',
      comments: [],
      timeline: []
    })),
  ...farmers
    .filter((f) => f.status === 'PENDING_VERIFICATION')
    .slice(0, 5)
    .map((f, i) => ({
      id: `task-reg-${f.id}`,
      instanceId: `wf-${f.id}`,
      stepCode: 'ONBOARD',
      taskType: 'VERIFICATION',
      title: `Complete onboarding for ${f.farmerCode}`,
      description: `Registry follow-up for ${f.firstName} ${f.lastName} in ${f.districtId}.`,
      subjectType: 'FARMER',
      subjectId: f.id,
      assigneeUserId: null,
      assigneeRoleCode: 'AGGREGATOR',
      priority: 50 + i,
      dueAt: `2026-08-2${6 + (i % 3)}T00:00:00Z`,
      status: 'PENDING',
      outcome: null,
      completedAt: null,
      createdAt: '2026-08-18T11:00:00Z',
      comments: [],
      timeline: []
    }))
];

export type InboxNotification = {
  id: string;
  channel: string;
  title: string;
  body: string;
  eventType: string;
  subjectType: string;
  subjectId: string;
  correlationId: string | null;
  templateCode: string | null;
  sentAt: string;
  readAt: string | null;
  status: string;
  createdAt: string;
  audienceRoles?: string[];
  audienceUsernames?: string[];
};

export const notifications: InboxNotification[] = [
  {
    id: 'ntf-001',
    channel: 'IN_APP',
    title: 'Drought watch on FARM-2026-001',
    body: 'Climate Intelligence opened ALT-2026-001 for the Kimironko maize holding.',
    eventType: 'CLIMATE_ALERT',
    subjectType: 'FARM',
    subjectId: 'farm-001',
    correlationId: null,
    templateCode: null,
    sentAt: '2026-08-18T07:00:00Z',
    readAt: null,
    status: 'SENT',
    createdAt: '2026-08-18T07:00:00Z',
    audienceRoles: ['FARMER', 'INSURANCE_OFFICER', 'INSURANCE_ADMIN', 'GOVERNMENT_ANALYST', 'SYSTEM_ADMIN']
  },
  {
    id: 'ntf-002',
    channel: 'IN_APP',
    title: 'Payout completed for POL-2026-001',
    body: 'Settlement SET-2026-001 of 540,000 RWF was completed to the registered mobile money account.',
    eventType: 'SETTLEMENT',
    subjectType: 'CLAIM',
    subjectId: 'claim-001',
    correlationId: null,
    templateCode: null,
    sentAt: '2026-07-21T11:35:00Z',
    readAt: null,
    status: 'SENT',
    createdAt: '2026-07-21T11:35:00Z',
    audienceUsernames: ['farmer1']
  },
  ...claims.slice(0, 8).map((c, i) => ({
    id: `ntf-claim-${c.id}`,
    channel: 'IN_APP',
    title: `Claim ${c.claimNumber} is ${c.status.toLowerCase().replace(/_/g, ' ')}`,
    body: `${c.description}`,
    eventType: 'CLAIM',
    subjectType: 'CLAIM',
    subjectId: c.id,
    correlationId: null,
    templateCode: null,
    sentAt: `2026-08-1${(i % 9) + 1}T07:10:00Z`,
    readAt: null,
    status: 'SENT',
    createdAt: `2026-08-1${(i % 9) + 1}T07:10:00Z`,
    audienceRoles: ['INSURANCE_OFFICER', 'INSURANCE_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT']
  })),
  ...settlements.slice(0, 6).map((s, i) => ({
    id: `ntf-set-${s.id}`,
    channel: 'IN_APP',
    title: `Settlement ${s.settlementNumber} ${s.status.toLowerCase()}`,
    body: `${s.amount.toLocaleString()} RWF via ${s.providerCode} for ${s.sourceReference}.`,
    eventType: 'SETTLEMENT',
    subjectType: 'SETTLEMENT',
    subjectId: s.id,
    correlationId: null,
    templateCode: null,
    sentAt: `2026-08-1${(i % 8) + 1}T07:20:00Z`,
    readAt: null,
    status: 'SENT',
    createdAt: `2026-08-1${(i % 8) + 1}T07:20:00Z`,
    audienceRoles: ['FI_OFFICER', 'SYSTEM_ADMIN', 'AUDITOR']
  })),
  {
    id: 'ntf-gov-001',
    channel: 'IN_APP',
    title: 'National drought watch — Nyagatare',
    body: 'District alert ALT-2026-004 closed after rainfall recovered.',
    eventType: 'CLIMATE_ALERT',
    subjectType: 'DISTRICT',
    subjectId: 'NYAGATARE',
    correlationId: null,
    templateCode: null,
    sentAt: '2026-07-08T16:10:00Z',
    readAt: null,
    status: 'SENT',
    createdAt: '2026-07-08T16:10:00Z',
    audienceRoles: ['GOVERNMENT_ANALYST', 'SYSTEM_ADMIN', 'AUDITOR']
  },
  {
    id: 'ntf-sup-001',
    channel: 'IN_APP',
    title: 'Access verification requested',
    body: 'Support ticket: verify operator access for insurance.officer.',
    eventType: 'SUPPORT',
    subjectType: 'USER',
    subjectId: 'user-ins',
    correlationId: null,
    templateCode: null,
    sentAt: '2026-08-19T09:00:00Z',
    readAt: null,
    status: 'SENT',
    createdAt: '2026-08-19T09:00:00Z',
    audienceRoles: ['SUPPORT', 'SYSTEM_ADMIN']
  },
  {
    id: 'ntf-agg-001',
    channel: 'IN_APP',
    title: 'Onboarding follow-up in Ruhango',
    body: 'A household still needs farm boundary confirmation.',
    eventType: 'REGISTRY',
    subjectType: 'FARMER',
    subjectId: farmers.find((f) => f.status === 'PENDING_VERIFICATION')?.id ?? 'farmer-013',
    correlationId: null,
    templateCode: null,
    sentAt: '2026-08-18T11:05:00Z',
    readAt: null,
    status: 'SENT',
    createdAt: '2026-08-18T11:05:00Z',
    audienceRoles: ['AGGREGATOR', 'SUPPORT', 'SYSTEM_ADMIN']
  }
];

export const ledgerEntries = settlements.flatMap((s, i) => {
  if (s.status !== 'COMPLETED') {
    return [
      {
        id: `le-${pad(i * 2 + 1)}`,
        ledgerTransactionId: `lt-${pad(i + 1)}`,
        settlementId: s.id,
        entryNo: 1,
        entryType: 'DEBIT',
        accountCode: 'PAYABLE.CLAIMS',
        amount: s.amount,
        currency: 'RWF',
        narration: `Debit payable for ${s.settlementNumber}`,
        postedAt: s.submittedAt,
        status: 'PENDING'
      }
    ];
  }
  return [
    {
      id: `le-${pad(i * 2 + 1)}`,
      ledgerTransactionId: `lt-${pad(i + 1)}`,
      settlementId: s.id,
      entryNo: 1,
      entryType: 'DEBIT',
      accountCode: 'PAYABLE.CLAIMS',
      amount: s.amount,
      currency: 'RWF',
      narration: `Debit payable for ${s.settlementNumber}`,
      postedAt: s.completedAt ?? s.submittedAt,
      status: 'POSTED'
    },
    {
      id: `le-${pad(i * 2 + 2)}`,
      ledgerTransactionId: `lt-${pad(i + 1)}`,
      settlementId: s.id,
      entryNo: 2,
      entryType: 'CREDIT',
      accountCode: s.paymentMethod === 'BANK_TRANSFER' ? 'CASH.BANK' : 'CASH.MOBILE',
      amount: s.amount,
      currency: 'RWF',
      narration: `Credit cash for ${s.settlementNumber}`,
      postedAt: s.completedAt ?? s.submittedAt,
      status: 'POSTED'
    }
  ];
});

export const observations = stations.flatMap((station, si) =>
  Array.from({ length: 8 }, (_, day) => {
    const d = 1 + day * 3;
    const rain = Number((4 + ((si + day) % 11) * 2.4).toFixed(1));
    return {
      id: `obs-${station.id}-${day}`,
      stationId: station.id,
      observedAt: `2026-08-${String(d).padStart(2, '0')}T06:00:00Z`,
      variableCode: 'RAIN_MM',
      value: rain,
      unit: 'mm',
      qualityFlag: 'OK',
      providerCode: 'MANUAL',
      temperatureC: Number((18.5 + ((si + day) % 8) * 0.8).toFixed(1)),
      rainfallMm: rain,
      humidityPct: 58 + ((si + day) % 20),
      windSpeedMs: Number((1.4 + (day % 4) * 0.6).toFixed(1))
    };
  })
);

export const importJobs = [
  {
    id: 'job-001',
    jobNumber: 'CLM-IMP-2026-000000012',
    providerCode: 'MANUAL',
    jobType: 'FILE_UPLOAD',
    fileName: 'rwanda-rainfall-august-2026.csv',
    status: 'COMPLETED',
    rowsRead: 98,
    rowsAccepted: 96,
    rowsRejected: 2,
    startedAt: '2026-08-12T05:10:00Z',
    completedAt: '2026-08-12T05:14:00Z',
    errorSummary: null,
    createdAt: '2026-08-12T05:10:00Z'
  },
  {
    id: 'job-002',
    jobNumber: 'CLM-IMP-2026-000000018',
    providerCode: 'MANUAL',
    jobType: 'FILE_UPLOAD',
    fileName: 'station-temperature-week-33.csv',
    status: 'COMPLETED',
    rowsRead: 48,
    rowsAccepted: 48,
    rowsRejected: 0,
    startedAt: '2026-08-18T05:02:00Z',
    completedAt: '2026-08-18T05:05:00Z',
    errorSummary: null,
    createdAt: '2026-08-18T05:02:00Z'
  }
];

export const datasets = [
  {
    id: 'ds-001',
    code: 'RW-RAIN-2026',
    name: 'National rainfall 2026',
    description: 'Seasonal rainfall series',
    datasetType: 'TIMESERIES',
    providerCode: 'MANUAL',
    status: 'ACTIVE',
    timeStart: '2026-01-01T00:00:00Z',
    timeEnd: '2026-08-18T00:00:00Z'
  },
  {
    id: 'ds-002',
    code: 'RW-TEMP-2026',
    name: 'National temperature 2026',
    description: 'Mean daily temperature by station',
    datasetType: 'TIMESERIES',
    providerCode: 'MANUAL',
    status: 'ACTIVE',
    timeStart: '2026-01-01T00:00:00Z',
    timeEnd: '2026-08-18T00:00:00Z'
  },
  {
    id: 'ds-003',
    code: 'RW-HUM-2026',
    name: 'National humidity 2026',
    description: 'Relative humidity observations',
    datasetType: 'TIMESERIES',
    providerCode: 'MANUAL',
    status: 'ACTIVE',
    timeStart: '2026-03-01T00:00:00Z',
    timeEnd: '2026-08-18T00:00:00Z'
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
  },
  {
    id: 'prod-002',
    code: 'MULTI-CROP-2026',
    name: 'Multi-crop weather index',
    description: 'Index cover for potato, beans, rice, coffee, banana, sorghum and tea.',
    pricingStrategyCode: 'WEATHER_INDEX',
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
  },
  {
    id: 'pkg-002',
    productId: 'prod-002',
    policyTypeId: 'ptype-rice',
    code: 'RICE-75',
    name: 'Rice scheme 75% coverage',
    coverageLevelPct: 75,
    maxSumInsured: 2500000,
    status: 'ACTIVE'
  }
];

export const policyTypes = [
  { id: 'ptype-maize', code: 'MAIZE', name: 'Maize weather index', productId: 'prod-001' },
  { id: 'ptype-potato', code: 'POTATO', name: 'Potato weather index', productId: 'prod-002' },
  { id: 'ptype-beans', code: 'BEANS', name: 'Beans weather index', productId: 'prod-002' },
  { id: 'ptype-rice', code: 'RICE', name: 'Rice weather index', productId: 'prod-002' },
  { id: 'ptype-coffee', code: 'COFFEE', name: 'Coffee weather index', productId: 'prod-002' },
  { id: 'ptype-banana', code: 'BANANA', name: 'Banana weather index', productId: 'prod-002' },
  { id: 'ptype-sorghum', code: 'SORGHUM', name: 'Sorghum weather index', productId: 'prod-002' },
  { id: 'ptype-tea', code: 'TEA', name: 'Tea weather index', productId: 'prod-002' }
];

export const crops = [
  { id: 'crop-maize', code: 'MAIZE', name: 'Maize', scientificName: 'Zea mays', status: 'ACTIVE' },
  { id: 'crop-potato', code: 'POTATO', name: 'Potato', scientificName: 'Solanum tuberosum', status: 'ACTIVE' },
  { id: 'crop-beans', code: 'BEANS', name: 'Beans', scientificName: 'Phaseolus vulgaris', status: 'ACTIVE' },
  { id: 'crop-rice', code: 'RICE', name: 'Rice', scientificName: 'Oryza sativa', status: 'ACTIVE' },
  { id: 'crop-coffee', code: 'COFFEE', name: 'Coffee', scientificName: 'Coffea arabica', status: 'ACTIVE' },
  { id: 'crop-banana', code: 'BANANA', name: 'Banana', scientificName: 'Musa spp.', status: 'ACTIVE' },
  { id: 'crop-sorghum', code: 'SORGHUM', name: 'Sorghum', scientificName: 'Sorghum bicolor', status: 'ACTIVE' },
  { id: 'crop-tea', code: 'TEA', name: 'Tea', scientificName: 'Camellia sinensis', status: 'ACTIVE' }
];

const LOAN_STATUSES = ['ACTIVE', 'UNDER_REVIEW', 'APPROVED', 'IN_ARREARS', 'CLOSED'] as const;
const LENDERS = ['BK Agricultural Finance', 'BRD Rural Credit', 'SACCO Umutanguha', 'Equity Agri Desk'];

export const loans = Array.from({ length: 24 }, (_, i) => {
  const n = i + 1;
  const farmer = farmers[i] ?? farmers[0];
  const farm = farms[i] ?? farms[0];
  const policy = policies[i] ?? null;
  const status = LOAN_STATUSES[i % LOAN_STATUSES.length];
  const grade = n % 7 === 0 ? 'HIGH' : n % 3 === 0 ? 'MODERATE' : 'LOW';
  return {
    id: `loan-${pad(n)}`,
    loanNumber: `LN-2026-${pad(n)}`,
    farmerId: farmer.id,
    farmId: farm.id,
    policyId: policy?.id ?? null,
    borrowerName: `${farmer.firstName} ${farmer.lastName}`,
    farmerCode: farmer.farmerCode,
    districtId: farmer.districtId,
    lenderName: LENDERS[i % LENDERS.length],
    purpose: `${farm.cropType} season A inputs`,
    principal: 420000 + n * 35000,
    currency: 'RWF',
    insuranceEmbedPct: 2.4 + (n % 5) * 0.2,
    status,
    repaymentRisk: grade,
    cropCondition: grade === 'HIGH' ? 'STRESSED' : grade === 'MODERATE' ? 'WATCH' : 'HEALTHY',
    disbursedAt: status === 'UNDER_REVIEW' ? null : '2026-03-12T00:00:00Z',
    maturityAt: '2026-09-30T00:00:00Z',
    createdAt: '2026-03-01T00:00:00Z'
  };
});

export const satelliteScenes = Array.from({ length: 12 }, (_, i) => {
  const farm = farms[i];
  const ndvi = Number((0.28 + ((i * 7) % 9) * 0.05).toFixed(2));
  return {
    id: `sat-${pad(i + 1)}`,
    districtCode: farm.districtId,
    capturedAt: `2026-08-${String(4 + i).padStart(2, '0')}T08:10:00Z`,
    sensor: 'Sentinel-2',
    ndvi,
    vegetationHealth: ndvi >= 0.55 ? 'HEALTHY' : ndvi >= 0.4 ? 'MODERATE' : 'STRESSED',
    droughtSeverity: ndvi < 0.38 ? 'HIGH' : ndvi < 0.48 ? 'MODERATE' : 'LOW',
    floodIndex: Number((0.05 + (i % 4) * 0.08).toFixed(2)),
    environmentalStress: ndvi < 0.4 ? 'HIGH' : 'LOW'
  };
});

export const inputSales = Array.from({ length: 18 }, (_, i) => {
  const n = i + 1;
  const farmer = farmers[i] ?? farmers[0];
  const kind = i % 2 === 0 ? 'SEED' : 'FERTILIZER';
  return {
    id: `input-${pad(n)}`,
    receiptNumber: `INP-2026-${pad(n)}`,
    aggregatorName: 'Rwanda Seed & Input Network',
    farmerId: farmer.id,
    farmerName: `${farmer.firstName} ${farmer.lastName}`,
    farmerCode: farmer.farmerCode,
    productType: kind,
    productName: kind === 'SEED' ? `${farms[i]?.cropType ?? 'Maize'} seed 10kg` : 'NPK fertilizer 50kg',
    quantity: kind === 'SEED' ? 10 : 50,
    unitPrice: kind === 'SEED' ? 8500 : 42000,
    premiumEmbedded: kind === 'SEED' ? 1200 : 2800,
    currency: 'RWF',
    soldAt: `2026-02-${String(8 + (i % 18)).padStart(2, '0')}T10:00:00Z`,
    status: 'SOLD'
  };
});

export const recommendations = [
  {
    id: 'rec-001',
    farmId: 'farm-001',
    farmerId: 'farmer-001',
    title: 'Delay top-dressing until next rainfall window',
    body: 'NDVI on FARM-2026-001 is 0.33 with 28 dry days. Apply nitrogen after 15 mm of rain to avoid volatilization.',
    kind: 'AGRONOMY',
    severity: 'HIGH',
    generatedAt: '2026-08-18T06:00:00Z'
  },
  {
    id: 'rec-002',
    farmId: 'farm-001',
    farmerId: 'farmer-001',
    title: 'Confirm drought cover with your insurer',
    body: 'Climate risk score for this holding is HIGH. Policy POL-2026-001 is active; keep mobile money details current for payout notices.',
    kind: 'INSURANCE',
    severity: 'MODERATE',
    generatedAt: '2026-08-18T06:05:00Z'
  },
  {
    id: 'rec-003',
    farmId: 'farm-002',
    farmerId: 'farmer-002',
    title: 'Watch excess moisture on potato ridges',
    body: 'Flood index elevated in Musanze. Improve drainage on the highland plot before the next wet spell.',
    kind: 'AGRONOMY',
    severity: 'MODERATE',
    generatedAt: '2026-08-17T06:00:00Z'
  }
];

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
      geometry: { type: 'Point', coordinates: [f.longitude, f.latitude] },
      properties: {
        farmId: f.id,
        farmCode: f.farmCode,
        grade: i % 7 === 0 ? 'HIGH' : i % 3 === 0 ? 'MODERATE' : 'LOW'
      }
    }))
  };
}
