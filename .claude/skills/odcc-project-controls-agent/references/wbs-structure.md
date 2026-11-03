# ساختار WBS پروژه ARUP فاز 2-2 (از راهنمای PDF کاربر + شیت PMS-Overall)

منبع: `ARUP_Phase_2-2_PMS_WBS_Unitwise_Guide_ColorCoded.pdf` (۱۱۳ صفحه؛ برگرفته از PMS-Overall فایل CSM-DR-P2-2026-Oct-19). فیلدها: LVL, Unit, Disc., Tag, Activity ID, Activity. نام فعالیت‌ها همان فایل است و چیزی ساخته نشده.

## سطح‌ها
| سطح | معنی | یادداشت |
|---|---|---|
| L1 | Project | Abadan Refinery Upgrading Project - Phase 2-2 (وزن ۱) |
| L2 | Major Phase | ۴ فاز: Mobilization & Demobilization، Construction & Installation - Overall، Pre-Commissioning، Assistance in Commissioning |
| L3 | Unit / گروه منطقه | یونیت‌ها در فازهای Construction و Pre-Commissioning |
| L4 | Discipline | دیسیپلین هر یونیت |
| L5 | گروه‌بندی کار / بسته | نام‌ها همان‌طور که در فایل است |
| L6 | Work Package | **ورود Actual Progress** (طبق توضیح کاربر) |
| L7 | Detailed activity | ریزترین سطح؛ **ورود Actual Progress** (طبق توضیح کاربر) |
Actual در L6/L7 وارد و با فرمول‌های فایل به سطوح بالا رول‌آپ می‌شود. در بعضی شاخه‌ها L5 بدون فرزند است (مثل Pile Fabrication / Pile Driving در Unit 20)؛ اینکه آنجا Actual در L5 وارد می‌شود یا خیر [تأیید شود].

## الگوی کد Activity ID [فایل]
- Mob: `O.2` → `O.2.CM` → `O.2.CM.CMOB` → `O.2.CM.0100` (Mobilization) → `0200` (Main Mobilization) → `0300` (Client Building…) → L7 مثل `O.2.CM.0301`.
- Construction: `O.2.CI.<unit>` (L3؛ مثل O.2.CI.20) → L4 دیسیپلین مثل `O.2.CI.20.Pil` (Piling) یا `O.2.CT.20.CV` (Civil Works)، `O.2.CI.20.ST` (Steel Structure) → L5 مثل `...CV.CRHG` (Rough Grading) یا `...CV.CEFC` (Equipment Foundation) → L6 مثل `...CV.0400` (Tank Foundation) → L7 مثل `...CV.0410` (TK-20316 A، فیلد Tag = شماره تجهیز).
- Tag معمولاً شماره تجهیز (TK-…, P-…) یا نام ساختمان است.

## یونیت‌های پروژه (تعداد شاخه دیسیپلین / ردیف L5-L7)
Construction & Installation: 17 LPG Treating Merox (8/186)، 20 Tankage Area (11/349)، 35 Flare System and Networks (9/138)، 40 Buildings (21/420)، 41 Power and Steam Generation (9/166)، 42 Water System (9/136)، 43 Natural Gas and Fuel System (9/155)، 44 Plant and Instrument Air (9/153)، 45 Interconnections (7/127)، 46 Effluent and Waste Water Treatment (9/157)، 47 Fire Fighting (9/65)، 48 Power Distribution (2/35)، 50 Roads, Civil Works and Site Preparation (2/32)، 52 Naphtha Hydrotreater (Splitter) (10/229)، 53 Continuous Catalytic Reforming (10/443)، 54 Kerosene Hydrotreater (10/220)، 57 Hydrogen Production (8/199)، 58 Amine Treating (9/212)، 59 Sulphur Recovery (9/213)، 66 Gas Oil Hydrotreater #1 (10/346)، 68 Isomerization (10/341)، 78 Sour Water Stripper (9/188)، 86 Gas Oil Hydrotreater #2 (10/346)، 91 & 92 Caustic Dissolving & Neutralization (10/215)، 93 New Control Systems (1/6)، 96 Telecommunication (1/14).
Mobilization & Demobilization: ۲ شاخه / ۲۰۲ ردیف. Pre-Commissioning: همان یونیت‌ها با شاخه‌های pre-commissioning و بدون ردیف L5-7 تفصیلی (۰). Assistance in Commissioning: بدون زیرمجموعه.

## دیسیپلین‌های رایج (L4)
Piling، Civil Works، Steel Structure، Piping (AG/UG)، Mechanical (Fixed Equipment؛ هر دو املا Fix/Fixed در فایل)، Process Machinery (Rotary)، Heat Transfer Equipment، Electrical، Instrument/Instrumentation (دو نام)، Insulation & Painting، Architectural، HVAC، و در Unit 40 ساختمان‌ها (BD-40-xx، SS 40-xx، RIB-40-04…). نام‌ها یکدست نیستند؛ در فرمول و VBA نام را با تطبیق دقیق فایل بنویس.

## ارتباط با پیمانکاران [استنباط، تأیید شود]
Ehdas: Piling و ساخت سازه مخزن (Unit 20 Tankage: Piling، Steel Structure، Civil Works/Tank Foundation). برای هر پیمانکار، یونیت/دیسیپلین‌های مربوطه را از کاربر بگیر؛ هنوز نگفته.

## رنگ‌بندی سطح‌ها (شیت Sheet5، سلول‌های B13:B20) [فایل + توضیح کاربر]
B13:B20 شماره سطح ۱ تا ۸ است و رنگ پر شدن (Fill) هر سلول، کد رنگ همان سطح در PMS-Overall است:
| سطح | رنگ (RGB) |
|---|---|
| 1 | 9DB7B4 (سبز-خاکستری) |
| 2 | 475A8D (آبی تیره) |
| 3 | خاکستری روشن (سفید تم با tint ‎-0.15 ≈ D9D9D9) |
| 4 | B8DA8D (سبز روشن) |
| 5 | F7A67B (نارنجی/سالمونی) |
| 6 | 4ED5C7 (فیروزه‌ای) |
| 7 | D44865 (سرخابی) |
| 8 | FFE699 (زرد روشن) |
راهنمای PDF ۷ سطح دارد؛ PMS-Overall ستون سطح ۸ هم دارد (K:R). شیت Sheet5 همچنین آیکون‌های وضعیت هوا دارد (A1:B9). برای فیلتر بر اساس رنگ یا تشخیص سطح ردیف، از ستون B (LVL) در PMS-Overall استفاده کن، نه فقط رنگ.
