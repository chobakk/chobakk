Attribute VB_Name = "Module3"
Option Explicit

Private FRJ_HasRunThisSession As Boolean

Public Sub Daily_Data_Entry_FRJ()

    Dim wbDest As Workbook
    Dim wbSrc As Workbook

    Dim wsFRJP As Worksheet
    Dim wsFRJE As Worksheet
    Dim wsPMS As Worksheet
    Dim wsExtra As Worksheet

    Dim selectedFile As Variant
    Dim sourceOpenedHere As Boolean

    Dim colFRJP As Long
    Dim colFRJE As Long

    Dim sequenceNumber As Variant
    Dim frjpDate As String

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

    Dim lastRow As Long
    Dim r As Long
    Dim key As String

    Dim occupied As String
    Dim oldScreen As Boolean
    Dim oldEvents As Boolean
    Dim oldCalc As XlCalculation
    Dim settingsSaved As Boolean

    Dim errText As String

    '--------------------------------------
    ' PREVENT RUNNING TWICE IN ONE SESSION
    '--------------------------------------

    If FRJ_HasRunThisSession Then
        MsgBox "FRJ macro has already been run in this Excel session.", _
               vbExclamation, "FRJ Daily Data Entry"
        Exit Sub
    End If

    If MsgBox("Run FRJ Daily Data Entry?" & vbCrLf & _
              "Please make sure the destination cells are empty.", _
              vbYesNo + vbQuestion + vbDefaultButton2, _
              "FRJ Update") <> vbYes Then Exit Sub

    '--------------------------------------
    ' SELECT SOURCE FILE
    '--------------------------------------

    selectedFile = Application.GetOpenFilename( _
        "Excel Binary Workbook (*.xlsb),*.xlsb", , _
        "Select FRJ.xlsb")

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

    '--------------------------------------
    ' OPEN SOURCE WORKBOOK
    '--------------------------------------

    Set wbSrc = GetOpenWorkbook(CStr(selectedFile))

    If wbSrc Is Nothing Then

        Set wbSrc = Workbooks.Open( _
            Filename:=CStr(selectedFile), _
            ReadOnly:=True, _
            UpdateLinks:=False)

        sourceOpenedHere = True

    End If

    Set wsPMS = wbSrc.Worksheets("PMS")
    Set wsExtra = wbSrc.Worksheets("Extra Work")

    Set wsFRJP = wbDest.Worksheets("FRJP")
    Set wsFRJE = wbDest.Worksheets("FRJE")

    '--------------------------------------
    ' FRJP: FIND SEQUENCE NUMBER IN ROW 5
    '--------------------------------------

    sequenceNumber = wsFRJP.Range("AT3").value

    colFRJP = FindNumberColumn( _
        wsFRJP, sequenceNumber, 5, 47)

    If colFRJP = 0 Then

        Err.Raise vbObjectError + 201, _
                  "FRJP", _
                  "Sequence number in FRJP!AT3 was not found in row 5."

    End If

    ' Date is located directly below the matching sequence number.
    frjpDate = wsFRJP.Cells(6, colFRJP).Text

    '--------------------------------------
    ' FRJE: FIND DATE IN ROW 3
    '--------------------------------------

    colFRJE = FindDateColumn( _
        wsFRJE, wsFRJE.Range("K2").value, 3, 12)

    If colFRJE = 0 Then

        Err.Raise vbObjectError + 202, _
                  "FRJE", _
                  "Date in FRJE!K2 was not found in row 3."

    End If

    '--------------------------------------
    ' CREATE LOOKUP DICTIONARIES
    '--------------------------------------

    Set dictPMS = CreateObject("Scripting.Dictionary")
    Set dictExtra = CreateObject("Scripting.Dictionary")
    Set dictPMSRow = CreateObject("Scripting.Dictionary")
    Set dictExtraRow = CreateObject("Scripting.Dictionary")
    Set logEntries = New Collection

    dictPMS.CompareMode = vbTextCompare
    dictExtra.CompareMode = vbTextCompare
    dictPMSRow.CompareMode = vbTextCompare
    dictExtraRow.CompareMode = vbTextCompare

    ' PMS:
    ' Source column B = lookup key
    ' Source column U = returned value

    lastRow = wsPMS.Cells(wsPMS.Rows.Count, "B").End(xlUp).Row

    For r = 1 To lastRow

        key = NormalizeKey(wsPMS.Cells(r, "B").value)

        If Len(key) > 0 Then

            If Not dictPMS.Exists(key) Then
                dictPMS.Add key, wsPMS.Cells(r, "U").value
                dictPMSRow.Add key, r
            End If

        End If

    Next r

    ' Extra Work:
    ' Source column A = lookup key
    ' Source column C = returned value

    lastRow = wsExtra.Cells(wsExtra.Rows.Count, "A").End(xlUp).Row

    For r = 1 To lastRow

        key = NormalizeKey(wsExtra.Cells(r, "A").value)

        If Len(key) > 0 Then

            If Not dictExtra.Exists(key) Then
                dictExtra.Add key, wsExtra.Cells(r, "C").value
                dictExtraRow.Add key, r
            End If

        End If

    Next r

    '--------------------------------------
    ' CHECK DESTINATION CELLS BEFORE IMPORT
    '--------------------------------------

    occupied = ""

    ' FRJP destination column

    lastRow = wsFRJP.Cells(wsFRJP.Rows.Count, "J").End(xlUp).Row

    For r = 9 To lastRow

        If CellHasContent(wsFRJP.Cells(r, colFRJP)) Then

            occupied = occupied & _
                "FRJP!" & wsFRJP.Cells(r, colFRJP).Address(False, False) & _
                vbCrLf

        End If

    Next r

    ' FRJE destination column

    For r = 4 To 31

        If CellHasContent(wsFRJE.Cells(r, colFRJE)) Then

            occupied = occupied & _
                "FRJE!" & wsFRJE.Cells(r, colFRJE).Address(False, False) & _
                vbCrLf

        End If

    Next r

    If Len(occupied) > 0 Then

        Err.Raise vbObjectError + 203, _
                  "Destination Check", _
                  "Import cancelled. Destination cells already contain data." & _
                  vbCrLf & vbCrLf & _
                  "FRJP sequence: " & CStr(sequenceNumber) & vbCrLf & _
                  "FRJP date: " & frjpDate & vbCrLf & _
                  "Occupied cells:" & vbCrLf & occupied

    End If

    '======================================
    ' IMPORT FRJP
    '======================================

    ' Destination:
    ' FRJP column J = lookup key
    ' FRJ.xlsb / PMS column B = lookup key
    ' FRJ.xlsb / PMS column U = returned value
    ' Destination = matching sequence column
    ' Rows = 9 to last used row in column J

    lastRow = wsFRJP.Cells(wsFRJP.Rows.Count, "J").End(xlUp).Row

    For r = 9 To lastRow

        key = NormalizeKey(wsFRJP.Cells(r, "J").value)

        If Len(key) > 0 Then

            If dictPMS.Exists(key) Then

                sourceRow = CLng(dictPMSRow(key))

                If Not IsBlankImportedValue(dictPMS(key)) Then
                    wsFRJP.Cells(r, colFRJP).value = dictPMS(key)
                    wsFRJP.Cells(r, colFRJP).NumberFormat = "0.00"
                    logEntries.Add Array( _
                        wsPMS.Cells(sourceRow, "B").value, _
                        wsFRJP.Cells(r, "J").value, _
                        dictPMS(key), _
                        "PMS!U" & sourceRow, _
                        "FRJP!" & wsFRJP.Cells(r, colFRJP).Address(False, False))
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
    ' IMPORT FRJE
    '======================================

    ' Destination:
    ' FRJE column C = lookup key
    ' FRJ.xlsb / Extra Work column A = lookup key
    ' FRJ.xlsb / Extra Work column C = returned value
    ' Destination = matching date column
    ' Rows = 4 to 31

    For r = 4 To 31

        key = NormalizeKey(wsFRJE.Cells(r, "C").value)

        If Len(key) > 0 Then

            If dictExtra.Exists(key) Then

                sourceRow = CLng(dictExtraRow(key))

                If Not IsBlankImportedValue(dictExtra(key)) Then
                    wsFRJE.Cells(r, colFRJE).value = dictExtra(key)
                    wsFRJE.Cells(r, colFRJE).NumberFormat = "0.00"
                    logEntries.Add Array( _
                        wsExtra.Cells(sourceRow, "A").value, _
                        wsFRJE.Cells(r, "C").value, _
                        dictExtra(key), _
                        "Extra Work!C" & sourceRow, _
                        "FRJE!" & wsFRJE.Cells(r, colFRJE).Address(False, False))
                    pastedCount = pastedCount + 1
                Else
                    blankSkipped = blankSkipped + 1
                End If

            Else
                unmatchedCount = unmatchedCount + 1
            End If

        End If

    Next r

    '--------------------------------------
    ' SUCCESS
    '--------------------------------------

    If pastedCount > 0 Then
        WriteContractorOperationLog wbDest, logEntries, "FRJ Data Entry"
        logStatus = "Detailed report opened in Operation Log sheet."
    Else
        logStatus = "No values were pasted; no log entries were created."
    End If

    FRJ_HasRunThisSession = True

    If sourceOpenedHere Then
        wbSrc.Close SaveChanges:=False
        sourceOpenedHere = False
    End If

    RestoreFRJSettings oldScreen, oldEvents, oldCalc
    settingsSaved = False

    MsgBox "FRJ Daily Data Entry completed successfully." & _
           vbCrLf & vbCrLf & _
           "Values pasted: " & pastedCount & _
           vbCrLf & _
           "Blank source values skipped: " & blankSkipped & _
           vbCrLf & _
           "Unmatched Activity IDs: " & unmatchedCount & _
           vbCrLf & vbCrLf & _
           logStatus & _
           vbCrLf & _
           "FRJP sequence: " & CStr(sequenceNumber) & _
           vbCrLf & _
           "FRJP date: " & frjpDate & _
           vbCrLf & _
           "FRJE date: " & wsFRJE.Cells(3, colFRJE).Text, _
           vbInformation, "FRJ Update"

    Exit Sub

