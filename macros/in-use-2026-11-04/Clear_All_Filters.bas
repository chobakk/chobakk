Attribute VB_Name = "Module5"
Option Explicit

Public Sub Clear_All_Filters()

    Dim ws As Worksheet
    Dim lo As ListObject
    Dim clearedCount As Long

    Dim oldScreen As Boolean
    Dim oldEvents As Boolean

    Dim errText As String

    If MsgBox("Clear all active filters in every worksheet?" & _
              vbCrLf & vbCrLf & _
              "Filter dropdown arrows will remain.", _
              vbYesNo + vbQuestion + vbDefaultButton2, _
              "Clear All Filters") <> vbYes Then Exit Sub

    On Error GoTo ErrorHandler

    oldScreen = Application.screenUpdating
    oldEvents = Application.enableEvents

    Application.screenUpdating = False
    Application.enableEvents = False

    For Each ws In ThisWorkbook.Worksheets

        ' Clear worksheet-level AutoFilter criteria.
        If ws.FilterMode Then
            ws.ShowAllData
            clearedCount = clearedCount + 1
        End If

        ' Clear filters inside Excel tables.
        For Each lo In ws.ListObjects

            If lo.ShowAutoFilter Then

                If lo.AutoFilter.FilterMode Then
                    lo.AutoFilter.ShowAllData
                    clearedCount = clearedCount + 1
                End If

            End If

        Next lo

    Next ws

    Application.enableEvents = oldEvents
    Application.screenUpdating = oldScreen

    MsgBox "Filter clearing completed." & vbCrLf & _
           "Active filter sets cleared: " & clearedCount, _
           vbInformation, "Clear All Filters"

    Exit Sub

ErrorHandler:

    errText = Err.Description

    Application.enableEvents = oldEvents
    Application.screenUpdating = oldScreen

    MsgBox "Could not clear all filters:" & vbCrLf & _
           errText, vbCritical, "Clear All Filters"

End Sub


