# یافته‌های بررسی (تا 2026-11-02)
برچسب: [فایل] = از خود فایل خوانده شده | [استنباط] = نتیجه‌گیری من، باید تأیید شود.

## ۱) نسخه‌ها و ساختار PMS-Overall
- نسخه 19 اکتبر (`CSM-DR-P2-2026-Oct-19.xlsx`, ۵۷MB، xlsx بدون VBA، ۳۲ شیت): ستون‌ها دو ستون عقب‌تر از نسخه 29 اکتبر بودند (Up to Last=X/Y، Today=Z/AA، Up to Now=AB/AC، سطح‌ها K:R، تاریخ مبنا AI3، منحنی AX:BDC).
- نسخه 29 اکتبر: A WBS, B LVL, C ID CODE, D Work Package, E Disc., F Unit, G Tag, H Sub Contractors, I Sub Contractors Activity id, J Sub Activity id, K Activity ID, L ROW, M:T سطح ۱..۸, U Activity, V W.V%, W W.F%, X/Y W.F%1/2, Z/AA Plan/Actual Up to Last, AB/AC Today, AD/AE Up to Now, AF Variance, AK3 تاریخ مبنا (ثابت = تاریخ Cov-P!AA7-1), AY ردیف منحنی, AZ:BDE منحنی تجمعی Plan روزانه (از 2025-04-22).
- Plan Up to Now: `HLOOKUP($AK$3,$AZ$7:$BDE$5659,$AY8)`؛ وزن کل: `W = V × VLOOKUP(J, $K$8:W, 13, 0)`؛ Actual والد = جمع وزنی فرزندان.
- ردیف پایان داده PMS-Overall: 5659 (علامت «x»). ماکرو تا 5800 می‌خواند.
- فایل xlsm 29 اکتبر نسخه اول بدون VBA بود؛ نسخه‌های `_1` و `_2` ماکرو (Module1 = Zero_maker) دارند.

## ۲) جریان داده بین شیت‌ها [فایل]
شیت‌های پیمانکار (SOL1, SOL2, Ehds, FRJP, FRJE + غیرفعال: EXR-Piling, EXR-86&66, IGC-PR45, PR-5301 FRJ) ← Cnt-V ← Prj-V؛ ← Cnt-t؛ ← PMS-Overall (Actual%)؛ PMS-Overall ← WPA, Unt-P-Weekly/Month, Dis-P-Weekly/Month, UDS-P, Unit Wise, Dashboard, Cov-P. Piling و Concrete Pouring ← Summary ← Dashboard/Prj-V. Manpower و Machinary ← Cov-P (و Cnt-t، Dashboard).
اجرای دوباره: `python tools/pms_inspect.py edges FILE SHEET`.

## ۳) ماکرو RunPMSUpdate (Zero_maker)
- نسخه نهایی: `macros/Zero_maker_10deab.bas` (برابر f3d6b6). اصلاح شده: Ehds AC7:AC800 → **AA7** (قبلاً Y7 بود که ستون Contract را خراب می‌کرد).
- ماکروی داخل `CSM-DR-P2-2026-Oct-29_2.xlsm` دقیقاً همین است. سرتیترها/آدرس‌های مورد استفاده با ساختار فایل جور بود.
- نکته‌های کم‌اهمیت (کاربر گفته لازم نیست): بازه‌هایی که از ردیف سرتیتر (۷ یا ۳ در FRJE) شروع می‌شوند متن سرتیتر Up to Last را با «Up to Now» عوض می‌کنند؛ قفل اجرا فقط تا بستن اکسل است؛ هیچ دکمه‌ای به ماکرو وصل نیست.
- ماکرو به Manpower, Machinary, Cnt-t, Piling, Concrete Pouring, Steel Structure, PR-5301 FRJ, EXR-*, IGC-PR45 دست نمی‌زند.

