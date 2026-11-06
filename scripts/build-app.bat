@echo off
setlocal EnableDelayedExpansion
rem ============================================================
rem  Builds the Windows apps:
rem     dist\ComLab Student\ComLab Student.exe   (copy this folder to the student laptop / lab PCs)
rem     dist\ComLab Admin\ComLab Admin.exe       (copy this folder to the admin laptop)
rem  Java is bundled inside, so those PCs do not need Java installed.
rem
rem  Before running: build the jar in IntelliJ
rem     Maven panel - comlab-monitor - Lifecycle - package
rem ============================================================
cd /d "%~dp0.."

if not exist "target\comlab.jar" (
  echo [!] target\comlab.jar not found.
  echo     In IntelliJ open the Maven panel, then comlab-monitor - Lifecycle - package. Then run this again.
  pause & exit /b 1
)

rem ---- find jpackage (comes with JDK 17+) ----
set "JP="
for /f "delims=" %%P in ('where jpackage 2^>nul') do if not defined JP set "JP=%%P"
if not defined JP if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JP=%JAVA_HOME%\bin\jpackage.exe"
if not defined JP for /d %%D in ("%USERPROFILE%\.jdks\*") do if exist "%%D\bin\jpackage.exe" set "JP=%%D\bin\jpackage.exe"
if not defined JP for /d %%D in ("%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Microsoft\*") do if exist "%%D\bin\jpackage.exe" set "JP=%%D\bin\jpackage.exe"
if not defined JP (
  echo [!] jpackage was not found. Install a JDK 17 or newer, or set JAVA_HOME.
  pause & exit /b 1
)
echo Using: "!JP!"

set "OUT=dist"
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%\_input"
copy /y "target\comlab.jar" "%OUT%\_input\" >nul

echo.
echo [1/2] Building ComLab Student.exe ...
"!JP!" --type app-image --name "ComLab Student" --app-version 1.0 --vendor "UE College of Engineering" ^
  --description "UE CompLab student kiosk" --input "%OUT%\_input" --main-jar comlab.jar ^
  --main-class ph.ue.comlab.Launcher --arguments student --icon "scripts\icons\student.ico" ^
  --java-options "-Dfile.encoding=UTF-8" --dest "%OUT%"
if errorlevel 1 goto fail

echo [2/2] Building ComLab Admin.exe ...
"!JP!" --type app-image --name "ComLab Admin" --app-version 1.0 --vendor "UE College of Engineering" ^
  --description "UE CompLab admin console" --input "%OUT%\_input" --main-jar comlab.jar ^
  --main-class ph.ue.comlab.Launcher --arguments admin --icon "scripts\icons\admin.ico" ^
  --java-options "-Dfile.encoding=UTF-8" --dest "%OUT%"
if errorlevel 1 goto fail
rmdir /s /q "%OUT%\_input"

rem ---- each laptop's files go next to its .exe ----
set "S=%OUT%\ComLab Student"
set "A=%OUT%\ComLab Admin"
copy /y "config.properties" "%S%\" >nul
copy /y "config.properties" "%A%\" >nul
copy /y "scripts\app\start-student.bat" "%S%\" >nul
copy /y "scripts\install-autostart.bat" "%S%\" >nul
copy /y "scripts\remove-autostart.bat" "%S%\" >nul
copy /y "scripts\kiosk-lockdown.reg" "%S%\" >nul
copy /y "scripts\kiosk-unlock.reg" "%S%\" >nul
copy /y "scripts\admin-open-firewall.bat" "%A%\" >nul
copy /y "schema.sql" "%A%\" >nul

echo.
echo Done.
echo   %CD%\%S%\ComLab Student.exe
echo   %CD%\%A%\ComLab Admin.exe
echo Copy each folder to its laptop and edit config.properties next to the .exe.
explorer "%OUT%"
pause
exit /b 0

:fail
echo.
echo [!] jpackage failed - see the message above.
pause
exit /b 1
