@echo off
rem ============================================================
rem  Starts ComLab Student.exe as the kiosk and RESTARTS it if it
rem  is ever closed or killed. Only the secret admin exit
rem  (Ctrl+Shift+F12 + password, exit code 99) stops the loop.
rem  install-autostart.bat runs this file when Windows signs in.
rem ============================================================
cd /d "%~dp0"
:loop
start "" /wait "ComLab Student.exe"
if %errorlevel%==99 goto end
timeout /t 1 /nobreak >nul
goto loop
:end
