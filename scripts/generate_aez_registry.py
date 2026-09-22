# Generate owner AEZ farmer registry (450 rows)
import json
from pathlib import Path

NAMES = [
    "Eric Niyonzima",
    "Emmanuel Munyaneza",
    "Claude Uwamahoro",
    "Innocent Ingabire",
    "Theogene Nkurunziza",
    "Aline Uwamariya",
    "Beatrice Mugabo",
    "Immaculee Nsengimana",
    "Chantal Mukamana",
    "Ange Mutesi",
    "Mugisha Dusenge",
    "Nadine Ishimwe",
    "Jean Mugisha",
    "Patrick Twagirimana",
    "Samuel Uwimana",
    "David Iradukunda",
    "Dieudonne Kayitesi",
    "Grace Hategekimana",
    "Diane Niyibizi",
    "Claudine Habimana",
    "Vestine Nshimiyimana",
    "Jeanne Niyomugabo",
    "Pacifique Mukeshimana",
    "Uwase Bizimana",
    "Olivia Ndayisenga",
]

BLOCKS = [
    (
        "ZONE A",
        "A1",
        "Sub-zone A1_Volcanic Highlands",
        ["Potato", "Maize", "Beans", "Wheat"],
        [
            ("Musanze", "Muhoza", "Cyabararika"),
            ("Burera", "Kinoni", "Kivumu"),
            ("Musanze", "Shingiro", "Kagano"),
            ("Burera", "Cyanika", "Gaseke"),
            ("Musanze", "Kinigi", "Nyarugina"),
            ("Burera", "Rugarama", "Ruyumba"),
        ],
        0.61,
    ),
    (
        "ZONE A",
        "A2",
        "Sub-zone A2_Buberuka Highlands",
        ["Potato", "Maize", "Beans", "Wheat"],
        [
            ("Gicumbi", "Byumba", "Gacurabwenge"),
            ("Rulindo", "Bushoki", "Bushoki"),
            ("Gicumbi", "Rukomo", "Rukomo"),
            ("Rulindo", "Base", "Base"),
            ("Gicumbi", "Kageyo", "Kageyo"),
            ("Rulindo", "Kinihira", "Kinihira"),
        ],
        3.36,
    ),
    (
        "ZONE A",
        "A3",
        "Sub-zone A3_Northern Highland Transition",
        ["Potato", "Maize", "Beans", "Wheat"],
        [
            ("Gakenke", "Muzo", "Muzo"),
            ("Gakenke", "Rushashi", "Rushashi"),
            ("Gakenke", "Kivuruga", "Kivuruga"),
        ],
        2.61,
    ),
    (
        "ZONE B",
        "B1",
        "Sub-zone B1_Southern Highlands",
        ["Maize", "Beans", "Irish Potato", "Sweet Potato"],
        [
            ("Nyaruguru", "Kibeho", "Kibeho"),
            ("Nyamagabe", "Kaduha", "Kaduha"),
            ("Nyaruguru", "Ruheru", "Ruheru"),
            ("Nyamagabe", "Gasaka", "Gasaka"),
            ("Nyaruguru", "Munini", "Munini"),
            ("Nyamagabe", "Uwinkingi", "Uwinkingi"),
        ],
        1.86,
    ),
    (
        "ZONE B",
        "B2",
        "Sub-zone B2_ Southern Central Plateau",
        ["Maize", "Beans", "Irish Potato", "Sweet Potato"],
        [
            ("Huye", "Ngoma", "Ngoma"),
            ("Gisagara", "Mamba", "Mamba"),
            ("Nyanza", "Muyira", "Muyira"),
        ],
        1.11,
    ),
    (
        "ZONE B",
        "B3",
        "Sub-zone B3_ Mayaga / Southern Lowlands",
        ["Maize", "Beans", "Irish Potato", "Sweet Potato"],
        [
            ("Ruhango", "Kinazi", "Kinazi"),
            ("Ruhango", "Byimana", "Byimana"),
            ("Ruhango", "Mbuye", "Mbuye"),
        ],
        3.86,
    ),
    (
        "ZONE B",
        "B4",
        "Sub-zone B4_ Central-Southern Plateau",
        ["Maize", "Beans", "Irish Potato", "Sweet Potato"],
        [
            ("Muhanga", "Nyamabuye", "Nyamabuye"),
            ("Kamonyi", "Gacurabwenge", "Gacurabwenge"),
            ("Muhanga", "Kibangu", "Kibangu"),
            ("Kamonyi", "Runda", "Runda"),
            ("Muhanga", "Shyogwe", "Shyogwe"),
            ("Kamonyi", "Kayenzi", "Kayenzi"),
        ],
        3.11,
    ),
    (
        "ZONE C",
        "C1",
        "Sb-zone C1_Eastern Dry Savanna",
        ["Maize", "Beans", "Sorghum", "Cassava"],
        [
            ("Nyagatare", "Nyagatare", "Nyagatare"),
            ("Gatsibo", "Kiziguro", "Kiziguro"),
            ("Nyagatare", "Karangazi", "Karangazi"),
            ("Gatsibo", "Kabarore", "Kabarore"),
            ("Nyagatare", "Mimuri", "Mimuri"),
            ("Gatsibo", "Muhura", "Muhura"),
        ],
        2.36,
    ),
    (
        "ZONE C",
        "C2",
        "Sb-zone C2_Eastern Plateau",
        ["Maize", "Beans", "Sorghum", "Cassava"],
        [
            ("Kayonza", "Mukarange", "Mukarange"),
            ("Rwamagana", "Kigabiro", "Kigabiro"),
            ("Kayonza", "Ndego", "Ndego"),
            ("Rwamagana", "Muhazi", "Muhazi"),
            ("Kayonza", "Nyamirama", "Nyamirama"),
            ("Rwamagana", "Nyakaliro", "Nyakaliro"),
        ],
        1.61,
    ),
    (
        "ZONE C",
        "C3",
        "Sb-zone C3_Southeastern Lowlands",
        ["Maize", "Beans", "Sorghum", "Cassava"],
        [
            ("Kirehe", "Kirehe", "Kirehe"),
            ("Ngoma", "Karembo", "Karembo"),
            ("Kirehe", "Nyamugari", "Nyamugari"),
            ("Ngoma", "Kibungo", "Kibungo"),
            ("Kirehe", "Gatore", "Gatore"),
            ("Ngoma", "Rukira", "Rukira"),
        ],
        0.86,
    ),
    (
        "ZONE C",
        "C4",
        "Sub-zone C4_Bugesera Climate Zone",
        ["Maize", "Beans", "Sorghum", "Cassava"],
        [
            ("Bugesera", "Nyamata", "Nyamata"),
            ("Bugesera", "Mayange", "Mayange"),
            ("Bugesera", "Juru", "Juru"),
        ],
        3.61,
    ),
    (
        "ZONE D",
        "D1",
        "Sub-zone D1_ Volcano / High-Rainfall Zone",
        ["Maize", "Beans", "Cassava", "Banana"],
        [
            ("Rubavu", "Gisenyi", "Gisenyi"),
            ("Nyabihu", "Jenda", "Jenda"),
            ("Rubavu", "Mudende", "Mudende"),
            ("Nyabihu", "Mukamira", "Mukamira"),
            ("Rubavu", "Nyundo", "Nyundo"),
            ("Nyabihu", "Rurembo", "Rurembo"),
        ],
        2.86,
    ),
    (
        "ZONE D",
        "D2",
        "Sub-zone D2_Congo-Nile Highlands",
        ["Maize", "Beans", "Cassava", "Banana"],
        [
            ("Rutsiro", "Gihango", "Gihango"),
            ("Ngororero", "Kageyo", "Kageyo"),
            ("Rutsiro", "Kivumu", "Kivumu"),
            ("Ngororero", "Ngororero", "Ngororero"),
            ("Rutsiro", "Kigeyo", "Kigeyo"),
            ("Ngororero", "Muhanda", "Muhanda"),
        ],
        2.11,
    ),
    (
        "ZONE D",
        "D3",
        "Sub-zone D3_ Lake Kivu Highlands",
        ["Maize", "Beans", "Cassava", "Banana"],
        [
            ("Karongi", "Bwishyura", "Bwishyura"),
            ("Nyamasheke", "Kanjongo", "Kanjongo"),
            ("Karongi", "Gitesi", "Gitesi"),
            ("Nyamasheke", "Kagano", "Kagano"),
            ("Karongi", "Rubengera", "Rubengera"),
            ("Nyamasheke", "Ruharambuga", "Ruharambuga"),
        ],
        1.36,
    ),
    (
        "ZONE D",
        "D4",
        "Sub-zone D4_Western Lowland / Bugarama",
        ["Maize", "Beans", "Cassava", "Banana"],
        [
            ("Rusizi", "Bugarama", "Bugarama"),
            ("Rusizi", "Kamembe", "Kamembe"),
            ("Rusizi", "Gihundwe", "Gihundwe"),
        ],
        0.61,
    ),
    (
        "ZONE E",
        "E1",
        "Sub-zone E1_Northern/Highland Kigali",
        ["Maize", "Beans", "Vegetables", "Avocado"],
        [
            ("Gasabo", "Jabana", "Jabana"),
            ("Gasabo", "Rutunga", "Rutunga"),
            ("Gasabo", "Gikomero", "Gikomero"),
        ],
        3.36,
    ),
    (
        "ZONE E",
        "E2",
        "Sub-zone E2_Central Kigali",
        ["Maize", "Beans", "Vegetables", "Avocado"],
        [
            ("Nyarugenge", "Nyamirambo", "Nyamirambo"),
            ("Nyarugenge", "Kigali", "Kigali"),
            ("Nyarugenge", "Muhima", "Muhima"),
        ],
        2.61,
    ),
    (
        "ZONE E",
        "E3",
        "Sub-zone E3_Southern Kigali",
        ["Maize", "Beans", "Vegetables", "Avocado"],
        [
            ("Kicukiro", "Masaka", "Masaka"),
            ("Kicukiro", "Nyarugunga", "Nyarugunga"),
            ("Kicukiro", "Gahanga", "Gahanga"),
        ],
        1.86,
    ),
]


