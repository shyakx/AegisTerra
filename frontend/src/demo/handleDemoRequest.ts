import { ApiError } from '../api/errors';
import {
  OPERATORS,
  alerts,
  claims,
  farms,
  farmers,
  households,
  kigaliBoundary,
  ledgerEntries,
  notifications,
  packages,
  page,
  policies,
  products,
  riskMap,
  settlements,
  stationMap,
  stations,
  tasks
} from './catalog';
import { readSessionUser, writeSessionUser } from './session';

function json(body: unknown, status = 200): { status: number; body: unknown } {
  return { status, body };
}

function requireUser() {
  const user = readSessionUser();
  if (!user) {
    throw new ApiError(401, 'Unauthorized');
  }
  return user;
}

function parseBody(raw: string | undefined): Record<string, unknown> {
  if (!raw) {
    return {};
  }
  try {
    return JSON.parse(raw) as Record<string, unknown>;
  } catch {
    return {};
  }
}

function idFrom(path: string, prefix: string): string | null {
  const match = path.match(new RegExp(`^${prefix}/([^/]+)`));
  return match?.[1] ?? null;
}

export async function handleDemoRequest(
  pathWithQuery: string,
  method: string,
  rawBody?: string
): Promise<unknown> {
  const url = new URL(pathWithQuery, 'https://aegisterra.local');
  const path = url.pathname.replace(/\/$/, '') || '/';
  const q = url.searchParams.get('q')?.toLowerCase() ?? '';
  const body = parseBody(rawBody);
  const verb = method.toUpperCase();

  if (path === '/api/v1/auth/login' && verb === 'POST') {
    const username = String(body.username ?? '').trim();
    const password = String(body.password ?? '');
    const account = OPERATORS.find((o) => o.username === username && o.password === password);
    if (!account) {
      throw new ApiError(401, 'Invalid credentials');
    }
    writeSessionUser(account.user);
    return { user: account.user };
  }

  if (path === '/api/v1/auth/logout' && verb === 'POST') {
    writeSessionUser(null);
    return undefined;
  }

  if (path === '/api/v1/auth/refresh' && verb === 'POST') {
    requireUser();
    return undefined;
  }

  if (path === '/api/v1/auth/forgot-password' && verb === 'POST') {
    return { message: 'If an account exists, a reset message has been queued.' };
  }

  if (path === '/api/v1/auth/reset-password' && verb === 'POST') {
    return { message: 'Password updated.' };
  }

  if (path === '/api/v1/health') {
    return { status: 'UP', service: 'aegisterra-platform', version: '1.0.0' };
  }

  const user = requireUser();

  if (path === '/api/v1/auth/me' && verb === 'GET') {
    return user;
  }

  if (path === '/api/v1/auth/profile' && verb === 'PUT') {
    const next = {
      ...user,
      email: String(body.email ?? user.email),
      displayName: String(body.displayName ?? user.displayName)
    };
    writeSessionUser(next);
    return next;
  }

  if (path === '/api/v1/auth/change-password' && verb === 'POST') {
    return { message: 'Password changed.' };
  }

  if (path === '/api/v1/executive/overview') {
    return {
      generatedAt: new Date().toISOString(),
      farmers: { total: farmers.length, active: farmers.length },
      farms: { total: farms.length, withBoundary: farms.length },
      policies: { total: policies.length, active: policies.length },
      claims: { total: claims.length, open: 1, approved: 1 },
      settlements: {
        total: settlements.length,
        pending: 1,
        completed: 1,
        pendingAmount: 410000,
        completedAmount: 540000,
        currency: 'RWF'
      },
      climate: {
        stations: stations.length,
        recentObservations: 24,
        openAlerts: 1,
        criticalAlerts: 1,
        farmsByRiskGrade: { HIGH: 1, MODERATE: 2, LOW: 0 }
      },
      tasks: { pending: tasks.length },
      notifications: { unread: notifications.filter((n) => !n.readAt).length }
    };
  }

  if (path === '/api/v1/executive/regional-summary') {
    return {
      districts: [
        { districtCode: 'GASABO', meanScore: 72, grade: 'HIGH' },
        { districtCode: 'MUSANZE', meanScore: 48, grade: 'MODERATE' },
        { districtCode: 'HUYE', meanScore: 31, grade: 'LOW' }
      ]
    };
  }

  if (path === '/api/v1/farmers' && verb === 'GET') {
    const rows = farmers.filter(
      (f) =>
        !q ||
        f.farmerCode.toLowerCase().includes(q) ||
        f.firstName.toLowerCase().includes(q) ||
        f.lastName.toLowerCase().includes(q)
    );
    return page(rows);
  }

  const farmerId = idFrom(path, '/api/v1/farmers');
  if (farmerId && verb === 'GET' && !path.includes('export')) {
    return farmers.find((f) => f.id === farmerId) ?? json(null, 404).body;
  }

  if (path === '/api/v1/farmers/export') {
    return 'farmerCode,name\nFRM-2026-001,Jean Niyonzima\n';
  }

  if (path === '/api/v1/households' && verb === 'GET') {
    return page(households);
  }

  if (path === '/api/v1/farms' && verb === 'GET') {
    const rows = farms.filter((f) => !q || f.farmCode.toLowerCase().includes(q) || f.farmName.toLowerCase().includes(q));
    return page(rows);
  }

  const farmId = idFrom(path, '/api/v1/farms');
  if (farmId && verb === 'GET' && !path.includes('export')) {
    return farms.find((f) => f.id === farmId);
  }

  if (path === '/api/v1/farm-boundaries') {
    const fid = url.searchParams.get('farmId') ?? 'farm-001';
    return [
      {
        id: `bound-${fid}`,
        farmId: fid,
        geoJson: kigaliBoundary,
        source: 'DIGITIZED',
        areaHa: 1.8,
        status: 'ACTIVE'
      }
    ];
  }

  if (path === '/api/v1/farm-boundaries/validate' && verb === 'POST') {
    return { valid: true, reason: null, areaHa: 1.8 };
  }

  if (path === '/api/v1/plots') {
    return [
      {
        id: 'plot-001',
        farmId: url.searchParams.get('farmId') ?? 'farm-001',
        plotCode: 'PLT-001',
        name: 'Main block',
        geoJson: kigaliBoundary,
        areaHa: 1.8,
        status: 'ACTIVE'
      }
    ];
  }

  if (path === '/api/v1/crops') {
    return [
      { id: 'crop-maize', code: 'MAIZE', name: 'Maize', scientificName: 'Zea mays', status: 'ACTIVE' },
      { id: 'crop-potato', code: 'POTATO', name: 'Potato', scientificName: 'Solanum tuberosum', status: 'ACTIVE' }
    ];
  }

  if (path === '/api/v1/seasons') {
    return [
      {
        id: 'season-a',
        code: '2026A',
        name: 'Season A 2026',
        startDate: '2026-03-01',
        endDate: '2026-08-31',
        status: 'ACTIVE'
      }
    ];
  }

  if (path === '/api/v1/crop-seasons') {
    return [
      {
        id: 'cs-001',
        farmId: url.searchParams.get('farmId') ?? 'farm-001',
        plotId: 'plot-001',
        cropId: 'crop-maize',
        seasonId: 'season-a',
        plantedAreaHa: 1.8,
        status: 'ACTIVE'
      }
    ];
  }

  if (path.startsWith('/api/v1/registration-drafts')) {
    return verb === 'GET' && path === '/api/v1/registration-drafts' ? [] : { id: 'draft-1', currentStep: 1, payloadJson: '{}', status: 'OPEN', expiresAt: null, farmerId: null };
  }

  if (path === '/api/v1/insurance-products') {
    return products;
  }
  if (path === '/api/v1/coverage-packages') {
    return packages;
  }
  if (path === '/api/v1/policy-types') {
    return [{ id: 'ptype-maize', code: 'MAIZE', name: 'Maize weather index', productId: 'prod-001' }];
  }
  if (path === '/api/v1/premiums/quote' && verb === 'POST') {
    return {
      id: 'quote-001',
      productId: 'prod-001',
      coveragePackageId: 'pkg-001',
      netAmount: 42000,
      grossAmount: 46200,
      baseAmount: 38000,
      coverageAmount: 850000,
      currency: 'RWF',
      breakdownJson: '{"areaHa":1.8}',
      expiresAt: '2026-12-31T00:00:00Z'
    };
  }

  if (path === '/api/v1/policies' && verb === 'GET') {
    const rows = policies.filter((p) => !q || p.policyNumber.toLowerCase().includes(q));
    return page(rows);
  }
  const policyId = idFrom(path, '/api/v1/policies');
  if (policyId && verb === 'GET' && !path.includes('/documents') && !path.includes('/export')) {
    return policies.find((p) => p.id === policyId);
  }
  if (policyId && path.endsWith('/documents')) {
    return [
      {
        id: 'doc-001',
        documentType: 'POLICY_SCHEDULE',
        versionNo: 1,
        contentText: 'Schedule for POL-2026-001 covering maize in Gasabo.',
        qrPayload: 'POL-2026-001',
        signatureStatus: 'SIGNED'
      }
    ];
  }
  if (path.startsWith('/api/v1/policies/') && verb === 'POST') {
    const id = policyId ?? 'policy-001';
    const current = policies.find((p) => p.id === id) ?? policies[0];
    return { ...current, status: path.includes('activate') ? 'ACTIVE' : current.status };
  }
  if (path.startsWith('/api/v1/insurance/reports/')) {
    return { reportCode: 'PORTFOLIO', rows: policies, totalCount: policies.length, totalAmount: 2270000 };
  }

  if (path === '/api/v1/claims' && verb === 'GET') {
    const rows = claims.filter((c) => !q || c.claimNumber.toLowerCase().includes(q));
    return page(rows);
  }
  if (path === '/api/v1/claim-types') {
    return [
      {
        id: 'ct-drought',
        code: 'DROUGHT',
        name: 'Drought',
        description: 'Rainfall deficit',
        nature: 'INDEX',
        workflowDefinitionCode: 'CLAIM_STANDARD',
        assessmentProfileJson: null,
        requiresGeo: false,
        status: 'ACTIVE'
      }
    ];
  }
  const claimId = idFrom(path, '/api/v1/claims');
  if (claimId && path === `/api/v1/claims/${claimId}` && verb === 'GET') {
    return claims.find((c) => c.id === claimId);
  }
  if (claimId && path.endsWith('/timeline')) {
    return [
      {
        kind: 'STATUS',
        fromStatus: 'SUBMITTED',
        toStatus: 'APPROVED',
        reason: 'Validated field evidence',
        actorId: user.id,
        occurredAt: '2026-06-20T10:00:00Z'
      }
    ];
  }
  if (claimId && path.endsWith('/evidence')) {
    return [
      {
        id: 'ev-001',
        claimId,
        documentType: 'FARMER_DECLARATION',
        title: 'Farmer declaration',
        documentId: null,
        storageUri: 'https://files.aegisterra.rw/claims/CLM-2026-001/farmer-declaration.pdf',
        contentSha256: null,
        latitude: -1.94,
        longitude: 30.06,
        capturedAt: '2026-06-13T09:00:00Z',
        source: 'UPLOAD',
        status: 'ACTIVE',
        createdAt: '2026-06-13T09:00:00Z'
      }
    ];
  }
  if (claimId && path.endsWith('/assessments')) {
    return [
      {
        id: 'as-001',
        claimId,
        inspectionId: 'ins-001',
        methodsJson: '{"method":"FIELD"}',
        findingsJson: '{"lossPct":64}',
        recommendedAmount: 540000,
        assessedAmount: 540000,
        currency: 'RWF',
        confidence: 0.86,
        fraudHintsJson: null,
        assessorId: user.id,
        accepted: true,
        notes: 'Confirmed drought loss on maize plot.',
        assessedAt: '2026-06-18T14:00:00Z',
        createdAt: '2026-06-18T14:00:00Z'
      }
    ];
  }
  if (claimId && path.endsWith('/inspections')) {
    return [
      {
        id: 'ins-001',
        claimId,
        inspectorId: user.id,
        scheduledAt: '2026-06-16T08:00:00Z',
        completedAt: '2026-06-16T11:00:00Z',
        checkInLatitude: -1.94,
        checkInLongitude: 30.06,
        findingsJson: '{"damage":"severe"}',
        checklistJson: '{"photos":true}',
        notes: 'Site inspection completed.',
        status: 'COMPLETED',
        createdAt: '2026-06-15T08:00:00Z'
      }
    ];
  }
  if (path.startsWith('/api/v1/workflows/instances/')) {
    const instanceId = path.split('/').pop();
    return {
      id: instanceId,
      currentStepCode: instanceId === 'wf-claim-001' ? 'DONE_APPROVED' : 'VALIDATION',
      status: instanceId === 'wf-claim-001' ? 'COMPLETED' : 'ACTIVE',
      events: [
        {
          id: 'we-001',
          eventType: 'CREATED',
          fromStepCode: null,
          toStepCode: 'VALIDATION',
          actionCode: null,
          message: 'Claim workflow opened.',
          occurredAt: '2026-06-14T08:05:00Z'
        },
        {
          id: 'we-002',
          eventType: 'COMPLETED',
          fromStepCode: 'ASSESSMENT',
          toStepCode: 'DONE_APPROVED',
          actionCode: 'APPROVE',
          message: 'Claim workflow completed for CLM-2026-001.',
          occurredAt: '2026-06-20T10:05:00Z'
        }
      ]
    };
  }

  if (path === '/api/v1/settlements' && (verb === 'GET' || verb === 'POST')) {
    const rows = settlements.filter((s) => !q || s.settlementNumber.toLowerCase().includes(q));
    return page(rows);
  }
  if (path === '/api/v1/settlements/search' && verb === 'POST') {
    return page(settlements);
  }
  const setId = idFrom(path, '/api/v1/settlements');
  if (setId && path === `/api/v1/settlements/${setId}`) {
    return settlements.find((s) => s.id === setId);
  }
  if (setId && path.endsWith('/timeline')) {
    return [
      {
        fromStatus: 'PENDING',
        toStatus: 'COMPLETED',
        reason: 'Manual confirmation',
        actorId: user.id,
        occurredAt: '2026-07-21T11:30:00Z'
      }
    ];
  }
  if (setId && path.endsWith('/ledger')) {
    return ledgerEntries.filter((e) => e.settlementId === setId);
  }
  if (path.startsWith('/api/v1/settlements/reports')) {
    return {
      reportCode: 'SETTLEMENTS',
      rows: settlements,
      totalCount: 2,
      totalAmount: 950000,
      pendingCount: 1,
      completedCount: 1,
      failedCount: 0,
      completedTotal: 540000,
      byStatus: [
        { status: 'COMPLETED', count: 1 },
        { status: 'PENDING', count: 1 }
      ],
      byProvider: [{ providerCode: 'MTN_MOMO', count: 1 }]
    };
  }

  if (path === '/api/v1/ledger' && verb === 'GET') {
    return page(ledgerEntries);
  }
  const ledgerId = idFrom(path, '/api/v1/ledger');
  if (ledgerId) {
    return (
      ledgerEntries.find((e) => e.id === ledgerId) ?? {
        id: 'lt-001',
        transactionNumber: 'LTX-2026-000000001',
        settlementId: 'set-001',
        transactionType: 'DISBURSEMENT',
        description: 'Settlement completion for SET-2026-001',
        currency: 'RWF',
        correlationId: 'settlement-801',
        postedAt: '2026-07-21T11:30:00Z',
        status: 'POSTED',
        entries: ledgerEntries
      }
    );
  }

  if (path === '/api/v1/payment-providers') {
    return [
      {
        id: 'pp-001',
        providerCode: 'MTN_MOMO',
        displayName: 'MTN Mobile Money',
        paymentMethod: 'MOBILE_MONEY',
        enabled: true,
        configJson: '{}',
        status: 'ACTIVE'
      }
    ];
  }

  if (path === '/api/v1/tasks' || path === '/api/v1/tasks/my') {
    return page(tasks);
  }
  const taskId = idFrom(path, '/api/v1/tasks');
  if (taskId && path === `/api/v1/tasks/${taskId}`) {
    return tasks.find((t) => t.id === taskId);
  }
  if (taskId && verb === 'POST') {
    const task = tasks.find((t) => t.id === taskId) ?? tasks[0];
    if (path.endsWith('/complete')) {
      return { ...task, status: 'COMPLETED', outcome: 'COMPLETED', completedAt: new Date().toISOString() };
    }
    if (path.endsWith('/comments')) {
      return { ...task, comments: [{ id: 'c1', authorId: user.id, body: String(body.body ?? ''), visibility: 'INTERNAL', createdAt: new Date().toISOString() }] };
    }
    return { ...task, status: path.endsWith('/claim') ? 'IN_PROGRESS' : task.status, assigneeUserId: user.id };
  }
  if (taskId && path.endsWith('/decisions') && verb === 'GET') {
    return [];
  }
  if (path === '/api/v1/decisions/types') {
    return [
      {
        code: 'APPROVE',
        name: 'Approve',
        description: 'Advance the workflow',
        requiresComment: false,
        requiresTargetUser: false,
        sortOrder: 1,
        status: 'ACTIVE'
      }
    ];
  }

  if (path === '/api/v1/notifications' || path === '/api/v1/notifications/unread') {
    return page(notifications);
  }
  if (path === '/api/v1/notifications/unread-count') {
    return { count: notifications.filter((n) => !n.readAt).length };
  }
  const ntfId = idFrom(path, '/api/v1/notifications');
  if (ntfId) {
    const item = notifications.find((n) => n.id === ntfId) ?? notifications[0];
    if (path.endsWith('/read')) {
      return { ...item, readAt: new Date().toISOString() };
    }
    return item;
  }
  if (path === '/api/v1/notification-preferences') {
    return [
      { channel: 'IN_APP', eventType: 'CLAIM', enabled: true },
      { channel: 'IN_APP', eventType: 'CLIMATE_ALERT', enabled: true }
    ];
  }

  if (path === '/api/v1/climate/dashboard') {
    return {
      providerCount: 1,
      enabledProviderCount: 1,
      stationCount: stations.length,
      observationCountRecent: 24,
      openImportJobs: 0,
      latestQualityGrade: 'A'
    };
  }
  if (path === '/api/v1/climate/providers') {
    return [{ id: 'prov-1', code: 'MANUAL', displayName: 'Manual CSV', enabled: true, capabilities: 'OBS', status: 'ACTIVE' }];
  }
  if (path === '/api/v1/climate/stations' && verb === 'GET') {
    return page(stations);
  }
  const stId = idFrom(path, '/api/v1/climate/stations');
  if (stId) {
    return stations.find((s) => s.id === stId);
  }
  if (path === '/api/v1/climate/observations') {
    return page([
      {
        id: 'obs-001',
        stationId: 'station-kgl',
        observedAt: '2026-08-01T06:00:00Z',
        variableCode: 'RAIN_MM',
        value: 12.5,
        unit: 'mm',
        qualityFlag: 'OK',
        providerCode: 'MANUAL',
        temperatureC: 22.1,
        rainfallMm: 12.5,
        humidityPct: 68,
        windSpeedMs: 2.1
      }
    ]);
  }
  if (path === '/api/v1/climate/import-jobs') {
    return page([]);
  }
  if (path === '/api/v1/climate/datasets') {
    return page([
      {
        id: 'ds-001',
        code: 'RW-RAIN-2026',
        name: 'National rainfall 2026',
        description: 'Seasonal rainfall series',
        datasetType: 'TIMESERIES',
        providerCode: 'MANUAL',
        status: 'ACTIVE',
        timeStart: '2026-01-01T00:00:00Z',
        timeEnd: '2026-08-01T00:00:00Z'
      }
    ]);
  }
  if (path === '/api/v1/climate/map/stations') {
    return stationMap();
  }
  if (path === '/api/v1/climate/map/footprints') {
    return { type: 'FeatureCollection', features: [] };
  }
  if (path.startsWith('/api/v1/climate/reports/')) {
    return { ok: true };
  }

  if (path === '/api/v1/climate-intel/national/dashboard') {
    return {
      farmsByGrade: { HIGH: 1, MODERATE: 2, LOW: 5 },
      openCriticalAlerts: 1,
      openAlerts: 1,
      districtHeat: [
        { districtCode: 'GASABO', meanScore: 72, grade: 'HIGH' },
        { districtCode: 'MUSANZE', meanScore: 48, grade: 'MODERATE' }
      ],
      dataCoveragePct: 96,
      generatedAt: new Date().toISOString()
    };
  }
  if (path === '/api/v1/climate-intel/alerts') {
    return page(alerts);
  }
  const alertId = idFrom(path, '/api/v1/climate-intel/alerts');
  if (alertId) {
    return alerts.find((a) => a.id === alertId) ?? alerts[0];
  }
  if (path.includes('/climate-intel/farms/') && path.endsWith('/risk-score')) {
    return {
      id: 'risk-001',
      farmId: 'farm-001',
      score: 72,
      confidence: 0.81,
      grade: 'HIGH',
      windowStart: '2026-06-01',
      windowEnd: '2026-06-30',
      componentsJson: '{"dryDays":28}',
      ruleSetCode: 'DROUGHT-V1',
      ruleVersion: '1.0.0',
      modelVersion: '1.0.0',
      calculatedAt: '2026-06-30T00:00:00Z'
    };
  }
  if (path.includes('/climate-intel/farms/') && path.endsWith('/profile')) {
    return {
      id: 'prof-001',
      farmId: 'farm-001',
      generatedAt: '2026-06-30T00:00:00Z',
      ruleVersion: '1.0.0',
      snapshotJson: '{"grade":"HIGH"}',
      latestRiskScore: 72,
      latestRiskGrade: 'HIGH'
    };
  }
  if (path.includes('/climate-intel/farms/') && path.endsWith('/timeline')) {
    return [
      {
        occurredAt: '2026-06-10T06:00:00Z',
        eventType: 'ALERT',
        severity: 'HIGH',
        title: 'Drought threshold exceeded',
        detail: '28 dry days on FARM-2026-001',
        evidenceJson: null
      }
    ];
  }
  if (path.includes('/weather-summary')) {
    return {
      farmId: 'farm-001',
      from: '2026-06-01',
      to: '2026-06-30',
      rainfallTotalMm: 18,
      meanTempC: 24.2,
      dryDays: 28,
      wetDays: 2,
      narrative: 'Severe rainfall deficit relative to the seasonal baseline.',
      metricsJson: null
    };
  }
  if (path.includes('/season-summary')) {
    return {
      farmId: 'farm-001',
      seasonCode: '2026A',
      grade: 'HIGH',
      metricsJson: '{}',
      narrative: 'Season A drought stress on maize.',
      generatedAt: '2026-06-30T00:00:00Z'
    };
  }
  if (path.includes('/climate-intel/districts/')) {
    return {
      districtCode: 'GASABO',
      meanScore: 72,
      p90Score: 81,
      farmCount: 3,
      openAlertCount: 1,
      grade: 'HIGH',
      metricsJson: null,
      generatedAt: new Date().toISOString()
    };
  }
  if (path === '/api/v1/climate-intel/map/risk') {
    return riskMap();
  }
  if (path === '/api/v1/climate-intel/jobs/recalculate' && verb === 'POST') {
    return {
      id: 'job-001',
      jobNumber: 'CLT-JOB-2026-000000001',
      jobType: 'RECALCULATE',
      status: 'COMPLETED',
      subjectsProcessed: 3,
      errorSummary: null,
      startedAt: new Date().toISOString(),
      completedAt: new Date().toISOString(),
      createdAt: new Date().toISOString()
    };
  }
  if (path === '/api/v1/climate-intel/indicators') {
    return page([]);
  }
  if (path.startsWith('/api/v1/climate-intel/reports/')) {
    return { ok: true };
  }

  if (path === '/api/v1/users' && verb === 'GET') {
    return page(
      OPERATORS.map((o) => ({
        id: o.user.id,
        username: o.user.username,
        email: o.user.email,
        displayName: o.user.displayName,
        role: o.user.roles[0],
        roles: o.user.roles,
        status: 'ACTIVE'
      }))
    );
  }
  if (path === '/api/v1/roles') {
    return OPERATORS.map((o) => ({
      id: o.user.roles[0],
      code: o.user.roles[0],
      name: o.label,
      description: null,
      systemRole: true
    }));
  }
  if (path === '/api/v1/permissions') {
    return user.permissions.map((code) => ({
      id: code,
      code,
      name: code,
      resource: code.split(':')[0],
      action: code.split(':')[1]
    }));
  }

  if (verb === 'POST' || verb === 'PUT' || verb === 'PATCH') {
    return { id: 'ok', status: 'ACTIVE', message: 'Saved' };
  }
  if (verb === 'DELETE') {
    return undefined;
  }

  return page([]);
}
