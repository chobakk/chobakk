PROFESSIONAL CONTRACTOR DATA-ENTRY UPDATE

Updated modules:
- FRJ DATA ENTRY.bas
- SOI DATA ENTRY.bas
- EHD DATA ENTRY.bas

Unchanged modules (restored from the original codes.rar archive):
- CLEAR ALL FILTER.bas
- Zero maker.bas
- FINAL TOUCH.bas

Changes in the three contractor data-entry modules:
1. Blank source values (including formulas returning an empty string) are skipped.
2. Genuine numeric zero values are still imported.
3. Imported numeric values retain the 0.00 number format.
4. A detailed "Operation Log" worksheet is created automatically when the first value is pasted.
5. Each successful transfer is logged with:
   - Source Activity ID
   - Destination Activity ID
   - Pasted Value
   - Source cell reference
   - Destination cell reference
   - Run date/time and operation name
6. A completion message reports values pasted, blank source values skipped,
   unmatched Activity IDs, and the location of the detailed report.

Activity ID handling:
The matched lookup key is used as the Activity ID on both sides. The source ID
is read from the source lookup-key cell and the destination ID from the
corresponding destination lookup-key cell. This follows the user's confirmation
that the source and destination Activity IDs are the same.

Important:
- Existing lookup mappings, target columns, sequence/date matching, duplicate
  handling, once-per-session checks, and destination-not-empty checks are kept.
- The three non-contractor utility modules listed above are unchanged.
- Test the imported modules on a COPY of the workbook before production use.
- The macros have been code-reviewed and statically checked, but not executed
  inside Excel with the user's live workbooks.
