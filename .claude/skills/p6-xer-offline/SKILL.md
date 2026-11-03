---
name: p6-xer-offline
description: "کار با Primavera P6 بدون سرور MCP: خواندن فایل XER صادرشده از P6، خلاصه پروژه، Data Date، فعالیت‌های بحرانی (TF<=0)، کنترل کیفیت برنامه به سبک DCMA 14، تبدیل XER به SQLite/CSV برای SQL و Power BI، و کوئری‌های فقط‌خواندنی روی دیتابیس P6 (PMDB). هر وقت کاربر فایل .xer فرستاد یا درباره P6/پریماورا/XER/مسیر بحرانی/فلوت/DCMA پرسید و ابزار p6_* (MCP) در دسترس نبود از این اسکیل استفاده کن. Offline Primavera P6 XER analysis, DCMA 14 checks, P6 database SQL."
---

# Primavera P6 بدون MCP (فایل XER)

اسکیل `p6-primavera-agent` فقط با سرور «MCP for Primavera P6» عدد می‌سازد. این اسکیل همان نیاز را با **فایل XER** برطرف می‌کند: کاربر در P6 از `File > Export > Primavera PM (XER)` خروجی می‌گیرد و فایل را آپلود می‌کند.

## قواعد
1. فقط‌خواندنی: XER را تغییر نده و فایل XER برای import نساز مگر کاربر صریحاً بخواهد (import اشتباه برنامه را خراب می‌کند).
2. عدد را از فیلد XER بخوان و نام جدول/فیلد را بگو. حدس نزن.
3. مدت و فلوت در XER به **ساعت** است؛ روز = ساعت ÷ `CALENDAR.day_hr_cnt` همان فعالیت (ابزار خودش این کار را می‌کند).
4. Data Date = `PROJECT.last_recalc_date`.
5. برای Baseline: XER پروژه فعلی Baseline را همراه ندارد مگر هنگام export تیک خورده باشد. برای مقایسه با Baseline، XER نسخه Baseline را جداگانه بخواه.

## ابزار
`scripts/xer_tool.py` (فقط کتابخانه استاندارد Python):
```
python .claude/skills/p6-xer-offline/scripts/xer_tool.py tables   FILE.xer
python .claude/skills/p6-xer-offline/scripts/xer_tool.py summary  FILE.xer
python .claude/skills/p6-xer-offline/scripts/xer_tool.py dcma     FILE.xer [--proj CODE]
python .claude/skills/p6-xer-offline/scripts/xer_tool.py critical FILE.xer [--limit 50]
python .claude/skills/p6-xer-offline/scripts/xer_tool.py dump     FILE.xer TASK --out task.csv
python .claude/skills/p6-xer-offline/scripts/xer_tool.py sqlite   FILE.xer p6.db
```
بعد از `sqlite`، با اسکیل `sql-project-controls` روی `p6.db` کوئری بزن.

## ساختار XER
فایل متنی Tab-delimited: `ERMHDR` (نسخه/تاریخ)، سپس برای هر جدول `%T نام`، `%F فیلدها`، چند `%R ردیف`، و در پایان `%E`.

| جدول | چیست | فیلدهای مهم |
|---|---|---|
| PROJECT | پروژه | proj_id, proj_short_name, last_recalc_date (Data Date), plan_start_date, scd_end_date |
| PROJWBS | گره‌های WBS | wbs_id, parent_wbs_id, wbs_short_name, wbs_name, proj_node_flag, seq_num |
| TASK | فعالیت‌ها | task_id, task_code (Activity ID), task_name, wbs_id, clndr_id, task_type, status_code, phys_complete_pct, complete_pct_type, target_drtn_hr_cnt, remain_drtn_hr_cnt, total_float_hr_cnt, free_float_hr_cnt, act_start_date, act_end_date, early_start_date, early_end_date, late_start_date, late_end_date, target_start_date, target_end_date, cstr_type, cstr_date, cstr_type2, driving_path_flag |
| TASKPRED | روابط | task_id (successor), pred_task_id, pred_type, lag_hr_cnt |
| CALENDAR | تقویم | clndr_id, clndr_name, day_hr_cnt |
| TASKRSRC / RSRC | منابع و تخصیص | rsrc_id, target_qty, act_reg_qty, remain_qty, target_cost |
| ACTVTYPE / ACTVCODE / TASKACTV | Activity Codeها | برای Unit، Discipline، Contractor |
| UDFTYPE / UDFVALUE | فیلدهای کاربری | |

