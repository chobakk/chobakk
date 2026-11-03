# مرجع کامل فایل PMS — CSM-DR-P2-YYYY-Mmm-DD (ARUP Phase 2-2)

منبع: تحلیل مستقیم فایل `CSM-DR-P2-2026-Oct-19.xlsx` (۵۷ مگابایت، ۳۲ شیت، xlsx بدون VBA).
برچسب‌ها: **[فایل]** مشاهده‌شده در فایل | **[استنباط]** نتیجه‌گیری من | **[تأیید شود]** باید از کاربر پرسیده شود.
هشدار تحلیل: فرمول‌های «shared» در استخراج من فقط متن مادر را نشان می‌دهند؛ ارجاع‌های نسبی فرزندان جابه‌جا می‌شوند. بنابراین «شبیه خطا بودن» مثل `=N7-J7` تکرارشده در چند ردیف، خطا نیست. فقط مقدار کش‌شده و فرمول در خود اکسل را قضاوت کن.

## ۱) هویت فایل
- CSM = Consortium (کنسرسیوم؛ همکاری دو یا چند شرکت/پیمانکار مستقل برای اجرای مشترک پروژه). CSM-DR-P2 = گزارش روزانه (Daily Report) فاز ۲ کنسرسیوم.
- فرمت کاری فایل: **xlsm** (ماکروها را خود کاربر نوشته؛ نسخه آپلودشده xlsx بود). هر فایل خروجی/ویرایش‌شده که قرار است ماکرو داشته باشد باید xlsm بماند.
- پروژه: Abadan Refinery Upgrading Project (ARUP)، فاز 2-2؛ گزارش: ARUP Construction Daily Report. شماره گزارش در Cov-P!I8 = `AA7-45606` (نمونه 0709).
- ODCC در این گزارش «سازمان اجرایی/مدیریت ساخت» است (در شیت Manpower ستون‌های ODCC و ODCC-Tehran دیده می‌شود).
- پیمانکاران [فایل]: IGC (کد IG2، منطقه 45-CV)، Exir/EXR (Piling؛ کد Ex2)، SKF & FRJ، Soleh Iran (Sol 1 و Sol 2)، Ehdas (Ehds)، همچنین ICI، PTS، FRK در Machinary.
- تاریخ: `Cov-P!AA7` تاریخ «روز بعد» (۲۰ اکتبر) است؛ تاریخ گزارش `Cov-P!R7 = AA7-1` (۱۹ اکتبر)؛ `PMS-Overall!K3 = AA7-1`. نام گزارش: `ARUP-P2-DR-` + TEXT(AA7-1). تقریباً هر شیت سرتیتر Date خود را از `Cov-P!R7` می‌سازد.
- `PMS-Overall!AI3` = 46313 (۱۸ اکتبر) ثابت (hard-code) است و یک روز عقب‌تر از K3 است؛ ستون Plan "Up to Now" با HLOOKUP روی AI3 کار می‌کند. [تأیید شود] جایگاه دقیق AI3 در چرخه روزانه (ماکرو «صفر کننده» AI3 را +۱ می‌کند).

## ۲) نقش شیت‌ها (۳۲ شیت)
| گروه | شیت‌ها | نقش |
|---|---|---|
| ورودی تاریخ/پوشش | **Cov-P** | هسته: تاریخ، شماره گزارش، هوا، نیروی انسانی کل، ماشین‌آلات، خلاصه Plan/Actual و SPI؛ منبع تاریخ همه شیت‌ها |
| هسته پیشرفت | **PMS-Overall** (۴٫۷ میلیون سلول، ردیف ۵۶۵۹، ستون تا CIQ) | WBS کل پروژه، وزن‌ها، Plan/Actual تجمعی، منحنی برنامه روزانه |
| گزارش پیشرفت | **WPA**, **Unt-P-Weekly/Month**, **Dis-P-Weekly/Month**, **UDS-P**, **Unit Wise**(مخفی) | نمای واحد/دیسیپلین/هفتگی/ماهانه از PMS-Overall |
| کمیت | **Cnt-V** (Contractors Value), **Prj-V** (Project Value), **Cnt-t** (Monthly Quantity) | کمیت‌های اجرایی هر پیمانکار، جمع کل و روند ماهانه |
| داده روزانه پیمانکاران | **SOL1**, **SOL2**, **Ehds**, **FRJP**, **FRJE**, **PR-5301 FRJ**(مخفی), **EXR-Piling**(مخفی), **EXR-86&66**(مخفی), **IGC-PR45**(مخفی) | ورود کمیت روزانه، Plan/Actual هر پیمانکار |
| پایلینگ/بتن/فولاد | **Piling**, **Concrete Pouring**, **Steel Structure**, **Summary** | وضعیت به‌تفکیک تگ/واحد و خلاصه Estimate/DWG/Done/Remain/Front |
| منابع | **Manpower**, **Machinary**, **ME List**, **Subco W.F** (Pivot) | نیرو (Day/Night/Rest، Local/Non-Local)، ماشین‌آلات (D/N/I/R)، فهرست تجهیزات، وزن زیرپیمانکاران |
| نمایش/راهنما | **Dashboard**, **Coding & About**, **Sheet5** | داشبورد، کدگذاری (Civil 101-119، Electrical 210-239، AG Piping 140-189، Pre-com 500-509) و جدول پیمانکاران |
مخفی: PR-5301 FRJ, EXR-Piling, EXR-86&66, IGC-PR45, Unit Wise.

