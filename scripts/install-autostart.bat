@echo off
rem ============================================================
rem  STUDENT LAPTOP: run once. Makes the kiosk start automatically
rem  every time this Windows user logs in (no console window).
rem ============================================================
set "HERE=%~dp0"
set "STARTUP=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup"
(
  echo Set sh = CreateObject("WScript.Shell"^)
  echo sh.Run """%HERE%start-student.bat""", 0, False
) > "%STARTUP%\UE-CompLab-Kiosk.vbs"
echo Installed. The kiosk will start the next time you sign in to Windows.
echo (File: %STARTUP%\UE-CompLab-Kiosk.vbs)
pause