کدهای رایج:
- `task_type`: TT_Task، TT_Mile (Start Milestone)، TT_FinMile، TT_LOE، TT_WBS (WBS Summary)، TT_Rsrc (Resource Dependent)
- `status_code`: TK_NotStart، TK_Active، TK_Complete
- `pred_type`: PR_FS، PR_SS، PR_FF، PR_SF
- `complete_pct_type`: CP_Phys (Physical)، CP_Drtn (Duration)، CP_Units
- `cstr_type`: CS_MSO / CS_MEO (Must Start/Finish On)، CS_MSOA / CS_MSOB (Start On or After/Before)، CS_MEOA / CS_MEOB، CS_ALAP، CS_MANDSTART / CS_MANDFIN

## DCMA 14-Point (آستانه‌های رایج)
| # | کنترل | آستانه |
|---|---|---|
| 1 | Logic: فعالیت ناتمام بدون Predecessor یا Successor | ≤ 5% |
| 2 | Leads (Lag منفی) | 0 |
| 3 | Lags | ≤ 5% روابط |
| 4 | Relationship Types: FS | ≥ 90% |
| 5 | Hard Constraints (MSO, MFO, SNLT, FNLT, Mandatory) | ≤ 5% |
| 6 | High Float: TF > 44 روز کاری | ≤ 5% |
| 7 | Negative Float | 0 |
| 8 | High Duration: مدت باقی‌مانده > 44 روز کاری | ≤ 5% |
| 9 | Invalid Dates: Actual بعد از Data Date یا Forecast قبل از آن | 0 |
| 10 | Resources: فعالیت‌های با مدت ≥ ۱ روز منبع/هزینه دارند | (قرارداد تعیین می‌کند) |
| 11 | Missed Tasks: نسبت به Baseline Finish | ≤ 5% |
| 12 | Critical Path Test | قبول/رد (در P6 انجام شود) |
| 13 | CPLI = (طول مسیر بحرانی + Total Float) ÷ طول مسیر بحرانی | ≥ 0.95 |
| 14 | BEI = فعالیت‌های تمام‌شده ÷ فعالیت‌هایی که طبق Baseline باید تمام می‌شدند | ≥ 0.95 |

ابزار کنترل‌های 1 تا 9، 11 و 14 را حساب می‌کند. 11 و 14 با `target_end_date` حساب می‌شوند که در P6 همان Planned Finish است، نه Baseline؛ در گزارش این را بنویس. DCMA معیار وزارت دفاع آمریکاست و الزام قراردادی نیست؛ رویه کنترل پروژه کارفرما مقدم است.

## کوئری روی دیتابیس P6 (PMDB)، فقط‌خواندنی
اگر کاربر دسترسی خواندن به دیتابیس P6 داشت (از DBA بگیرد؛ هرگز با کاربر ادمین ننویس): نام جدول‌ها همان جدول‌های XER است (TASK، PROJWBS، TASKPRED، PROJECT، ...). در Oracle معمولاً در schema `ADMUSER`، در SQL Server در `dbo`. در نسخه‌های جدید ردیف‌های حذف‌شده با `delete_session_id IS NOT NULL` می‌مانند؛ آن‌ها را فیلتر کن. نمونه کوئری‌ها در `sql-project-controls`.

## ارتباط با PMS ما
PMS-Overall ستون K = Activity ID است. برای تطبیق با P6: `TASK.task_code` ↔ `PMS-Overall!K`. اول روی چند ردیف تست کن؛ شاید فرمت کد یکی نباشد. منحنی Plan روزانه PMS (AZ:BDE) احتمالاً از P6 آمده [سؤال باز ۱۰]؛ قبل از ادعا بپرس.
