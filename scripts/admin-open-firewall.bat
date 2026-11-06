@echo off
rem ============================================================
rem  ADMIN LAPTOP: right-click > Run as administrator.
rem  Lets the student laptop reach MySQL on port 3306.
rem ============================================================
netsh advfirewall firewall add rule name="UE CompLab MySQL 3306" dir=in action=allow protocol=TCP localport=3306
echo.
echo This laptop's IP address (put it in db.host on the STUDENT laptop):
ipconfig | findstr /i "IPv4"
pause
