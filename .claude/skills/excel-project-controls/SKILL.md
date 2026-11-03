---
name: excel-project-controls
description: "اکسل برای کنترل پروژه: کار امن با xlsm/xlsb/xlsx سنگین (PMS)، فرمول‌های پیشرفت وزنی (SUMPRODUCT، VLOOKUP/XLOOKUP، HLOOKUP روی تاریخ)، پیدا کردن و ریشه‌یابی #REF!/#N/A، فیلتر Today، تطبیق فایل پیمانکار با فایل ما، Power Query برای جمع کردن فایل‌های روزانه پیمانکاران، منحنی S و داشبورد اکسل، و تاریخ شمسی. هر وقت کاربر درباره فرمول، شیت، فایل اکسل، xlsm، xlsb، Power Query یا ساخت فایل اکسل کنترل پروژه پرسید از این اسکیل استفاده کن. Excel for project controls, heavy workbook handling, progress formulas, Power Query."
---

# اکسل برای کنترل پروژه

برای ساختن فایل اکسل جدید (قالب، جدول، نمودار) از `anthropic-skills:xlsx` هم استفاده کن. قواعد `CLAUDE.md` (کار روی کپی، دست نزدن به ماکروها، xlsm ماندن) همیشه مقدم است.

## کار امن با فایل‌ها
| کار | ابزار | هشدار |
|---|---|---|
| خواندن PMS سنگین (۵۷MB) | `tools/pms_inspect.py` (استریم XML) | openpyxl/pandas روی PMS-Overall قفل می‌کند |
| خواندن xlsb پیمانکار | `pyxlsb` یا `pms_inspect.py xlsb-head` | فرمول‌ها را نمی‌دهد، فقط مقدار |
| خواندن xlsx/xlsm کوچک | `openpyxl.load_workbook(f, read_only=True, data_only=True)` | `data_only=True` مقدار کش‌شده را می‌دهد، نه فرمول |
| ذخیره xlsm | **نکن**. openpyxl با `keep_vba=True` هم ممکن است شکل‌ها، نمودارها، کنترل‌های فرم و بخشی از قالب‌بندی را از دست بدهد | خروجی را در فایل جدید بنویس؛ فایل کاری را خود کاربر در اکسل تغییر دهد |

مقدار کش‌شده‌ی فرمول همان است که آخرین بار اکسل حساب کرده. اگر فایل بعد از تغییر ذخیره نشده یا Calculation روی Manual بوده، ممکن است کهنه باشد. این را در گزارش بگو.

## فرمول‌های رایج کنترل پروژه
- **پیشرفت وزنی والد:** `=SUMPRODUCT(weights, progress)/SUM(weights)`. جمع وزن را جدا چک کن (باید 100% شود).
- **پیشرفت کل از وزن کل:** فقط روی ردیف‌های برگ جمع بزن تا والد و فرزند دوبار شمرده نشوند.
- **Plan تا تاریخ از منحنی روزانه** (همان PMS-Overall): `=HLOOKUP($AK$3, $AZ$7:$BDE$5659, $AY8, FALSE)`. ردیف ۷ تاریخ‌هاست و AY شماره ردیف نسبی.
- **VLOOKUP با شماره ستون ثابت** با درج ستون می‌شکند. در Excel 365/2021 از `XLOOKUP` یا `INDEX/MATCH` استفاده کن. قبل از پیشنهاد، نسخه اکسل کاربر را بپرس.
- **Variance:** `=Actual − Plan` به واحد درصد. **SPI فیزیکی:** `=IF(Plan=0,"",Actual/Plan)`.
- **ردیف‌های Today غیرصفر** (Excel 365): `=FILTER(A8:AC800, AB8:AB800<>0, "none")`.
- **تطبیق با Tag + نام فعالیت:** کلید کمکی `=TRIM(UPPER(G8))&"|"&TRIM(UPPER(U8))` در هر دو فایل، بعد `XLOOKUP` یا `COUNTIFS` روی کلید.

## خطاها
- `IFERROR(...,0)` خطا را پنهان می‌کند: `#REF!` شکسته مقدار ۰ نشان می‌دهد (همین اتفاق در ستون‌های Plan شیت‌های پیمانکار افتاده). در گزارش، صفر حاصل از IFERROR را «نامعلوم» بنام، نه صفر.
- `#REF!` یعنی محدوده یا شیت مرجع حذف شده؛ با `pms_inspect.py` محل ارجاع را پیدا کن و به کاربر بگو. خودت فرمول را تعمیر نکن.
- `#N/A` در VLOOKUP/HLOOKUP: کلید یا تاریخ پیدا نشده. نوع داده (متن در برابر عدد) و فاصله‌های اضافه را چک کن.
- Defined Nameهای `#REF!` و استایل‌های زیاد فایل را سنگین می‌کنند. حذفشان فقط با اجازه کاربر و روی کپی.

## Power Query (جمع کردن فایل‌های پیمانکاران)
الگو: `Data > Get Data > From Folder` روی پوشه `CONTRACTORS` → فیلتر نام فایل → `Excel.Workbook([Content])` → انتخاب شیت (مثلاً `PMS (9)`) → حذف ردیف‌های سرتیتر → `Promote Headers` → فیلتر Today ≠ 0.
```m
let
    Src   = Folder.Files("C:\Copy\CONTRACTORS"),
    Soleh = Table.SelectRows(Src, each Text.StartsWith([Name], "Daily Report")),
    Wb    = Excel.Workbook(Soleh{0}[Content], null, true),
    Sh    = Wb{[Item = "PMS (9)", Kind = "Sheet"]}[Data],
    Skip  = Table.Skip(Sh, 6),
    Hdr   = Table.PromoteHeaders(Skip, [PromoteAllScalars = true])
in
    Hdr
```
تعداد ردیف‌های سرتیتر (`Table.Skip`) و نام ستون‌ها را از فایل واقعی بخوان. اگر پیمانکار ساختار را عوض کند، کوئری می‌شکند؛ پس یک مرحله کنترل (تعداد ستون، نام ستون کلیدی) اضافه کن. Power Query را روی خود فایل PMS سنگین اجرا نکن؛ در یک فایل کمکی جدا بساز.

## تاریخ شمسی
فایل‌های پیمانکار تاریخ شمسی دارند (مثلاً `1405.08.07` = 2026-10-29، طبق نام فایل). برای تبدیل در Python: `pip install jdatetime`، سپس `jdatetime.date(1405,8,7).togregorian()`. در اکسل، اگر Windows با Persian calendar تنظیم نیست، از جدول تبدیل کمکی استفاده کن. هر تبدیل را با یک تاریخ شناخته‌شده چک کن.

## داشبورد و منحنی S در اکسل
- داده را در یک Table (Ctrl+T) با ستون‌های Date | Plan Cum | Actual Cum نگه دار؛ نمودار Line روی Table خودکار بزرگ می‌شود.
- Actual را بعد از Data Date خالی (`NA()`) بگذار تا خط به صفر سقوط نکند.
- برای Early/Late، دو سری Plan بگذار. محور تاریخ را Date axis کن، نه Text.
