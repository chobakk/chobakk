Attribute VB_Name = "Module1"
Option Explicit

Private EHD_ExecutedThisSession As Boolean

Public Sub Daily_Data_Entry_EHD()

    Dim wsDest As Worksheet
    Dim wsSource As Worksheet
    Dim wbSource As Workbook
    Dim wb As Workbook

    Dim sourcePath As Variant
    Dim sourceOpenedByMacro As Boolean

    Dim sequenceValue As Variant
    Dim foundColumn As Long
    Dim lastHeaderCol As Long
    Dim lastSourceRow As Long

    Dim sourceKeys As Variant
    Dim sourceValues As Variant
    Dim destinationKeys As Variant
    Dim outputData() As Variant

    Dim dict As Object
    Dim dictSourceRow As Object
    Dim logEntries As Collection
    Dim i As Long
    Dim key As String
    Dim matchedCount As Long
    Dim blankSkipped As Long
    Dim unmatchedCount As Long
    Dim sourceRow As Long
    Dim logStatus As String

    Dim targetRange As Range

    Dim oldScreenUpdating As Boolean
    Dim oldEnableEvents As Boolean
    Dim oldCalculation As XlCalculation
    Dim settingsChanged As Boolean

    Dim errorMessage As String

    On Error GoTo ErrorHandler

    '-----------------------------------------
    ' ONCE-PER-SESSION CHECK
    '-----------------------------------------

    If EHD_ExecutedThisSession Then
        MsgBox "Daily Data Entry EHD has already been executed in this Excel session.", _
               vbExclamation, "EHD Daily Entry"
        Exit Sub
    End If

    If MsgBox("Do you want to execute Daily Data Entry for EHD?", _
              vbYesNo + vbQuestion, "Confirm EHD Import") <> vbYes Then
        Exit Sub
    End If

    '-----------------------------------------
    ' DESTINATION SHEET
    '-----------------------------------------

    Set wsDest = ThisWorkbook.Worksheets("Ehds")

    '-----------------------------------------
    ' READ SEQUENCE NUMBER
    '-----------------------------------------

    sequenceValue = wsDest.Range("AT3").value2

    If IsError(sequenceValue) Or Len(Trim$(CStr(sequenceValue))) = 0 Then
        MsgBox "No valid sequence number found in Ehds!AT3." & _
               vbCrLf & "Import cancelled.", _
               vbCritical, "Sequence Error"
        Exit Sub
    End If

    '-----------------------------------------
    ' FIND DESTINATION COLUMN
    ' Search row 5 starting from AU
    '-----------------------------------------

    lastHeaderCol = wsDest.Cells(5, wsDest.Columns.Count).End(xlToLeft).Column

    If lastHeaderCol < wsDest.Range("AU5").Column Then
        MsgBox "No sequence headers found in Ehds row 5 from AU onward.", _
               vbCritical, "Sequence Not Found"
        Exit Sub
    End If

    foundColumn = FindSequenceColumn( _
        wsDest.Range(wsDest.Cells(5, wsDest.Range("AU5").Column), _
                     wsDest.Cells(5, lastHeaderCol)), sequenceValue)

    If foundColumn = 0 Then
        MsgBox "Sequence number from Ehds!AT3 was not found in row 5, starting from AU." & _
               vbCrLf & "Import cancelled. No data was changed.", _
               vbCritical, "Sequence Not Found"
        Exit Sub
    End If

    Set targetRange = wsDest.Cells(9, foundColumn).Resize(465, 1)

    '-----------------------------------------
    ' CHECK DESTINATION BEFORE IMPORT
    '-----------------------------------------

    If Application.CountA(targetRange) > 0 Then
        MsgBox "Destination " & targetRange.Address(False, False) & _
               " already contains data." & vbCrLf & _
               "Import cancelled. No data was changed.", _
               vbCritical, "Destination Not Empty"
        Exit Sub
    End If

    '-----------------------------------------
    ' FIND OR OPEN SOURCE WORKBOOK
    '-----------------------------------------

    For Each wb In Application.Workbooks
        If StrComp(wb.Name, "EHD.xlsx", vbTextCompare) = 0 Then
            Set wbSource = wb
            Exit For
        End If
    Next wb

    If wbSource Is Nothing Then

        sourcePath = Application.GetOpenFilename( _
            FileFilter:="Excel Files (*.xlsx;*.xlsm;*.xlsb;*.xls),*.xlsx;*.xlsm;*.xlsb;*.xls", _
            Title:="Select EHD Source Workbook")

        If VarType(sourcePath) = vbBoolean Then
            If sourcePath = False Then Exit Sub
        End If

        Set wbSource = Workbooks.Open( _
            Filename:=CStr(sourcePath), _
            UpdateLinks:=False, _
            ReadOnly:=True)

        sourceOpenedByMacro = True

    End If

    '-----------------------------------------
    ' SOURCE SHEET
    '-----------------------------------------

    On Error Resume Next
    Set wsSource = wbSource.Worksheets("PMS-01")
    On Error GoTo ErrorHandler

    If wsSource Is Nothing Then
        Err.Raise vbObjectError + 1001, _
                  "Daily_Data_Entry_EHD", _
                  "Sheet PMS-01 was not found in the source workbook."
    End If

    '-----------------------------------------
    ' READ SOURCE DATA
    ' Key column: B
    ' Return column: R
    '-----------------------------------------

    lastSourceRow = wsSource.Cells(wsSource.Rows.Count, "B").End(xlUp).Row

    If lastSourceRow < 2 Then
        Err.Raise vbObjectError + 1002, _
                  "Daily_Data_Entry_EHD", _
                  "No source records were found in PMS-01 column B."
    End If

    sourceKeys = wsSource.Range("B2:B" & lastSourceRow).value2
    sourceValues = wsSource.Range("R2:R" & lastSourceRow).value2

    '-----------------------------------------
    ' BUILD LOOKUP DICTIONARY
    ' First duplicate key wins
    '-----------------------------------------

    Set dict = CreateObject("Scripting.Dictionary")
    Set dictSourceRow = CreateObject("Scripting.Dictionary")
    Set logEntries = New Collection
    dict.CompareMode = vbTextCompare
    dictSourceRow.CompareMode = vbTextCompare

    For i = 1 To UBound(sourceKeys, 1)

        If Not IsError(sourceKeys(i, 1)) Then

            key = Trim$(CStr(sourceKeys(i, 1)))

            If Len(key) > 0 Then
                If Not dict.Exists(key) Then
                    dict.Add key, sourceValues(i, 1)
                    dictSourceRow.Add key, i + 1
                End If
            End If

        End If

    Next i

    '-----------------------------------------
    ' READ DESTINATION LOOKUP KEYS
    ' Ehds column J, rows 9:473
    '-----------------------------------------

    destinationKeys = wsDest.Range("J9:J473").value2

    ReDim outputData(1 To 465, 1 To 1)

    '-----------------------------------------
    ' MATCH AND PREPARE OUTPUT
    '-----------------------------------------

    matchedCount = 0

    For i = 1 To 465

        If Not IsError(destinationKeys(i, 1)) Then

            key = Trim$(CStr(destinationKeys(i, 1)))

            If Len(key) > 0 Then

                If dict.Exists(key) Then
                    outputData(i, 1) = dict(key)
                    sourceRow = CLng(dictSourceRow(key))

                    If Not IsBlankImportedValue(outputData(i, 1)) Then
                        matchedCount = matchedCount + 1
                        logEntries.Add Array( _
                            wsSource.Cells(sourceRow, "B").value, _
                            wsDest.Cells(i + 8, "J").value, _
                            outputData(i, 1), _
                            "PMS-01!R" & sourceRow, _
                            "Ehds!" & targetRange.Cells(i, 1).Address(False, False))
                    Else
                        blankSkipped = blankSkipped + 1
                    End If
                Else
                    outputData(i, 1) = Empty
                    unmatchedCount = unmatchedCount + 1
                End If

            Else
                outputData(i, 1) = Empty
            End If

        Else
            outputData(i, 1) = Empty
        End If

    Next i

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
    ' IMPORT VALUES ONLY
    ' Write only nonblank source values. Blank/unmatched source values
    ' remain truly empty; genuine numeric zero is still imported.
    '-----------------------------------------

    For i = 1 To 465
        If Not IsBlankImportedValue(outputData(i, 1)) Then
            targetRange.Cells(i, 1).value2 = outputData(i, 1)
        End If
    Next i

    'Display imported values with 2 decimals
    targetRange.NumberFormat = "0.00"

    '-----------------------------------------
    ' CLOSE SOURCE IF OPENED BY MACRO
    '-----------------------------------------

    If sourceOpenedByMacro Then
        wbSource.Close SaveChanges:=False
        Set wbSource = Nothing
    End If

    '-----------------------------------------
    ' SUCCESS
    '-----------------------------------------

    If matchedCount > 0 Then
        WriteContractorOperationLog ThisWorkbook, logEntries, "EHD Data Entry"
        logStatus = "Detailed report opened in Operation Log sheet."
    Else
        logStatus = "No values were pasted; no log entries were created."
    End If

    EHD_ExecutedThisSession = True

    MsgBox "EHD Daily Data Entry completed successfully!" & _
           vbCrLf & vbCrLf & _
           "Destination sheet: Ehds" & vbCrLf & _
           "Destination range: " & targetRange.Address(False, False) & vbCrLf & _
           "Source sheet: PMS-01" & vbCrLf & _
           "Lookup column: B" & vbCrLf & _
           "Imported values column: R" & vbCrLf & _
           "Values pasted: " & matchedCount & vbCrLf & _
           "Blank source values skipped: " & blankSkipped & vbCrLf & _
           "Unmatched Activity IDs: " & unmatchedCount & vbCrLf & vbCrLf & _
           logStatus, _
           vbInformation, "EHD Import Completed"

    GoTo CleanExit

