# کارهای جاری (Task Board)

الگو: learn-claude-code (s05 TodoWrite، s10 Task System، s17 Goal Loop). هر کار یک **شرط پایان قابل‌بررسی** دارد؛ کار فقط وقتی «انجام شد» است که آن شرط با شواهد (سلول، فایل، خروجی دستور) نشان داده شود، نه با ادعا.
وضعیت‌ها: `[ ]` باز · `[~]` در حال انجام · `[x]` انجام شد · `[!]` منتظر کاربر.
در شروع هر Session این فایل را بخوان؛ در پایان به‌روزش کن.

## منتظر کاربر
- [!] **آپلود فایل‌ها (کپی):** آخرین `CSM-DR-P2-...xlsm` + فایل‌های Soleh، Sekaf & Farjoud، Ehdas همان روز. شرط پایان: `pms_inspect.py sheets` روی هر فایل خروجی بدهد.
- [!] **جدول ۳۱ روزه بعد از ۲۲ نوامبر:** کاربر روالش را بگوید. شرط پایان: روال در `memory-notes.md` ثبت و سؤال ۷ در `open-questions.md` بسته شود.
- [!] **Plan% شیت‌های پیمانکار:** کدام ستون/سلول (فرمول `#REF!`). شرط پایان: آدرس سلول ثبت شود (سؤال ۴).

## بعد از رسیدن فایل‌ها
- [ ] ساختار فایل نوامبر را با `docs/findings.md` مقایسه کن (ستون‌ها، AT3، AK3، ردیف آخر SOL2، شیت `Contractors`). شرط پایان: جدول «تطبیق/تغییر» در `findings.md` با آدرس سلول‌ها.
- [ ] تطبیق Today فایل پیمانکار با فایل ما (Tag + نام فعالیت). شرط پایان: جدول same / different / only-ours / only-contractor برای هر پیمانکار.
- [ ] رابطه Activity ID با `Contractors!B` را از فایل پیدا کن. شرط پایان: چند ردیف نمونه با آدرس سلول در `findings.md`.

## انجام شد
- [x] 2026-11-03 workspace روی ریپو (PR #1).
- [x] 2026-11-03 اسکیل‌های Excel / Power BI / SQL / P6 آفلاین + `PRINCIPLES.md` (PR #2).
- [x] 2026-11-03 تحلیل مرحله ۱ ماکروها؛ کاربر: همه درست‌اند، تغییری لازم نیست (PR #2، #3).
- [x] 2026-11-03 اسکیل‌های بیرونی MIT (PR #4).
- [x] 2026-11-03 **تست مجوز اسکریپت‌ها در Session جدید** (بدون بلاک اجرا شدند):
  - `python3 -I .claude/skills/excel-vba-workbook-engineering/scripts/lint_vba_source.py macros/pile-compile-package` → exit 0، «VBA source lint OK: 6 modules, 32 procedures, 6 public entry subs». فقط ۶ هشدار: `Attribute VB_Name` (Module1..6) با نام فایل یکی نیست (طبیعی برای ماژول‌های export‌شده؛ ماکروها تغییر نکردند).
  - `evm_calculator.py` نیاز به `pip install -r .claude/skills/evm/requirements.txt` داشت (matplotlib نصب نبود؛ در هر Session ابری تازه دوباره لازم است). بعد از نصب: هر دو `examples/h2-pipeline-snapshot.csv` و `h2-pipeline-timephased.csv` → exit 0، گزارش + نمودارها (snapshot: ۳ نمودار، timephased: ۵ نمودار). نمونه timephased: CPI 0.949، SPI 0.855.
  - خروجی‌ها فقط در scratchpad Session نوشته شدند، نه در ریپو.
