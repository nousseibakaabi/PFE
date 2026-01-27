@echo off
echo ========================================
echo        NEXUS INSTALLATION
echo ========================================
echo.

REM Go to C:\Nexus
cd /d C:\Nexus

echo Step 1: Cleaning up...
rmdir /s /q nexus 2>nul
del nexus.zip 2>nul

echo Step 2: Downloading Nexus...
powershell -Command "wget 'https://download.sonatype.com/nexus/3/nexus-3.61.0-02-win64.zip' -OutFile 'nexus.zip'"

if not exist nexus.zip (
    echo Download failed! Trying alternative...
    powershell -Command "Invoke-WebRequest -Uri 'https://sonatype-download.global.ssl.fastly.net/repository/downloads-prod-group/3/nexus-3.61.0-02-win64.zip' -OutFile 'nexus.zip' -UseBasicParsing"
)

if not exist nexus.zip (
    echo ERROR: Cannot download Nexus!
    pause
    exit /b 1
)

echo Step 3: Extracting...
powershell -Command "Expand-Archive -Path 'nexus.zip' -DestinationPath '.' -Force"

echo Step 4: Finding extracted folder...
dir /ad

REM Look for nexus-* folder
set found=0
for /d %%i in (nexus-*) do (
    echo Found: %%i
    set found=1
    move "%%i" "nexus"
)

if %found%==0 (
    echo No nexus-* folder found! Checking contents...
    dir
    pause
    exit /b 1
)

echo Step 5: Verifying...
if exist "nexus\bin\nexus.exe" (
    echo ✓ nexus.exe found
) else (
    echo ✗ nexus.exe NOT found
    dir nexus
    pause
    exit /b 1
)

echo.
echo ========================================
echo        STARTING NEXUS
echo ========================================
echo.

cd nexus\bin
echo Starting Nexus...
echo This will take 3-5 minutes on first run...
echo.

nexus.exe /run

echo.
echo If successful, access at: http://localhost:8081
echo Admin password: C:\Nexus\sonatype-work\nexus3\admin.password
echo.
pause