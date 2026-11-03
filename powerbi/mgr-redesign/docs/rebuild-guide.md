# راهنمای بازسازی داشبورد MGR در Power BI Desktop

ترتیب: اول مدل (۱ تا ۵)، بعد ظاهر (۶ تا ۸). روی **کپی** pbix کار کن یا فایل جدید بساز (پیشنهاد: فایل جدید، چون مدل قدیمی ۳۵ جدول و رابطه‌های غلط دارد).

## ۱. دیتابیس
1. `python -I scripts/build_model.py "<مسیر>\MGR-25-SEP-2026.xlsx" "<مسیر>\MGR-Model.xlsx"`
2. شیت `Check` را باز کن: ۲۵ ردیف OK و ۴ ردیف DIFF با توضیح (پایین همین صفحه). اگر DIFF تازه‌ای دیدی، ادامه نده.

## ۲. تنظیمات فایل
- File > Options > Current file > Data load: **Auto date/time را خاموش کن** (۱۰ جدول مخفی LocalDateTable حذف می‌شود).
- Regional settings: English (United States) تا اعشار و تاریخ یکسان خوانده شود.

## ۳. Power Query
از `model/PowerQuery.m`: پارامتر `ModelPath`، تابع `fnLoad`، سپس ۲۳ کوئری (هر کدام یک خط). هر ماه فقط `ModelPath` عوض می‌شود.

## ۴. مدل
- رابطه‌ها: فهرست پایین `PowerQuery.m` (همه Many-to-one، Single). **هیچ رابطه دوطرفه یا یک‌به‌یک لازم نیست.**
- `DimDate` → Table tools > Mark as date table.
- Sort by column طبق فهرست. ستون‌های کلید در Factها و جدول `Snapshot` را Hide کن.

## ۵. Measureها
- Home > Enter data > نام `_Measures` > Load. هر بلوک `model/Measures.dax` را New measure کن، Format و Display folder طبق توضیح.
- **قاعده:** هیچ ستون عددی را مستقیم (Sum خودکار) در ویژوال نگذار؛ همیشه Measure. SPI هیچ‌وقت جمع زده نمی‌شود.
- کنترل: کارت `[Plan % @ Cut-off]` = 14.52% و `[Actual % @ Cut-off]` = 15.94% (برابر `1-Overall Plan!C2` و `1-Overall Actual!C2`)؛ SPI پیمانکارها: Souleh 0.91، Sekaf 0.99، Ehdas 0.97.

## ۶. تم و قالب صفحه
- View > Themes > Browse for themes > `theme/ODCC-Navy.json`.
- اندازه صفحه 16:9 (1280×720). نوار بالا: Rectangle سرمه‌ای `#1F3864` ارتفاع ۸۰، لوگو چپ، عنوان (Text box، Segoe UI Semibold 20، سفید)، Card با `[Report Subtitle]` زیر عنوان، و در گوشه راست Card کوچک `[Cut-off Date]`. این نوار را در یک صفحه بساز، Group کن و به همه صفحه‌ها Copy کن (Ctrl+C/V جای یکسان می‌گذارد).
- فاصله ثابت: لبه ۱۶px، بین ویژوال‌ها ۱۲px. Format > Align برای هم‌ترازی.
- نام صفحه‌ها: `1 Executive Summary`، `2 Discipline x Unit`، `3 Major Quantities`، `4 Contractors`، `5 6-Month Window`، `6 Manpower`. Page navigator (Insert > Buttons > Navigator) زیر نوار.

## ۷. نقشه صفحه‌ها (همان ماکت)
هر پنل ماکت پایینش نوشته کدام ویژوال و کدام Measure. خلاصه:

