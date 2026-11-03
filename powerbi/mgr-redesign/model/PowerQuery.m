// =====================================================================
// Power Query (M) for MGR-Model.xlsx
// How to use (Power BI Desktop):
//   1. Home > Transform data > New Source > Blank Query, rename to ModelPath,
//      open Advanced Editor and paste block [1].
//   2. New Blank Query, rename to fnLoad, paste block [2].
//   3. For each table below: New Blank Query, rename to the table name,
//      paste its one-line body. (Or copy all queries at once in the
//      Queries pane with Ctrl+V after pasting this whole file into one
//      query and splitting.)
//   4. File > Options > Current file > Data load: turn OFF
//      "Auto date/time" (removes the hidden LocalDateTable tables).
// Every month: change only ModelPath, then Refresh.
// =====================================================================

// [1] ModelPath  (Parameter: Type = Text)
"C:\PPC\PowerBI\MGR-Model.xlsx" meta [IsParameterQuery = true, Type = "Text", IsParameterQueryRequired = true]

// [2] fnLoad: loads Excel Table t<Name> and applies column types
(name as text, types as list) as table =>
let
    Book   = Excel.Workbook(File.Contents(ModelPath), true, true),
    Data   = Book{[Item = "t" & name, Kind = "Table"]}[Data],
    Typed  = Table.TransformColumnTypes(Data, types, "en-US")
in
    Typed

// ------------------------------------------------------------- dimensions
// DimDate          (then: Table tools > Mark as date table > Date)
fnLoad("DimDate", {{"Date", type date}, {"Year", Int64.Type}, {"MonthNo", Int64.Type}, {"MonthName", type text}, {"YearMonth", type text}, {"YearMonthSort", Int64.Type}, {"ISOWeek", Int64.Type}, {"JYear", Int64.Type}, {"JMonthNo", Int64.Type}, {"JMonthName", type text}, {"JYearMonth", type text}, {"JYearMonthSort", Int64.Type}, {"JDate", type text}, {"IsOnOrBeforeCutOff", type logical}})

// DimContractor
fnLoad("DimContractor", {{"ContractorCode", type text}, {"ContractorName", type text}, {"Status", type text}, {"SortOrder", Int64.Type}, {"IsTotal", type logical}})

// DimScope
fnLoad("DimScope", {{"ScopeOrder", Int64.Type}, {"ScopeCode", type text}, {"ScopeName", type text}, {"Weight", type number}})

// DimDiscipline
fnLoad("DimDiscipline", {{"DisciplineOrder", Int64.Type}, {"Discipline", type text}})

// DimUnit
fnLoad("DimUnit", {{"Unit", type text}, {"UnitLabel", type text}})

// DimContractorItem
fnLoad("DimContractorItem", {{"Item", type text}})

// Snapshot
fnLoad("Snapshot", {{"Key", type text}, {"Description", type text}, {"Value", type number}, {"ValueDate", type date}, {"Unit", type text}, {"SourceCell", type text}})

// ------------------------------------------------------------- facts
// FactSCurveDaily
fnLoad("FactSCurveDaily", {{"Date", type date}, {"ScopeCode", type text}, {"CumPlan", type number}, {"CumActual", type number}})

// FactWBSStatus
fnLoad("FactWBSStatus", {{"WBSOrder", Int64.Type}, {"WBSName", type text}, {"Weight", type number}, {"PlanPct", type number}, {"ActualPct", type number}})

// FactDisciplineStatus
fnLoad("FactDisciplineStatus", {{"DisciplineOrder", Int64.Type}, {"Discipline", type text}, {"Weight", type number}, {"PlanPct", type number}, {"ActualPct", type number}, {"SPI_Source", type number}})

// FactDisciplineByUnit
fnLoad("FactDisciplineByUnit", {{"Unit", type text}, {"Discipline", type text}, {"WeightInProject", type number}, {"PlanPct", type number}, {"ActualPct", type number}})

// FactQuantityByUnit
fnLoad("FactQuantityByUnit", {{"Activity", type text}, {"Area", type text}, {"Unit", type text}, {"Estimate", type number}, {"DWGReceived", type number}, {"Done", type number}, {"Remain", type number}, {"WorkFront", type number}})

// FactMajorItems
fnLoad("FactMajorItems", {{"ItemOrder", Int64.Type}, {"Item", type text}, {"UOM", type text}, {"Estimate", type number}, {"PlanQty", type number}, {"DoneQty", type number}, {"RemainQty", type number}, {"WorkFront", type number}})

