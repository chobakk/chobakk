# ODCC Abadan — دستیار گزارش‌گیری و کنترل پروژه (ARUP Phase 2-2)

نام پروژه: **ARUP = Abadan Refinery Upgrading Project، فاز 2-2** (طرح ارتقای پالایشگاه آبادان). فایل‌ها: `CSM-DR-P2` = Consortium Daily Report Phase 2.

## کاربر
کارشناس رپورتینگ واحد برنامه‌ریزی و کنترل پروژه (PPC) شرکت ODCC آبادان (تازه شروع کرده). پروژه ساخت‌وساز/نفتی، Abadan Refinery Upgrading Project فاز 2-2. فارسی بنویس؛ اصطلاحات فنی (WBS, Activity ID, Baseline, SPI, Up to Last/Today/Up to Now) را انگلیسی نگه دار.

## قواعد ثابت (قبل از هر اقدام)
1. حدس نزن: نام شیت، آدرس سلول، فرمول، مسیر و عدد را از فایل واقعی بخوان یا از کاربر بپرس. مشاهده‌ی فایل، استنباط و ابهام را جدا بنویس (در docs از برچسب [فایل]/[استنباط]/[تأیید شود] استفاده شده).
2. **هیچ فایل اصلی را در جا تغییر نده.** روی کپی کار کن، قبل از هر نوشتن یا حذف توضیح بده چه می‌کنی و تأیید صریح بگیر. درایو `Z:` شبکه کاری است؛ بدون تأیید روی آن چیزی نساز، کپی نکن، پاک نکن.
3. فایل‌های کاری xlsm‌اند؛ xlsm را به xlsx تبدیل نکن و VBA را حذف نکن. فرمول‌ها، قالب‌بندی، Defined Nameها و استایل‌ها را بدون اجازه تغییر نده.
4. **ماکروها را خود کاربر نوشته و همه درست‌اند (تأیید 2026-11-03).** درباره‌شان سؤال نپرس، تغییر پیشنهاد نده و بازنویسی نکن، مگر صریحاً بخواهد. نسخه فعلی: `macros/pile-compile-package/` (فقط مرجع)؛ `macros/Zero_maker_10deab.bas` نسخه قدیمی است.
5. فایل PMS خیلی سنگین است (۵۷MB؛ PMS-Overall ≈ 380MB XML). از openpyxl/pandas برای باز کردن کامل استفاده نکن؛ از `tools/pms_inspect.py` (استریم XML) استفاده کن. خروجی‌های بزرگ را در فایل بنویس، نه ترمینال.
6. تطبیق ردیف‌های پیمانکار با فایل ما با **Tag + نام فعالیت** است؛ شماره ردیف و Activity ID بین دو فایل یکی نیست.
7. Exir (EXR*) و IGC (IGC-PR45) غیرفعال‌اند؛ شیت `PR-5301 FRJ` قدیمی است. به آن‌ها دست نزن.
8. هر عددی که گزارش می‌دهی باید از یک سلول/فیلد مشخص باشد؛ جمع و تفریق دستی نکن مگر کاربر بخواهد و روش را بگو.
9. اگر کاربر گفت «فعلاً کد ننویس / فقط صحبت کنیم»: فقط منطق، بدون کد یا فایل.