| صفحه | ویژوال‌ها | جایگزین چه |
|---|---|---|
| 1 Executive Summary | ۶ Card (Plan، Actual، Variance، SPI، Man-hours، Machine-hours)؛ Line منحنی S با Slicer Scope؛ جدول WBS سطح ۱ با رنگ SPI؛ Bar دیسیپلین Plan/Actual | Gauge، Scrolling Text، جدول Estimate 1 |
| 2 Discipline x Unit | Matrix Unit×Discipline با Conditional formatting گرادیان | Gaugeهای Plan/Actual هر Unit |
| 3 Major Quantities | جدول اقلام با Data bars و آیکن SPI؛ Stacked column شمع/بتن هر Unit | ۷ Liquid Fill Gauge، دو Bullet Chart |
| 4 Contractors | ۳ Card پیمانکار با SPI رنگی؛ Line منحنی S پیمانکار (Slicer تک‌انتخابی یا Small multiples)؛ جدول اقلام | ۳ Liquid Fill، ۲ Waffle، Bullet OKViz |
| 5 6-Month Window | Card SPI محاسبه‌شده و گزارش‌شده؛ Line منحنی S پنجره؛ یک Column با Small multiples بر حسب Item؛ جدول پیمانکار | ۶ Liquid Fill، ۳ نمودار جدا |
| 6 Manpower | Card؛ Column هیستوگرام ODCC با Slicer نسخه برنامه (47/60 ماهه)؛ جدول واحدها با Data bars؛ Stacked column پیمانکار ماهانه | Sunburst، ۵ Card جدا |

رنگ SPI همه جا یکی: قرمز < 0.95، کهربایی 0.95 تا 1، سبز ≥ 1، خاکستری = بدون Plan. (Format > Conditional formatting > Field value > Measure رنگ).

## ۸. پاک‌سازی pbix قدیمی (اگر روی کپی کار می‌کنی)
- View > Visualizations > … > ویژوال‌های سفارشی بلااستفاده را Delete کن: ۳۳ تا در فایل است، فقط ۶ تا استفاده شده (Liquid Fill Gauge، Bullet Chart، Bullet OKViz، Waffle، Sunburst، Scrolling Text). بعد از بازطراحی هیچ‌کدام لازم نیست.
- رابطه‌های غلط فعلی (همه را حذف کن): `1-WBS[V.R]→Estimate 3[V.R]`، `1-WBS[Row]→Overall ODCC[Row]`، `03-Discipline[Row]→Overall ODCC[Row]`، `Estimate 1[Row]↔Overall ODCC` (1:1 Both)، `1-Overall Plan[Min]↔1-Overall Actual[Min]` (1:1 Both).

## اختلاف‌های فایل منبع (شیت Check) [تأیید شود]
1. **EST2!D38** (بتن Process & Utility، Unit = Overall) عدد تایپی 118,115.26؛ جمع Unitها 91,721.50 (اختلاف 26,393.76). مدل جمع Unitها را نگه داشته.
2. **Contractor!E18:G18** (Overall، Aug 2026) تایپی، جمع 23,948؛ جمع چهار پیمانکار 26,534 (اختلاف 2,586). مدل ردیف پیمانکارها را نگه داشته که با `Overall Contractor!A2` = 238,889 برابر است.
3. **دیسیپلین‌ها:** میانگین وزنی `03-Discipline` به عدد کل نمی‌رسد (Plan 13.38% در برابر 14.52%، Actual 15.88% در برابر 15.94%). WBS سطح ۱ دقیقاً می‌رسد؛ پس KPI کل از WBS است و Total در نمودار دیسیپلین خاموش.
4. **ODCC** دو نسخه برنامه (47 و 60 ماهه) روی ماه‌های یکسان دارد؛ Actual هر دو تقریباً یکی (اختلاف ۵۰ نفر-ساعت). هرگز جمع نزن؛ Slicer تک‌انتخابی.
5. **SPI-6M** (0.97، 0.77، …) فرمول ندارد؛ Measure `[6M SPI (calculated)]` از منحنی پنجره 0.99 می‌دهد. منشأ اعداد تایپی را بپرس.
6. `03-Discipline` جایی که Plan = 0 است SPI = 1 گذاشته؛ در مدل BLANK است (درست‌تر).
