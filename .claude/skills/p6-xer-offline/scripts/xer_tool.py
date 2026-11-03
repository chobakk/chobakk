#!/usr/bin/env python3
"""xer_tool.py - read Primavera P6 XER exports offline (no P6, no MCP).

Read-only: never writes to the XER. Outputs go to stdout or to files you name.

Commands:
  tables   FILE                      list tables and row counts
  summary  FILE                      projects, data date, activity counts by status/type
  dump     FILE TABLE [--out x.csv]  write one table as CSV
  sqlite   FILE OUT.db               load every table into SQLite (then query with SQL)
  dcma     FILE [--proj CODE]        DCMA 14-point style checks (subset computable from XER)
  critical FILE [--limit N]          activities with total float <= 0, sorted by early start

Notes:
  * XER durations/float are in HOURS. Days = hours / calendar day_hr_cnt (default 8).
  * Encoding: P6 usually writes cp1252; Persian text may be utf-8. We try utf-8, then cp1252.
"""
import argparse, csv, sqlite3, sys
from collections import Counter, defaultdict
from datetime import datetime


def read_xer(path):
    raw = open(path, "rb").read()
    for enc in ("utf-8-sig", "cp1252", "latin-1"):
        try:
            text = raw.decode(enc)
            break
        except UnicodeDecodeError:
            continue
    tables, cur, fields = {}, None, []
    for line in text.splitlines():
        parts = line.split("\t")
        tag = parts[0]
        if tag == "%T":
            cur = parts[1].strip()
            tables[cur] = []
        elif tag == "%F":
            fields = [f.strip() for f in parts[1:]]
        elif tag == "%R" and cur:
            vals = parts[1:] + [""] * (len(fields) - len(parts) + 1)
            tables[cur].append(dict(zip(fields, vals)))
        elif tag == "%E":
            break
    return tables


def num(v, default=0.0):
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


