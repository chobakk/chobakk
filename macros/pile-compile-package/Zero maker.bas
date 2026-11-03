Attribute VB_Name = "Module1"

Option Explicit

Private hasRunThisSession As Boolean

Public Sub RunPMSUpdate()

    Dim wb As Workbook
    Dim oldScreenUpdating As Boolean
    Dim oldEnableEvents As Boolean
    Dim oldCalculation As XlCalculation
    Dim settingsSaved As Boolean
    Dim errorMessage As String

    Set wb = ThisWorkbook

    ' Prevent repeated execution in the same session
    If hasRunThisSession Then
        MsgBox "The PMS update has already been run in this Excel session." & _
               vbCrLf & _
               "Close and reopen the workbook to run it again.", _
               vbExclamation, "PMS Update"
        Exit Sub
    End If

    If MsgBox("Run all PMS updates?" & vbCrLf & _
              "Please make a backup before running.", _
              vbYesNo + vbQuestion + vbDefaultButton2, _
              "PMS Update") <> vbYes Then Exit Sub

    On Error GoTo ErrorHandler

    oldScreenUpdating = Application.screenUpdating
    oldEnableEvents = Application.enableEvents
    oldCalculation = Application.calculation
    settingsSaved = True

    Application.screenUpdating = False
    Application.enableEvents = False
    Application.calculation = xlCalculationManual

    ' =========================
    ' WPA
    ' =========================
    CopyValues wb, "WPA", "N5:O38", "J5"
    CopyValues wb, "WPA", "N45:O75", "J45"

    ' =========================
    ' PMS-Overall
    ' =========================
    CopyValues wb, "PMS-Overall", "AD7:AE5800", "Z7"
    IncrementDate wb, "PMS-Overall", "AK3"

    ' =========================
    ' Cov-P
    ' =========================
    CopyValues wb, "Cov-P", "M13", "AE34"
    CopyValues wb, "Cov-P", "AE34", "AB34"
    CopyValues wb, "Cov-P", "M17", "AB38"
    CopyValues wb, "Cov-P", "AD15:AE17", "AB15"
    IncrementDate wb, "Cov-P", "AA7"

    ' =========================
    ' FRJE
    ' =========================
    CopyValues wb, "FRJE", "G3:G60", "E3"
    IncrementDate wb, "FRJE", "K2"

    ' =========================
    ' FRJP
    ' =========================
    CopyValues wb, "FRJP", "AH7:AI170", "AD7"
    CopyValues wb, "FRJP", "AC7:AC170", "AA7"
    IncrementNumber wb, "FRJP", "AT3"

    ' =========================
    ' SOL1
    ' =========================
    CopyValues wb, "SOL1", "AH7:AI1300", "AD7"
    CopyValues wb, "SOL1", "AC7:AC1300", "AA7"
    IncrementNumber wb, "SOL1", "AT3"

    ' =========================
    ' SOL2
    ' =========================
    CopyValues wb, "SOL2", "AH7:AI1000", "AD7"
    CopyValues wb, "SOL2", "AC7:AC1000", "AA7"
    IncrementNumber wb, "SOL2", "AT3"

    ' =========================
    ' Ehds
    ' =========================
    CopyValues wb, "Ehds", "AH7:AI800", "AD7"
    CopyValues wb, "Ehds", "AC7:AC800", "AA7"
    IncrementNumber wb, "Ehds", "AT3"

    ' Mark as completed only after all operations succeed
    hasRunThisSession = True

    RestoreExcelSettings oldScreenUpdating, _
                         oldEnableEvents, oldCalculation

    MsgBox "All operations completed successfully." & vbCrLf & _
           "The macro is now locked for this Excel session.", _
           vbInformation, "PMS Update"

    Exit Sub

ErrorHandler:

    errorMessage = Err.Description

    If settingsSaved Then
        RestoreExcelSettings oldScreenUpdating, _
                             oldEnableEvents, oldCalculation
    End If

    MsgBox "An error occurred:" & vbCrLf & _
           errorMessage & vbCrLf & vbCrLf & _
           "Some changes may already have been applied." & vbCrLf & _
           "Check the workbook before trying again.", _
           vbCritical, "PMS Update"

End Sub


Private Sub CopyValues( _
    ByVal wb As Workbook, _
    ByVal sheetName As String, _
    ByVal sourceAddress As String, _
    ByVal destinationAddress As String)

    Dim ws As Worksheet
    Dim src As Range
    Dim dst As Range
    Dim sourceValue As Variant
    Dim r As Long
    Dim c As Long

    Set ws = wb.Worksheets(sheetName)
    Set src = ws.Range(sourceAddress)

    Set dst = ws.Range(destinationAddress).Resize( _
        src.Rows.Count, src.Columns.Count)

    ' Copy nonblank values only. Blank cells and formulas returning "" are skipped.
    ' Real numeric zero values and Excel error values are preserved.
    ' Cell-by-cell processing supports both single-cell and multi-cell ranges.
    For r = 1 To src.Rows.Count
        For c = 1 To src.Columns.Count
            sourceValue = src.Cells(r, c).Value2

            If IsError(sourceValue) Then
                dst.Cells(r, c).Value = sourceValue
            ElseIf Len(CStr(sourceValue)) > 0 Then
                dst.Cells(r, c).Value = sourceValue
            End If
        Next c
    Next r

End Sub


Private Sub IncrementDate( _
    ByVal wb As Workbook, _
    ByVal sheetName As String, _
    ByVal cellAddress As String)

    Dim c As Range

    Set c = wb.Worksheets(sheetName).Range(cellAddress)

    If IsError(c.Value) Then
        Err.Raise vbObjectError + 101, _
                  "IncrementDate", _
                  "Invalid date value in " & _
                  sheetName & "!" & cellAddress
    End If

    If Not IsDate(c.Value) Then
        Err.Raise vbObjectError + 102, _
                  "IncrementDate", _
                  "Invalid date value in " & _
                  sheetName & "!" & cellAddress
    End If

    c.Value = DateAdd("d", 1, CDate(c.Value))

End Sub


Private Sub IncrementNumber( _
    ByVal wb As Workbook, _
    ByVal sheetName As String, _
    ByVal cellAddress As String)

    Dim c As Range

    Set c = wb.Worksheets(sheetName).Range(cellAddress)

    If IsError(c.Value) Then
        Err.Raise vbObjectError + 103, _
                  "IncrementNumber", _
                  "Invalid numeric value in " & _
                  sheetName & "!" & cellAddress
    End If

    If Len(Trim$(CStr(c.Value))) = 0 Or _
       Not IsNumeric(c.Value) Then

        Err.Raise vbObjectError + 104, _
                  "IncrementNumber", _
                  "Invalid numeric value in " & _
                  sheetName & "!" & cellAddress
    End If

    c.Value = CDbl(c.Value) + 1

End Sub


Private Sub RestoreExcelSettings( _
    ByVal screenUpdating As Boolean, _
    ByVal enableEvents As Boolean, _
    ByVal calculation As XlCalculation)

    On Error Resume Next

    Application.calculation = calculation
    Application.enableEvents = enableEvents
    Application.screenUpdating = screenUpdating

    On Error GoTo 0

End Sub


