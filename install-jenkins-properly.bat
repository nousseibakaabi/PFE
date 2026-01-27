@echo off
echo ========================================
echo    PROPER JENKINS INSTALLATION - PORT 8088
echo ========================================
echo.

REM Check Java
echo [1/8] Checking Java installation...
where java
java -version
if errorlevel 1 (
    echo ERROR: Java not found!
    echo Please install Java 17 from: https://adoptium.net/
    pause
    exit /b 1
)
echo.

REM Kill everything on port 8088
echo [2/8] Clearing port 8088...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8088') do (
    echo Killing process %%a using port 8088...
    taskkill /F /PID %%a 2>nul
)
echo.

REM Create temporary directory
echo [3/8] Creating installation directory...
set INSTALL_DIR=C:\JenkinsNew
if exist "%INSTALL_DIR%" rmdir /S /Q "%INSTALL_DIR%"
mkdir "%INSTALL_DIR%"
cd /d "%INSTALL_DIR%"
echo.

REM Download Jenkins WAR file
echo [4/8] Downloading Jenkins...
powershell -Command "Invoke-WebRequest -Uri 'https://get.jenkins.io/war-stable/latest/jenkins.war' -OutFile 'jenkins.war'"
echo.

REM Create proper jenkins.xml
echo [5/8] Creating service configuration...
echo ^<?xml version="1.0" encoding="UTF-8"?^> > jenkins.xml
echo ^<service^> >> jenkins.xml
echo   ^<id^>Jenkins^</id^> >> jenkins.xml
echo   ^<name^>Jenkins^</name^> >> jenkins.xml
echo   ^<description^>Jenkins CI/CD Server^</description^> >> jenkins.xml
echo   ^<env name="JENKINS_HOME" value="%INSTALL_DIR%"/^> >> jenkins.xml
echo   ^<env name="JENKINS_PORT" value="8088"/^> >> jenkins.xml
echo   ^<executable^>%JAVA_HOME%\bin\java.exe^</executable^> >> jenkins.xml
echo   ^<arguments^>-Xrs -Xmx1024m -Dhudson.lifecycle=hudson.lifecycle.WindowsServiceLifecycle -jar "%INSTALL_DIR%\jenkins.war" --httpPort=8088 --webroot="%INSTALL_DIR%\war"^</arguments^> >> jenkins.xml
echo   ^<logmode^>rotate^</logmode^> >> jenkins.xml
echo   ^<onfailure action="restart"/^> >> jenkins.xml
echo ^</service^> >> jenkins.xml
echo.

REM Install as Windows Service
echo [6/8] Installing as Windows Service...
REM Download and install with NSSM (better than built-in service)
powershell -Command "Invoke-WebRequest -Uri 'https://nssm.cc/release/nssm-2.24.zip' -OutFile 'nssm.zip'"
powershell -Command "Expand-Archive -Path nssm.zip -DestinationPath ."

REM Install service
nssm-2.24\win64\nssm.exe install Jenkins "%JAVA_HOME%\bin\java.exe" "-Xmx1024m -jar \"%INSTALL_DIR%\jenkins.war\" --httpPort=8088"
nssm-2.24\win64\nssm.exe set Jenkins AppDirectory "%INSTALL_DIR%"
nssm-2.24\win64\nssm.exe set Jenkins AppStdout "%INSTALL_DIR%\jenkins.log"
nssm-2.24\win64\nssm.exe set Jenkins AppStderr "%INSTALL_DIR%\jenkins-error.log"
nssm-2.24\win64\nssm.exe set Jenkins Start SERVICE_AUTO_START
echo.

REM Start service
echo [7/8] Starting Jenkins service...
net start Jenkins
echo.

REM Wait and test
echo [8/8] Testing Jenkins...
timeout /t 10 /nobreak
echo Checking if Jenkins is running...
netstat -ano | findstr :8088
echo.
if errorlevel 1 (
    echo Jenkins might take a moment to start...
    timeout /t 10 /nobreak
    netstat -ano | findstr :8088
)

echo.
echo ========================================
echo    INSTALLATION COMPLETE
echo ========================================
echo.
echo Jenkins Directory: %INSTALL_DIR%
echo Jenkins URL: http://localhost:8088
echo.
echo If Jenkins is running, access it in browser.
echo If not, check logs in: %INSTALL_DIR%
echo.
echo To start Jenkins manually (if service fails):
echo cd /d "%INSTALL_DIR%"
echo java -jar jenkins.war --httpPort=8088
echo.
pause