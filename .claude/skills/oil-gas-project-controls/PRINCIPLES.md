# اصول برنامه‌ریزی و کنترل پروژه نفت و گاز (مرجع)

این فایل مرجع مفهومی است. **قرارداد، رویه کنترل پروژه و PMS مصوب پروژه همیشه مقدم‌اند.** هر عدد «نمونه» فقط مثال است.

## ۱) چرخه عمر و قرارداد
- **فازهای سرمایه‌گذاری:** FEL-1 (Appraise)، FEL-2 (Select)، FEL-3 / FEED (Define)، Execute (EPC)، Operate. بیشترین اثر تصمیم روی هزینه در فازهای اولیه است.
- **قراردادهای رایج:** EPC / EPCC (شامل Commissioning)، LSTK (مبلغ مقطوع کلید در دست)، Unit Rate (فهرست‌بها)، Reimbursable / Cost-plus. نوع قرارداد تعیین می‌کند پیشرفت برای پرداخت چطور سنجیده شود.
- **ساختار پیمان:** کارفرما ← مشاور / PMC ← پیمانکار اصلی (اینجا Consortium، یعنی CSM) ← پیمانکاران جزء (Soleh، Sekaf & Farjoud، Ehdas).

## ۲) سطوح برنامه زمان‌بندی (Schedule Levels)
| سطح | نام | مخاطب |
|---|---|---|
| L1 | Executive / Milestone Summary | مدیریت ارشد، کارفرما |
| L2 | Management Summary (فاز و یونیت) | مدیر پروژه |
| L3 | Project Control Schedule (CPM، مبنای کنترل) | تیم کنترل پروژه |
| L4 | Execution / Detailed (به تفکیک دیسیپلین و منطقه) | سرپرستان کارگاه |
| L5 | Work Level / Lookahead (روزانه، هفتگی) | اجرا |
سطح‌های WBS در PMS ما (L1..L8) **با سطوح برنامه یکی نیستند**. دو مفهوم را قاطی نکن.

## ۳) WBS و کدینگ
- WBS **محصول‌محور** است (چه چیزی ساخته می‌شود)، نه سازمان‌محور. قاعده ۱۰۰٪: جمع فرزندان دقیقاً برابر والد است، بی‌کم و بیش.
- کدینگ استاندارد معمولاً Project / Phase / Unit / Discipline / Work Package / Activity است. Tag در سطح تجهیز، مثل V-6620 یا E-6604 A/B.
- WBS، OBS (چه کسی) و CBS (هزینه) را با کد مشترک به هم وصل کن.

## ۴) اندازه‌گیری پیشرفت (Progress Measurement)
**روش‌ها:**
1. **Units Completed:** مقدار انجام‌شده ÷ مقدار کل، مثلاً m³ بتن یا Inch-Dia لوله. برای کارهای تکراری و قابل‌شمارش.
2. **Weighted / Incremental Milestones (Rules of Credit):** فعالیت به گام‌های وزن‌دار تقسیم می‌شود.
3. **Start/Finish:** مثلاً 0/100 یا 50/50، برای فعالیت‌های کوتاه.
4. **Supervisor Opinion:** فقط برای کارهای کم‌وزن و غیرقابل‌اندازه‌گیری. کمترین اعتبار را دارد.
5. **Level of Effort:** برای مدیریت و پشتیبانی؛ پیشرفت را با زمان جلو می‌برد، نه با کار.

**نمونه Rules of Credit (فرضی؛ وزن‌های واقعی از PMS مصوب):**
- مدارک مهندسی: IFR 50% / IFA 20% / IFC 30%
- خرید: PR 5% / RFQ 5% / TBE 10% / PO 15% / ساخت 40% / FAT 10% / حمل و تحویل 15%
- فونداسیون: خاک‌برداری 10% / بتن مگر 5% / آرماتور 30% / قالب 20% / بتن‌ریزی 30% / بک‌فیل 5%
- لوله‌کشی: Erection 40% / Welding 40% / NDT 10% / Test 10%

