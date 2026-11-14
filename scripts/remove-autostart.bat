@echo off
rem Undo install-autostart.bat for this Windows user.
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "UE-CompLab-Kiosk" /f >nul 2>&1
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\Serialize" /f >nul 2>&1
del "%LOCALAPPDATA%\UE-CompLab\launch-kiosk.vbs" 2>nul
del "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\UE-CompLab-Kiosk.vbs" 2>nul
echo Auto-start removed.
pause
