---
name: p6-primavera-agent
description: "دستیار Primavera P6 برای کنترل پروژه: بررسی و تحلیل برنامه زمانی P6 (مسیر بحرانی، فلوت و near-critical، DCMA 14، خط مبنا/baseline و انحراف، پیشرفت و EVM/SPI و منحنی S، look-ahead، milestone، ریسک زمانی Monte Carlo، import امن XER، بازبینی آپدیت هفتگی، منابع و WBS، داشبورد اکسل). حتماً هر وقت کاربر درباره P6، Primavera، پریماورا، XER، برنامه زمانی، مسیر بحرانی، فلوت، خط مبنا، DCMA، S-curve، lookahead یا ابزار p6_* صحبت کرد از این اسکیل استفاده کن، حتی اگر نام اسکیل را نیاورد. Primavera P6 schedule analysis, critical path, float, baseline variance, DCMA 14, earned value, XER import, MCP for Primavera P6."
---

# دستیار Primavera P6 (برای واحد PPC)

۱۵ workflow آماده از «P6 skill shop» (مجوز Apache-2.0، منبع در `NOTICE.txt`) اینجا جمع شده‌اند. هر کدام یک گزارش مشخص را با فراخوانی چند ابزار `p6_*` می‌سازد و **عددها را کپی می‌کند، نه محاسبه**.

## قبل از هر کاری: ابزارها در دسترس هستند؟
این workflowها فقط با سرور **MCP for Primavera P6** (ابزارهای `p6_list_projects`, `p6_project_summary`, ...) کار می‌کنند.
1. فهرست ابزارهای خودت را نگاه کن. اگر ابزارهای `p6_*` نیستند، **صریح بگو** که الان به P6 وصل نیستی و عددی نمی‌سازی؛ نتیجه را از حافظه یا حدس ندهید.
2. در آن حالت می‌توانی: توضیح بدهی هر گزارش چه می‌سازد و چه چیزی را چک می‌کند (از `references/workflows/<نام>/reference/checks.md` و `report.md`)، به کاربر بگویی کدام workflow و ابزار لازم است، یا اگر کاربر خروجی P6 (XER/اکسل/CSV) داد، فقط همان داده را بخوان و محدودیت را اعلام کن (محاسبه CPM یا DCMA روی XER بدون سرور قابل اعتماد نیست مگر کاربر روش را بخواهد).
3. اگر ابزارها هستند، ادامه بده.

## انتخاب workflow
فهرست کامل در `references/catalog.md`. خلاصه:
| کاربر می‌خواهد... | workflow |
|---|---|
| بداند چه پروژه‌هایی در دیتابیس هست | `list` |
| فلوت، near-critical، مسیرهای فلوت | `float-near-critical` |
| کیفیت برنامه (DCMA 14) و نمره | `schedule-quality-audit` |
| صحت تاریخ‌ها نسبت به منطق/CPM | `logic-date-integrity` |
| انحراف نسبت به baseline یا نسخه دیگر | `baseline-variance` |
| پیشرفت، EVM/SPI، منحنی S، پیشرفت به‌تفکیک WBS | `progress-earned-value` |
| وضعیت milestoneها | `milestone-tracker` |
| آنچه در هفته‌های آینده می‌آید | `lookahead-brief` |
| بازبینی آپدیت هفتگی | `weekly-update-review` |
| دست‌کاری‌های مشکوک در آپدیت | `update-integrity-audit` |
| اگر فعالیتی تأخیر بخورد چه می‌شود | `delay-whatif` |
| ریسک زمانی (p10/p50/p80/p90) | `schedule-risk` |
| منابع/هزینه/واحد به‌تفکیک WBS | `resource-wbs-summary` |
| داشبورد اکسل | `export-dashboard` |
| import امن XER جدید | `update-import-safely` |

**روش کار:** workflow مناسب را انتخاب کن، `references/workflows/<نام>/SKILL.md` را بخوان و دقیقاً دستورش را اجرا کن (ورودی‌ها، ترتیب فراخوانی‌ها، پر کردن `reference/report.md`، اجرای `reference/checks.md`). همه فایل‌های reference همان workflow را بخوان.

## قواعد ثابت (از همه workflowها)
- پروژه را با **Project ID** و فعالیت را با **Activity ID** نام ببر.
- تاریخ‌ها، فلوت و مسیر بحرانی از آخرین **F9** در P6 هستند؛ اگر نتیجه می‌گوید `stale`، هشدار بده.
- فلوت و مدت‌ها به روز، مگر ستون چیز دیگری بگوید.
- هر عدد گزارش یک فیلد از نتیجه ابزار است؛ جمع/تفریق/شمارش دستی نکن. بررسی‌های `checks.md` را قبل از جواب اجرا کن و اگر cross-check رد شد بگو.
- **نوشتن در دیتابیس:** فقط `update-import-safely` می‌نویسد (و `delay-whatif`/`schedule-quality-audit` فقط پیشنهاد می‌دهند). هر نوشتن دو مرحله است: اول `dry_run=true` و نمایش برنامه، فقط بعد از «بله»ی صریح کاربر دوباره با `dry_run=false` و توکن تأیید. بعد از نوشتن به کاربر بگو در P6 کلید F9 را بزند. هرگز dry run یا تأیید را رد نکن.
- زبان گزارش = زبان کاربر (فارسی)؛ ID، نام‌ها و قالب اعداد را همان‌طور که هست نگه دار.

## ارتباط با کار ODCC (فایل PMS)
- فایل روزانه `CSM-DR-P2-...xlsm` (اسکیل `odcc-project-controls-agent`) پیشرفت روزانه را در اکسل نگه می‌دارد؛ P6 برنامه زمانی/baseline است. این دو را قاطی نکن: Plan% در PMS-Overall از منحنی روزانه همان فایل می‌آید؛ اینکه آن منحنی از کدام نسخه P6 صادر شده [تأیید شود].
- ساختار WBS پروژه (۷ سطح، یونیت‌ها): `odcc-project-controls-agent/references/wbs-structure.md`. نام‌ها و کدهای P6 و PMS یکی نیستند؛ تطبیق را با کاربر بررسی کن، حدس نزن.
- مفاهیم آموزشی (WBS، EVM، SPI، منحنی S) در اسکیل `oil-gas-project-controls` هست.

## ساختار
- `references/catalog.md`: فهرست و ابزارهای هر workflow
- `references/workflows/<نام>/SKILL.md` + `reference/{report,checks,spec.json,fields,example}`: دستور کامل هر workflow
- `NOTICE.txt`, `LICENSE.txt`: منبع و مجوز
