"""Build a clean star-schema workbook (MGR-Model.xlsx) from the monthly
management-report workbook (MGR-<date>.xlsx).

* Reads the source read-only; never writes to it.
* Every number in the output comes from a source cell (no new figures);
  only reshaping (unpivot), unit scaling (0-100 -> 0-1) and name cleaning.
* A `Check` sheet compares totals between source and model.

Usage:
    python -I build_model.py SOURCE.xlsx OUTPUT.xlsx
"""
import sys
import datetime as dt

import pandas as pd
from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.table import Table, TableStyleInfo

SRC, OUT = sys.argv[1], sys.argv[2]
NAVY = "1F3864"

# ---------------------------------------------------------------- helpers
CONTRACTOR_ALIASES = {
    "Ehdas": "EHD", "EHD": "EHD",
    "Souleh Iran": "SOL", "SOL": "SOL", "Soleh": "SOL",
    "Sekaf": "FRJ", "FRJ": "FRJ", "Farjoud - Sekaf": "FRJ", "Farjoud": "FRJ",
    "EXIR": "EXR",
    "Overall": "ALL",
}
DIM_CONTRACTOR = pd.DataFrame([
    # Code, Name, Status, SortOrder
    ("SOL", "Souleh Iran", "Active", 1),
    ("FRJ", "Sekaf & Farjoud", "Active", 2),
    ("EHD", "Ehdas", "Active", 3),
    ("EXR", "EXIR", "Inactive (history kept)", 4),
    ("ALL", "Overall", "Total row (not additive)", 0),
], columns=["ContractorCode", "ContractorName", "Status", "SortOrder"])
DIM_CONTRACTOR["IsTotal"] = DIM_CONTRACTOR.ContractorCode == "ALL"

ITEM_FIX = {"Hysro Test": "Hydro Test", "Formworking": "Formwork",
            "Fit-Up": "Fit Up", "Weld": "Welding"}


def sheet(name, **kw):
    return pd.read_excel(SRC, sheet_name=name, **kw)


def clean_item(s):
    s = str(s).strip()
    return ITEM_FIX.get(s, s)


def to_num(s):
    return pd.to_numeric(s, errors="coerce")


