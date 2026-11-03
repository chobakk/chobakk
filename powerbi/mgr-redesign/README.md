# بازطراحی داشبورد مدیریتی MGR (Power BI)

منبع: `MGR-25-SEP-2026.xlsx` و `MGR-25-SEP-2026.pbix` (Cut-off: 2026-09-25). **فایل‌های اصلی تغییر نکرده‌اند**؛ همه خروجی‌ها فایل جدیدند.

| فایل | چیست |
|---|---|
| `scripts/build_model.py` | ساخت `MGR-Model.xlsx` از فایل ماهانه: `python -I scripts/build_model.py MGR-<date>.xlsx model/MGR-Model.xlsx` |
| `model/MGR-Model.xlsx` | دیتابیس تمیز (ستاره‌ای، Excel Table، شیت Check). در git نیست (`.gitignore`)؛ با اسکریپت دوباره ساخته می‌شود |
| `model/PowerQuery.m` | پارامتر مسیر، تابع بارگذاری، ۲۳ کوئری، لیست رابطه‌ها و Sort by |
| `model/Measures.dax` | حدود ۹۰ Measure در ۷ پوشه (Report، S-curve، WBS/Discipline، Quantities، Contractors، 6M، Manpower، رنگ) |
| `theme/ODCC-Navy.json` | تم سرمه‌ای (View > Themes > Browse for themes) |
| `docs/rebuild-guide.md` | قدم‌به‌قدم ساخت در Power BI Desktop + نقشه صفحه‌ها |
| `docs/mockup.template.html` | قالب ماکت تعاملی (داده با اسکریپت تزریق می‌شود) |

ماکت منتشرشده: https://claude.ai/artifact/26dgaheBp3PceAHfmADF2q (خصوصی)

**تست نشده:** M و DAX در Power BI Desktop اجرا نشده‌اند (این محیط ویندوز/Power BI ندارد). اعداد کلیدی Measureها در پایتون روی همین داده بازتولید شد (Overall 14.52% / 15.94%، SPI پیمانکارها). اولین اجرا در Desktop را با شیت Check مقایسه کن.
