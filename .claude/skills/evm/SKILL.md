---
name: evm
description: >
  Earned Value Management (EVM) analysis for large capital projects.
  Computes BCWS/BCWP/ACWP, CPI, SPI, EAC (three methods), TCPI, VAC,
  generates five PNG charts (S-curve, indices trend, WBS variance waterfall,
  EAC comparison, CPI/SPI quadrant), and a structured markdown report with
  RAG status and escalation flags. Input: a WBS COST CSV (snapshot or
  time-phased) with bac/bcws/bcwp/acwp columns. Use only for cost-based EVM
  ("run EVM", "earned value", "CPI", "EAC forecast", "project cost
  performance"). Not for the PMS daily physical-progress report (use
  odcc-project-controls-agent for that).
metadata:
  version: 1.1.0
  dependencies: python>=3.9, pandas>=2.0.0, matplotlib>=3.7.0, numpy>=1.24.0
  standard: ANSI/EIA-748, PMI Practice Standard for EVM (3rd ed.)
  domain: capital project management / project controls
---

# EVM Skill — Earned Value Management Analysis

> **Project note (ODCC):** the rules in `CLAUDE.md` override this skill. Briefly say what you will run before running it, never treat physical Plan%/Actual% from PMS as BCWS/BCWP without the user's approval, and label any hypothetical cost data as hypothetical.

## When to Use This Skill

Activate automatically whenever the user:
- Mentions "earned value", "EVM", "CPI", "SPI", "BCWP", "BCWS", "ACWP", "EAC"
- Asks to "analyse project cost performance", "check schedule/cost status"
- Provides or references a CSV with columns: `bac`, `bcws_cum`, `bcwp_cum`, `acwp_cum`
- Asks for a "monthly EVM report" (cost-based; not the PMS physical progress report)
- Asks "are we over budget?" or "are we behind schedule?" in a project context

## ⚠️ CRITICAL BEHAVIOUR REQUIREMENTS ⚠️

**DO NOT ask what the user wants to do with the data.**
**DO NOT list analysis options and wait for a choice.**
**IMMEDIATELY run the full analysis and present ALL results.**

The user wants a complete EVM report. Execute it now.

### Execution Steps (always in this order):

1. **Identify the CSV file** — from the user's message, the current project directory
   (`projects/<id>/03-cost/`), or ask for the path if no file is evident.

2. **Validate the data before analysis** (see Data Validation Checks below).
   Report any anomalies found, then proceed — do not block the analysis unless
   the data is unusable.

3. **Run the calculator**:
   ```bash
   python ~/.claude/skills/evm/evm_calculator.py <path-to-csv> --output /tmp/evm-output/
   ```

4. **Display the markdown report** in full — do not summarise or truncate it.

5. **Show all five charts** — present each PNG inline with a caption.

6. **Interpret the results** — add a 3–5 sentence plain-language interpretation
   after the report tables, covering:
   - Overall project health (cost + schedule)
   - The worst-performing WBS package and why it matters
   - The recommended EAC and what it means for the approved budget
   - Whether any escalation flags require immediate action

7. **Benchmark the EAC** — if an `estimate-report.md` from the
   `budget-estimator` skill or an approved FID CAPEX (from `project-brief.md`)
   is available, state explicitly where the recommended EAC sits relative to
   the P50 planning basis and the P90 ceiling. EAC > P90 is itself an
   escalation-worthy finding.

8. **Suggest next steps** — one concise bullet list of ≤5 actionable items
   (e.g., investigate specific package, update risk register entry, convene recovery meeting).

## Data Validation Checks (run before analysis)

Inspect the CSV and flag any of the following before presenting results:

| Check | Why it matters |
|-------|----------------|
| Cumulative values must be non-decreasing per WBS across periods | Decreasing BCWS/BCWP/ACWP usually means period values were supplied instead of cumulative — results would be meaningless |
| BCWP must not exceed BAC for any WBS | Earned value above budget at completion indicates a progress-measurement or baseline error |
| BCWS at the latest period should not exceed BAC | Planned value above BAC indicates a baseline inconsistency |
| Zero ACWP with non-zero BCWP (or vice versa) | Likely missing actuals or missing progress capture for that package |
| Missing periods in a time-phased series | Gaps distort trend charts and consecutive-period escalation rules |
| Reporting period older than 60 days | Flag as potentially stale (consistent with project CLAUDE.md data rules) |

Report anomalies in a short "Data Quality Notes" block ahead of the
interpretation. Never silently correct the data.

## Input CSV Formats

### Format 1 — Snapshot (one row per WBS element)
Required columns (exact names):
```
wbs_code, wbs_description, bac, bcws_cum, bcwp_cum, acwp_cum
```
Optional: `period` (reporting period label, e.g. "2026-05")

Example:
```csv
wbs_code,wbs_description,bac,bcws_cum,bcwp_cum,acwp_cum,period
WBS-1.1,Mainline Construction N,485000000,58200000,43650000,48000000,2026-05
WBS-1.2,Mainline Construction S,493000000,52000000,50000000,49500000,2026-05
```

### Format 2 — Time-phased (multiple rows per WBS, one per period)
Same columns plus `period`. The script auto-detects this format when any
`wbs_code` appears more than once.

