PILE COMPILE / PMS VBA MODULE PACKAGE

Included modules:
- CLEAR ALL FILTER.bas
- EHD DATA ENTRY.bas
- FINAL TOUCH.bas
- FRJ DATA ENTRY.bas
- SOI DATA ENTRY.bas
- Zero maker.bas

Important notes:
1. The three contractor data-entry modules (FRJ, SOI, EHD) include the professional completion report and Operation Log changes from the prior package.
2. CLEAR ALL FILTER and FINAL TOUCH are unchanged from the prior package.
3. Zero maker is based on the original module supplied by the user. Only its CopyValues procedure was updated to skip blank source cells / formulas returning an empty string, while preserving real numeric zeros and error values. It processes cells individually so single-cell and multi-cell source ranges are both supported. Its mappings, increments, and operation order are otherwise unchanged.
4. Import into a BACKUP copy of the workbook first. These modules have not been executed inside the user's Excel environment; test each macro before using the live workbook.

Import: Excel > Alt+F11 > File > Import File... > select each .bas module.
