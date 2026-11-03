# فایل‌هایی که باید کنار پروژه بگذاری
(حجم زیادند و داخل zip نیستند؛ از مسیر خودت کپی کن، و **فقط روی کپی کار کن**.)

| فایل | چیست | نکته |
|---|---|---|
| `CSM-DR-P2-2026-Oct-29_2.xlsm` (۵۷MB) | آخرین نسخه PMS با ماکرو (Module1 = Zero_maker) | نسخه مرجع ساختار 29 اکتبر؛ `_1` هم ماکرو دارد؛ نسخه اول `Oct-29.xlsm` ماکرو نداشت |
| `CSM-DR-P2-2026-Oct-19.xlsx` | نسخه قدیمی‌تر (ساختار ستون‌ها دو ستون عقب‌تر) | فقط برای مقایسه |
| `Daily Report(1405.08.07)-29-10-2026).xlsb` | فایل Soleh Iran، 29 اکتبر | شیت‌ها: `PMS (9)`, `PMS EXTRA`, `Man Power`, `Machinery ` |
| `SEKAF&FARJOUD-DR-2026.10.29.xlsb` | فایل Sekaf & Farjoud، 29 اکتبر | `PMS`, `Extra Work`, `Man Power`, `Machinery ` |
| `EHS-DR-CIV-0272 (1405.08.07)-(29-10-2026).xlsx` و `...0273 (30-10-2026)` | فایل Ehdas | `PMS-01`, `Man Power`, `Machinery` |
| `Zero_maker*.bas` | ماکرو (۴ نسخه) | فقط `macros/Zero_maker_10deab.bas` معتبر است |
| `p6-mcp-shop-main.zip`، `PILE-P6-CLAUDE-SKILLS-FA.zip` | مخزن اسکیل‌های P6 | محتوای هر دو یکی است؛ در `.claude/skills/p6-primavera-agent` هست |
| قالب مشترک Manpower/Machinery (کاربر ساخته) | هنوز نفرستاده |  |
خواندن xlsb: `pip install pyxlsb` و `python tools/pms_inspect.py xlsb-head FILE "PMS (9)" --rows 6-9`.