## کجا چه چیزی هست
- `docs/memory-notes.md`: همه چیزهایی که کاربر خودش گفته (روال روزانه، پیمانکارها، مسیرها، تعریف‌ها).
- `docs/findings.md`: یافته‌ها: ساختار ستون‌های PMS-Overall، جریان داده، ماکرو، شیت‌های پیمانکار، خطاها، تطبیق 29 اکتبر.
- `docs/open-questions.md`: چیزهایی که هنوز نمی‌دانیم.
- `docs/tasks.md`: **کارهای جاری** با شرط پایان قابل‌بررسی؛ اول هر Session بخوان، آخرش به‌روز کن.
- `docs/vba-review.md`: تحلیل مرحله ۱ بسته ماکرو PILE COMPILE (۶ ماژول در `macros/pile-compile-package/`)؛ کاربر تصمیم گرفت تغییری لازم نیست (فقط مرجع).
- `docs/file-inventory.md`: فایل‌هایی که باید کنار پروژه بگذاری و هر کدام چیست.
- `docs/reference/`: راهنمای WBS (PDF)، سند زمینه کاری، اسکرین‌شات پوشه‌ها، دستورالعمل قدیمی پروژه.
- `.claude/skills/`: اسکیل‌ها:
  - `odcc-project-controls-agent` (اصلی، شامل references/pms-workbook.md، wbs-structure.md، daily-routine.md)
  - `oil-gas-project-controls` (آموزش + `PRINCIPLES.md` مرجع اصول)
  - `excel-project-controls` (فرمول، فایل سنگین، Power Query، تاریخ شمسی)
  - `power-bi-progress-model` (مدل و DAX پیشرفت/منحنی S) + `power-bi-dax` (DAX عمومی)
  - `sql-project-controls` (رول‌آپ وزنی، منحنی S، تطبیق، کوئری P6)
  - `p6-xer-offline` (P6 با فایل XER، بدون MCP؛ ابزار `scripts/xer_tool.py`) + `p6-primavera-agent` (۱۵ workflow، فقط با MCP Primavera P6)
  - اسکیل‌های بیرونی (MIT، از GitHub؛ اسکریپت‌هایشان هنوز اجرا/تست نشده، قواعد این فایل مقدم است):
    `excel-vba-workbook-engineering`، `excel-workbook-qa-auditor`، `power-query-m-engineering`، `power-pivot-dax-modeling`، `power-bi-semantic-model` (از 90le/microsoft-excel-bi-agent؛ ابزارها در `vendor/microsoft-excel-bi-agent/tools/`)، `sql-to-dax` (Microsoft semantic-link-labs)، `evm` (EVM هزینه‌ای؛ برای گزارش پیشرفت فیزیکی PMS نیست)
  - اسکیل‌های صرفه‌جویی توکن (MIT، بیرونی): `token-optimization` (valorisa/claude-skills؛ بعضی تنظیمات پیشنهادی‌اش مثل `contextWindow` و `context: fork` تأیید نشده‌اند) و `token-saver-claude-code` (otnc/skills_ai-agent-saving-tech؛ ابزار فقط‌خواندنی `scripts/audit.mjs`)
- `tools/pms_inspect.py`: خواندن امن و سریع فایل‌های بزرگ اکسل (help: `python tools/pms_inspect.py -h`).

## روال روزانه (خلاصه؛ جزئیات در اسکیل odcc-project-controls-agent)
1. پوشه روز قبل را کپی کن، نام روز جدید؛ نام xlsm را یک روز جلو ببر؛ PDF قدیمی را پاک کن؛ `CONTRACTORS` را خالی کن (کاربر انجام می‌دهد).
2. ماکرو RunPMSUpdate را اجرا کن (کاربر).
3. فایل پیمانکارها را در `CONTRACTORS` بگذار؛ از هر فایل شیت‌های لازم: Soleh: `PMS (9)`→SOL1، `PMS EXTRA`→SOL2؛ Sekaf & Farjoud: `PMS`→FRJP، `Extra Work`→FRJE؛ Ehdas: `PMS-01`→Ehds؛ هر سه: `Man Power`، `Machinery`.
4. داده امروز در ستون روز (شماره = AT3 در همه شیت‌ها، از جمله FRJP) از ستون AV به بعد وارد می‌شود؛ ماکروهای FRJ/SOI/EHD Data Entry این کار را انجام می‌دهند؛ Today با فرمول می‌آید؛ PMS-Overall از شیت‌های پیمانکار می‌خواند.
5. کنترل‌ها (AV2 در شیت‌های پیمانکار، Prj-V) و خروجی (PDF/ارسال): هنوز از کاربر نپرسیده‌ام.
**نقش من در این مرحله:** فهمیدن و کنترل، نه وارد کردن داده. کاربر گفته «بعداً درباره وارد کردن داده حرف می‌زنیم».

## ایجنت بودن
- **شروع هر Session:** `docs/tasks.md` و بخش‌های مرتبط `docs/memory-notes.md` را بخوان. حافظه زمینه است؛ درخواست فعلی کاربر مقدم است.
- **پایان کار:** فقط وقتی بگو «انجام شد» که شرط پایان در `docs/tasks.md` با شواهد (سلول، خروجی دستور، فایل) نشان داده شده باشد.
- قبل از هر کار چندمرحله‌ای، برنامه کوتاه بنویس و منتظر تأیید بمان (به‌ویژه برای نوشتن/حذف).
- بعد از هر یافته جدید یا قاعده جدیدی که کاربر گفت، `docs/memory-notes.md` یا `docs/findings.md` را به‌روز کن.
- Claude Code حافظه گفتگوی claude.ai را ندارد؛ این پوشه تنها حافظه است.