def size_seq(start: float, n: int = 25) -> list[float]:
    out: list[float] = []
    v = start
    for _ in range(n):
        vv = round(v, 2)
        out.append(vv)
        v = round(v + 0.48, 2)
        if v > 3.99:
            v = round(v - 3.5, 2)
    return out


rows = []
fid = 1
for zone, code, label, crops, locs, start in BLOCKS:
    sizes = size_seq(start, 25)
    for i in range(25):
        name = NAMES[i]
        first, last = name.split(" ", 1)
        district, sector, cell = locs[i % len(locs)]
        crop = crops[i % len(crops)]
        phone = f"+250 789{str(fid).zfill(6)}"
        rows.append(
            {
                "farmerId": f"AGT-{str(fid).zfill(4)}",
                "mainZone": zone,
                "subzoneCode": code,
                "subzone": label,
                "farmerName": name,
                "firstName": first,
                "lastName": last,
                "phoneNumber": phone,
                "crop": crop,
                "fieldSizeHa": sizes[i],
                "district": district,
                "sector": sector,
                "cell": cell,
                "registrationStatus": "Registered",
            }
        )
        fid += 1

assert len(rows) == 450, len(rows)
assert rows[0]["farmerId"] == "AGT-0001"
assert rows[0]["district"] == "Musanze" and rows[0]["crop"] == "Potato"
assert rows[25]["subzoneCode"] == "A2"
assert rows[449]["farmerId"] == "AGT-0450"

root = Path(r"c:\Users\s.shyaka\Desktop\GRACE PROJECT\AegisTerra\frontend\src\demo")
(root / "aezFarmerRegistry.json").write_text(json.dumps(rows, indent=2), encoding="utf-8")
(root / "aezFarmerRegistry.ts").write_text(
    "export type AezFarmerRecord = {\n"
    "  farmerId: string;\n"
    "  mainZone: string;\n"
    "  subzoneCode: string;\n"
    "  subzone: string;\n"
    "  farmerName: string;\n"
    "  firstName: string;\n"
    "  lastName: string;\n"
    "  phoneNumber: string;\n"
    "  crop: string;\n"
    "  fieldSizeHa: number;\n"
    "  district: string;\n"
    "  sector: string;\n"
    "  cell: string;\n"
    "  registrationStatus: string;\n"
    "};\n\n"
    f"export const AEZ_FARMER_REGISTRY: AezFarmerRecord[] = {json.dumps(rows, indent=2)};\n",
    encoding="utf-8",
)
print("ok", len(rows), "A1 sizes", size_seq(0.61)[:5], "...", size_seq(0.61)[-3:])