## ۳) جریان داده (مهم‌ترین بخش)
```
[ورودی روزانه: ستون تاریخ هر شیت پیمانکار] SOL1 / SOL2 / Ehds / FRJP / FRJE / PR-5301 FRJ / EXR-Piling / EXR-86&66 / IGC-PR45
      │  «Today» = VLOOKUP/تطبیق ستون تاریخ با Cov-P!AA7-1 ; «Up to Now» = Up to Last + Today
      ├──► Cnt-V  (SUMIF بر اساس کد فعالیت 101,102,103,104,105,106,108,109,120,122) ──► Prj-V (جمع پیمانکاران، کنترل Error/Ok)
      ├──► Cnt-t  (SUMIF روی ستون Today و Up to Now) ──► جدول ماهانه (دوره 26 تا 25)
      └──► PMS-Overall ستون AC (Actual%) و گاهی G (کد فعالیت زیرپیمانکار)
Piling, Concrete Pouring ──► Summary (SUMIFS بر Phase/Unit/Category) ──► Dashboard, Prj-V(P14 از Summary!L59)
PMS-Overall: Actual% برگ ← پیمانکار؛ سطوح بالا = SUMPRODUCT(AC فرزندان, U وزن)/U ؛ Plan% = HLOOKUP(AI3, AX7:BDC5659, AW) از منحنی روزانه
PMS-Overall ──► WPA (XLOOKUP کد فعالیت), Unt-P-Weekly/Month, Dis-P-Weekly/Month, UDS-P, Unit Wise, Dashboard, Cov-P (SPI و درصدها)
Manpower ──► Cov-P (مجموع direct/indirect), Cnt-t, Dashboard ; Machinary ──► Cov-P
Cov-P!AA7 ──► همه سرتیترهای Date + PMS-Overall!K3
```
### ارجاع‌های بین شیت‌ها (تعداد فرمول، از فایل)
Cov-P ← Manpower, PMS-Overall, Machinary, Dis-P-Month, Dis-P-Weekly | Prj-V ← Cnt-V(۲۲۸), Summary, Cov-P | Cnt-V ← EXR-86&66, EXR-Piling, FRJP, FRJE, PR-5301 FRJ, SOL1, SOL2, Ehds | Cnt-t ← EXR-86&66, FRJP, FRJE, SOL1, SOL2, Ehds, Manpower, PMS-Overall | WPA ← PMS-Overall(۲۳۳) | Unt-P-Weekly/Month ← PMS-Overall(۹۹۸) | Dis-P-Weekly(۹۳۳)/Month(۱۲۲۸) ← PMS-Overall | UDS-P ← PMS-Overall(۸۸۴) | Unit Wise ← PMS-Overall(۲۰۸) | Dashboard ← PMS-Overall(۲۱۱), Manpower, Summary, Piling, Concrete Pouring, Cov-P | Summary ← Piling(۲۶۴), Concrete Pouring(۱۵۴) | Concrete Pouring ← SOL1 (VLOOKUP ستون R) | PMS-Overall ← Summary(۵۱), SOL1(۹۱), SOL2(۳۱), Ehds(۱۵), EXR-86&66(۲۴), Piling(۸), IGC-PR45(۳), FRJE(۲)، Cov-P(K3).
بدون ارجاع به شیت دیگر: ME List, Subco W.F, FRJE(داخلی), Piling, Steel Structure, Sheet5.