'-----------------------------------------
' ERROR HANDLING
'-----------------------------------------

ErrorHandler:

    errorMessage = Err.Description

    On Error Resume Next

    If sourceOpenedByMacro Then
        If Not wbSource Is Nothing Then
            wbSource.Close SaveChanges:=False
        End If
    End If

    On Error GoTo 0

    MsgBox "EHD Daily Data Entry encountered an error:" & _
           vbCrLf & vbCrLf & errorMessage & _
           vbCrLf & vbCrLf & _
           "Please check the workbook before trying again.", _
           vbCritical, "EHD Import Error"

    GoTo CleanExit

CleanExit:

    If settingsChanged Then
        Application.screenUpdating = oldScreenUpdating
        Application.enableEvents = oldEnableEvents
        Application.calculation = oldCalculation
    End If

End Sub


'=========================================
' FIND SEQUENCE COLUMN
'=========================================

Private Function FindSequenceColumn(ByVal searchRange As Range, _
                                    ByVal targetValue As Variant) As Long

    Dim cell As Range

    For Each cell In searchRange.Cells

        If Not IsError(cell.value2) Then

            If StrComp(Trim$(CStr(cell.value2)), _
                       Trim$(CStr(targetValue)), vbTextCompare) = 0 Then

                FindSequenceColumn = cell.Column
                Exit Function

            End If

        End If

    Next cell

    FindSequenceColumn = 0

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


