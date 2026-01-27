@echo off
echo ========================================
echo     STOPPING ALL SERVICES
echo ========================================
echo.

echo Stopping Jenkins...
taskkill /F /IM java.exe 2>nul | findstr /i "jenkins"
net stop Jenkins 2>nul

echo Stopping SonarQube...
taskkill /F /IM java.exe 2>nul | findstr /i "sonar"

echo Stopping Nexus...
net stop nexus 2>nul
taskkill /F /IM nexus.exe 2>nul

timeout /t 3 /nobreak >nul

echo.
echo Checking if services are stopped...
echo.
netstat -ano | findstr :8088 >nul && echo ✗ Jenkins still running || echo ✓ Jenkins stopped
netstat -ano | findstr :9000 >nul && echo ✗ SonarQube still running || echo ✓ SonarQube stopped
netstat -ano | findstr :8081 >nul && echo ✗ Nexus still running || echo ✓ Nexus stopped
echo.
pause