## ۴) شیت‌های پیمانکار (مثال Ehds) [فایل]
- جدول روزانه: AV:CA (۳۱ ستون)؛ AV5 `W0`؛ AW5..CA5 = 1..31؛ AW6 = 2026-10-23 (ثابت)، هر ستون +۱ روز. AT3 = شماره روز امروز (7 = 29 اکتبر ← ستون BC)؛ AU3 = `SUMPRODUCT((AV5:CA5=AT3)*COLUMN(AV5:CA5))-COLUMN(AU5)+1`.
- Today کمیت: `AB = VLOOKUP(AU<ردیف>, $AU$5:$CA$453, $AU$3, 0)`؛ `AC = AB + AA`؛ `AG = AI − AE`.
- چک‌ها: `AV2` = Error اگر AI>1 یا AG<0؛ `AW2` فقط ستون AT را می‌شمرد و AV2 را نمی‌بیند [فایل، باید تست شود]؛ فرمول‌های ردیف ۴ تاریخ را با ردیف تاریخ شیت `EXR-86&66` مقایسه می‌کنند نه Ehds.
- پس از ۳۱ روز (AT3 = 31 → 22 نوامبر) جدول جدید لازم است؛ کاربر هنوز نگفته چطور.
- ستون‌های Plan شیت‌های پیمانکار: فرمول `IFERROR(HLOOKUP($AH$5,#REF!,...),0)` شکسته؛ مقدار کش‌شده ۰ (SOL1 AG: ۵۲۵ فرمول همه ۰؛ Ehds AF: ۴۴۵). کاربر می‌گوید Plan خالی نیست [حل‌نشده؛ ستون/سلول دقیق را بپرس].

## ۵) تطبیق Today فایل پیمانکار با فایل ما (29 اکتبر) — فقط مشاهده، ورود داده بعداً
- SOL1 ↔ `PMS (9)` (Soleh): هر دو ۲۲ ردیف Today غیرصفر؛ ۱۹ مورد یکی؛ Pedestal Formwork (V-6620) پیمانکار 29.578 و ما 26.4؛ E-6604 A/B: Footing Isolation 25 و Pedestal Rebar 0.07 در فایل ما نیست؛ دو ردیف Pedestal Formwork یونیت 86 (ردیف‌های 778 و 795) در ما 0.5 دارند ولی پیمانکار Today ندارد.
- SOL2 ↔ `PMS EXTRA`: پیمانکار ۲۱ ردیف، ما ۱۳؛ ۱۰ مورد یکی؛ Formwork SS-40-07: 56.6 در برابر 38.6؛ Concrete: 252.48 در برابر 252.0؛ ۹ ردیف E-5201 A/B/C با 0.1 (احتمالاً مقدار پرکننده [استنباط]) در ما نیست؛ Pile Chipping 17 (ردیف 188) فقط در ماست.
- کلید تطبیق: Tag + نام فعالیت. شماره ردیف و Activity ID بین دو فایل یکی نیست (ردیف ما معمولاً ۱ کمتر، در یونیت 40 تا ۶ کمتر).

## ۶) فایل‌های پیمانکار [فایل]
- Soleh (`.xlsb`): شیت‌ها: General Information & HSE, Narrative, Man Power, Machinery(با فاصله), Material, PMS (9), PMS EXTRA, P6, P.6, site2-2, man power (2). تاریخ `PMS (9)!AH6` و PMS EXTRA!AM4؛ Today کمیت: PMS (9)!AA، PMS EXTRA!AF.
- Sekaf & Farjoud (`.xlsb`): General Information & HSE, Narrative, Summry Report, PMS-PILL (قدیمی 2025-07-28), PMS-PR (قدیمی 2025-07-22 = ↔ PR-5301 FRJ ما), Man Power, Machinery(با فاصله), PMS (فعال, S1 = تاریخ), Extra Work (جدول TAG/Estimate/Today/Done/Remain), Summary, Front, Sheet1, Fabrication, Pile Driving.
- Ehdas (`.xlsx`, ۲۷ شیت): PMS-01 (Q=UTL, R=TODAY, S=UTN; وزن بر اساس Price؛ تاریخ از Narrative!K2؛ UTN = SUM(AK:RS))، PMS-SM (نسخه روز قبل؟ [استنباط]), Man Power, Machinery. فایل 30 اکتبر فقط ۳ ردیف Today دارد (Rebar 600 KG ×2، Form Working 15 M2).
- Man Power و Machinery هر سه ساختار مشابه Manpower ما دارند (Direct/Indirect × Local/Non Local × Day/Night/Rest).

