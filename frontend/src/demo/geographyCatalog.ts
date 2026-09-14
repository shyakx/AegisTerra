import type { AgroecologicalSubzone, AgroecologicalZone, District, Province } from '../api/geography';

const PROVINCES: Province[] = [
  { id: '44444444-4444-4444-4444-444444440001', code: 'NORTH', name: 'Northern Province', status: 'ACTIVE' },
  { id: '44444444-4444-4444-4444-444444440002', code: 'SOUTH', name: 'Southern Province', status: 'ACTIVE' },
  { id: '44444444-4444-4444-4444-444444440003', code: 'EAST', name: 'Eastern Province', status: 'ACTIVE' },
  { id: '44444444-4444-4444-4444-444444440004', code: 'WEST', name: 'Western Province', status: 'ACTIVE' },
  { id: '44444444-4444-4444-4444-444444440005', code: 'KIGALI', name: 'Kigali City', status: 'ACTIVE' }
];

const ZONES: AgroecologicalZone[] = [
  { id: '55555555-5555-5555-5555-555555550001', code: 'A', name: 'ZONE A', status: 'ACTIVE' },
  { id: '55555555-5555-5555-5555-555555550002', code: 'B', name: 'ZONE B', status: 'ACTIVE' },
  { id: '55555555-5555-5555-5555-555555550003', code: 'C', name: 'ZONE C', status: 'ACTIVE' },
  { id: '55555555-5555-5555-5555-555555550004', code: 'D', name: 'ZONE D', status: 'ACTIVE' },
  { id: '55555555-5555-5555-5555-555555550005', code: 'E', name: 'ZONE E', status: 'ACTIVE' }
];

const SUBZONES: AgroecologicalSubzone[] = [
  { id: '66666666-6666-6666-6666-666666660001', code: 'A1', name: 'Volcanic Highlands', status: 'ACTIVE', zoneId: ZONES[0].id, zoneCode: 'A', zoneName: 'ZONE A' },
  { id: '66666666-6666-6666-6666-666666660002', code: 'A2', name: 'Buberuka Highlands', status: 'ACTIVE', zoneId: ZONES[0].id, zoneCode: 'A', zoneName: 'ZONE A' },
  { id: '66666666-6666-6666-6666-666666660003', code: 'A3', name: 'Northern Highland Transition', status: 'ACTIVE', zoneId: ZONES[0].id, zoneCode: 'A', zoneName: 'ZONE A' },
  { id: '66666666-6666-6666-6666-666666660004', code: 'B1', name: 'Southern Highlands', status: 'ACTIVE', zoneId: ZONES[1].id, zoneCode: 'B', zoneName: 'ZONE B' },
  { id: '66666666-6666-6666-6666-666666660005', code: 'B2', name: 'Southern Central Plateau', status: 'ACTIVE', zoneId: ZONES[1].id, zoneCode: 'B', zoneName: 'ZONE B' },
  { id: '66666666-6666-6666-6666-666666660006', code: 'B3', name: 'Mayaga / Southern Lowlands', status: 'ACTIVE', zoneId: ZONES[1].id, zoneCode: 'B', zoneName: 'ZONE B' },
  { id: '66666666-6666-6666-6666-666666660007', code: 'B4', name: 'Central-Southern Plateau', status: 'ACTIVE', zoneId: ZONES[1].id, zoneCode: 'B', zoneName: 'ZONE B' },
  { id: '66666666-6666-6666-6666-666666660008', code: 'C1', name: 'Eastern Dry Savanna', status: 'ACTIVE', zoneId: ZONES[2].id, zoneCode: 'C', zoneName: 'ZONE C' },
  { id: '66666666-6666-6666-6666-666666660009', code: 'C2', name: 'Eastern Plateau', status: 'ACTIVE', zoneId: ZONES[2].id, zoneCode: 'C', zoneName: 'ZONE C' },
  { id: '66666666-6666-6666-6666-666666660010', code: 'C3', name: 'Southeastern Lowlands', status: 'ACTIVE', zoneId: ZONES[2].id, zoneCode: 'C', zoneName: 'ZONE C' },
  { id: '66666666-6666-6666-6666-666666660011', code: 'C4', name: 'Bugesera Climate Zone', status: 'ACTIVE', zoneId: ZONES[2].id, zoneCode: 'C', zoneName: 'ZONE C' },
  { id: '66666666-6666-6666-6666-666666660012', code: 'D1', name: 'Volcano / High-Rainfall Zone', status: 'ACTIVE', zoneId: ZONES[3].id, zoneCode: 'D', zoneName: 'ZONE D' },
  { id: '66666666-6666-6666-6666-666666660013', code: 'D2', name: 'Congo-Nile Highlands', status: 'ACTIVE', zoneId: ZONES[3].id, zoneCode: 'D', zoneName: 'ZONE D' },
  { id: '66666666-6666-6666-6666-666666660014', code: 'D3', name: 'Lake Kivu Highlands', status: 'ACTIVE', zoneId: ZONES[3].id, zoneCode: 'D', zoneName: 'ZONE D' },
  { id: '66666666-6666-6666-6666-666666660015', code: 'D4', name: 'Western Lowland / Bugarama', status: 'ACTIVE', zoneId: ZONES[3].id, zoneCode: 'D', zoneName: 'ZONE D' },
  { id: '66666666-6666-6666-6666-666666660016', code: 'E1', name: 'Northern/Highland Kigali', status: 'ACTIVE', zoneId: ZONES[4].id, zoneCode: 'E', zoneName: 'ZONE E' },
  { id: '66666666-6666-6666-6666-666666660017', code: 'E2', name: 'Central Kigali', status: 'ACTIVE', zoneId: ZONES[4].id, zoneCode: 'E', zoneName: 'ZONE E' },
  { id: '66666666-6666-6666-6666-666666660018', code: 'E3', name: 'Southern Kigali', status: 'ACTIVE', zoneId: ZONES[4].id, zoneCode: 'E', zoneName: 'ZONE E' }
];

