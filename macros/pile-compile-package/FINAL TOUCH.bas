Attribute VB_Name = "Module6"

Option Explicit

Private FinalTouchExecuted As Boolean

Public Sub Final_Touch()

    Dim wsCnt As Worksheet
    Dim wsCov As Worksheet
    Dim wsPMS As Worksheet

    Dim dateCnt As Date
    Dim datePMS As Date

    Dim foundCnt As Long
    Dim foundPMS As Long
    Dim lastCol As Long

    Dim sourceCnt As Range
    Dim targetCnt As Range
    Dim sourcePMS As Range
    Dim targetPMS As Range

    Dim dataCnt As Variant
    Dim dataPMS As Variant
    Dim i As Long

    Dim oldScreenUpdating As Boolean
    Dim oldEnableEvents As Boolean
    Dim oldCalculation As XlCalculation

    Dim settingsChanged As Boolean
    Dim firstWriteCompleted As Boolean
    Dim errorMessage As String

    On Error GoTo ErrorHandler

    '-----------------------------------------
    ' ONCE-PER-SESSION CHECK
    '-----------------------------------------

    If FinalTouchExecuted Then
        MsgBox "Final Touch has already been executed in this Excel session.", _
               vbExclamation, "Final Touch"
        Exit Sub
    End If

    If MsgBox("Are you sure you want to execute FINAL TOUCH?", _
              vbYesNo + vbQuestion, "Confirm Final Touch") <> vbYes Then
        Exit Sub
    End If

    '-----------------------------------------
    ' SET WORKSHEETS
    '-----------------------------------------

    Set wsCnt = ThisWorkbook.Worksheets("Cnt-t")
    Set wsCov = ThisWorkbook.Worksheets("Cov-P")
    Set wsPMS = ThisWorkbook.Worksheets("PMS-Overall")

    '-----------------------------------------
    ' OPERATION 1 - CNT-T
    '-----------------------------------------

    Set sourceCnt = wsCnt.Range("D12:D333")

    If Not TryReadDate(wsCov.Range("R7"), dateCnt) Then
        MsgBox "Invalid date in Cov-P!R7." & vbCrLf & _
               "Final Touch cancelled.", vbCritical, "Date Error"
        Exit Sub
    End If

    foundCnt = FindDateColumn(wsCnt.Range("K11:AO11"), dateCnt)

    If foundCnt = 0 Then
        MsgBox "Date from Cov-P!R7 was not found in Cnt-t!K11:AO11." & _
               vbCrLf & "Final Touch cancelled. No data was changed.", _
               vbCritical, "Date Not Found"
        Exit Sub
    End If

    Set targetCnt = wsCnt.Cells(12, foundCnt).Resize(322, 1)

    '-----------------------------------------
    ' OPERATION 2 - PMS-OVERALL
    '-----------------------------------------

    Set sourcePMS = wsPMS.Range("AE8:AE5700")

    If Not TryReadDate(wsPMS.Range("AK3"), datePMS) Then
        MsgBox "Invalid date in PMS-Overall!AK3: " & _
               wsPMS.Range("AK3").Text & vbCrLf & _
               "Expected a valid date such as 11/1/2026." & _
               vbCrLf & "Final Touch cancelled.", _
               vbCritical, "Date Error"
        Exit Sub
    End If

    'Search date headers from BDL7 to the right
    lastCol = wsPMS.Cells(7, wsPMS.Columns.Count).End(xlToLeft).Column

    If lastCol < wsPMS.Range("BDL7").Column Then
        MsgBox "No date headers found in PMS-Overall row 7 from BDL onward." & _
               vbCrLf & "Final Touch cancelled.", _
               vbCritical, "Date Not Found"
        Exit Sub
    End If

    foundPMS = FindDateColumn( _
        wsPMS.Range(wsPMS.Cells(7, wsPMS.Range("BDL7").Column), _
                    wsPMS.Cells(7, lastCol)), datePMS)

    If foundPMS = 0 Then
        MsgBox "Date from PMS-Overall!AK3 was not found in row 7, starting from BDL7." & _
               vbCrLf & "Final Touch cancelled. No data was changed.", _
               vbCritical, "Date Not Found"
        Exit Sub
    End If

    Set targetPMS = wsPMS.Cells(8, foundPMS).Resize(5693, 1)

    '-----------------------------------------
    ' CHECK BOTH DESTINATIONS
    '-----------------------------------------

    If Application.CountA(targetCnt) > 0 Then
        MsgBox "Destination Cnt-t!" & targetCnt.Address(False, False) & _
               " already contains data." & vbCrLf & _
               "Final Touch cancelled. No data was changed.", _
               vbCritical, "Destination Not Empty"
        Exit Sub
    End If

    If Application.CountA(targetPMS) > 0 Then
        MsgBox "Destination PMS-Overall!" & targetPMS.Address(False, False) & _
               " already contains data." & vbCrLf & _
               "Final Touch cancelled. No data was changed.", _
               vbCritical, "Destination Not Empty"
        Exit Sub
    End If

    '-----------------------------------------
    ' READ BOTH SOURCES BEFORE WRITING
    '-----------------------------------------

    dataCnt = sourceCnt.value2
    dataPMS = sourcePMS.value2

    '-----------------------------------------
    ' SAVE EXCEL SETTINGS
    '-----------------------------------------

    oldScreenUpdating = Application.screenUpdating
    oldEnableEvents = Application.enableEvents
    oldCalculation = Application.calculation

    Application.screenUpdating = False
    Application.enableEvents = False
    Application.calculation = xlCalculationManual

    settingsChanged = True

    '-----------------------------------------
    ' OPERATION 1 - WRITE CNT-T
    '-----------------------------------------

    For i = 1 To UBound(dataCnt, 1)
        If Not IsBlankImportedValue(dataCnt(i, 1)) Then
            targetCnt.Cells(i, 1).Value2 = dataCnt(i, 1)
        End If
    Next i
    firstWriteCompleted = True

    '-----------------------------------------
    ' OPERATION 2 - WRITE PMS-OVERALL
    '-----------------------------------------

    For i = 1 To UBound(dataPMS, 1)
        If Not IsBlankImportedValue(dataPMS(i, 1)) Then
            targetPMS.Cells(i, 1).Value2 = dataPMS(i, 1)
        End If
    Next i

    '-----------------------------------------
    ' SUCCESS
    '-----------------------------------------

    FinalTouchExecuted = True

    MsgBox "FINAL TOUCH completed successfully!" & vbCrLf & vbCrLf & _
           "1. Cnt-t updated under " & _
           wsCnt.Cells(11, foundCnt).Address(False, False) & vbCrLf & _
           "2. PMS-Overall updated under " & _
           wsPMS.Cells(7, foundPMS).Address(False, False), _
           vbInformation, "Final Touch Completed"

    GoTo CleanExit

