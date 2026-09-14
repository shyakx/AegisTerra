import type { AuthUser } from '../api/auth';
import { ApiError } from '../api/errors';
import {
  OPERATORS,
  alerts,
  claims,
  crops,
  datasets,
  farms,
  farmers,
  hasPermission,
  importJobs,
  inputSales,
  isFarmerUser,
  kigaliBoundary,
  ledgerEntries,
  loans,
  notifications,
  observations,
  packages,
  page,
  policies,
  policyTypes,
  products,
  recommendations,
  riskMap,
  satelliteScenes,
  scopedClaims,
  scopedFarms,
  scopedFarmers,
  scopedHouseholds,
  scopedNotifications,
  scopedPolicies,
  scopedTasks,
  settlements,
  stationMap,
  stations,
  tasks
} from './catalog';
import { demoGeography } from './geographyCatalog';
import { readSessionUser, writeSessionUser } from './session';

function requireUser() {
  const user = readSessionUser();
  if (!user) {
    throw new ApiError(401, 'Unauthorized');
  }
  return user;
}

function deny(): never {
  throw new ApiError(403, 'Access denied');
}

function notFound(): never {
  throw new ApiError(404, 'Not found');
}

function assertPerm(user: AuthUser, permission: string) {
  if (!hasPermission(user, permission)) {
    deny();
  }
}

function isWrite(verb: string) {
  return verb === 'POST' || verb === 'PUT' || verb === 'PATCH' || verb === 'DELETE';
}

function enforceRbac(user: AuthUser, path: string, verb: string) {
  if (path.startsWith('/api/v1/auth')) {
    return;
  }

  if (isFarmerUser(user)) {
    const blocked = [
      '/api/v1/executive',
      '/api/v1/users',
      '/api/v1/roles',
      '/api/v1/permissions',
      '/api/v1/households',
      '/api/v1/settlements',
      '/api/v1/ledger',
      '/api/v1/payment-providers',
      '/api/v1/tasks',
      '/api/v1/decisions',
      '/api/v1/workflows',
      '/api/v1/climate',
      '/api/v1/insurance-products',
      '/api/v1/coverage-packages',
      '/api/v1/premiums',
      '/api/v1/insurance/reports',
      '/api/v1/registration'
    ];
    if (blocked.some((prefix) => path.startsWith(prefix))) {
      deny();
    }
  }

  const write = isWrite(verb);
  const need = (read: string, writePerm = read.replace(':read', ':write')) => {
    assertPerm(user, write ? writePerm : read);
  };

  if (path.startsWith('/api/v1/executive')) {
    if (isFarmerUser(user)) {
      deny();
    }
    const any = [
      'farmers:read',
      'policies:read',
      'claims:read',
      'settlements:read',
      'climate:read',
      'climate-intel:read',
      'users:read',
      'tasks:read',
      'loans:read',
      'satellite:read'
    ];
    if (!any.some((permission) => hasPermission(user, permission))) {
      deny();
    }
    return;
  }

  if (path.startsWith('/api/v1/users')) {
    need('users:read', 'users:write');
    return;
  }
  if (path.startsWith('/api/v1/roles')) {
    if (write) {
      assertPerm(user, 'users:write');
    } else if (!hasPermission(user, 'roles:read') && !hasPermission(user, 'users:read')) {
      deny();
    }
    return;
  }
  if (path.startsWith('/api/v1/permissions')) {
    if (!hasPermission(user, 'permissions:read') && !hasPermission(user, 'users:read')) {
      deny();
    }
    return;
  }
  if (path.startsWith('/api/v1/farmers') || path.startsWith('/api/v1/households')) {
    need('farmers:read', 'farmers:write');
    return;
  }
  if (
    path.startsWith('/api/v1/provinces') ||
    path.startsWith('/api/v1/districts') ||
    path.startsWith('/api/v1/agroecological-zones') ||
    path.startsWith('/api/v1/agroecological-subzones')
  ) {
    need('farmers:read', 'farmers:write');
    return;
  }
  if (
    path.startsWith('/api/v1/farms') ||
    path.startsWith('/api/v1/plots') ||
    path.startsWith('/api/v1/crops') ||
    path.startsWith('/api/v1/seasons') ||
    path.startsWith('/api/v1/crop-seasons') ||
    path.startsWith('/api/v1/farm-boundaries') ||
    path.startsWith('/api/v1/registration')
  ) {
    need('farms:read', 'farms:write');
    return;
  }
  if (
    path.startsWith('/api/v1/policies') ||
    path.startsWith('/api/v1/insurance') ||
    path.startsWith('/api/v1/premiums') ||
    path.startsWith('/api/v1/coverage-packages') ||
    path.startsWith('/api/v1/policy-types') ||
    path.startsWith('/api/v1/insurance-products')
  ) {
    if (write && path.includes('approve')) {
      if (!hasPermission(user, 'policies:approve') && !hasPermission(user, 'policies:write')) {
        deny();
      }
      return;
    }
    need('policies:read', 'policies:write');
    return;
  }
  if (path.startsWith('/api/v1/claims') || path.startsWith('/api/v1/claim-types')) {
    if (write && (path.includes('assess') || path.endsWith('/assessments') || path.endsWith('/inspections'))) {
      if (!hasPermission(user, 'claims:assess') && !hasPermission(user, 'claims:write')) {
        deny();
      }
      return;
    }
    need('claims:read', 'claims:write');
    return;
  }
  if (path.startsWith('/api/v1/settlements') || path.startsWith('/api/v1/payment-providers')) {
    if (
      write &&
      (path.includes('approve') || path.includes('process') || path.includes('complete') || path.includes('disburse'))
    ) {
      if (
        !hasPermission(user, 'settlements:approve') &&
        !hasPermission(user, 'settlements:process') &&
        !hasPermission(user, 'settlements:write')
      ) {
        deny();
      }
      return;
    }
    need('settlements:read', 'settlements:write');
    return;
  }
  if (path.startsWith('/api/v1/ledger')) {
    assertPerm(user, 'ledger:read');
    return;
  }
  if (path.startsWith('/api/v1/tasks') || path.startsWith('/api/v1/decisions') || path.startsWith('/api/v1/workflows')) {
    if (write) {
      if (!hasPermission(user, 'tasks:act') && !hasPermission(user, 'decisions:act')) {
        deny();
      }
      return;
    }
    if (path.startsWith('/api/v1/workflows')) {
      if (!hasPermission(user, 'workflows:read') && !hasPermission(user, 'tasks:read')) {
        deny();
      }
      return;
    }
    assertPerm(user, 'tasks:read');
    return;
  }
  if (path.startsWith('/api/v1/notifications') || path.startsWith('/api/v1/notification-preferences')) {
    need('notifications:read', 'notifications:write');
    return;
  }
  if (path.startsWith('/api/v1/loans')) {
    need('loans:read', 'loans:write');
    return;
  }
  if (path.startsWith('/api/v1/satellite')) {
    if (!hasPermission(user, 'satellite:read') && !hasPermission(user, 'climate-intel:read')) {
      deny();
    }
    return;
  }
  if (path.startsWith('/api/v1/insured-inputs')) {
    need('inputs:read', 'inputs:write');
    return;
  }
  if (path.startsWith('/api/v1/recommendations')) {
    if (!hasPermission(user, 'farmers:read') && !hasPermission(user, 'notifications:read')) {
      deny();
    }
    return;
  }
  if (path.startsWith('/api/v1/climate-intel')) {
    if (write) {
      if (!hasPermission(user, 'climate-intel:write') && !hasPermission(user, 'climate-intel:admin')) {
        deny();
      }
      return;
    }
    if (!hasPermission(user, 'climate-intel:read') && !hasPermission(user, 'alerts:climate')) {
      deny();
    }
    return;
  }
  if (path.startsWith('/api/v1/climate')) {
    if (write) {
      if (!hasPermission(user, 'climate:write') && !hasPermission(user, 'climate:import')) {
        deny();
      }
      return;
    }
    assertPerm(user, 'climate:read');
  }
}

