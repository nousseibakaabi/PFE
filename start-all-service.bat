@echo off
echo ========================================
echo     STARTING ALL SERVICES
echo ========================================
echo.

echo [1] Checking existing services...
echo.

echo --- Jenkins ---
netstat -ano | findstr :8088 >nul
if %errorLevel% == 0 (
    echo ✓ Jenkins already running on port 8088
) else (
    echo Starting Jenkins...
    if exist "C:\JenkinsNew\jenkins.war" (
        start "Jenkins" cmd /k "cd /d C:\JenkinsNew && java -jar jenkins.war --httpPort=8088"
    ) else (
        echo ✗ Jenkins not found at C:\JenkinsNew
    )
)
echo.

echo --- SonarQube ---
netstat -ano | findstr :9000 >nul
if %errorLevel% == 0 (
    echo ✓ SonarQube already running on port 9000
) else (
    echo Starting SonarQube...
    if exist "C:\SonarQube\sonarqube\bin\windows-x86-64\StartSonar.bat" (
        start "SonarQube" cmd /k "cd /d C:\SonarQube\sonarqube\bin\windows-x86-64 && StartSonar.bat"
    ) else (
        echo ✗ SonarQube not found at C:\SonarQube
    )
)
echo.

echo --- Nexus ---
netstat -ano | findstr :8081 >nul
if %errorLevel% == 0 (
    echo ✓ Nexus already running on port 8081
) else (
    echo Starting Nexus...
    if exist "C:\Nexus\nexus\bin\nexus.exe" (
        start "Nexus" cmd /k "cd /d C:\Nexus\nexus\bin && nexus.exe /run"
    ) else (
        echo ✗ Nexus not found at C:\Nexus\nexus\bin\nexus.exe
        echo Trying to start as service...
        sc query nexus >nul 2>&1 && (
            net start nexus
        ) || (
            echo Nexus service not installed
        )
    )
)
echo.

timeout /t 5 /nobreak >nul

echo ========================================
echo     SERVICES STATUS
echo ========================================
echo.

echo Checking ports...
echo Port 8088 (Jenkins): 
netstat -ano | findstr :8088 >nul && echo ✓ RUNNING || echo ✗ NOT RUNNING

echo Port 9000 (SonarQube): 
netstat -ano | findstr :9000 >nul && echo ✓ RUNNING || echo ✗ NOT RUNNING

echo Port 8081 (Nexus): 
netstat -ano | findstr :8081 >nul && echo ✓ RUNNING || echo ✗ NOT RUNNING
echo.

echo ========================================
echo     ACCESS URLs
echo ========================================
echo.
echo Jenkins:     http://localhost:8088
echo SonarQube:   http://localhost:9000
echo Nexus:       http://localhost:8081
echo.
echo Note: Services may take 1-3 minutes to fully start
echo.

echo ========================================
echo     QUICK CHECK COMMANDS
echo ========================================
echo.
echo To check status:    netstat -ano ^| findstr :8088 ^|^| :9000 ^|^| :8081
echo To stop all:        taskkill /F /IM java.exe
echo.

REM Optionally open all URLs in browser
set /p OPEN="Open all in browser? (y/n): "
if /i "%OPEN%"=="y" (
    start http://localhost:8088
    start http://localhost:9000
    start http://localhost:8081
)

echo.
pause