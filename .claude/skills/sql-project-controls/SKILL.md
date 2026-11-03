---
name: sql-project-controls
description: "نوشتن SQL برای داده‌های کنترل پروژه: پیشرفت وزنی و رول‌آپ WBS، منحنی S تجمعی با Window Functions، Plan در برابر Actual، SPI، فیلتر Today، تطبیق دو منبع (فایل پیمانکار و PMS) با Tag + نام فعالیت، و کوئری روی جدول‌های P6 (TASK، PROJWBS، TASKPRED) در SQLite، SQL Server یا Oracle. هر وقت کاربر SQL، کوئری، دیتابیس، SQLite، SQL Server یا جدول P6 را برای کار برنامه‌ریزی و کنترل پروژه خواست از این اسکیل استفاده کن. SQL for project controls, progress rollup, S-curve, P6 PMDB queries."
---

# SQL برای کنترل پروژه

برای اصول عمومی SQL از `data:sql-queries` و `data:write-query` هم استفاده کن. این اسکیل الگوهای مخصوص کنترل پروژه را دارد.

## قواعد
1. اول بپرس دیتابیس چیست (SQLite، SQL Server، Oracle، Power Query). نحو `TOP`، `LIMIT` و تابع‌های تاریخ فرق دارد.
2. روی دیتابیس واقعی P6 یا شرکت فقط `SELECT`. `UPDATE`، `DELETE` و `INSERT` فقط روی کپی و با تأیید صریح.
3. وزن‌ها را جمع بزن و چک کن جمع وزن هر والد 100% (یا 1) باشد؛ اگر نبود، قبل از گزارش بگو.
4. درصدها را با واحد یکسان نگه دار (کسری 0..1 یا درصد 0..100) و در خروجی بنویس کدام است.

## مدل داده پیشنهادی (از PMS ما)
```
progress_daily(report_date, activity_id, contractor, plan_cum, actual_cum, today_qty)
activity(activity_id, wbs_code, parent_id, level, unit, discipline, tag, activity_name, weight_in_parent, weight_overall)
```
`weight_in_parent` = W.V% در PMS-Overall ستون V؛ `weight_overall` = W.F% ستون W [تعریف W.V و W.F سؤال باز ۱۱ است؛ تأیید شود].

## الگوها

**پیشرفت وزنی یک سطح (رول‌آپ والد از فرزندان)**
```sql
SELECT a.parent_id,
       SUM(a.weight_in_parent * p.actual_cum) / NULLIF(SUM(a.weight_in_parent), 0) AS actual_pct,
       SUM(a.weight_in_parent * p.plan_cum)   / NULLIF(SUM(a.weight_in_parent), 0) AS plan_pct,
       SUM(a.weight_in_parent)                                                     AS weight_check
FROM activity a
JOIN progress_daily p ON p.activity_id = a.activity_id AND p.report_date = :d
GROUP BY a.parent_id;
```

**پیشرفت کل پروژه با وزن کل**
```sql
SELECT p.report_date,
       SUM(a.weight_overall * p.plan_cum)   AS plan_overall,
       SUM(a.weight_overall * p.actual_cum) AS actual_overall,
       SUM(a.weight_overall * p.actual_cum) - SUM(a.weight_overall * p.plan_cum) AS variance
FROM activity a JOIN progress_daily p USING (activity_id)
WHERE a.level = (SELECT MAX(level) FROM activity)   -- فقط برگ‌ها، تا دوباره‌شماری نشود
GROUP BY p.report_date ORDER BY p.report_date;
```

**منحنی S از مقادیر روزانه (تجمعی با Window)**
```sql
SELECT report_date,
       SUM(daily_plan)   OVER (ORDER BY report_date ROWS UNBOUNDED PRECEDING) AS plan_cum,
       SUM(daily_actual) OVER (ORDER BY report_date ROWS UNBOUNDED PRECEDING) AS actual_cum
FROM daily_totals;
```

**SPI فیزیکی** (این EVM واقعی نیست؛ نسبت درصد وزنی است):
`actual_overall / NULLIF(plan_overall, 0)`

**Today غیرصفر (همان فیلتری که کاربر دستی در اکسل می‌زند)**
```sql
SELECT contractor, tag, activity_name, today_qty FROM contractor_today
WHERE today_qty <> 0 ORDER BY contractor, tag;
```

**تطبیق فایل پیمانکار با فایل ما: کلید = Tag + نام فعالیت** (شماره ردیف و Activity ID یکی نیست)
```sql
SELECT COALESCE(c.tag, o.tag) AS tag, COALESCE(c.activity_name, o.activity_name) AS activity,
       c.today_qty AS contractor_today, o.today_qty AS our_today,
       CASE WHEN c.tag IS NULL THEN 'only ours' WHEN o.tag IS NULL THEN 'only contractor'
            WHEN ABS(c.today_qty - o.today_qty) > 0.001 THEN 'different' ELSE 'same' END AS status
FROM contractor_today c
FULL OUTER JOIN our_today o
  ON TRIM(UPPER(c.tag)) = TRIM(UPPER(o.tag)) AND TRIM(UPPER(c.activity_name)) = TRIM(UPPER(o.activity_name));
```
SQLite قدیمی‌تر از 3.39 `FULL OUTER JOIN` ندارد: دو `LEFT JOIN` را با `UNION` ترکیب کن.

## کوئری‌های P6 (روی خروجی `xer_tool.py sqlite` یا PMDB)
```sql
-- فعالیت‌های بحرانی ناتمام (فلوت به روز، با تقویم خود فعالیت)
SELECT t.task_code, t.task_name,
       CAST(t.total_float_hr_cnt AS REAL) / c.day_hr_cnt AS tf_days,
       t.early_start_date, t.early_end_date
FROM TASK t JOIN CALENDAR c ON c.clndr_id = t.clndr_id
WHERE t.status_code <> 'TK_Complete' AND t.task_type NOT IN ('TT_WBS','TT_LOE')
  AND CAST(t.total_float_hr_cnt AS REAL) <= 0
ORDER BY t.early_start_date;

-- فعالیت بدون Predecessor یا Successor
SELECT t.task_code, t.task_name FROM TASK t
WHERE t.status_code <> 'TK_Complete'
  AND (NOT EXISTS (SELECT 1 FROM TASKPRED r WHERE r.task_id = t.task_id)
    OR NOT EXISTS (SELECT 1 FROM TASKPRED r WHERE r.pred_task_id = t.task_id));

-- مسیر WBS هر فعالیت (CTE بازگشتی)
WITH RECURSIVE w(wbs_id, path) AS (
  SELECT wbs_id, wbs_short_name FROM PROJWBS WHERE proj_node_flag = 'Y'
  UNION ALL
  SELECT c.wbs_id, w.path || '.' || c.wbs_short_name FROM PROJWBS c JOIN w ON c.parent_wbs_id = w.wbs_id)
SELECT t.task_code, w.path, t.task_name FROM TASK t JOIN w ON w.wbs_id = t.wbs_id;

-- فعالیت‌هایی که باید شروع می‌شدند ولی نشده‌اند (نسبت به Data Date)
SELECT t.task_code, t.task_name, t.target_start_date
FROM TASK t, PROJECT p
WHERE t.proj_id = p.proj_id AND t.status_code = 'TK_NotStart'
  AND t.target_start_date < p.last_recalc_date;
```
در SQLite همه فیلدهای XER متن‌اند: برای عدد `CAST(... AS REAL)` لازم است. تاریخ‌ها `YYYY-MM-DD HH:MM` هستند و مقایسه متنی‌شان درست کار می‌کند.