function owned<T extends { id: string }>(row: T | undefined, allowed: boolean): T {
  if (!row) {
    notFound();
  }
  if (!allowed) {
    deny();
  }
  return row;
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
  const statusFilter = url.searchParams.get('status') ?? '';
  const pageNo = Number(url.searchParams.get('page') ?? 0) || 0;
  const pageSize = Number(url.searchParams.get('size') ?? 20) || 20;
  const paged = <T,>(rows: T[]) => page(rows, pageNo, pageSize);
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
  enforceRbac(user, path, verb);

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
    const farmerRows = scopedFarmers(user);
    const farmRows = scopedFarms(user);
    const policyRows = scopedPolicies(user);
    const claimRows = scopedClaims(user);
    const inbox = scopedNotifications(user);
    const queue = scopedTasks(user);
    const openClaims = claimRows.filter((c) => !['CLOSED', 'SETTLED', 'REJECTED'].includes(c.status)).length;
    const approvedClaims = claimRows.filter((c) => c.status === 'APPROVED' || c.status === 'PAYMENT_PENDING').length;
    const pendingSettlements = settlements.filter((s) => s.status === 'PENDING' || s.status === 'APPROVED');
    const completedSettlements = settlements.filter((s) => s.status === 'COMPLETED');
    const openAlerts = alerts.filter((a) => a.status === 'OPEN');
    return {
      generatedAt: new Date().toISOString(),
      farmers: { total: farmerRows.length, active: farmerRows.filter((f) => f.status === 'ACTIVE').length },
      farms: { total: farmRows.length, withBoundary: farmRows.length },
      policies: { total: policyRows.length, active: policyRows.filter((p) => p.status === 'ACTIVE').length },
      claims: { total: claimRows.length, open: openClaims, approved: approvedClaims },
      settlements: {
        total: settlements.length,
        pending: pendingSettlements.length,
        completed: completedSettlements.length,
        pendingAmount: pendingSettlements.reduce((sum, s) => sum + s.amount, 0),
        completedAmount: completedSettlements.reduce((sum, s) => sum + s.amount, 0),
        currency: 'RWF'
      },
      climate: {
        stations: stations.length,
        recentObservations: observations.length,
        openAlerts: openAlerts.length,
        criticalAlerts: alerts.filter((a) => a.severity === 'CRITICAL' || a.severity === 'HIGH').length,
        farmsByRiskGrade: {
          HIGH: farms.filter((_, i) => i % 7 === 0).length,
          MODERATE: farms.filter((_, i) => i % 7 !== 0 && i % 3 === 0).length,
          LOW: farms.filter((_, i) => i % 7 !== 0 && i % 3 !== 0).length
        }
      },
      tasks: { pending: queue.filter((t) => t.status !== 'COMPLETED').length },
      notifications: { unread: inbox.filter((n) => !n.readAt).length }
    };
  }

  if (path === '/api/v1/executive/regional-summary') {
    return {
      districts: [
        { districtCode: 'GASABO', meanScore: 72, grade: 'HIGH' },
        { districtCode: 'MUSANZE', meanScore: 48, grade: 'MODERATE' },
        { districtCode: 'HUYE', meanScore: 54, grade: 'MODERATE' },
        { districtCode: 'NYAGATARE', meanScore: 81, grade: 'HIGH' },
        { districtCode: 'RUBAVU', meanScore: 29, grade: 'LOW' },
        { districtCode: 'KAYONZA', meanScore: 36, grade: 'LOW' },
        { districtCode: 'RUHANGO', meanScore: 41, grade: 'MODERATE' },
        { districtCode: 'BUGESERA', meanScore: 33, grade: 'LOW' }
      ]
    };
  }

  if (path === '/api/v1/farmers' && verb === 'GET') {
    const rows = scopedFarmers(user).filter(
      (f) =>
        (!statusFilter || f.status === statusFilter) &&
        (!q ||
          f.farmerCode.toLowerCase().includes(q) ||
          f.firstName.toLowerCase().includes(q) ||
          f.lastName.toLowerCase().includes(q) ||
          f.nationalId.includes(q) ||
          f.phoneNumber.includes(q))
    );
    return paged(rows);
  }

  const farmerId = idFrom(path, '/api/v1/farmers');
  if (farmerId && verb === 'GET' && !path.includes('export')) {
    const row = farmers.find((f) => f.id === farmerId);
    return owned(row, scopedFarmers(user).some((f) => f.id === farmerId));
  }

  if (path === '/api/v1/farmers/export') {
    const rows = scopedFarmers(user);
    return `farmerCode,name\n${rows.map((f) => `${f.farmerCode},${f.firstName} ${f.lastName}`).join('\n')}\n`;
  }

  if (path === '/api/v1/households' && verb === 'GET') {
    return paged(scopedHouseholds(user));
  }

  if (path === '/api/v1/provinces' && verb === 'GET') {
    return demoGeography.provinces();
  }
  const provinceId = idFrom(path, '/api/v1/provinces');
  if (provinceId && verb === 'GET') {
    return demoGeography.province(provinceId) ?? notFound();
  }
  if (path === '/api/v1/districts' && verb === 'GET') {
    return demoGeography.districts(url.searchParams.get('provinceId'));
  }
  const districtId = idFrom(path, '/api/v1/districts');
  if (districtId && verb === 'GET') {
    return demoGeography.district(districtId) ?? notFound();
  }
  if (path === '/api/v1/agroecological-zones' && verb === 'GET') {
    return demoGeography.zones();
  }
  const zoneId = idFrom(path, '/api/v1/agroecological-zones');
  if (zoneId && verb === 'GET') {
    return demoGeography.zone(zoneId) ?? notFound();
  }
  if (path === '/api/v1/agroecological-subzones' && verb === 'GET') {
    return demoGeography.subzones(url.searchParams.get('zoneId'), url.searchParams.get('districtId'));
  }
  const subzoneId = idFrom(path, '/api/v1/agroecological-subzones');
  if (subzoneId && verb === 'GET') {
    return demoGeography.subzone(subzoneId) ?? notFound();
  }

  if (path === '/api/v1/farms' && verb === 'GET') {
    const farmerIdQ = url.searchParams.get('farmerId');
    const rows = scopedFarms(user).filter(
      (f) =>
        (!farmerIdQ || f.farmerId === farmerIdQ) &&
        (!statusFilter || f.status === statusFilter) &&
        (!q || f.farmCode.toLowerCase().includes(q) || f.farmName.toLowerCase().includes(q))
    );
    return paged(rows);
  }

  const farmId = idFrom(path, '/api/v1/farms');
  if (farmId && verb === 'GET' && !path.includes('export')) {
    const row = farms.find((f) => f.id === farmId);
    return owned(row, scopedFarms(user).some((f) => f.id === farmId));
  }

  if (path === '/api/v1/farm-boundaries') {
    const fid = url.searchParams.get('farmId') ?? scopedFarms(user)[0]?.id ?? 'farm-001';
    if (!scopedFarms(user).some((f) => f.id === fid)) {
      deny();
    }
    const farm = farms.find((f) => f.id === fid);
    return [
      {
        id: `bound-${fid}`,
        farmId: fid,
        geoJson: kigaliBoundary,
        source: 'DIGITIZED',
        areaHa: farm?.farmSizeHa ?? 1.8,
        status: 'ACTIVE'
      }
    ];
  }

  if (path === '/api/v1/farm-boundaries/validate' && verb === 'POST') {
    return { valid: true, reason: null, areaHa: 1.8 };
  }

  if (path === '/api/v1/plots') {
    const fid = url.searchParams.get('farmId') ?? scopedFarms(user)[0]?.id ?? 'farm-001';
    if (!scopedFarms(user).some((f) => f.id === fid)) {
      deny();
    }
    const farm = farms.find((f) => f.id === fid);
    return [
      {
        id: `plot-${fid}`,
        farmId: fid,
        plotCode: 'PLT-001',
        name: 'Main block',
        geoJson: kigaliBoundary,
        areaHa: farm?.farmSizeHa ?? 1.8,
        status: 'ACTIVE'
      }
    ];
  }

  if (path === '/api/v1/crops') {
    return crops;
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
    const fid = url.searchParams.get('farmId') ?? scopedFarms(user)[0]?.id ?? 'farm-001';
    if (!scopedFarms(user).some((f) => f.id === fid)) {
      deny();
    }
    const farm = farms.find((f) => f.id === fid);
    return [
      {
        id: `cs-${fid}`,
        farmId: fid,
        plotId: `plot-${fid}`,
        cropId: 'crop-maize',
        seasonId: 'season-a',
        plantedAreaHa: farm?.farmSizeHa ?? 1.8,
        yieldTHa: 2.4,
        status: 'HARVESTED'
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
    return policyTypes;
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
    const farmerIdQ = url.searchParams.get('farmerId');
    const rows = scopedPolicies(user).filter(
      (p) =>
        (!farmerIdQ || p.farmerId === farmerIdQ) &&
        (!statusFilter || p.status === statusFilter) &&
        (!q || p.policyNumber.toLowerCase().includes(q))
    );
    return paged(rows);
  }
  const policyId = idFrom(path, '/api/v1/policies');
  if (policyId && verb === 'GET' && !path.includes('/documents') && !path.includes('/export')) {
    const row = policies.find((p) => p.id === policyId);
    return owned(row, scopedPolicies(user).some((p) => p.id === policyId));
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
    const rows = scopedPolicies(user);
    return {
      reportCode: 'PORTFOLIO',
      rows,
      totalCount: rows.length,
      totalAmount: rows.reduce((sum, p) => sum + p.coverageAmount, 0)
    };
  }

  if (path === '/api/v1/claims' && verb === 'GET') {
    const farmerIdQ = url.searchParams.get('farmerId');
    const typeCode = url.searchParams.get('claimTypeCode');
    const rows = scopedClaims(user).filter(
      (c) =>
        (!farmerIdQ || c.farmerId === farmerIdQ) &&
        (!statusFilter || c.status === statusFilter) &&
        (!typeCode || c.claimTypeCode === typeCode) &&
        (!q || c.claimNumber.toLowerCase().includes(q))
    );
    return paged(rows);
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
      },
      {
        id: 'ct-flood',
        code: 'FLOOD',
        name: 'Flood',
        description: 'Excess rainfall and inundation',
        nature: 'INDEX',
        workflowDefinitionCode: 'CLAIM_STANDARD',
        assessmentProfileJson: null,
        requiresGeo: true,
        status: 'ACTIVE'
      },
      {
        id: 'ct-hail',
        code: 'HAIL',
        name: 'Hail',
        description: 'Hail damage',
        nature: 'INDEMNITY',
        workflowDefinitionCode: 'CLAIM_STANDARD',
        assessmentProfileJson: null,
        requiresGeo: true,
        status: 'ACTIVE'
      }
    ];
  }
  const claimId = idFrom(path, '/api/v1/claims');
  if (claimId && path === `/api/v1/claims/${claimId}` && verb === 'GET') {
    const row = claims.find((c) => c.id === claimId);
    return owned(row, scopedClaims(user).some((c) => c.id === claimId));
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
    return paged(rows);
  }
  if (path === '/api/v1/settlements/search' && verb === 'POST') {
    return paged(settlements);
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
      totalCount: settlements.length,
      totalAmount: settlements.reduce((sum, s) => sum + s.amount, 0),
      pendingCount: settlements.filter((s) => s.status === 'PENDING').length,
      completedCount: settlements.filter((s) => s.status === 'COMPLETED').length,
      failedCount: 0,
      completedTotal: settlements.filter((s) => s.status === 'COMPLETED').reduce((sum, s) => sum + s.amount, 0),
      byStatus: [
        { status: 'COMPLETED', count: settlements.filter((s) => s.status === 'COMPLETED').length },
        { status: 'PENDING', count: settlements.filter((s) => s.status === 'PENDING').length },
        { status: 'APPROVED', count: settlements.filter((s) => s.status === 'APPROVED').length }
      ],
      byProvider: [
        { providerCode: 'MTN_MOMO', count: 1 },
        { providerCode: 'BNR_RTGS', count: 1 },
        { providerCode: 'AIRTEL_MONEY', count: 1 }
      ]
    };
  }

  if (path === '/api/v1/ledger' && verb === 'GET') {
    return paged(ledgerEntries);
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
      },
      {
        id: 'pp-002',
        providerCode: 'AIRTEL_MONEY',
        displayName: 'Airtel Money',
        paymentMethod: 'MOBILE_MONEY',
        enabled: true,
        configJson: '{}',
        status: 'ACTIVE'
      },
      {
        id: 'pp-003',
        providerCode: 'BNR_RTGS',
        displayName: 'Bank transfer (RTGS)',
        paymentMethod: 'BANK_TRANSFER',
        enabled: true,
        configJson: '{}',
        status: 'ACTIVE'
      }
    ];
  }

  if (path === '/api/v1/tasks' || path === '/api/v1/tasks/my') {
    return paged(scopedTasks(user));
  }
  const taskId = idFrom(path, '/api/v1/tasks');
  if (taskId && path === `/api/v1/tasks/${taskId}`) {
    const task = tasks.find((t) => t.id === taskId);
    return owned(task, scopedTasks(user).some((t) => t.id === taskId));
  }
  if (taskId && verb === 'POST') {
    const task = owned(
      tasks.find((t) => t.id === taskId),
      scopedTasks(user).some((t) => t.id === taskId)
    );
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
    return paged(scopedNotifications(user));
  }
  if (path === '/api/v1/notifications/unread-count') {
    return { count: scopedNotifications(user).filter((n) => !n.readAt).length };
  }
  const ntfId = idFrom(path, '/api/v1/notifications');
  if (ntfId) {
    const inbox = scopedNotifications(user);
    const item = owned(
      inbox.find((n) => n.id === ntfId) ?? notifications.find((n) => n.id === ntfId),
      inbox.some((n) => n.id === ntfId)
    );
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
      observationCountRecent: observations.length,
      openImportJobs: 0,
      latestQualityGrade: 'A'
    };
  }
  if (path === '/api/v1/climate/providers') {
    return [{ id: 'prov-1', code: 'MANUAL', displayName: 'Manual CSV', enabled: true, capabilities: 'OBS', status: 'ACTIVE' }];
  }
  if (path === '/api/v1/climate/stations' && verb === 'GET') {
    return paged(stations);
  }
  const stId = idFrom(path, '/api/v1/climate/stations');
  if (stId) {
    return stations.find((s) => s.id === stId || s.code === stId);
  }
  if (path === '/api/v1/climate/observations') {
    const stationId = url.searchParams.get('stationId');
    const rows = observations.filter((o) => !stationId || o.stationId === stationId);
    return paged(rows);
  }
  if (path === '/api/v1/climate/import-jobs') {
    return paged(importJobs);
  }
  if (path === '/api/v1/climate/datasets') {
    return paged(datasets);
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
    const zoneHeat = [
      { code: 'A', name: 'ZONE A', meanScore: 48, grade: 'MODERATE', districtCount: 1, farmCount: 3 },
      { code: 'B', name: 'ZONE B', meanScore: 54, grade: 'HIGH', districtCount: 1, farmCount: 2 },
      { code: 'C', name: 'ZONE C', meanScore: 81, grade: 'EXTREME', districtCount: 1, farmCount: 3 },
      { code: 'E', name: 'ZONE E', meanScore: 72, grade: 'HIGH', districtCount: 1, farmCount: 3 }
    ];
    const subzoneHeat = [
      { code: 'A1', name: 'Volcanic Highlands', zoneCode: 'A', zoneName: 'ZONE A', meanScore: 48, grade: 'MODERATE', districtCount: 1, farmCount: 3 },
      { code: 'B2', name: 'Southern Central Plateau', zoneCode: 'B', zoneName: 'ZONE B', meanScore: 54, grade: 'HIGH', districtCount: 1, farmCount: 2 },
      { code: 'C1', name: 'Eastern Dry Savanna', zoneCode: 'C', zoneName: 'ZONE C', meanScore: 81, grade: 'EXTREME', districtCount: 1, farmCount: 3 },
      { code: 'E1', name: 'Northern/Highland Kigali', zoneCode: 'E', zoneName: 'ZONE E', meanScore: 72, grade: 'HIGH', districtCount: 1, farmCount: 3 }
    ];
    return {
      farmsByGrade: {
        HIGH: farms.filter((_, i) => i % 7 === 0).length,
        MODERATE: farms.filter((_, i) => i % 7 !== 0 && i % 3 === 0).length,
        LOW: farms.filter((_, i) => i % 7 !== 0 && i % 3 !== 0).length
      },
      openCriticalAlerts: alerts.filter((a) => a.status === 'OPEN' && (a.severity === 'CRITICAL' || a.severity === 'HIGH')).length,
      openAlerts: alerts.filter((a) => a.status === 'OPEN').length,
      districtHeat: [
        { districtCode: 'GASABO', meanScore: 72, grade: 'HIGH' },
        { districtCode: 'MUSANZE', meanScore: 48, grade: 'MODERATE' },
        { districtCode: 'HUYE', meanScore: 54, grade: 'MODERATE' },
        { districtCode: 'NYAGATARE', meanScore: 81, grade: 'HIGH' }
      ],
      dataCoveragePct: 96,
      generatedAt: new Date().toISOString(),
      zoneHeat,
      subzoneHeat,
      unmappedDistrictCodes: [],
      unmappedCount: 0
    };
  }
  if (path === '/api/v1/climate-intel/aez/risk-summary') {
    return {
      zones: [
        { code: 'A', name: 'ZONE A', meanScore: 48, grade: 'MODERATE', districtCount: 1, farmCount: 3 },
        { code: 'B', name: 'ZONE B', meanScore: 54, grade: 'HIGH', districtCount: 1, farmCount: 2 },
        { code: 'C', name: 'ZONE C', meanScore: 81, grade: 'EXTREME', districtCount: 1, farmCount: 3 },
        { code: 'E', name: 'ZONE E', meanScore: 72, grade: 'HIGH', districtCount: 1, farmCount: 3 }
      ],
      subzones: [
        { code: 'A1', name: 'Volcanic Highlands', zoneCode: 'A', zoneName: 'ZONE A', meanScore: 48, grade: 'MODERATE', districtCount: 1, farmCount: 3 },
        { code: 'B2', name: 'Southern Central Plateau', zoneCode: 'B', zoneName: 'ZONE B', meanScore: 54, grade: 'HIGH', districtCount: 1, farmCount: 2 },
        { code: 'C1', name: 'Eastern Dry Savanna', zoneCode: 'C', zoneName: 'ZONE C', meanScore: 81, grade: 'EXTREME', districtCount: 1, farmCount: 3 },
        { code: 'E1', name: 'Northern/Highland Kigali', zoneCode: 'E', zoneName: 'ZONE E', meanScore: 72, grade: 'HIGH', districtCount: 1, farmCount: 3 }
      ],
      unmappedDistrictCodes: [],
      unmappedCount: 0,
      generatedAt: new Date().toISOString()
    };
  }
  if (path === '/api/v1/climate-intel/aez/zones/risk-summary') {
    return [
      { code: 'A', name: 'ZONE A', meanScore: 48, grade: 'MODERATE', districtCount: 1, farmCount: 3 },
      { code: 'B', name: 'ZONE B', meanScore: 54, grade: 'HIGH', districtCount: 1, farmCount: 2 },
      { code: 'C', name: 'ZONE C', meanScore: 81, grade: 'EXTREME', districtCount: 1, farmCount: 3 },
      { code: 'E', name: 'ZONE E', meanScore: 72, grade: 'HIGH', districtCount: 1, farmCount: 3 }
    ];
  }
  if (path === '/api/v1/climate-intel/aez/subzones/risk-summary') {
    return [
      { code: 'A1', name: 'Volcanic Highlands', zoneCode: 'A', zoneName: 'ZONE A', meanScore: 48, grade: 'MODERATE', districtCount: 1, farmCount: 3 },
      { code: 'B2', name: 'Southern Central Plateau', zoneCode: 'B', zoneName: 'ZONE B', meanScore: 54, grade: 'HIGH', districtCount: 1, farmCount: 2 },
      { code: 'C1', name: 'Eastern Dry Savanna', zoneCode: 'C', zoneName: 'ZONE C', meanScore: 81, grade: 'EXTREME', districtCount: 1, farmCount: 3 },
      { code: 'E1', name: 'Northern/Highland Kigali', zoneCode: 'E', zoneName: 'ZONE E', meanScore: 72, grade: 'HIGH', districtCount: 1, farmCount: 3 }
    ];
  }
  if (path === '/api/v1/climate-intel/planning/yield-outlook') {
    return {
      referenceYear: 2026,
      pastWindowStartYear: 2016,
      pastWindowEndYear: 2023,
      recentWindowStartYear: 2024,
      recentWindowEndYear: 2025,
      nationalMeanRiskScore: 63.75,
      nationalRiskGrade: 'HIGH',
      methodology:
        'Past = mean yield in seasons ending years Y-10..Y-3; Recent = mean yield in Y-2..Y-1; Predicted = recent + 0.5×(recent−past) × (1 − 0.35×nationalRisk/100).',
      generatedAt: new Date().toISOString(),
      crops: [
        {
          cropCode: 'MAIZE',
          cropName: 'Maize',
          pastMeanYieldTHa: 3.04,
          recentMeanYieldTHa: 2.38,
          predictedYieldTHa: 2.05,
          yieldChangePctRecentVsPast: -21.7,
          outlookLabel: 'YIELD_PRESSURE',
          narrative:
            'For Maize, past window averaged 3.04 t/ha; recent seasons averaged 2.38 t/ha. Under current national climate risk 63.8 (HIGH), the explainable outlook for the next season is 2.05 t/ha.',
          farmCount: 3,
          seasonSampleCount: 14,
          yearlySeries: [
            { year: 2016, meanYieldTHa: 3.4, sampleCount: 1 },
            { year: 2018, meanYieldTHa: 3.05, sampleCount: 2 },
            { year: 2020, meanYieldTHa: 2.73, sampleCount: 2 },
            { year: 2022, meanYieldTHa: 2.55, sampleCount: 2 },
            { year: 2024, meanYieldTHa: 2.3, sampleCount: 2 },
            { year: 2025, meanYieldTHa: 2.33, sampleCount: 2 }
          ]
        },
        {
          cropCode: 'BEANS',
          cropName: 'Beans',
          pastMeanYieldTHa: 1.3,
          recentMeanYieldTHa: 1.03,
          predictedYieldTHa: 0.89,
          yieldChangePctRecentVsPast: -20.8,
          outlookLabel: 'YIELD_PRESSURE',
          narrative:
            'For Beans, past window averaged 1.30 t/ha; recent seasons averaged 1.03 t/ha. Under current national climate risk 63.8 (HIGH), the explainable outlook for the next season is 0.89 t/ha.',
          farmCount: 1,
          seasonSampleCount: 10,
          yearlySeries: [
            { year: 2016, meanYieldTHa: 1.45, sampleCount: 1 },
            { year: 2020, meanYieldTHa: 1.2, sampleCount: 1 },
            { year: 2024, meanYieldTHa: 1.05, sampleCount: 1 },
            { year: 2025, meanYieldTHa: 1.0, sampleCount: 1 }
          ]
        },
        {
          cropCode: 'POTATO',
          cropName: 'Irish Potato',
          pastMeanYieldTHa: 11.14,
          recentMeanYieldTHa: 8.85,
          predictedYieldTHa: 7.65,
          yieldChangePctRecentVsPast: -20.6,
          outlookLabel: 'YIELD_PRESSURE',
          narrative:
            'For Irish Potato, past window averaged 11.14 t/ha; recent seasons averaged 8.85 t/ha. Under current national climate risk 63.8 (HIGH), the explainable outlook for the next season is 7.65 t/ha.',
          farmCount: 1,
          seasonSampleCount: 10,
          yearlySeries: [
            { year: 2016, meanYieldTHa: 12.0, sampleCount: 1 },
            { year: 2020, meanYieldTHa: 10.5, sampleCount: 1 },
            { year: 2024, meanYieldTHa: 9.0, sampleCount: 1 },
            { year: 2025, meanYieldTHa: 8.7, sampleCount: 1 }
          ]
        }
      ]
    };
  }
  if (path === '/api/v1/climate-intel/planning/ml-spike/maize') {
    return {
      cropCode: 'MAIZE',
      featureRowCount: 6,
      features: [
        { harvestYear: 2018, meanYieldTHa: 3.05, sampleCount: 2, farmCount: 2, lag1YieldTHa: 3.4, lag2YieldTHa: null, seasonRainMm: 910, yearIndex: 2 },
        { harvestYear: 2020, meanYieldTHa: 2.73, sampleCount: 2, farmCount: 2, lag1YieldTHa: 3.05, lag2YieldTHa: 3.4, seasonRainMm: 760, yearIndex: 4 },
        { harvestYear: 2022, meanYieldTHa: 2.55, sampleCount: 2, farmCount: 2, lag1YieldTHa: 2.73, lag2YieldTHa: 3.05, seasonRainMm: 740, yearIndex: 6 },
        { harvestYear: 2024, meanYieldTHa: 2.3, sampleCount: 2, farmCount: 2, lag1YieldTHa: 2.55, lag2YieldTHa: 2.73, seasonRainMm: 680, yearIndex: 8 },
        { harvestYear: 2025, meanYieldTHa: 2.33, sampleCount: 2, farmCount: 2, lag1YieldTHa: 2.3, lag2YieldTHa: 2.55, seasonRainMm: 660, yearIndex: 9 }
      ],
      leaveOneYearOut: [
        {
          holdoutYear: 2024,
          actualYieldTHa: 2.3,
          naiveLastYearPred: 2.55,
          ruleRecentMeanPred: 2.64,
          linearLagRainPred: 2.41,
          absErrorNaive: 0.25,
          absErrorRule: 0.34,
          absErrorLinear: 0.11
        }
      ],
      metrics: {
        folds: 4,
        maeNaiveLastYear: 0.22,
        maeRuleRecentMean: 0.28,
        maeLinearLagRain: 0.18,
        bestMethod: 'LINEAR_LAG_RAIN',
        linearBeatsRule: true
      },
      verdict: 'LINEAR_BEATS_RULE — candidate for Stage B batch inference after real yield/climate volume grows',
      notes: 'Demo stub for ML spike. Live API uses leave-one-year-out OLS on seeded maize + ML_SPIKE_DEMO rain.',
      generatedAt: new Date().toISOString()
    };
  }
  if (path === '/api/v1/climate-intel/alerts') {
    const rows = alerts.filter((a) => !statusFilter || a.status === statusFilter);
    return paged(rows);
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
      generatedAt: new Date().toISOString(),
      provinceCode: 'KIGALI',
      provinceName: 'Kigali City',
      agroecologicalZoneCode: 'E',
      agroecologicalZoneName: 'ZONE E',
      agroecologicalSubzoneCode: 'E1',
      agroecologicalSubzoneName: 'Northern/Highland Kigali'
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
      subjectsProcessed: 60,
      errorSummary: null,
      startedAt: new Date().toISOString(),
      completedAt: new Date().toISOString(),
      createdAt: new Date().toISOString()
    };
  }
  if (path === '/api/v1/climate-intel/indicators') {
    return paged([]);
  }
  if (path.startsWith('/api/v1/climate-intel/reports/')) {
    return { ok: true };
  }

  if (path === '/api/v1/loans' && verb === 'GET') {
    const rows = loans.filter(
      (l) =>
        (!statusFilter || l.status === statusFilter) &&
        (!q ||
          l.loanNumber.toLowerCase().includes(q) ||
          l.borrowerName.toLowerCase().includes(q) ||
          l.farmerCode.toLowerCase().includes(q))
    );
    return paged(rows);
  }
  const loanId = idFrom(path, '/api/v1/loans');
  if (loanId && path === `/api/v1/loans/${loanId}`) {
    return owned(
      loans.find((l) => l.id === loanId),
      true
    );
  }

  if (path === '/api/v1/satellite/summary') {
    const stressed = satelliteScenes.filter((s) => s.vegetationHealth === 'STRESSED' || s.environmentalStress === 'HIGH');
    return {
      sceneCount: satelliteScenes.length,
      stressedDistricts: new Set(stressed.map((s) => s.districtCode)).size,
      highDrought: satelliteScenes.filter((s) => s.droughtSeverity === 'HIGH').length,
      generatedAt: new Date().toISOString(),
      scenes: satelliteScenes
    };
  }

  if (path === '/api/v1/insured-inputs' && verb === 'GET') {
    const rows = inputSales.filter(
      (s) =>
        !q ||
        s.receiptNumber.toLowerCase().includes(q) ||
        s.farmerName.toLowerCase().includes(q) ||
        s.productName.toLowerCase().includes(q)
    );
    return paged(rows);
  }

  if (path === '/api/v1/recommendations') {
    const rows = isFarmerUser(user)
      ? recommendations.filter((r) => r.farmerId === user.farmerId)
      : recommendations;
    return rows;
  }

  if (path === '/api/v1/users' && verb === 'GET') {
    return paged(
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
  const userAuditMatch = path.match(/^\/api\/v1\/users\/([^/]+)\/audit$/);
  if (userAuditMatch && verb === 'GET') {
    return [];
  }
  const userOneMatch = path.match(/^\/api\/v1\/users\/([^/]+)$/);
  if (userOneMatch && verb === 'GET') {
    const op = OPERATORS.find((o) => o.user.id === userOneMatch[1]);
    if (!op) notFound();
    return {
      id: op.user.id,
      username: op.user.username,
      email: op.user.email,
      displayName: op.user.displayName,
      role: op.user.roles[0],
      roles: op.user.roles,
      status: 'ACTIVE'
    };
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

  return paged([]);
}