**مبنای وزن‌دهی:** نفر-ساعت (Man-hours، رایج‌ترین در ساخت)، هزینه یا قیمت قرارداد. فایل Ehdas وزن را بر اساس Price دارد. **وزن‌ها در هر سطح جمعشان ۱۰۰٪ است.**

**قواعد طلایی:**
- پیشرفت باید پشتوانه داشته باشد: صورت‌جلسه، Inspection Release، Mechanical Completion Certificate، Delivery Note.
- **Cut-off** ثابت است: هر کاری بعد از آن برای دوره بعد حساب می‌شود.
- دوباره‌شماری نکن؛ هر کار فقط در یک ردیف برگ ثبت شود.
- درصد فیزیکی، درصد زمانی (مدت گذشته) و درصد هزینه‌ای سه چیز متفاوت‌اند.

## ۵) Baseline و کنترل تغییرات
- **Baseline** برنامه مصوب و منجمد مبنای مقایسه است. فقط با Change Order یا تصمیم رسمی Re-baseline می‌شود.
- **Data Date / Status Date:** تاریخی که پیشرفت تا آن ثبت شده. هر Actual قبل از آن است و هر Forecast بعد از آن.
- **Update:** Actual Start/Finish، Remaining Duration و % Complete را وارد کن، سپس Schedule (F9) را اجرا کن، و پیش‌بینی پایان را با Baseline مقایسه کن.
- **Change Management:** Variation / Change Order ← برآورد اثر زمان و هزینه ← تأیید ← به‌روزرسانی Baseline یا Current Budget. Trend Log برای تغییرات بالقوه.

## ۶) زمان‌بندی (CPM)
- **Total Float:** چقدر فعالیت می‌تواند تأخیر کند بدون تأخیر در پایان پروژه. **Free Float:** بدون تأخیر در فعالیت بعدی.
- **مسیر بحرانی:** طولانی‌ترین مسیر منطقی (TF ≤ 0 یا Longest Path در P6).
- **Near-Critical:** معمولاً TF ≤ 10 تا 20 روز کاری؛ آستانه را پروژه تعیین می‌کند.
- **کیفیت برنامه:** DCMA 14-Point (جدول در اسکیل `p6-xer-offline`)، GAO Schedule Assessment Guide (۱۰ Best Practice).
- **Constraint سخت** منطق را می‌شکند و فلوت را جعلی می‌کند. باید کم و توجیه‌شده باشد.

## ۷) Earned Value Management
| شاخص | فرمول | معنا |
|---|---|---|
| PV (BCWS) | بودجه کار برنامه‌ریزی‌شده تا تاریخ | |
| EV (BCWP) | بودجه کار انجام‌شده = BAC × %Complete | |
| AC (ACWP) | هزینه واقعی | |
| SV / SPI | EV − PV / EV ÷ PV | SPI < 1 یعنی عقب‌تر از برنامه |
| CV / CPI | EV − AC / EV ÷ AC | CPI < 1 یعنی بیش از بودجه |
| EAC | BAC ÷ CPI (اگر روند فعلی ادامه یابد)، یا AC + (BAC − EV) (اگر باقی‌مانده طبق بودجه باشد) | |
| ETC / VAC | EAC − AC / BAC − EAC | |
| TCPI | (BAC − EV) ÷ (BAC − AC) | کارایی لازم برای ماندن در بودجه |
- **Earned Schedule:** ES = زمانی که در آن PV برابر EV فعلی بوده، و SPI(t) = ES ÷ AT. برخلاف SPI که در پایان پروژه همیشه به ۱ می‌رسد، SPI(t) معنی‌دار می‌ماند.
- در گزارش فیزیکی PMS، «SPI» معمولاً Actual% ÷ Plan% وزنی است، نه EVM هزینه‌ای. در گزارش بنویس کدام است.
- **Productivity Factor:** نفر-ساعت ارزش‌کسب‌شده ÷ نفر-ساعت واقعی. اگر کمتر از ۱ باشد، بهره‌وری پایین است.