## ۴) ساختار هر خانواده
**Cov-P**: AA7 تاریخ ورودی (Ctrl+:)، R7=AA7-1، AA8 وضعیت هوا، AD5/AD7/AD8 تاریخ‌های شروع و پایان پروژه، W10:W17 ماه/روز باقی‌مانده، M13=AB21+AC21 (Man-hour تجمعی)، R13/T13 direct/indirect از Manpower!AU112.. و AU220..، ستون‌های L/P/S/V ارجاع به PMS-Overall و Dis-P-Month، X=V/S (SPI). ستون‌های AG:AR (CLPlan, COPlan, Actual, PlanSPI, ActualSPI, projectid, TMin/TMax, DayDate) کمکی.
**PMS-Overall (نسخه 29 اکتبر — ساختار فعلی)** [فایل]: ردیف ۶ سرتیتر گروه‌ها (Up to Last / Today / Up to Now)، ردیف ۷ نام ستون‌ها، داده از ردیف ۸. A WBS، B LVL (۱..۸)، C ID CODE، D Work Package، E Disc.، F Unit، G Tag، H Sub Contractors، I Sub Contractors Activity id، J Sub Activity id (= کد والد)، K Activity ID، L ROW، M:T سطح ۱ تا ۸، U Activity (نام)، V W.V% (وزن نسبت به والد)، W W.F% (وزن کل = V×وزن والد با VLOOKUP روی J)، X/Y W.F%1 و W.F%2، **Z/AA = Plan%/Actual% Up to Last، AB/AC = Today (=AD−Z، =AE−AA)، AD/AE = Up to Now**، AF Variance (=AE−AD)، AH:AJ Actual W.F%، AK3 تاریخ مبنای Plan (ثابت، در فایل 29 اکتبر = 46324 = M3 = 'Cov-P'!AA7-1 یعنی 2026-10-29)، AK8 .. مقدار کمکی، AM، AN:AO Plan Start/Finish، AP/AQ Start Hour، AR Duration، AU Duration Tehran، AW/AX Actual Start/Finish، AY شماره ردیف منحنی، AZ:BDE منحنی تجمعی Plan روزانه (AZ7=45769=2025-04-22). Plan Up to Now: `AD8 = HLOOKUP($AK$3,$AZ$7:$BDE$5659,$AY8)`. Actual Up to Now در ردیف والد: جمع وزنی فرزندان، مثل `AE8=(AE9*W9+AE215*W215+AE5554*W5554+AE5657*W5657)/W8`. کنترل Error در AN2/AO2 (AC بزرگتر از ۱ یا کوچکتر از ۰). ردیف‌ها تا ۵۶۵۹.
- هشدار نسخه‌ها: در نسخه 19 اکتبر ستون‌ها دو ستون عقب‌تر بودند (Up to Last=X/Y، Today=Z/AA، Up to Now=AB/AC، سطح‌ها K:R، تاریخ مبنا AI3). هر فرمول/ماکرو/ارجاع که به آدرس ستون‌های PMS-Overall وابسته است باید با نسخه واقعی فایل تطبیق داده شود؛ آدرس را از فایل جاری بخوان.
**قرارداد پیمانکاران**: سرتیتر ردیف ۶، Estimate (Contract/New)، Up to Last/Today/Up to Now (کمیت)، Plan%/Actual% در سه بخش، Variance، "Subcontractor Pro%"، Actual Start/Finish، ستون‌های روزانه (۳۱ روز، از ۲۳ سپتامبر ۲۰۲۶ تا ۲۳ اکتبر)، ردیف ۵ شماره روز ۰..۳۱ و AU/AS3 اندیس ستون امروز.
**Cnt-t**: K11:AO11 تاریخ واقعی (نمایش فقط روز با فرمت `dd`)، K10=D1 (نمایش ماه‌سال)، D12:D400 مقدار Today (SUMIF به شیت پیمانکار)، H=Total Done، J=جمع ماه (K:AO)، AQ=مقدار ثابت دوره قبل؛ دوره گزارش ماهانه از 26 تا 25 (فرمول `DATE(...,26)`).
**Manpower**: سطر ۶ پیمانکارها (ODCC, ODCC-Tehran, IGC, EXR-Mobilization, Ehdas, SKF&FRJ, Soleh Iran, Total)، هر کدام Local/Non-Local × Direct/Indirect × D/N/R؛ ردیف ۱۱۲ و ۲۲۰ جمع‌های Cov-P. **Machinary**: کد MCVxxx، دسته (Heavy…)، D/N/I/R برای هر پیمانکار.
**Summary/Piling/Concrete Pouring**: ردیف‌های واحد با کلید (Phase=A، Unit=B، Category=C مثل PU/GTG) و SUMIFS؛ ستون‌های Estimate/DWG Received/Done/Remain/Front؛ Remain=Estimate−Done، Front=DWG−Done.

