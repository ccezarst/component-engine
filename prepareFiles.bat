@echo off
REM -- Launch the PowerShell script from the same folder as this .bat --
set SCRIPT="%~dp0genHashFiles.ps1"

REM -- Bypass policy to allow unsigned scripts, don't load your PowerShell profile --
PowerShell.exe -NoProfile -ExecutionPolicy Bypass -Command "& %SCRIPT%"

REM -- Pause so you can see the output before the window closes --
pause