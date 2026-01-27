@echo off
echo ========================================
echo    FRESH SONARQUBE INSTALL - PORT 9000
echo ========================================
echo.

echo [1] Stopping everything on ports 9000-9005...
for /L %%i in (9000,1,9005) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :%%i') do (
        taskkill /F /PID %%a 2>nul
    )
)

echo [2] Setting Java 17...
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
java -version
echo.

echo [3] Cleaning old SonarQube...
if exist "C:\SonarQube" (
    echo Deleting old SonarQube...
    rmdir /S /Q "C:\SonarQube" 2>nul
)
mkdir "C:\SonarQube"
cd /d "C:\SonarQube"
echo.

echo [4] Downloading SonarQube...
if not exist sonarqube.zip (
    powershell -Command "Invoke-WebRequest -Uri 'https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.3.0.82913.zip' -OutFile 'sonarqube.zip'"
)

echo [5] Extracting...
if not exist sonarqube (
    powershell -Command "Expand-Archive -Path sonarqube.zip -DestinationPath ."
    for /d %%i in (sonarqube-*) do ren "%%i" sonarqube
)
echo.

echo [6] Creating minimal configuration...
(
    echo sonar.web.host=0.0.0.0
    echo sonar.web.port=9000
    echo sonar.web.context=/
    echo sonar.embeddedDatabase.port=9092
    echo sonar.search.port=9001
) > sonarqube\conf\sonar.properties
echo.

echo [7] Starting SonarQube...
cd sonarqube\bin\windows-x86-64
echo.
echo ========================================
echo    SONARQUBE STARTING ON PORT 9000
echo ========================================
echo.
echo FIRST START TAKES 3-5 MINUTES!
echo Please be patient...
echo.
echo Watch the console for "SonarQube is operational"
echo Then access: http://localhost:9000
echo Default: admin/admin
echo.
echo To stop: Press Ctrl+C
echo ========================================
echo.
StartSonar.bat
pause