```csv
wbs_code,wbs_description,bac,period,bcws_cum,bcwp_cum,acwp_cum
WBS-1.1,Mainline Construction N,485000000,2026-01,5000000,4500000,4800000
WBS-1.1,Mainline Construction N,485000000,2026-02,12000000,10500000,11500000
```

**Time-phased input produces two additional charts**: S-curve and indices trend.

## Output

All outputs saved to `--output` directory (default `./evm-output/`):

| File | Description |
|------|-------------|
| `evm-report.md` | Full markdown report with tables and escalation flags |
| `01_s_curve.png` | Cumulative BCWS / BCWP / ACWP S-curve (time-phased only) |
| `02_indices_trend.png` | CPI and SPI trend over reporting periods (time-phased only) |
| `03_wbs_variance.png` | Cost Variance by WBS package (RAG-coloured horizontal bars) |
| `04_eac_comparison.png` | BAC vs three EAC methods per WBS package |
| `05_cpi_spi_quadrant.png` | CPI/SPI four-quadrant map, bubble size ∝ BAC |

## EVM Metrics Reference

| Symbol | Name | Formula | Interpretation |
|--------|------|---------|----------------|
| BCWS | Planned Value (PV) | Budgeted cost of scheduled work | What we planned to spend |
| BCWP | Earned Value (EV) | Budgeted cost of completed work | What we earned for what we did |
| ACWP | Actual Cost (AC) | Actual cost of completed work | What we actually spent |
| CV | Cost Variance | BCWP − ACWP | +ve = under budget |
| SV | Schedule Variance | BCWP − BCWS | +ve = ahead of schedule |
| CPI | Cost Performance Index | BCWP ÷ ACWP | >1 = under budget |
| SPI | Schedule Performance Index | BCWP ÷ BCWS | >1 = ahead of schedule |
| EAC | Estimate at Completion | BAC ÷ CPI (primary) | Forecast final cost |
| ETC | Estimate to Complete | EAC − ACWP | Remaining spend forecast |
| VAC | Variance at Completion | BAC − EAC | Expected over/under at finish |
| TCPI | To-Complete Performance Index | (BAC−BCWP) ÷ (BAC−ACWP) | Required future CPI; >1.10 = unrealistic |

## RAG Thresholds (capital project standard)

| Index | GREEN | AMBER | RED |
|-------|-------|-------|-----|
| CPI | ≥ 0.95 | 0.85 – 0.94 | < 0.85 |
| SPI | ≥ 0.95 | 0.85 – 0.94 | < 0.85 |

## Escalation Rules (applied automatically)

- CPI < 0.85 → flag for Project Director notification
- SPI < 0.85 → flag for Project Director notification
- SPI < 0.85 for **2 consecutive periods** → require written escalation memo
- TCPI > 1.10 → warn that re-baselining may be necessary

## Integration with Capital Project Scaffolding

This skill integrates with the capital project data structure:

```
projects/<project-id>/
├── 03-cost/
│   ├── evm-snapshot-<period>.csv    ← primary input
│   ├── evm-timephased.csv           ← append new period each month
│   └── evm-output/                  ← skill writes here
│       ├── evm-report.md
│       ├── 01_s_curve.png
│       └── ...
└── heuristics/pipeline-cost.yaml   ← benchmarks for EAC sanity check
```

After generating the report, compare the EAC against:
- Approved FID CAPEX (from `project-brief.md`)
- Parametric benchmark (from `heuristics/pipeline-cost.yaml`)
- P50/P90 from the latest `budget-estimator` report, if present
- Previous period's EAC (trend direction — an EAC drifting upward three periods
  in a row is a finding even when CPI is still AMBER)

## Integration with the Skill Suite

- **`budget-estimator`** — the P50 planning basis and P90 ceiling are the
  reference frame for every EAC. If TCPI > 1.10 or re-baselining is on the
  table, recommend a fresh P50/P90 estimate at the current AACE class before
  any new baseline is approved.
- **`gate-readiness`** — before a re-baselining decision or an execution-phase
  gate, recommend assembling the gate evidence pack with the gate-readiness
  skill rather than approving on the EVM report alone.
- **`systems-thinking`** — a persistent RED package whose root cause is
  structural (interface, regulatory, supply chain) rather than productivity
  suggests a definition gap; recommend a targeted systems-thinking re-audit.
- **`lessons-learned`** — at project completion (or major package close-out),
  recommend the lessons-learned skill to compare final actuals against the
  original estimate and calibrate the heuristics library.

## Example Usage

```
User: "Analyse the EVM data in projects/H2-PIPE-DE-001/03-cost/evm-snapshot-2026-05.csv"
```
→ Run calculator, display full report, show all charts, add plain-language interpretation.

```
User: "How is EPC-001 performing on the pipeline project?"
```
→ Locate the relevant cost CSV, run the skill, focus interpretation on EPC-001 row.

```
User: "Generate a monthly EVM report"
```
→ Find `evm-timephased.csv` in the active project's `03-cost/` directory, run full analysis.

## Files

- `evm_calculator.py` — Core calculation engine (metrics, forecasting, 5 charts)
- `requirements.txt` — Python dependencies
- `README.md` — Detailed user documentation
- `examples/h2-pipeline-snapshot.csv` — Single-period sample (H2 pipeline project)
- `examples/h2-pipeline-timephased.csv` — Five-period sample with trend data
