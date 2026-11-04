Attribute VB_Name = "Module4"
Option Explicit

Private SOI_HasRunThisSession As Boolean

Public Sub Daily_Data_Entry_SOI()

    Dim wbDest As Workbook
    Dim wbSrc As Workbook

    Dim wsSOL1 As Worksheet
    Dim wsSOL2 As Worksheet
    Dim wsPMS As Worksheet
    Dim wsPMSExtra As Worksheet

    Dim selectedFile As Variant
    Dim sourceOpenedHere As Boolean

    Dim colSOL1 As Long
    Dim colSOL2 As Long

    Dim sequenceSOL1 As Variant
    Dim sequenceSOL2 As Variant

    Dim dateSOL1 As String
    Dim dateSOL2 As String

    Dim dictPMS As Object
    Dim dictExtra As Object
    Dim dictPMSRow As Object
    Dim dictExtraRow As Object
    Dim logEntries As Collection
    Dim sourceRow As Long
    Dim pastedCount As Long
    Dim blankSkipped As Long
    Dim unmatchedCount As Long
    Dim logStatus As String

    Dim r As Long
    Dim lastRow As Long
    Dim key As String

    Dim occupied As String
    Dim oldScreen As Boolean
    Dim oldEvents As Boolean
    Dim oldCalc As XlCalculation
    Dim settingsSaved As Boolean

    Dim errText As String

    '======================================
    ' PREVENT RUNNING TWICE IN ONE SESSION
    '======================================

    If SOI_HasRunThisSession Then

        MsgBox "SOI macro has already been run in this Excel session.", _
               vbExclamation, "SOI Daily Data Entry"

        Exit Sub

    End If

    If MsgBox("Run SOI Daily Data Entry?" & vbCrLf & _
              "Please make sure destination cells are empty.", _
              vbYesNo + vbQuestion + vbDefaultButton2, _
              "SOI Update") <> vbYes Then Exit Sub

    '======================================
    ' SELECT SOURCE WORKBOOK
    '======================================

    selectedFile = Application.GetOpenFilename( _
        "Excel Workbooks (*.xlsb;*.xlsx),*.xlsb;*.xlsx", , _
        "Select SOLEH Daily Report")

    If VarType(selectedFile) = vbBoolean Then Exit Sub

    On Error GoTo ErrorHandler

    Set wbDest = ThisWorkbook

    oldScreen = Application.screenUpdating
    oldEvents = Application.enableEvents
    oldCalc = Application.calculation
    settingsSaved = True

    Application.screenUpdating = False
    Application.enableEvents = False
    Application.calculation = xlCalculationManual

    '======================================
    ' OPEN SOURCE WORKBOOK
    '======================================

    Set wbSrc = GetOpenSOIWorkbook(CStr(selectedFile))

    If wbSrc Is Nothing Then

        Set wbSrc = Workbooks.Open( _
            Filename:=CStr(selectedFile), _
            ReadOnly:=True, _
            UpdateLinks:=False)

        sourceOpenedHere = True

    End If

    Set wsPMS = wbSrc.Worksheets("PMS (9)")
    Set wsPMSExtra = wbSrc.Worksheets("PMS EXTRA")

    Set wsSOL1 = wbDest.Worksheets("SOL1")
    Set wsSOL2 = wbDest.Worksheets("SOL2")

    '======================================
    ' SOL1: FIND SEQUENCE NUMBER IN ROW 5
    '======================================

    sequenceSOL1 = wsSOL1.Range("AT3").value

    colSOL1 = FindSOISequenceColumn( _
        wsSOL1, sequenceSOL1, 5, 47)

    If colSOL1 = 0 Then

        Err.Raise vbObjectError + 301, _
                  "SOL1", _
                  "Sequence number in SOL1!AT3 was not found in row 5."

    End If

    dateSOL1 = wsSOL1.Cells(6, colSOL1).Text

    '======================================
    ' SOL2: FIND SEQUENCE NUMBER IN ROW 5
    '======================================

    sequenceSOL2 = wsSOL2.Range("AT3").value

    colSOL2 = FindSOISequenceColumn( _
        wsSOL2, sequenceSOL2, 5, 47)

    If colSOL2 = 0 Then

        Err.Raise vbObjectError + 302, _
                  "SOL2", _
                  "Sequence number in SOL2!AT3 was not found in row 5."

    End If

    dateSOL2 = wsSOL2.Cells(6, colSOL2).Text

    '======================================
    ' CREATE LOOKUP DICTIONARIES
    '======================================

    Set dictPMS = CreateObject("Scripting.Dictionary")
    Set dictExtra = CreateObject("Scripting.Dictionary")
    Set dictPMSRow = CreateObject("Scripting.Dictionary")
    Set dictExtraRow = CreateObject("Scripting.Dictionary")
    Set logEntries = New Collection

    dictPMS.CompareMode = vbTextCompare
    dictExtra.CompareMode = vbTextCompare
    dictPMSRow.CompareMode = vbTextCompare
    dictExtraRow.CompareMode = vbTextCompare

    '--------------------------------------
    ' SOURCE: PMS (9)
    ' Column B = lookup key
    ' Column AC = returned value
    '--------------------------------------

    lastRow = wsPMS.Cells(wsPMS.Rows.Count, "B").End(xlUp).Row

    For r = 1 To lastRow

        key = NormalizeSOIKey(wsPMS.Cells(r, "B").value)

        If Len(key) > 0 Then

            If Not dictPMS.Exists(key) Then
                dictPMS.Add key, wsPMS.Cells(r, "AC").value
                dictPMSRow.Add key, r
            End If

        End If

    Next r

    '--------------------------------------
    ' SOURCE: PMS EXTRA
    ' Column B = lookup key
    ' Column AH = returned value
    '--------------------------------------

    lastRow = wsPMSExtra.Cells(wsPMSExtra.Rows.Count, "B").End(xlUp).Row

    For r = 1 To lastRow

        key = NormalizeSOIKey(wsPMSExtra.Cells(r, "B").value)

        If Len(key) > 0 Then

            If Not dictExtra.Exists(key) Then
                dictExtra.Add key, wsPMSExtra.Cells(r, "AH").value
                dictExtraRow.Add key, r
            End If

        End If

    Next r

    '======================================
    ' CHECK ALL DESTINATION CELLS
    ' BEFORE IMPORTING ANY DATA
    '======================================

    occupied = ""

    ' SOL1: rows 9 to 1253

    For r = 9 To 1253

        If CellHasSOIContent(wsSOL1.Cells(r, colSOL1)) Then

            occupied = occupied & _
                "SOL1!" & _
                wsSOL1.Cells(r, colSOL1).Address(False, False) & _
                vbCrLf

        End If

    Next r

    ' SOL2: rows 9 to 716

    For r = 9 To 716

        If CellHasSOIContent(wsSOL2.Cells(r, colSOL2)) Then

            occupied = occupied & _
                "SOL2!" & _
                wsSOL2.Cells(r, colSOL2).Address(False, False) & _
                vbCrLf

        End If

    Next r

    If Len(occupied) > 0 Then

        Err.Raise vbObjectError + 303, _
                  "Destination Check", _
                  "Import cancelled. Destination cells already contain data." & _
                  vbCrLf & vbCrLf & _
                  "Occupied cells:" & vbCrLf & occupied

    End If

    '======================================
    ' IMPORT SOL1
    '======================================

    ' SOL1 column J = lookup key
    ' PMS (9) column B = lookup key
    ' PMS (9) column AC = returned value
    ' Destination = matching sequence column
    ' Rows = 9 to 1253

    For r = 9 To 1253

        key = NormalizeSOIKey(wsSOL1.Cells(r, "J").value)

        If Len(key) > 0 Then

            If dictPMS.Exists(key) Then

                sourceRow = CLng(dictPMSRow(key))

                If Not IsBlankImportedValue(dictPMS(key)) Then
                    wsSOL1.Cells(r, colSOL1).value = dictPMS(key)
                    wsSOL1.Cells(r, colSOL1).NumberFormat = "0.00"
                    logEntries.Add Array( _
                        wsPMS.Cells(sourceRow, "B").value, _
                        wsSOL1.Cells(r, "J").value, _
                        dictPMS(key), _
                        "PMS (9)!AC" & sourceRow, _
                        "SOL1!" & wsSOL1.Cells(r, colSOL1).Address(False, False))
                    pastedCount = pastedCount + 1
                Else
                    blankSkipped = blankSkipped + 1
                End If

            Else
                unmatchedCount = unmatchedCount + 1
            End If

        End If

    Next r

    '======================================
    ' IMPORT SOL2
    '======================================

    ' SOL2 column J = lookup key
    ' PMS EXTRA column B = lookup key
    ' PMS EXTRA column AH = returned value
    ' Destination = matching sequence column
    ' Rows = 9 to 716

    For r = 9 To 716

        key = NormalizeSOIKey(wsSOL2.Cells(r, "J").value)

        If Len(key) > 0 Then

            If dictExtra.Exists(key) Then

                sourceRow = CLng(dictExtraRow(key))

                If Not IsBlankImportedValue(dictExtra(key)) Then
                    wsSOL2.Cells(r, colSOL2).value = dictExtra(key)
                    wsSOL2.Cells(r, colSOL2).NumberFormat = "0.00"
                    logEntries.Add Array( _
                        wsPMSExtra.Cells(sourceRow, "B").value, _
                        wsSOL2.Cells(r, "J").value, _
                        dictExtra(key), _
                        "PMS EXTRA!AH" & sourceRow, _
                        "SOL2!" & wsSOL2.Cells(r, colSOL2).Address(False, False))
                    pastedCount = pastedCount + 1
                Else
                    blankSkipped = blankSkipped + 1
                End If

            Else
                unmatchedCount = unmatchedCount + 1
            End If

        End If

    Next r

    '======================================
    ' SUCCESS
    '======================================

    If pastedCount > 0 Then
        WriteContractorOperationLog wbDest, logEntries, "SOI Data Entry"
        logStatus = "Detailed report opened in Operation Log sheet."
    Else
        logStatus = "No values were pasted; no log entries were created."
    End If

    SOI_HasRunThisSession = True

    If sourceOpenedHere Then

        wbSrc.Close SaveChanges:=False
        sourceOpenedHere = False

    End If

    RestoreSOISettings oldScreen, oldEvents, oldCalc
    settingsSaved = False

    MsgBox "SOI Daily Data Entry completed successfully." & _
           vbCrLf & vbCrLf & _
           "Values pasted: " & pastedCount & _
           vbCrLf & _
           "Blank source values skipped: " & blankSkipped & _
           vbCrLf & _
           "Unmatched Activity IDs: " & unmatchedCount & _
           vbCrLf & vbCrLf & _
           logStatus & _
           vbCrLf & _
           "SOL1 sequence: " & CStr(sequenceSOL1) & _
           vbCrLf & _
           "SOL1 date: " & dateSOL1 & _
           vbCrLf & _
           "SOL2 sequence: " & CStr(sequenceSOL2) & _
           vbCrLf & _
           "SOL2 date: " & dateSOL2, _
           vbInformation, "SOI Update"

    Exit Sub