## ۷) خطاها و نکته‌های فایل ما [فایل]
- `#REF!`: Plan پیمانکاران (بالا)، `PMS-Overall!BDC4`, Cov-P S37/V37, Unit Wise S36/U36؛ `#N/A`: PMS-Overall BIV.. (ردیف 983 به بعد)، Concrete Pouring ستون R، بخشی از Piling؛ `#VALUE!` Cnt-t ستون A.
- فایل سنگین: PMS-Overall ≈ 380MB XML، ۳۴۴۶ Defined Name (۳۱۵۱ مخفی، ۸۹۶ با #REF!)، styles ≈ 11MB. بدون اجازه پاک نکن.
- `Machinary` غلط املایی است (درست: Machinery)؛ فقط ۲ فرمول در Cov-P و یک فیلتر مخفی به آن ارجاع دارند؛ ماکرو ندارد. کاربر گفت فعلاً دست نزن.
- Cnt-t: K11:AO11 تاریخ واقعی با فرمت `dd`؛ دوره ماهانه 26 تا 25.

## ۸) داشبورد مدیریتی MGR (Power BI) — بررسی 2026-11-03 [فایل]
فایل‌ها: `MGR-25-SEP-2026.xlsx` (۲۴ شیت، Cut-off = `Time 6 Month!C2` = 2026-09-25) و `MGR-25-SEP-2026.pbix`. خروجی بازطراحی: `powerbi/mgr-redesign/`.
- **pbix:** ۳۵ جدول (۲۴ شیت + ۱۰ LocalDateTable + ۱ Template؛ Auto date/time روشن)؛ **صفر Measure**؛ همه ویژوال‌ها Sum خودکار ستون (از جمله Sum روی SPI). مسیر منبع ثابت `C:\Users\h.sheykhmiri\Desktop\MGR-25-SEP-2026\...`.
- رابطه‌های غلط: `1-WBS[V.R]→Estimate 3[V.R]`، `1-WBS[Row]→Overall ODCC[Row]`، `03-Discipline[Row]→Overall ODCC[Row]`، `Estimate 1[Row]↔Overall ODCC` (1:1 Both)، `1-Overall Plan[Min]↔1-Overall Actual[Min]` (1:1 Both).
- ۳۳ ویژوال سفارشی داخل فایل، فقط ۶ استفاده‌شده (Liquid Fill Gauge ×17، Bullet Chart، Bullet OKViz، Waffle، Sunburst، Scrolling Text).
- اکسل: جدول‌های پهن (`Estimate 2`، `SPI`، `SPI-6M`، `6M-S-curve-m`، `Overall Contractor`)؛ `03-Discipline` درصد 0-100 و بقیه 0-1؛ `SPI-6M` و `Overall ODCC` بدون فرمول.
- کنترل‌شده: میانگین وزنی `1-WBS` = Overall دقیقاً (14.5175% / 15.9423%)؛ SPI پیمانکار = آخرین ردیف Actual در `S-Curve-Contractor` (برابر `Progress-SPI-Contractor`).
- اختلاف‌ها [تأیید شود]: `EST2!D38` تایپی 118,115.26 ≠ جمع Unitها 91,721.50؛ `Contractor!E18:G18` (Overall Aug-2026) تایپی 23,948 ≠ جمع پیمانکارها 26,534؛ میانگین وزنی دیسیپلین ≠ Overall (Plan 13.38%)؛ شیت `ODCC` دو نسخه برنامه 47 و 60 ماهه روی ماه‌های یکسان دارد (جمع نزن).