'-----------------------------------------
' ERROR HANDLING
'-----------------------------------------

ErrorHandler:

    errorMessage = Err.Description

    On Error Resume Next

    If firstWriteCompleted Then
        targetCnt.ClearContents
    End If

    On Error GoTo 0

    MsgBox "Final Touch encountered an error:" & vbCrLf & _
           errorMessage & vbCrLf & vbCrLf & _
           "Please inspect the workbook before trying again.", _
           vbCritical, "Final Touch Error"

    GoTo CleanExit

CleanExit:

    If settingsChanged Then
        Application.screenUpdating = oldScreenUpdating
        Application.enableEvents = oldEnableEvents
        Application.calculation = oldCalculation
    End If

End Sub


'=========================================
' DATE READER
'=========================================

Private Function TryReadDate(ByVal dateCell As Range, _
                             ByRef resultDate As Date) As Boolean

    Dim v As Variant
    Dim dateText As String
    Dim parts() As String

    On Error GoTo InvalidDate

    v = dateCell.value

    If IsError(v) Or IsEmpty(v) Then GoTo InvalidDate

    'Real Excel date or numeric serial
    If IsNumeric(dateCell.value2) And VarType(v) <> vbString Then

        If CDbl(dateCell.value2) <= 0 Then GoTo InvalidDate

        resultDate = DateSerial(1899, 12, 30) + _
                     Int(CDbl(dateCell.value2))

        TryReadDate = True
        Exit Function

    End If

    'Text date, e.g. 11/1/2026
    dateText = Trim$(CStr(v))
    dateText = Replace(dateText, "-", "/")
    dateText = Replace(dateText, ".", "/")

    parts = Split(dateText, "/")

    If UBound(parts) = 2 Then

        'Interpret as month/day/year
        If Len(parts(2)) = 4 And _
           IsNumeric(parts(0)) And _
           IsNumeric(parts(1)) And _
           IsNumeric(parts(2)) Then

            resultDate = DateSerial(CInt(parts(2)), _
                                    CInt(parts(0)), _
                                    CInt(parts(1)))

            If Month(resultDate) <> CInt(parts(0)) Or _
               Day(resultDate) <> CInt(parts(1)) Or _
               Year(resultDate) <> CInt(parts(2)) Then GoTo InvalidDate

            TryReadDate = True
            Exit Function

        End If

    End If

    If IsDate(dateText) Then
        resultDate = DateValue(dateText)
        TryReadDate = True
        Exit Function
    End If

InvalidDate:
    TryReadDate = False

End Function


'=========================================
' DATE COLUMN FINDER
'=========================================

Private Function FindDateColumn(ByVal searchRange As Range, _
                                ByVal targetDate As Date) As Long

    Dim cell As Range
    Dim cellDate As Date

    For Each cell In searchRange.Cells

        If TryReadDate(cell, cellDate) Then

            If cellDate = targetDate Then
                FindDateColumn = cell.Column
                Exit Function
            End If

        End If

    Next cell

    FindDateColumn = 0

End Function




'=========================================
' CHECK WHETHER A SOURCE VALUE IS BLANK
'=========================================

Private Function IsBlankImportedValue(ByVal v As Variant) As Boolean
    If IsEmpty(v) Or IsNull(v) Then
        IsBlankImportedValue = True
    ElseIf VarType(v) = vbString Then
        IsBlankImportedValue = (Len(Trim$(CStr(v))) = 0)
    Else
        IsBlankImportedValue = False
    End If
End Function