## ۸) گزارش‌دهی
- **روزانه:** کار انجام‌شده (کمیت Today)، نیروی انسانی و ماشین‌آلات، HSE، موانع.
- **هفتگی:** Plan / Actual / Variance به تفکیک Unit و Discipline، منحنی S، Lookahead سه‌هفته‌ای، فعالیت‌های بحرانی، اقدامات.
- **ماهانه:** همه موارد هفتگی، به‌اضافه EVM، Cash Flow، وضعیت تغییرات، ریسک‌ها، پیش‌بینی پایان، و Recovery / Catch-up Plan اگر عقب هستیم.
- **اصل:** هر عدد منبع دارد (شیت/سلول/فیلد). علت تأخیر را با شواهد بنویس، نه حدس.

## ۹) تحلیل تأخیر و ادعا
- انواع تأخیر: Excusable / Non-excusable، Compensable / Non-compensable، و Concurrent Delay.
- روش‌ها (AACE RP 29R-03 و SCL Delay and Disruption Protocol): As-Planned vs As-Built، Impacted As-Planned، **Time Impact Analysis (TIA)**، Windows / Time-Slice Analysis، Collapsed As-Built.
- پایه هر ادعا: برنامه به‌روز، گزارش‌های روزانه منظم، نامه‌ها و صورت‌جلسه‌ها. **گزارش روزانه دقیق، مهم‌ترین مدرک ادعاست.**

## ۱۰) ریسک
- Risk Register: شرح، علت، اثر، احتمال × شدت، مالک، اقدام پاسخ.
- QSRA (تحلیل ریسک زمان‌بندی Monte Carlo): P50 و P80 تاریخ پایان. Contingency زمانی و هزینه‌ای.

## ۱۱) برآورد هزینه (AACE RP 18R-97)
کلاس‌های برآورد بر اساس بلوغ تعریف پروژه: Class 5 (0–2%)، Class 4 (1–15%)، Class 3 (10–40%، معمولاً برای بودجه)، Class 2 (30–75%)، Class 1 (65–100%). هرچه تعریف کامل‌تر، دامنه دقت تنگ‌تر.

## ۱۲) تکمیل و راه‌اندازی
Construction ← **Mechanical Completion (MC)** ← Pre-commissioning (Flushing، Pressure Test، Loop Check) ← **Ready for Start-up (RFSU)** ← Commissioning ← Start-up ← Performance Test ← Handover. Punch List: Cat A (قبل از MC یا RFSU رفع شود) و Cat B (بعداً). پیشرفت در این فاز بر اساس **System / Sub-system** است، نه یونیت جغرافیایی.

## ۱۳) دیسیپلین‌ها و واحد اندازه‌گیری (نمونه)
| دیسیپلین | واحد |
|---|---|
| Civil: خاک‌برداری، بتن، آرماتور، قالب | m³، ton، m² |
| Piling (شمع بتنی، Pile Chipping) | عدد، متر |
| Steel Structure | ton |
| Equipment Installation | عدد، ton |
| Piping: Spool Erection، Welding | Inch-Dia، متر |
| Electrical: کابل‌کشی، Termination | متر، عدد |
| Instrumentation: نصب، Loop | عدد |
| Painting / Insulation | m² |

## منابع رسمی
PMI (PMBOK، Practice Standard for Scheduling، EVM)، AACE International (TCM Framework؛ RP 10S-90 واژه‌نامه، 18R-97، 29R-03)، GAO Schedule Assessment Guide (GAO-16-89G)، GAO Cost Guide (GAO-20-195G)، DCMA 14-Point Assessment، مستندات Oracle Primavera P6.
