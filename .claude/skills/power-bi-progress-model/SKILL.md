---
name: power-bi-progress-model
description: "ساختن داشبورد Power BI برای پیشرفت پروژه از فایل PMS: مدل ستاره‌ای (Activity/WBS، Date، Progress، PlanCurve، Manpower، Machinery)، Power Query برای استخراج از فایل‌های روزانه، و DAX برای Plan/Actual تجمعی در تاریخ گزارش، Variance، SPI فیزیکی، پیشرفت روزانه/هفتگی، منحنی S و تفکیک Unit/Discipline/Contractor. هر وقت کاربر درباره Power BI، داشبورد پیشرفت، منحنی S در Power BI یا اتصال PMS به Power BI صحبت کرد از این اسکیل استفاده کن؛ برای جزئیات DAX عمومی از power-bi-dax هم کمک بگیر. Power BI progress dashboard, S-curve DAX, PMS data model."
---

# مدل Power BI پیشرفت پروژه (PMS)

این اسکیل ساختار مدل و Measureهای مخصوص پیشرفت را می‌دهد. قواعد عمومی DAX (Filter Context، Date Table، کارایی) در `power-bi-dax` است.

## قبل از ساخت
1. منبع داده را از فایل واقعی بخوان: کدام شیت، کدام ستون‌ها، کدام ردیف‌ها. فرض نکن PMS آماده اتصال است.
2. PMS-Overall حدود ۵۶۵۰ ردیف دارد و منحنی Plan روزانه حدود ۱۵۰۰ ستون. وارد کردن مستقیم شیت ۳۸۰MB در Power Query بسیار کند است. پیشنهاد: هر روز یک خروجی سبک (CSV یا xlsx) از ستون‌های لازم بگیر (با `tools/pms_inspect.py` یا یک شیت خروجی که کاربر می‌سازد) و Power BI را به پوشه آن خروجی‌ها وصل کن.
3. تعریف‌ها را از کاربر تأیید کن: W.V و W.F، سطح ورود Actual (L6/L7)، تاریخ گزارش (AK3).

## مدل ستاره‌ای
```
DimActivity (ActivityKey, ActivityID, WBS, ParentKey, Level, Unit, Discipline, WorkPackage, Tag, Contractor, WeightOverall, IsLeaf)
DimDate     (Date, Year, Month, Week, IsReportDay)          ← Mark as Date Table
FactProgress(ActivityKey, Date, PlanCum, ActualCum)          ← یک ردیف به ازای هر فعالیت برگ در هر روز گزارش
FactPlanCurve(ActivityKey, Date, PlanCum)                    ← از AZ:BDE، Unpivot شده؛ اختیاری
FactManpower(Date, Contractor, Category, Shift, Count)
FactMachinery(Date, Contractor, Equipment, Count)
```
- رابطه‌ها: یک‌به‌چند از Dim به Fact، جهت فیلتر Single.
- **فقط ردیف‌های برگ** (`IsLeaf = TRUE`) در FactProgress بیایند. والدها را DAX با وزن جمع می‌زند؛ آوردن والد و فرزند با هم یعنی دوباره‌شماری.
- برای سلسله‌مراتب WBS در Matrix، ستون‌های L1..L8 را در DimActivity صاف (flatten) کن یا از `PATH()` استفاده کن.

## Power Query: Unpivot منحنی Plan
```m
let
    Src      = Excel.Workbook(File.Contents("C:\Copy\plan_curve.xlsx"), null, true){[Item="Curve",Kind="Sheet"]}[Data],
    Hdr      = Table.PromoteHeaders(Src, [PromoteAllScalars=true]),
    Unpivot  = Table.UnpivotOtherColumns(Hdr, {"ActivityKey"}, "Date", "PlanCum"),
    Types    = Table.TransformColumnTypes(Unpivot, {{"Date", type date}, {"PlanCum", type number}}),
    NoZero   = Table.SelectRows(Types, each [PlanCum] <> null)
in
    NoZero
```

## Measureها
مقادیر PMS **تجمعی**اند؛ پس در یک تاریخ نباید روی روزها SUM زد. باید مقدار آخرین روز گزارشِ تا تاریخ انتخاب‌شده را برداشت.
```dax
Report Date :=
VAR maxd = MAX ( DimDate[Date] )
RETURN CALCULATE ( MAX ( FactProgress[Date] ), REMOVEFILTERS ( DimDate ), DimDate[Date] <= maxd )

Actual Cum % :=
VAR d = [Report Date]
RETURN
    CALCULATE (
        SUMX ( FactProgress, FactProgress[ActualCum] * RELATED ( DimActivity[WeightOverall] ) ),
        REMOVEFILTERS ( DimDate ), DimDate[Date] = d )
    / CALCULATE ( SUM ( DimActivity[WeightOverall] ), DimActivity[IsLeaf] = TRUE () )

Plan Cum % :=
VAR d = [Report Date]
RETURN
    CALCULATE (
        SUMX ( FactProgress, FactProgress[PlanCum] * RELATED ( DimActivity[WeightOverall] ) ),
        REMOVEFILTERS ( DimDate ), DimDate[Date] = d )
    / CALCULATE ( SUM ( DimActivity[WeightOverall] ), DimActivity[IsLeaf] = TRUE () )

Variance % := [Actual Cum %] - [Plan Cum %]

SPI (physical) := DIVIDE ( [Actual Cum %], [Plan Cum %] )

Actual Period % :=
VAR d     = [Report Date]
VAR prevd = CALCULATE ( MAX ( FactProgress[Date] ), REMOVEFILTERS ( DimDate ), FactProgress[Date] < d )
VAR prev  = CALCULATE ( [Actual Cum %], REMOVEFILTERS ( DimDate ), DimDate[Date] = prevd )
RETURN [Actual Cum %] - prev
```
- تقسیم بر جمع وزن در Context باعث می‌شود یک Unit یا Discipline با وزن خودش ۱۰۰٪ شود (پیشرفت داخلی). برای «سهم از کل پروژه»، Measure جدا بساز که مخرجش `REMOVEFILTERS ( DimActivity )` داشته باشد.
- `SPI (physical)` نسبت درصد وزنی است، نه SPI در EVM (EV/PV با هزینه). روی داشبورد همین را بنویس.
- **منحنی S:** نمودار Line با محور `DimDate[Date]` و دو سری Plan Cum % و Actual Cum %. Actual را بعد از آخرین روز گزارش BLANK کن: `IF ( MAX ( DimDate[Date] ) > MAX ( FactProgress[Date] ), BLANK (), [Actual Cum %] )` با `REMOVEFILTERS` مناسب.
- **Manpower:** مقادیر روزانه‌اند (تجمعی نیستند)؛ پس SUM برای یک روز درست است، و برای ماه `AVERAGEX ( VALUES ( DimDate[Date] ), ... )` یا Peak با `MAXX`.

## کنترل نتیجه
عدد کل پروژه در Power BI در تاریخ گزارش باید با سلول کل PMS-Overall (Plan/Actual Up to Now در ردیف L1) برابر باشد. قبل از ارائه داشبورد، این دو را کنار هم بگذار و اختلاف را گزارش کن.