## ۵) مشکلات موجود در فایل [فایل]
- (توضیح کاربر) ستون‌های Plan%/Actual% در شیت پیمانکاران زیر سرتیتر ردیف ۶ قرار دارند و همان سلول بالایی مشخص می‌کند Up to Last / Today / Up to Now است. در SOL1: AC/AD=Up to Last، AE/AF=Today، AG/AH=Up to Now.
- مقدار واقعی فایل: در SOL1 ستون AG (Plan Up to Now) ۵۲۵ فرمول دارد که همه ۰ برمی‌گردانند؛ در Ehds ستون AF همین‌طور (۴۴۵ مورد ۰)؛ AC در SOL1 خالی/۰ است. پس اگر کاربر مقدار Plan می‌بیند، احتمالاً در ستون‌های دیگر یا PMS-Overall است؛ قبل از ادعا، ستون و سلول دقیق را بپرس.
- **#REF! در Plan پیمانکاران**: `IFERROR(HLOOKUP($AH$5,#REF!,$I8,FALSE),0)` در ستون AF/AG: SOL1(۵۲۵)، SOL2(۳۰۶)، Ehds(۴۴۵)، FRJP(۱۱۵)، PR-5301 FRJ(۳۷). IFERROR آن‌ها را ۰ می‌کند و خطا دیده نمی‌شود؛ در این شیت‌ها Plan% پیمانکار ثابت/ناقص است (AC/AD مقدار دستی).
- `PMS-Overall!BDC4 = #REF!+1` در انتهای محدوده HLOOKUP؛ ستون‌های BIV.. با ۱۷۴ خطای #N/A (ردیف ۹۸۳ به بعد). Cov-P S37/V37 و Unit Wise S36/U36 = #REF!.
- Cnt-t ستون A: ۳۱۲ خطای #VALUE! (ستون کمکی انتخاب مقدار روز/AQ).
- Concrete Pouring ستون R: ۱۱۸ #N/A (VLOOKUP به SOL1)؛ Piling ستون‌های Q:X: چند #N/A.
- Prj-V AO12:AO13 فرمول `AN11+AM11` (ممکن است باید AN12+AM12 باشد) [تأیید شود]؛ Prj-V AA11:AA20 آستانه‌های Error ثابت (200، 17000، 250، 800).
- Prj-V ستون AA و Cnt-V U41 مقادیر محاسباتی دستی دارند؛ Concrete Pouring Cov/Summary بعضی L16 مثل `2100-L43` ثابت.
- **سنگینی فایل**: PMS-Overall به‌تنهایی ~۳۸۰MB XML؛ ۳۴۴۶ Defined Name که ۳۱۵۱ مخفی و ۸۹۶ دارای #REF! (بازمانده از فایل کره‌ای «작업일보.xls»)؛ styles.xml ۱۱MB؛ ۷ نمودار، یک PivotTable (Subco W.F)، ۴ کامنت. به همین دلیل Open/Save کند است. هرگز نام‌ها/استایل‌ها را بدون اجازه پاک نکن.

## ۶) چرخه روزانه [استنباط + سابقه کاربر]
۱) کپی فایل روز قبل (ماکروی کپی). ۲) ورود کمیت‌های امروز در ستون تاریخ شیت پیمانکاران، Manpower, Machinary, Piling/Concrete, هوا. ۳) AA7 در Cov-P را روز بعد کن. ۴) کنترل‌ها: Prj-V ستون AA (Ok/Error)، PMS-Overall AL2/AM2، Cnt-V تطبیق جمع. ۵) «صفر کننده PMS»: Up to Now → Up to Last، پاک کردن Today، +۱ تاریخ‌ها (جزئیات در macros.md). ۶) Final Touch: تثبیت Today در Cnt-t به‌صورت مقدار. ۷) خروجی/ارسال گزارش.

## ۷) سؤال‌های باز [تأیید شود]
- منبع Plan% پیمانکاران چون HLOOKUP شکسته است و مقدار کش‌شده ۰ است: کاربر می‌گوید Plan خالی نیست؛ ستون/سلول دقیق را از او بگیر.
- معنی ستون‌های ثابت AC/AD، AK/AL در شیت‌های پیمانکار (Subcontractor Pro%).


## ۸) یادداشت‌های سلولی (Comments) در نسخه 29 اکتبر [فایل]
- PMS-Overall V4425 (Tag E-6803) و V4457 (Tag H-6801)، یونیت 68 Civil Works: «مطابق اطلاعات مهندسی فونداسیون ندارد؛ هماهنگی با تهران» (Gholam Reza Darvish Nezhad) — وزن W.V% آن‌ها باید با تهران هماهنگ شود.
- IGC-PR45 W81, W172, W176, W180: «احجام بر اساس پلن ارائه‌شده بروزرسانی شده است» (Hamid Norouzi).
- Ehds Z202: «استیمیت پیمانکار لحاظ شده» (Gholam Reza Darvish Nezhad).
- Manpower CM40, CM99, CM167: توضیح نقش‌ها (برشکار و کمکی؛ راننده جرثقیل ۵۰ تن؛ آتش‌نشان) (Ali Dehvarian).
- فایل `.xlsm` نسخه 29 اکتبر شامل `vbaProject` نیست (ماکرو داخلش نیست)؛ ترتیب شیت‌ها: Steel Structure قبل از Summary شده و Dashboard مخفی است.