def jalali(gy, gm, gd):
    """Gregorian -> Jalali (algorithm by K. Borkowski, public domain)."""
    g_d_m = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334]
    gy2 = gy + 1 if gm > 2 else gy
    days = (355666 + 365 * gy + (gy2 + 3) // 4 - (gy2 + 99) // 100
            + (gy2 + 399) // 400 + gd + g_d_m[gm - 1])
    jy = -1595 + 33 * (days // 12053)
    days %= 12053
    jy += 4 * (days // 1461)
    days %= 1461
    if days > 365:
        jy += (days - 1) // 365
        days = (days - 1) % 365
    if days < 186:
        jm, jd = 1 + days // 31, 1 + days % 31
    else:
        jm, jd = 7 + (days - 186) // 30, 1 + (days - 186) % 30
    return jy, jm, jd


JMONTHS = ["فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
           "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"]

tables = {}      # name -> DataFrame
checks = []      # (Check, Source, SourceValue, ModelValue)


def chk(name, source, src_val, model_val):
    checks.append((name, source, float(src_val), float(model_val)))


# ---------------------------------------------------------------- snapshot
t6 = sheet("Time 6 Month").dropna(how="all", axis=1)
cutoff = pd.to_datetime(t6.loc[0, "Cut Of Date"]).date()
ov_plan = sheet("1-Overall Plan").loc[0, "Plan %"]
ov_act = sheet("1-Overall Actual").loc[0, "Actual %"]
mp = sheet("1-Manpower")
spi6 = sheet("SPI-6M").dropna(how="all", axis=1)
snap = [
    ("CutOffDate", "Report cut-off date", cutoff, "date", "Time 6 Month!C2"),
    ("OverallPlanPct", "Overall Plan % (cumulative)", ov_plan, "0-1", "1-Overall Plan!C2"),
    ("OverallActualPct", "Overall Actual % (cumulative)", ov_act, "0-1", "1-Overall Actual!C2"),
    ("TotalManhours", "Total manpower till now (Man-hour)", mp.iloc[0, 0], "Mh", "1-Manpower!A2"),
    ("TotalMachineHours", "Machinery till now (Machine-hour)", mp.iloc[0, 1], "Mach-h", "1-Manpower!B2"),
    ("Window6MStart", "6-month window start", pd.to_datetime(t6.loc[0, "Start"]).date(), "date", "Time 6 Month!A2"),
    ("Window6MFinish", "6-month window finish", pd.to_datetime(t6.loc[0, "Finish"]).date(), "date", "Time 6 Month!B2"),
    ("Window6MRemainingDays", "6-month window remaining days", t6.loc[0, "Remaining Days"], "days", "Time 6 Month!D2"),
]
for col in spi6.columns:
    snap.append((f"SPI6M_{col.replace(' ', '')}", f"6M SPI - {col} (typed by hand in source)",
                 spi6.loc[0, col], "ratio", "SPI-6M (no formula)"))
snap_df = pd.DataFrame(snap, columns=["Key", "Description", "Value", "Unit", "SourceCell"])
is_date = snap_df.Unit == "date"
snap_df.insert(3, "ValueDate", snap_df.Value.where(is_date))
snap_df["Value"] = pd.to_numeric(snap_df.Value.where(~is_date), errors="coerce")
tables["Snapshot"] = snap_df

# ---------------------------------------------------------------- DimScope / FactSCurve (daily, overall)
sc = sheet("1-Overall S- Curve", usecols="A:E")
sc = sc.dropna(subset=["Date"])
scope_names = {"Overall": "Overall Project", "MOB&DMOB": "Mobilization & Demobilization",
               "Cons": "Construction & Installation", "Pre-Com": "Pre-Commissioning",
               "Com": "Commissioning"}
dim_scope = (sc[["A", "A1"]].drop_duplicates()
             .rename(columns={"A": "ScopeOrder", "A1": "ScopeCode"}))
dim_scope["ScopeName"] = dim_scope["ScopeCode"].map(scope_names)
w = sheet("1-WBS").dropna(how="all", axis=1)
wf = dict(zip(w["Title"].str.strip(), w["W.F"]))
dim_scope["Weight"] = dim_scope["ScopeName"].map(wf)
dim_scope.loc[dim_scope.ScopeCode == "Overall", "Weight"] = 1.0
tables["DimScope"] = dim_scope[["ScopeOrder", "ScopeCode", "ScopeName", "Weight"]]

f_sc = pd.DataFrame({
    "Date": pd.to_datetime(sc["Date"]).dt.date,
    "ScopeCode": sc["A1"],
    "CumPlan": to_num(sc["Cum Plan"]),
    "CumActual": to_num(sc["Cum Actual"]),
})
tables["FactSCurveDaily"] = f_sc
o = f_sc[(f_sc.ScopeCode == "Overall") & (f_sc.Date == cutoff)].iloc[0]
chk("Overall Plan % at cut-off", "1-Overall Plan!C2", ov_plan, o.CumPlan)
chk("Overall Actual % at cut-off", "1-Overall Actual!C2", ov_act, o.CumActual)
chk("S-curve rows", "1-Overall S- Curve (rows with date)", len(sc), len(f_sc))

# ---------------------------------------------------------------- WBS level-1 snapshot
f_wbs = pd.DataFrame({
    "WBSOrder": w["Row"], "WBSName": w["Title"].str.strip(), "Weight": w["W.F"],
    "PlanPct": to_num(w["Plan% "]), "ActualPct": to_num(w["Actual%"]),
})
tables["FactWBSStatus"] = f_wbs
chk("WBS weights sum", "1-WBS!C", w["W.F"].sum(), f_wbs.Weight.sum())
chk("WBS weighted Plan % = overall Plan %", "1-Overall Plan!C2", ov_plan, (f_wbs.Weight * f_wbs.PlanPct).sum())
chk("WBS weighted Actual % = overall Actual %", "1-Overall Actual!C2", ov_act, (f_wbs.Weight * f_wbs.ActualPct).sum())

# ---------------------------------------------------------------- Discipline (overall, 0-100 -> 0-1)
d = sheet("03-Discipline  Status Cons").dropna(how="all", axis=1).dropna(subset=["Discipline"])
f_disc = pd.DataFrame({
    "DisciplineOrder": d["Row"].astype(int), "Discipline": d["Discipline"].str.strip(),
    "Weight": to_num(d["W.F"]) / 100, "PlanPct": to_num(d["Plan%"]) / 100,
    "ActualPct": to_num(d["Actual%"]) / 100, "SPI_Source": to_num(d["SPI"]),
})
tables["DimDiscipline"] = f_disc[["DisciplineOrder", "Discipline"]]
tables["FactDisciplineStatus"] = f_disc
chk("Discipline weights sum (x100)", "03-Discipline!C", to_num(d["W.F"]).sum(), f_disc.Weight.sum() * 100)
chk("Discipline weighted Plan % = overall Plan %", "1-Overall Plan!C2", ov_plan,
    (f_disc.Weight * f_disc.PlanPct.fillna(0)).sum() / f_disc.Weight.sum())
chk("Discipline weighted Actual % = overall Actual %", "1-Overall Actual!C2", ov_act,
    (f_disc.Weight * f_disc.ActualPct.fillna(0)).sum() / f_disc.Weight.sum())

# ---------------------------------------------------------------- EST2: quantities by area/unit + discipline by unit
e = sheet("EST2")
q = e.iloc[:, :8].dropna(subset=["Activity"]).copy()
q.columns = ["Activity", "Area", "Unit", "Estimate", "DWGReceived", "Done", "Remain", "WorkFront"]
q["Area"] = q["Area"].str.replace(r"\s+", " ", regex=True).str.strip()
q["Unit"] = q["Unit"].astype(str)
sub = q[q.Unit == "Overall"]
det = q[q.Unit != "Overall"].copy()
for c in ["Estimate", "DWGReceived", "Done", "Remain", "WorkFront"]:
    det[c] = to_num(det[c])
tables["FactQuantityByUnit"] = det
for act in det.Activity.unique():
    for c in ["Estimate", "Done"]:
        chk(f"EST2 {act} {c}: detail rows = 'Overall' subtotal rows",
            "EST2 (Unit = Overall)", to_num(sub[sub.Activity == act][c]).sum(),
            det[det.Activity == act][c].sum())

du = e.iloc[:, [2] + list(range(14, 18))].dropna(subset=["Discipline"]).copy()
du.columns = ["Unit", "Discipline", "WeightInProject", "PlanPct", "ActualPct"]
du["Unit"] = du["Unit"].astype(str)
du["Discipline"] = du["Discipline"].str.strip()
tables["FactDisciplineByUnit"] = du[du.Unit != "Overall"].reset_index(drop=True)
units = sorted(set(det.Unit) | set(tables["FactDisciplineByUnit"].Unit), key=lambda u: (len(u), u))
tables["DimUnit"] = pd.DataFrame({"Unit": units, "UnitLabel": ["Unit " + u for u in units]})

# ---------------------------------------------------------------- Major items (overall) + 6M window
e1 = sheet("Estimate 1").dropna(how="all", axis=1)
f_mi = pd.DataFrame({
    "ItemOrder": e1["Row"], "Item": e1["Activity"].str.strip(), "UOM": e1["Unit"],
    "Estimate": to_num(e1["Estimate"]), "PlanQty": to_num(e1["Plan"]), "DoneQty": to_num(e1["Done"]),
    "RemainQty": to_num(e1["Remain"]), "WorkFront": to_num(e1["Work Front"]),
})
tables["FactMajorItems"] = f_mi
chk("Major items Estimate total", "Estimate 1!D", to_num(e1["Estimate"]).sum(), f_mi.Estimate.sum())

m6 = sheet("6M").dropna(how="all", axis=1)
f6 = pd.DataFrame({
    "ContractorCode": m6["A"].map(CONTRACTOR_ALIASES), "Item": m6["Activity"].str.strip(),
    "UOM": m6["Unit"], "Estimate6M": to_num(m6["Estimate"]), "PlanQty6M": to_num(m6["Plan"]),
    "DoneQty6M": to_num(m6["Done"]), "RemainQty6M": to_num(m6["Remain"]),
})
tables["FactMajorItems6M"] = f6

# 6M monthly by item (wide 'Discipline 1/2/3' -> long)
mm = sheet("6M-S-curve-m").dropna(how="all", axis=1)
rows = []
for i in (1, 2, 3):
    part = mm[["Date", f"Discipline {i}", f"Plan {i}", f"Actual {i}"]].copy()
    part.columns = ["PeriodEnd", "Item", "PlanQty", "ActualQty"]
    rows.append(part)
f6m = pd.concat(rows, ignore_index=True)
f6m["PeriodEnd"] = pd.to_datetime(f6m["PeriodEnd"]).dt.date
tables["FactItemMonthly6M"] = f6m
chk("6M monthly plan qty total", "6M-S-curve-m Plan 1+2+3",
    sum(to_num(mm[f"Plan {i}"]).sum() for i in (1, 2, 3)), to_num(f6m.PlanQty).sum())

s6 = sheet("6M-S-Curve").dropna(how="all", axis=1)
tables["FactSCurve6M"] = pd.DataFrame({
    "PeriodEnd": pd.to_datetime(s6["Date"]).dt.date,
    "MonthlyPlan": to_num(s6["Monthly Plan"]), "MonthlyActual": to_num(s6["Monthly Actual"]),
    "CumPlan": to_num(s6["Cumulative Plan "]), "CumActual": to_num(s6["Cumulative Actual "]),
})

# ---------------------------------------------------------------- Contractor S-curve (monthly)
scc = sheet("S-Curve-Contractor").iloc[:, :6].dropna(subset=["Contractor"])
tables["FactSCurveContractor"] = pd.DataFrame({
    "ContractorCode": scc["Contractor"].str.strip().map(CONTRACTOR_ALIASES),
    "PeriodEnd": pd.to_datetime(scc["Date"]).dt.date,
    "MonthlyPlan": to_num(scc["Monthly Plan"]), "MonthlyActual": to_num(scc["Monthly Actual"]),
    "CumPlan": to_num(scc["Cumulative Plan "]), "CumActual": to_num(scc["Cumulative Actual "]),
})
ps = sheet("Progress-SPI-Contractor").dropna(how="all", axis=1)
fsc = tables["FactSCurveContractor"]
for _, r in ps.iterrows():
    code = CONTRACTOR_ALIASES[r["A1"].strip()]
    last = fsc[(fsc.ContractorCode == code) & fsc.CumActual.notna()].sort_values("PeriodEnd").iloc[-1]
    chk(f"{r['A1']} Actual % = last S-curve actual", "Progress-SPI-Contractor!C", r["Actual "], last.CumActual)
    chk(f"{r['A1']} Plan % = same-row S-curve plan", "Progress-SPI-Contractor!B", r["Plan "], last.CumPlan)

# ---------------------------------------------------------------- Contractor quantities (periodic + summary)
pc = sheet("Plan Contractor")
pc["ContractorCode"] = pc["Contractor"].str.strip().map(CONTRACTOR_ALIASES)
pc["Item"] = pc["Description"].map(clean_item)
summ = pc[pc["Date"].astype(str).str.strip() == "-"]
per = pc[pc["Date"].astype(str).str.strip() != "-"]
tables["FactContractorQtyPeriodic"] = pd.DataFrame({
    "ContractorCode": per["ContractorCode"], "PeriodEnd": pd.to_datetime(per["Date"]).dt.date,
    "Item": per["Item"], "PlanQty": to_num(per["Plan"]), "ActualQty": to_num(per["Actual"]),
})
tables["FactContractorQtyStatus"] = pd.DataFrame({
    "ContractorCode": summ["ContractorCode"], "Item": summ["Item"], "UOM": summ["UOM"].str.strip(),
    "Estimate": to_num(summ["Estimate"]), "PlanQty": to_num(summ["Plan 1"]),
    "ActualQty": to_num(summ["Actual 1"]), "RemainQty": to_num(summ["Remain"]),
})
items = sorted(set(tables["FactContractorQtyPeriodic"].Item) | set(tables["FactContractorQtyStatus"].Item))
tables["DimContractorItem"] = pd.DataFrame({"Item": items})

# ---------------------------------------------------------------- Manpower
od = sheet("ODCC").dropna(how="all", axis=1)
tables["FactManpowerODCC"] = pd.DataFrame({
    "MonthStart": pd.to_datetime(od["Date"], format="%b-%Y").dt.date,
    "ProjectMonthNo": od["Level"], "ContractPhase": od["Monthly No."],
    "PlanManDay": to_num(od["Plan-Man/Day"]), "ActualManDay": to_num(od["Actual-Man/Day"]),
    "PlanManhourBaseline": to_num(od["Total Plan-Man/Hrs"]), "PlanManhour": to_num(od["Plan-Man/Hrs"]),
    "ActualManhour": to_num(od["Actual-Man/Hrs"]),
})
fo = tables["FactManpowerODCC"]
for ph in od["Monthly No."].dropna().unique():
    chk(f"ODCC Actual man-hours, plan '{ph}'", f"ODCC!M (Monthly No. = {ph})",
        to_num(od[od["Monthly No."] == ph]["Actual-Man/Hrs"]).sum(), fo[fo.ContractPhase == ph].ActualManhour.sum())

dep = sheet("Overall ODCC").dropna(how="all", axis=1)
tables["FactODCCDepartment"] = pd.DataFrame({
    "DeptOrder": dep["Row"], "Department": dep["Description"].str.strip(),
    "PlanHeadcount": dep["Plan"], "ActualHeadcount": dep["Actual"],
})

ct = sheet("Contractor").dropna(how="all", axis=1)
ct["ContractorCode"] = ct["Contractor"].str.strip().map(CONTRACTOR_ALIASES)
ct["MonthStart"] = pd.to_datetime(ct["Date"], format="%b %Y").dt.date
long = ct[ct.ContractorCode != "ALL"].melt(
    id_vars=["MonthStart", "ContractorCode"], value_vars=["Direct", "Indirect", "Rest"],
    var_name="Category", value_name="Manpower")
tables["FactManpowerContractor"] = long
allrows = ct[ct.ContractorCode == "ALL"]
chk("Contractor manpower: sum of contractors = 'Overall' rows", "Contractor (Contractor = Overall)!H",
    to_num(allrows["Total"]).sum(), long.Manpower.sum())
oc = sheet("Overall Contractor").dropna(how="all", axis=1)
chk("Contractor manpower grand total", "Overall Contractor!A2", oc.loc[0, "Grand Total"], long.Manpower.sum())
for col in ["Ehdas", "EXIR", "FRJ", "SOL"]:
    code = CONTRACTOR_ALIASES[col]
    chk(f"Manpower total {col}", f"Overall Contractor ({col})", oc.loc[0, col],
        long[long.ContractorCode == code].Manpower.sum())

# ---------------------------------------------------------------- DimDate (with Jalali)
start = dt.date(2025, 4, 1)
end = dt.date(2030, 3, 31)
dates = pd.date_range(start, end, freq="D")
rows = []
for x in dates:
    jy, jm, jd = jalali(x.year, x.month, x.day)
    rows.append((x.date(), x.year, x.month, x.strftime("%b"), x.strftime("%Y-%m"),
                 x.year * 100 + x.month, x.isocalendar()[1], jy, jm, JMONTHS[jm - 1],
                 f"{jy}-{jm:02d}", jy * 100 + jm, f"{jy}/{jm:02d}/{jd:02d}",
                 x.date() <= cutoff))
tables["DimDate"] = pd.DataFrame(rows, columns=[
    "Date", "Year", "MonthNo", "MonthName", "YearMonth", "YearMonthSort", "ISOWeek",
    "JYear", "JMonthNo", "JMonthName", "JYearMonth", "JYearMonthSort", "JDate", "IsOnOrBeforeCutOff"])
tables["DimContractor"] = DIM_CONTRACTOR

# ---------------------------------------------------------------- write workbook
ORDER = ["README", "Check", "Snapshot", "DimDate", "DimContractor", "DimScope", "DimDiscipline",
         "DimUnit", "DimContractorItem", "FactSCurveDaily", "FactWBSStatus", "FactDisciplineStatus",
         "FactDisciplineByUnit", "FactQuantityByUnit", "FactMajorItems", "FactMajorItems6M",
         "FactItemMonthly6M", "FactSCurve6M", "FactSCurveContractor", "FactContractorQtyPeriodic",
         "FactContractorQtyStatus", "FactManpowerODCC", "FactODCCDepartment", "FactManpowerContractor"]
PCT_COLS = {"CumPlan", "CumActual", "PlanPct", "ActualPct", "Weight", "WeightInProject",
            "MonthlyPlan", "MonthlyActual"}
hdr_fill = PatternFill("solid", fgColor=NAVY)
hdr_font = Font(bold=True, color="FFFFFF")

wb = Workbook()
wb.remove(wb.active)
readme = wb.create_sheet("README")
lines = [
    ("MGR Model - clean data model for the ARUP Phase 2-2 management dashboard", True),
    (f"Source: {SRC.split('/')[-1]}   |   Cut-off: {cutoff}   |   Built: {dt.date.today()}", False),
    ("", False),
    ("Rules", True),
    ("1. Every number is copied from the source sheet named in the Check sheet / table notes. No new figures.", False),
    ("2. Tables are long format (one fact per row). Percentages are 0-1 everywhere (Discipline source was 0-100).", False),
    ("3. Subtotal rows ('Overall' unit, 'Overall' contractor, summary rows with date '-') are not mixed into detail tables;"
     " the Check sheet proves detail = subtotal.", False),
    ("4. Contractor names are mapped to codes: SOL = Souleh Iran, FRJ = Sekaf & Farjoud, EHD = Ehdas, EXR = EXIR (inactive, history kept).", False),
    ("5. Item names cleaned: trailing spaces removed; 'Hysro Test' -> 'Hydro Test'; 'Formworking' -> 'Formwork'.", False),
    ("6. SPI is NOT stored; Power BI computes it as DIVIDE(Actual, Plan) so it is never summed.", False),
    ("", False),
    ("Tables", True),
]
desc = {
    "Check": "Source vs model reconciliation (must all be OK)",
    "Snapshot": "Single values at cut-off (KPIs, 6-month window, hand-typed 6M SPI)",
    "DimDate": "Daily calendar 2025-04-01..2030-03-31 with Jalali (Persian) columns; mark as Date table",
    "DimContractor": "Contractor codes, names, status",
    "DimScope": "Level-1 scopes of the overall S-curve with weights",
    "DimDiscipline": "Discipline list in report order",
    "DimUnit": "Process units",
    "DimContractorItem": "Contractor work items (cleaned names)",
    "FactSCurveDaily": "Daily cumulative Plan / Actual per scope (from 1-Overall S- Curve)",
    "FactWBSStatus": "Level-1 WBS status at cut-off (from 1-WBS)",
    "FactDisciplineStatus": "Discipline status at cut-off (from 03-Discipline Status Cons, scaled to 0-1)",
    "FactDisciplineByUnit": "Discipline progress per unit (from EST2 columns O:R)",
    "FactQuantityByUnit": "Pile / Concrete quantities per area & unit (from EST2 A:H, subtotals removed)",
    "FactMajorItems": "Major construction items, overall (from Estimate 1)",
    "FactMajorItems6M": "Major items in 6-month window by contractor (from 6M)",
    "FactItemMonthly6M": "Monthly plan/actual quantity of key items in 6-month window (from 6M-S-curve-m, unpivoted)",
    "FactSCurve6M": "6-month window S-curve (from 6M-S-Curve)",
    "FactSCurveContractor": "Monthly S-curve per contractor (from S-Curve-Contractor)",
    "FactContractorQtyPeriodic": "Periodic plan/actual quantities per contractor item (from Plan Contractor, dated rows)",
    "FactContractorQtyStatus": "Status to date per contractor item (from Plan Contractor, rows with date '-')",
    "FactManpowerODCC": "ODCC monthly manpower plan/actual (from ODCC); two plan versions in ContractPhase (47 / 60 Monthly) - filter to one",
    "FactODCCDepartment": "ODCC headcount per department (from Overall ODCC)",
    "FactManpowerContractor": "Contractor monthly manpower by category (from Contractor, unpivoted, 'Overall' rows removed)",
}
for n in ORDER[1:]:
    lines.append((f"{n}: {desc[n]}", False))
for i, (txt, bold) in enumerate(lines, 1):
    c = readme.cell(row=i, column=1, value=txt)
    c.font = Font(bold=bold, size=14 if i == 1 else 11, color=NAVY if bold else "000000")
readme.column_dimensions["A"].width = 140

ck = pd.DataFrame(checks, columns=["Check", "Source", "SourceValue", "ModelValue"])
ck["Difference"] = ck.ModelValue - ck.SourceValue
ck["Status"] = ["OK" if abs(dv) <= 1e-6 * max(1, abs(sv)) else "DIFF"
                for dv, sv in zip(ck.Difference, ck.SourceValue)]
NOTES = {
    "ODCC Actual man-hours": "ODCC sheet holds two plans over the same months (47-month and 60-month). Never add them: "
                             "use ContractPhase as a single-select slicer.",
    "Discipline weighted": "Weighted average of 03-Discipline rows does not reproduce the overall % (Plan 13.38% vs 14.52%). "
                           "Discipline rows are shown as-is; overall KPIs come from WBS level 1. CONFIRM with user.",
    "EST2 Concrete Estimate": "Source EST2!D38 (Concrete, Process & Utility Area, Unit=Overall) is a typed value "
                              "118,115.26; its unit rows sum to 91,721.50. Model keeps the unit rows. CONFIRM with user.",
    "Contractor manpower: sum": "Source Contractor!E18:G18 (Overall, Aug 2026) are typed values (total 23,948); "
                                "the four contractors sum to 26,534. Model uses contractor rows, which match "
                                "Overall Contractor!A2 (238,889).",
}
ck["Note"] = [next((v for k, v in NOTES.items() if c.startswith(k)), "") for c in ck.Check]
tables["Check"] = ck

for n in ORDER[1:]:
    df = tables[n].reset_index(drop=True)
    ws = wb.create_sheet(n)
    ws.append(list(df.columns))
    for row in df.itertuples(index=False):
        ws.append([None if (isinstance(v, float) and pd.isna(v)) else v for v in row])
    ref = f"A1:{get_column_letter(len(df.columns))}{len(df) + 1}"
    tbl = Table(displayName=f"t{n}", ref=ref)
    tbl.tableStyleInfo = TableStyleInfo(name="TableStyleMedium2", showRowStripes=True)
    ws.add_table(tbl)
    ws.freeze_panes = "A2"
    for j, col in enumerate(df.columns, 1):
        cell = ws.cell(row=1, column=j)
        cell.fill, cell.font = hdr_fill, hdr_font
        cell.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
        width = max(len(str(col)), *(len(str(v)) for v in df[col].head(200))) if len(df) else len(col)
        ws.column_dimensions[get_column_letter(j)].width = min(max(10, width + 2), 48)
        fmt = None
        if col in PCT_COLS:
            fmt = "0.00%"
        elif col in ("Date", "PeriodEnd", "MonthStart", "ValueDate"):
            fmt = "yyyy-mm-dd"
        elif df[col].dtype.kind == "f":
            fmt = "#,##0.00"
        if fmt:
            for r in range(2, len(df) + 2):
                ws.cell(row=r, column=j).number_format = fmt
    if n == "Check":
        for r in range(2, len(df) + 2):
            st = ws.cell(row=r, column=6)
            st.font = Font(bold=True, color="2E7D32" if st.value == "OK" else "C62828")
wb.save(OUT)

print(ck.to_string())
print("\nTables:", {n: len(tables[n]) for n in ORDER[1:]})
