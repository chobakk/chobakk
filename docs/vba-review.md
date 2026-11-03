# PILE COMPILE VBA package: stage 1 analysis (code review only)

Date: 2026-11-03. Source: `macros/pile-compile-package/` (the user's ZIP, stored unmodified).
**Nothing here was executed in Excel.** This is a static code review, checked against the earlier
structure notes in `docs/findings.md` and `.claude/skills/odcc-project-controls-agent/references/macros.md`
(which were read from the 29-Oct workbook). Tags: [code] = read in the .bas, [notes] = from earlier
workbook notes, [verify] = must be confirmed on a copy of the real workbook.

No code has been changed. Stage 2 waits for the user's approval.

---

## 1. Module-by-module

### A. `Zero maker.bas` (VB_Name `Module1`): `RunPMSUpdate`
- Flow: lock check → confirm (default No) → save settings → copy blocks → increments → lock → restore → message. Error handler restores settings and warns that changes may be partial.
- Helpers (all Private): `CopyValues`, `IncrementDate`, `IncrementNumber`, `RestoreExcelSettings`. No external references.
- `CopyValues` (new version): loops cell by cell over `Value2`. It skips `""` and Empty, copies numbers (including 0), copies error values, and works for 1×1 ranges. Values only.
- Mappings match the spec: WPA `N5:O38→J5` is N→J, O→K. All the others match too.
- **Differences from the spec and the earlier verified version:**
  1. **FRJP increment cell changed from `AS3` to `AT3`.** The verified module `macros/Zero_maker_10deab.bas` (identical to the macro inside `CSM-DR-P2-2026-Oct-29_2.xlsm`) increments `FRJP!AS3`. The earlier notes say FRJP's day number is in **AS3**, unlike the other contractor sheets (AT3) [notes]. The README says the mappings are "otherwise unchanged", but they are not. [verify, high]
  2. **Phase order:** the spec requires every copy first, then every increment. The code instead interleaves them per sheet (`PMS-Overall` AK3 is incremented before the Cov-P copies, and so on) [code]. Calculation is Manual during the run, so the values read should still be the pre-run cached values, and the practical effect is probably nil. It still does not match the spec. [verify]
  3. **Cov-P chain:** `M13→AE34` runs first and `AE34→AB34` runs second. So AB34 receives the **new** AE34 (that is, M13), not yesterday's AE34. If the intent was "AB34 = yesterday's AE34" (an Up to Last pattern), the order is inverted. Same order as the spec, so this is a question, not a change. [verify]
- Behaviour notes:
  - Blank-skip means a destination keeps its old value when the source becomes blank. That can leave stale "Up to Last" values.
  - Error values (`#N/A`, `#REF!`) are copied and frozen into Up to Last.
  - Ranges that start on header row 7 overwrite the Up to Last header text with "Up to Now". The user said this is accepted.
  - The session lock is a module variable. A VBA project reset (editing code, an `End` statement, the Debug→Reset button) clears it.

### B. `FRJ DATA ENTRY.bas` (`Module2`): `Daily_Data_Entry_FRJ`
- Flow: lock → confirm → pick `.xlsb` → open read-only (or reuse it if already open, matched by full path) → find the FRJP column (`AT3` in row 5 from col 47 = AU; date = row 6) → find the FRJE column (`K2` in row 3 from col 12 = L) → build dictionaries (first duplicate wins, case-insensitive, trimmed) → destination-empty check (FRJP rows 9..last J, FRJE rows 4–31) → import (0.00 format) → log → lock → close source → message.
- Source: `PMS` B→U, `Extra Work` A→C. Destination: `FRJP` J, `FRJE` C. Matches the spec.
- Uses `FRJP!AT3` as the sequence. Same AS3/AT3 question as above. [verify, high]
- Helpers: `GetOpenWorkbook`, `FindNumberColumn`, `FindDateColumn`, `SameDate`, `NormalizeKey`, `CellHasContent`, `RestoreFRJSettings`, `IsBlankImportedValue`, `WriteContractorOperationLog`.

### C. `SOI DATA ENTRY.bas` (`Module3`): `Daily_Data_Entry_SOI`
- Same structure as FRJ. Source `PMS (9)` B→**AC**, `PMS EXTRA` B→**AH**. Destination `SOL1` J rows 9–1253, `SOL2` J rows 9–716. Each sheet has its own AT3; search is in row 5 from AU.
- **SOL2 row limit:** the earlier notes give the last data rows as SOL1 = 1225 and **SOL2 = 719** [notes]. Rows 717–719 of SOL2 fall outside 9–716 and would never be imported or checked. [verify, high]
- **Source value columns:** the earlier notes on the Soleh file put the Today quantity at `PMS (9)!AA` and `PMS EXTRA!AF` [notes]. The macro reads AC and AH. These may be the right columns for a newer file layout, or they may be Today % or Up to Now. [verify, high]

### D. `EHD DATA ENTRY.bas` (`Module4`): `Daily_Data_Entry_EHD`
- Source `PMS-01` B→**R**. The notes confirm R = TODAY, Q = UTL, S = UTN. Destination `Ehds` J9:J473 (465 rows), sequence AT3, row 5 from AU.
- Reads arrays (`Value2`) and writes only non-blank values. Applies `0.00` to the whole 465-row target.
- Differs from FRJ/SOI:
  - The open-workbook check looks for the exact name `EHD.xlsx`. Real files are named like `EHS-DR-CIV-0272 (...).xlsx`, so the check never matches and the user is always prompted. If an unrelated `EHD.xlsx` is open, it is used silently.
  - The destination check uses `CountA`, which also counts formulas returning `""`. FRJ/SOI use `Len(Value2)>0`.
  - The sequence match is a text compare (`StrComp`), not numeric.
  - The confirmation default is Yes (no `vbDefaultButton2`).
  - The source is read from row 2, not row 1.
  - The file filter allows xlsx/xlsm/xlsb/xls.

### E. `FINAL TOUCH.bas` (`Module6`): `Final_Touch`
- Op 1: `Cnt-t!D12:D333` → the column in `Cnt-t!K11:AO11` matching the date in `Cov-P!R7`, rows 12–333.
- Op 2: `PMS-Overall!AE8:AE5700` → the column in row 7, from `BDL7` rightwards, matching the date in `PMS-Overall!AK3`, rows 8–5700.
- Checks both destinations are empty (`CountA`) before writing. Reads both sources before writing. Writes non-blank values only.
- Rollback: if op 2 fails, it only clears the op 1 target (which was empty before). Partial op 2 writes are not undone. **Not transactional.**
- `TryReadDate` reads text dates as m/d/yyyy. A d/m text date would be misread.
- `CountA` would block on error values or `""` formulas sitting in the target column. That is relevant because the notes list `#N/A` cells in PMS-Overall around `BIV…`. [verify]
- README calls this module "unchanged from the prior package", yet it contains the blank-skip helper `IsBlankImportedValue`. Confirm which version is the original. [verify]

### F. `CLEAR ALL FILTER.bas` (`Module5`): `Clear_All_Filters`
- Confirm (default No) → for each sheet: `ShowAllData` if `FilterMode`, then each ListObject with an active filter → count → message. It keeps the dropdowns and doesn't delete anything.
- A protected sheet (or any error) stops the whole loop part-way. The message then says "could not clear all" without saying which sheet. It does not change Calculation.

---

## 2. Activity ID relationship: NOT verified
- In all three contractor modules, the log writes the **lookup key** on both sides: Source ID = source B (or A for Extra Work), Destination ID = destination J (or C). After a match these are the same text, so both columns repeat the same ID. The spec says not to do this unless the workbook confirms it.
- The README justifies this as "the user's confirmation that the IDs are the same". That conflicts with the current instruction.
- **There is no `Contractors` worksheet in my earlier notes on the 29-Oct workbook** (there are `Cnt-V`, `Cnt-t`, `Prj-V`; `CONTRACTORS` is a folder). Either it was added later or the name differs. [verify]
- Hypothesis [inference only]: PMS-Overall has `I` Sub Contractors Activity id, `J` Sub Activity id, `K` Activity ID. If the contractor sheets follow that layout, then destination **J is the contractor's own code**, which is why it matches source B, and **our Activity ID is in K** of the same row. The earlier notes also say "Activity ID is not the same between the two files". The Destination Activity ID would then come from K (or from `Contractors!B` via some key), and the Source Activity ID would be the contractor's code. **This can only be decided with the workbook.**
- Needed from the workbook copy: `Contractors!A1:Z20` and its row count; rows 6–12 of FRJP/FRJE/SOL1/SOL2/Ehds (columns A–AC); rows 1–10 of the source sheets.

---

## 3. Bugs and risks (ranked)
| # | Severity | Where | Issue |
|---|---|---|---|
| 1 | ~~High~~ Closed | Zero maker, FRJ | (User confirmed the new files are correct: FRJP uses AT3.) `FRJP!AT3` vs the verified `FRJP!AS3`. Possible wrong-cell increment or a mid-run error (after copies are done) |
| 2 | High | all .bas | Files are **UTF-8 with BOM** and five have **LF-only** line endings. The VBE importer expects ANSI + CRLF. The BOM can turn line 1 into `ï»¿Attribute VB_Name…`, giving a syntax error and/or a wrong module name. Re-save as ANSI/CRLF before import, or check after import. |
| 3 | High | SOI | SOL2 range 9–716 vs last data row 719 |
| 4 | High | SOI | Source value columns AC/AH vs the noted Today columns AA/AF |
| 5 | High | FRJ/SOI/EHD | Activity ID log columns are the lookup key twice (not verified against `Contractors!B`) |
| 6 | Medium | FRJ/SOI/EHD | "Unmatched" counts only destination rows with no source match. **Source rows with a value and no destination row are silently dropped and not reported.** This is the real data-loss case. |
| 7 | Medium | FRJ/SOI/EHD | Duplicate keys: the first source duplicate wins silently. A duplicate destination key receives the same value twice (double counting). Neither is reported. |
| 8 | Medium | FRJ/SOI/EHD | An error after some writes leaves partial data, and nothing is logged for it. The lock isn't set, but the destination check then blocks a re-run (safe, but the user must clear by hand). |
| 9 | Medium | Zero maker | Phase order differs from the spec. The Cov-P AE34→AB34 order is questionable. |
| 10 | Low | Operation Log | Column order is Date/Time, Operation, Src ID, Dst ID, Value, Src Cell, Dst Cell. The spec order is Src ID, Dst ID, Value, Src Cell, Dst Cell, Operation, Date/Time. The message says "opened in Operation Log sheet" but gives no row/range. |
| 11 | Low | EHD | Inconsistencies (CountA vs Len, text vs numeric sequence, default button, `EHD.xlsx` name check) |
| 12 | Low | all | Zero values are pasted and counted as "pasted" (spec-conform). The log can be mostly zeros. |
| 13 | Low | Clear filters | A protected sheet aborts the loop |
| 14 | Info | all | The session lock resets on a VBA project reset. `Scripting.Dictionary` is late-bound and Windows-only (fine here). |

## 4. Proposed changes (contractor modules only; nothing until approved)
1. Activity ID columns from the **verified** relationship (after the workbook check). If it can't be verified, write "not verified" rather than repeat the key.
2. Operation Log columns in the spec order (7 columns). The message gives the exact location (`'Operation Log'!A{first}:G{last}`).
3. Count and log **unplaced source records** (source has a non-blank value but no destination key) as `UNMATCHED-SOURCE` rows. Keep the destination-side count as `UNMATCHED-DESTINATION`. The message shows both.
4. Report duplicate source and destination keys (as log rows and counts) without changing which value wins.
5. On error after writes started, write the log entries collected so far, marked `PARTIAL`, so the user knows exactly which cells were written.
6. SOI: change `SOL2` 716 → the real last row, and confirm AC/AH, **only if the workbook check confirms it** (these are mapping changes and need explicit approval).
7. Deliver the .bas files as ANSI/CRLF without a BOM, keeping `VB_Name` and the public macro names.

## 5. Explicitly not recommended
- Touching Zero maker, Final Touch or Clear All Filters while doing the reporting work. Items 1 and 9 above are raised as questions only.
- Merging modules, renaming modules or public macros, or moving the log helper into a shared module (that adds a cross-module dependency).
- Switching the matching key to Tag + activity name, or changing ranges, sequence or date logic, without approval.
- Calling any rollback "transactional". Adding a full undo/rollback system.
- Early-bound references (Microsoft Scripting Runtime) or any add-in.
- Converting the workbook to xlsx.

## 6. Questions for the user
1. ~~FRJP: is the day number in `AS3` or `AT3` today?~~ **Answered 2026-11-03: the new files are correct, so FRJP uses `AT3`.** Risk #1 is closed.
2. Cov-P: should AB34 get yesterday's AE34 (copy AB34←AE34 first) or today's M13 (current order)?
3. SOI: are AC/AH the Today quantity columns in the current Soleh file? Does SOL2 data end at 716 or 719?
4. Where is the `Contractors` sheet, and what are its columns?
5. Is `FINAL TOUCH.bas` in this package the version currently in the workbook?
