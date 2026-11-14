@echo off
rem ============================================================
rem  STUDENT LAPTOP: run once, signed in to the account that should
rem  be the kiosk (e.g. "Lab"). Starts the kiosk IMMEDIATELY every
rem  time this Windows user signs in. No admin rights needed.
rem ============================================================
set "HERE=%~dp0"
set "DIR=%LOCALAPPDATA%\UE-CompLab"
set "VBS=%DIR%\launch-kiosk.vbs"
if not exist "%DIR%" mkdir "%DIR%"

rem Small launcher that starts start-student.bat without a black console window
(
  echo Set sh = CreateObject("WScript.Shell"^)
  echo sh.Run """%HERE%start-student.bat""", 0, False
) > "%VBS%"

rem 1) Windows "Run" list: starts right at sign-in (earlier than the Startup folder)
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "UE-CompLab-Kiosk" /t REG_SZ /d "wscript.exe \"%VBS%\"" /f >nul

rem 2) Turn off the built-in delay Windows adds before starting sign-in programs
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\Serialize" /v StartupDelayInMSec /t REG_DWORD /d 0 /f >nul
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\Serialize" /v WaitForIdleState /t REG_DWORD /d 0 /f >nul

rem 3) Remove the old Startup-folder launcher so the kiosk does not start twice
del "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\UE-CompLab-Kiosk.vbs" 2>nul

echo Installed. The kiosk will start as soon as this Windows user signs in.
echo (Kiosk folder: %HERE%)
pause