ErrorHandler:

    errText = Err.Description

    On Error Resume Next

    If sourceOpenedHere Then
        wbSrc.Close SaveChanges:=False
    End If

    If settingsSaved Then
        RestoreFRJSettings oldScreen, oldEvents, oldCalc
    End If

    On Error GoTo 0

    MsgBox "FRJ update stopped:" & vbCrLf & vbCrLf & _
           errText & vbCrLf & vbCrLf & _
           "Check the workbook before trying again.", _
           vbCritical, "FRJ Update"

End Sub


'==========================================
' FIND OPEN WORKBOOK
'==========================================

Private Function GetOpenWorkbook( _
    ByVal fullPath As String) As Workbook

    Dim wb As Workbook

    For Each wb In Application.Workbooks

        If StrComp(wb.FullName, fullPath, vbTextCompare) = 0 Then

            Set GetOpenWorkbook = wb
            Exit Function

        End If

    Next wb

End Function


'==========================================
' FIND SEQUENCE NUMBER IN ROW 5
'==========================================

Private Function FindNumberColumn( _
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

                    FindNumberColumn = c
                    Exit Function

                End If

            End If

        End If

    Next c

End Function


'==========================================
' FIND DATE IN HEADER ROW
'==========================================

Private Function FindDateColumn( _
    ByVal ws As Worksheet, _
    ByVal targetDate As Variant, _
    ByVal headerRow As Long, _
    ByVal firstColumn As Long) As Long

    Dim lastCol As Long
    Dim c As Long

    lastCol = ws.Cells(headerRow, ws.Columns.Count).End(xlToLeft).Column

    For c = firstColumn To lastCol

        If SameDate(ws.Cells(headerRow, c).value, targetDate) Then

            FindDateColumn = c
            Exit Function

        End If

    Next c

End Function


'==========================================
' COMPARE DATES
'==========================================

Private Function SameDate( _
    ByVal value1 As Variant, _
    ByVal value2 As Variant) As Boolean

    If IsError(value1) Or IsError(value2) Then Exit Function

    If Not IsDate(value1) Or Not IsDate(value2) Then Exit Function

    SameDate = (CLng(DateValue(CDate(value1))) = _
                CLng(DateValue(CDate(value2))))

End Function


'==========================================
' NORMALIZE LOOKUP KEYS
'==========================================

Private Function NormalizeKey(ByVal value As Variant) As String

    If IsError(value) Or IsEmpty(value) Then Exit Function

    NormalizeKey = Trim$(CStr(value))

End Function


'==========================================
' CHECK WHETHER CELL CONTAINS DATA
'==========================================

Private Function CellHasContent(ByVal c As Range) As Boolean

    If IsError(c.value) Then
        CellHasContent = True
    Else
        CellHasContent = (Len(CStr(c.value2)) > 0)
    End If

End Function


'==========================================
' RESTORE EXCEL SETTINGS
'==========================================

Private Sub RestoreFRJSettings( _
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
