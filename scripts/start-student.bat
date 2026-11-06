@echo off
rem ============================================================
rem  Starts the student kiosk and RESTARTS it if it is ever
rem  closed or killed. Only the secret admin exit (exit code 99)
rem  stops the loop.
rem ============================================================
cd /d "%~dp0"
:loop
javaw -jar comlab.jar student
if %errorlevel%==99 goto end
timeout /t 1 /nobreak >nul
goto loop
:end