ErrorHandler:

    errText = Err.Description

    On Error Resume Next

    If sourceOpenedHere Then
        wbSrc.Close SaveChanges:=False
    End If

    If settingsSaved Then
        RestoreSOISettings oldScreen, oldEvents, oldCalc
    End If

    On Error GoTo 0

    MsgBox "SOI update stopped:" & vbCrLf & vbCrLf & _
           errText & vbCrLf & vbCrLf & _
           "Check the workbook before trying again.", _
           vbCritical, "SOI Update"

End Sub


'==========================================
' FIND OPEN SOURCE WORKBOOK
'==========================================

Private Function GetOpenSOIWorkbook( _
    ByVal fullPath As String) As Workbook

    Dim wb As Workbook

    For Each wb In Application.Workbooks

        If StrComp(wb.FullName, fullPath, vbTextCompare) = 0 Then

            Set GetOpenSOIWorkbook = wb
            Exit Function

        End If

    Next wb

End Function


'==========================================
' FIND SEQUENCE NUMBER IN ROW 5
'==========================================

Private Function FindSOISequenceColumn( _
    ByVal ws As Worksheet, _
    ByVal targetNumber As Variant, _
    ByVal headerRow As Long, _
    ByVal firstColumn As Long) As Long

    Dim lastCol As Long
    Dim c As Long

    If IsError(targetNumber) Then Exit Function
    If Not IsNumeric(targetNumber) Then Exit Function

    lastCol = ws.Cells(headerRow, ws.Columns.Count).End(xlToLeft).Column

    For c = firstColumn To lastCol

        If Not IsError(ws.Cells(headerRow, c).value) Then

            If IsNumeric(ws.Cells(headerRow, c).value) Then

                If CDbl(ws.Cells(headerRow, c).value) = _
                   CDbl(targetNumber) Then

                    FindSOISequenceColumn = c
                    Exit Function

                End If

            End If

        End If

    Next c

End Function


'==========================================
' NORMALIZE LOOKUP KEYS
'==========================================

Private Function NormalizeSOIKey(ByVal value As Variant) As String

    If IsError(value) Or IsEmpty(value) Then Exit Function

    NormalizeSOIKey = Trim$(CStr(value))

End Function


'==========================================
' CHECK DESTINATION CELL CONTENT
'==========================================

Private Function CellHasSOIContent(ByVal c As Range) As Boolean

    If IsError(c.value) Then

        CellHasSOIContent = True

    Else

        CellHasSOIContent = (Len(CStr(c.value2)) > 0)

    End If

End Function


'==========================================
' RESTORE EXCEL SETTINGS
'==========================================

Private Sub RestoreSOISettings( _
    ByVal screenUpdating As Boolean, _
    ByVal enableEvents As Boolean, _
    ByVal calculation As XlCalculation)

    On Error Resume Next

    Application.calculation = calculation
    Application.enableEvents = enableEvents
    Application.screenUpdating = screenUpdating

    On Error GoTo 0

End Sub




'==========================================
' CHECK WHETHER A SOURCE VALUE IS BLANK
'==========================================

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