const DISTRICT_ROWS: Array<[string, string, string, string, string]> = [
  ['01', 'BURERA', 'Burera', 'NORTH', 'A1'],
  ['02', 'MUSANZE', 'Musanze', 'NORTH', 'A1'],
  ['03', 'GICUMBI', 'Gicumbi', 'NORTH', 'A2'],
  ['04', 'RULINDO', 'Rulindo', 'NORTH', 'A2'],
  ['05', 'GAKENKE', 'Gakenke', 'NORTH', 'A3'],
  ['06', 'NYAMAGABE', 'Nyamagabe', 'SOUTH', 'B1'],
  ['07', 'NYARUGURU', 'Nyaruguru', 'SOUTH', 'B1'],
  ['08', 'GISAGARA', 'Gisagara', 'SOUTH', 'B2'],
  ['09', 'HUYE', 'Huye', 'SOUTH', 'B2'],
  ['10', 'NYANZA', 'Nyanza', 'SOUTH', 'B2'],
  ['11', 'RUHANGO', 'Ruhango', 'SOUTH', 'B3'],
  ['12', 'KAMONYI', 'Kamonyi', 'SOUTH', 'B4'],
  ['13', 'MUHANGA', 'Muhanga', 'SOUTH', 'B4'],
  ['14', 'GATSIBO', 'Gatsibo', 'EAST', 'C1'],
  ['15', 'NYAGATARE', 'Nyagatare', 'EAST', 'C1'],
  ['16', 'KAYONZA', 'Kayonza', 'EAST', 'C2'],
  ['17', 'RWAMAGANA', 'Rwamagana', 'EAST', 'C2'],
  ['18', 'KIREHE', 'Kirehe', 'EAST', 'C3'],
  ['19', 'NGOMA', 'Ngoma', 'EAST', 'C3'],
  ['20', 'BUGESERA', 'Bugesera', 'EAST', 'C4'],
  ['21', 'NYABIHU', 'Nyabihu', 'WEST', 'D1'],
  ['22', 'RUBAVU', 'Rubavu', 'WEST', 'D1'],
  ['23', 'NGORORERO', 'Ngororero', 'WEST', 'D2'],
  ['24', 'RUTSIRO', 'Rutsiro', 'WEST', 'D2'],
  ['25', 'KARONGI', 'Karongi', 'WEST', 'D3'],
  ['26', 'NYAMASHEKE', 'Nyamasheke', 'WEST', 'D3'],
  ['27', 'RUSIZI', 'Rusizi', 'WEST', 'D4'],
  ['28', 'GASABO', 'Gasabo', 'KIGALI', 'E1'],
  ['29', 'NYARUGENGE', 'Nyarugenge', 'KIGALI', 'E2'],
  ['30', 'KICUKIRO', 'Kicukiro', 'KIGALI', 'E3']
];

const DISTRICTS: District[] = DISTRICT_ROWS.map(([suffix, code, name, provinceCode, subzoneCode]) => {
  const province = PROVINCES.find((row) => row.code === provinceCode)!;
  const subzone = SUBZONES.find((row) => row.code === subzoneCode)!;
  return {
    id: `77777777-7777-7777-7777-7777777700${suffix}`,
    code,
    name,
    status: 'ACTIVE',
    provinceId: province.id,
    provinceCode: province.code,
    provinceName: province.name,
    agroecologicalSubzoneId: subzone.id,
    agroecologicalSubzoneCode: subzone.code,
    agroecologicalSubzoneName: subzone.name,
    agroecologicalZoneId: subzone.zoneId,
    agroecologicalZoneCode: subzone.zoneCode,
    agroecologicalZoneName: subzone.zoneName
  };
});

function byName<T extends { name: string }>(rows: T[]): T[] {
  return [...rows].sort((a, b) => a.name.localeCompare(b.name));
}

function byCode<T extends { code: string }>(rows: T[]): T[] {
  return [...rows].sort((a, b) => a.code.localeCompare(b.code));
}

export const demoGeography = {
  provinces: () => byName(PROVINCES),
  province: (id: string) => PROVINCES.find((row) => row.id === id),
  districts: (provinceId?: string | null) =>
    byName(provinceId ? DISTRICTS.filter((row) => row.provinceId === provinceId) : DISTRICTS),
  district: (id: string) => DISTRICTS.find((row) => row.id === id),
  zones: () => byCode(ZONES),
  zone: (id: string) => ZONES.find((row) => row.id === id),
  subzones: (zoneId?: string | null, districtId?: string | null) => {
    if (districtId) {
      const district = DISTRICTS.find((row) => row.id === districtId);
      const subzone = district ? SUBZONES.find((row) => row.id === district.agroecologicalSubzoneId) : undefined;
      if (!subzone) return [];
      if (zoneId && subzone.zoneId !== zoneId) return [];
      return [subzone];
    }
    return byCode(zoneId ? SUBZONES.filter((row) => row.zoneId === zoneId) : SUBZONES);
  },
  subzone: (id: string) => SUBZONES.find((row) => row.id === id)
};