def date(v):
    if not v:
        return None
    for fmt in ("%Y-%m-%d %H:%M", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            return datetime.strptime(v.strip(), fmt)
        except ValueError:
            pass
    return None


def hours_per_day(t):
    cal = {c.get("clndr_id"): num(c.get("day_hr_cnt"), 8) or 8 for c in t.get("CALENDAR", [])}
    return lambda task: cal.get(task.get("clndr_id"), 8) or 8


def pick_tasks(t, proj_code):
    projs = t.get("PROJECT", [])
    if proj_code:
        ids = {p["proj_id"] for p in projs if p.get("proj_short_name") == proj_code}
        if not ids:
            sys.exit(f"project {proj_code!r} not found; have: {[p.get('proj_short_name') for p in projs]}")
    else:
        ids = {p["proj_id"] for p in projs}
    return [x for x in t.get("TASK", []) if x.get("proj_id") in ids], ids


def cmd_tables(t, a):
    for name, rows in t.items():
        print(f"{name:20s} {len(rows):8d}")


def cmd_summary(t, a):
    for p in t.get("PROJECT", []):
        print(f"Project {p.get('proj_short_name')}  data date={p.get('last_recalc_date')}  "
              f"plan start={p.get('plan_start_date')}  sched finish={p.get('scd_end_date')}")
    tasks = t.get("TASK", [])
    print("activities:", len(tasks))
    print("by status:", dict(Counter(x.get("status_code") for x in tasks)))
    print("by type:  ", dict(Counter(x.get("task_type") for x in tasks)))
    print("relationships:", len(t.get("TASKPRED", [])), " WBS nodes:", len(t.get("PROJWBS", [])))


def cmd_dump(t, a):
    rows = t.get(a.table)
    if rows is None:
        sys.exit(f"no table {a.table}; have {list(t)}")
    out = open(a.out, "w", newline="", encoding="utf-8-sig") if a.out else sys.stdout
    w = csv.DictWriter(out, fieldnames=list(rows[0]) if rows else [])
    w.writeheader(); w.writerows(rows)


def cmd_sqlite(t, a):
    con = sqlite3.connect(a.out)
    for name, rows in t.items():
        if not rows:
            continue
        cols = list(rows[0])
        con.execute(f'DROP TABLE IF EXISTS "{name}"')
        con.execute(f'CREATE TABLE "{name}" ({", ".join(chr(34)+c+chr(34) for c in cols)})')
        con.executemany(f'INSERT INTO "{name}" VALUES ({",".join("?"*len(cols))})',
                        [[r.get(c) for c in cols] for r in rows])
    con.commit(); con.close()
    print("wrote", a.out)


def pct(n, d):
    return 0.0 if d == 0 else 100.0 * n / d


def cmd_dcma(t, a):
    tasks, ids = pick_tasks(t, a.proj)
    hpd = hours_per_day(t)
    dd = None
    for p in t.get("PROJECT", []):
        if p["proj_id"] in ids:
            dd = date(p.get("last_recalc_date"))
    # DCMA counts "incomplete normal activities": exclude WBS summary, LOE, completed
    norm = [x for x in tasks if x.get("task_type") not in ("TT_WBS", "TT_LOE")]
    inc = [x for x in norm if x.get("status_code") != "TK_Complete"]
    by_id = {x["task_id"]: x for x in tasks}
    preds, succs = defaultdict(list), defaultdict(list)
    rels = [r for r in t.get("TASKPRED", []) if r.get("task_id") in by_id or r.get("pred_task_id") in by_id]
    for r in rels:
        preds[r["task_id"]].append(r); succs[r["pred_task_id"]].append(r)
    inc_rels = [r for r in rels if by_id.get(r["task_id"], {}).get("status_code") != "TK_Complete"]
    n, nr = len(inc), len(inc_rels)
    res = []
    def add(no, name, count, base, limit, unit="%", ok=None):
        val = pct(count, base) if unit == "%" else count
        passed = ok if ok is not None else (val <= limit)
        res.append((no, name, count, base, f"{val:.1f}{unit if unit=='%' else ''}", limit, "PASS" if passed else "FAIL"))
    # 1 logic: missing predecessor or successor. The project start/finish milestones are
    #   legitimate exceptions; review the list with --verbose-style dump before judging.
    missing = [x for x in inc if not preds[x["task_id"]] or not succs[x["task_id"]]]
    add(1, "Missing logic", len(missing), n, 5)
    add(2, "Leads (negative lag)", sum(num(r.get("lag_hr_cnt")) < 0 for r in inc_rels), nr, 0, unit="")
    add(3, "Lags (positive lag)", sum(num(r.get("lag_hr_cnt")) > 0 for r in inc_rels), nr, 5)
    nonfs = sum(r.get("pred_type") != "PR_FS" for r in inc_rels)
    add(4, "Relationship types (non-FS)", nonfs, nr, 10)
    hard = {"CS_MSO", "CS_MEO", "CS_MSOB", "CS_MEOB", "CS_MANDSTART", "CS_MANDFIN"}
    add(5, "Hard constraints", sum(x.get("cstr_type") in hard or x.get("cstr_type2") in hard for x in inc), n, 5)
    add(6, "High float (>44 wd)", sum(num(x.get("total_float_hr_cnt")) / hpd(x) > 44 for x in inc), n, 5)
    add(7, "Negative float", sum(num(x.get("total_float_hr_cnt")) < 0 for x in inc), n, 0, unit="")
    add(8, "High duration (>44 wd)", sum(x.get("task_type") == "TT_Task" and num(x.get("remain_drtn_hr_cnt")) / hpd(x) > 44 for x in inc), n, 5)
    if dd:
        bad = 0
        for x in norm:
            s, f = date(x.get("act_start_date")), date(x.get("act_end_date"))
            es, ef = date(x.get("early_start_date")), date(x.get("early_end_date"))
            if (s and s > dd) or (f and f > dd):
                bad += 1
            elif x.get("status_code") != "TK_Complete" and ((ef and ef < dd and not f) or (es and es < dd and not s)):
                bad += 1
        add(9, "Invalid dates (vs data date)", bad, len(norm), 0, unit="")
        due = [x for x in norm if date(x.get("target_end_date")) and date(x.get("target_end_date")) <= dd]
        def fin(x):  # start milestones carry only an actual start
            return date(x.get("act_end_date")) or (date(x.get("act_start_date")) if x.get("task_type") == "TT_Mile" else None)
        missed = [x for x in due if not (fin(x) and fin(x) <= date(x.get("target_end_date")))]
        add(11, "Missed tasks (vs planned finish*)", len(missed), len(due), 5)
        done_due = sum(1 for x in due if x.get("status_code") == "TK_Complete")
        bei = done_due / len(due) if due else 1.0
        res.append((14, "BEI (completed / due*)", done_due, len(due), f"{bei:.2f}", ">=0.95", "PASS" if bei >= 0.95 else "FAIL"))
    print(f"{'#':>2} {'check':36s} {'count':>6} {'of':>6} {'value':>8} {'limit':>6}  result")
    for r in res:
        print(f"{r[0]:>2} {r[1]:36s} {r[2]:>6} {r[3]:>6} {r[4]:>8} {str(r[5]):>6}  {r[6]}")
    print("\n* uses target_end_date (P6 'Planned Finish'). True DCMA uses the BASELINE finish: "
          "export the baseline project too and compare. Checks 10 (resources), 12 (critical path test) "
          "and 13 (CPLI) need P6 itself or the baseline; not computed here.")


def cmd_critical(t, a):
    tasks = [x for x in t.get("TASK", []) if x.get("status_code") != "TK_Complete"
             and x.get("task_type") not in ("TT_WBS", "TT_LOE") and num(x.get("total_float_hr_cnt"), 1) <= 0]
    hpd = hours_per_day(t)
    tasks.sort(key=lambda x: x.get("early_start_date") or "")
    for x in tasks[: a.limit]:
        print(f"{x.get('task_code'):20s} TF={num(x.get('total_float_hr_cnt'))/hpd(x):7.1f}d  "
              f"{x.get('early_start_date','')[:10]} -> {x.get('early_end_date','')[:10]}  {x.get('task_name')}")
    print(f"({len(tasks)} activities with TF <= 0)")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = ap.add_subparsers(dest="cmd", required=True)
    for c in ("tables", "summary"):
        sp.add_parser(c).add_argument("file")
    p = sp.add_parser("dump"); p.add_argument("file"); p.add_argument("table"); p.add_argument("--out")
    p = sp.add_parser("sqlite"); p.add_argument("file"); p.add_argument("out")
    p = sp.add_parser("dcma"); p.add_argument("file"); p.add_argument("--proj")
    p = sp.add_parser("critical"); p.add_argument("file"); p.add_argument("--limit", type=int, default=50)
    a = ap.parse_args()
    t = read_xer(a.file)
    globals()["cmd_" + a.cmd](t, a)


if __name__ == "__main__":
    main()