'==========================================
' OPERATION LOG - APPEND SUCCESSFUL IMPORTS
'==========================================

Private Sub WriteContractorOperationLog( _
    ByVal wb As Workbook, _
    ByVal entries As Collection, _
    ByVal operationName As String)

    Dim wsLog As Worksheet
    Dim nextRow As Long
    Dim data() As Variant
    Dim item As Variant
    Dim i As Long

    If entries Is Nothing Then Exit Sub
    If entries.Count = 0 Then Exit Sub

    On Error Resume Next
    Set wsLog = wb.Worksheets("Operation Log")
    On Error GoTo 0

    If wsLog Is Nothing Then
        Set wsLog = wb.Worksheets.Add(After:=wb.Worksheets(wb.Worksheets.Count))
        wsLog.Name = "Operation Log"
    End If

    If Len(CStr(wsLog.Cells(1, 1).value)) = 0 Then
        wsLog.Cells(1, 1).value = "Run Date/Time"
        wsLog.Cells(1, 2).value = "Operation"
        wsLog.Cells(1, 3).value = "Source Activity ID"
        wsLog.Cells(1, 4).value = "Destination Activity ID"
        wsLog.Cells(1, 5).value = "Pasted Value"
        wsLog.Cells(1, 6).value = "Source Cell"
        wsLog.Cells(1, 7).value = "Destination Cell"
        With wsLog.Range("A1:G1")
            .Font.Bold = True
            .Font.Color = RGB(255, 255, 255)
            .Interior.Color = RGB(31, 78, 121)
            .AutoFilter
        End With
    End If

    nextRow = wsLog.Cells(wsLog.Rows.Count, "A").End(xlUp).Row + 1
    ReDim data(1 To entries.Count, 1 To 7)

    For i = 1 To entries.Count
        item = entries(i)
        data(i, 1) = Now
        data(i, 2) = operationName
        data(i, 3) = item(0)
        data(i, 4) = item(1)
        data(i, 5) = item(2)
        data(i, 6) = item(3)
        data(i, 7) = item(4)
    Next i

    wsLog.Cells(nextRow, 1).Resize(entries.Count, 7).value = data
    wsLog.Range(wsLog.Cells(nextRow, 1), _
                wsLog.Cells(nextRow + entries.Count - 1, 1)).NumberFormat = "yyyy-mm-dd hh:mm:ss"
    wsLog.Range(wsLog.Cells(nextRow, 5), _
                wsLog.Cells(nextRow + entries.Count - 1, 5)).NumberFormat = "0.00"
    wsLog.Columns("A:G").AutoFit
    wsLog.Activate
    wsLog.Cells(nextRow, 1).Select

End Sub