// FactMajorItems6M
fnLoad("FactMajorItems6M", {{"ContractorCode", type text}, {"Item", type text}, {"UOM", type text}, {"Estimate6M", type number}, {"PlanQty6M", type number}, {"DoneQty6M", type number}, {"RemainQty6M", type number}})

// FactItemMonthly6M
fnLoad("FactItemMonthly6M", {{"PeriodEnd", type date}, {"Item", type text}, {"PlanQty", type number}, {"ActualQty", type number}})

// FactSCurve6M
fnLoad("FactSCurve6M", {{"PeriodEnd", type date}, {"MonthlyPlan", type number}, {"MonthlyActual", type number}, {"CumPlan", type number}, {"CumActual", type number}})

// FactSCurveContractor
fnLoad("FactSCurveContractor", {{"ContractorCode", type text}, {"PeriodEnd", type date}, {"MonthlyPlan", type number}, {"MonthlyActual", type number}, {"CumPlan", type number}, {"CumActual", type number}})

// FactContractorQtyPeriodic
fnLoad("FactContractorQtyPeriodic", {{"ContractorCode", type text}, {"PeriodEnd", type date}, {"Item", type text}, {"PlanQty", type number}, {"ActualQty", type number}})

// FactContractorQtyStatus
fnLoad("FactContractorQtyStatus", {{"ContractorCode", type text}, {"Item", type text}, {"UOM", type text}, {"Estimate", type number}, {"PlanQty", type number}, {"ActualQty", type number}, {"RemainQty", type number}})

// FactManpowerODCC
fnLoad("FactManpowerODCC", {{"MonthStart", type date}, {"ProjectMonthNo", Int64.Type}, {"ContractPhase", type text}, {"PlanManDay", type number}, {"ActualManDay", type number}, {"PlanManhourBaseline", type number}, {"PlanManhour", type number}, {"ActualManhour", type number}})

// FactODCCDepartment
fnLoad("FactODCCDepartment", {{"DeptOrder", Int64.Type}, {"Department", type text}, {"PlanHeadcount", Int64.Type}, {"ActualHeadcount", Int64.Type}})

// FactManpowerContractor
fnLoad("FactManpowerContractor", {{"MonthStart", type date}, {"ContractorCode", type text}, {"Category", type text}, {"Manpower", type number}})

// =====================================================================
// Relationships (Model view). All: Many-to-one, Single direction.
// Delete every old relationship first.
//   FactSCurveDaily[Date]                -> DimDate[Date]
//   FactSCurveDaily[ScopeCode]           -> DimScope[ScopeCode]
//   FactSCurveContractor[PeriodEnd]      -> DimDate[Date]
//   FactSCurveContractor[ContractorCode] -> DimContractor[ContractorCode]
//   FactSCurve6M[PeriodEnd]              -> DimDate[Date]
//   FactItemMonthly6M[PeriodEnd]         -> DimDate[Date]
//   FactMajorItems6M[ContractorCode]     -> DimContractor[ContractorCode]
//   FactContractorQtyPeriodic[PeriodEnd] -> DimDate[Date]
//   FactContractorQtyPeriodic[ContractorCode] -> DimContractor[ContractorCode]
//   FactContractorQtyPeriodic[Item]      -> DimContractorItem[Item]
//   FactContractorQtyStatus[ContractorCode]   -> DimContractor[ContractorCode]
//   FactContractorQtyStatus[Item]        -> DimContractorItem[Item]
//   FactDisciplineStatus[Discipline]     -> DimDiscipline[Discipline]
//   FactDisciplineByUnit[Discipline]     -> DimDiscipline[Discipline]
//   FactDisciplineByUnit[Unit]           -> DimUnit[Unit]
//   FactQuantityByUnit[Unit]             -> DimUnit[Unit]
//   FactManpowerODCC[MonthStart]         -> DimDate[Date]
//   FactManpowerContractor[MonthStart]   -> DimDate[Date]
//   FactManpowerContractor[ContractorCode] -> DimContractor[ContractorCode]
// Sort by column:  DimDate[MonthName] by MonthNo, DimDate[YearMonth] by YearMonthSort,
//   DimDate[JYearMonth] by JYearMonthSort, DimDate[JMonthName] by JMonthNo,
//   DimContractor[ContractorName] by SortOrder, DimScope[ScopeName] by ScopeOrder,
//   DimDiscipline[Discipline] by DisciplineOrder.
// Hide in report view: all key columns on fact tables, Snapshot table, sort columns.
// =====================================================================
