@echo off
REM ============================================================
REM DRS-Initial - JavaFX Prototype Launcher (Windows)
REM
REM Double-click this file to start the application.
REM No NetBeans, no PowerShell, no typing commands required.
REM
REM Requires ONE of:
REM   * JDK 17+ and Maven on PATH (standard developer setup), OR
REM   * Apache NetBeans installed at the default location
REM     (C:\Program Files\Apache NetBeans), which bundles both.
REM
REM Author: Fabian Roberto Guzman (12287570)
REM Unit:   COIT20258 Software Engineering - Assessment 2
REM ============================================================

setlocal

REM Move to the project folder (where this .bat lives)
cd /d "%~dp0"

echo ============================================================
echo  DRS-Initial - Disaster Response System
echo  Fabian Roberto Guzman (12287570) - COIT20258 Assessment 2
echo ============================================================
echo.

REM 1) Try Maven from PATH first (standard developer setup)
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Using Maven from PATH.
    goto launch
)

REM 2) Fall back to Maven bundled with Apache NetBeans
set "NB_HOME=C:\Program Files\Apache NetBeans"
set "NB_MAVEN=%NB_HOME%\java\maven\bin\mvn.cmd"
set "NB_JDK=%NB_HOME%\jdk"

if exist "%NB_MAVEN%" (
    echo [INFO] Using Maven bundled with Apache NetBeans.
    set "PATH=%NB_HOME%\java\maven\bin;%NB_JDK%\bin;%PATH%"
    if exist "%NB_JDK%\bin\java.exe" set "JAVA_HOME=%NB_JDK%"
    goto launch
)

echo [ERROR] Could not find Maven or a JDK on this machine.
echo.
echo Please install ONE of:
echo   - Apache Maven (https://maven.apache.org/) on the system PATH, plus
echo     a Java JDK 17 or newer
echo   - Apache NetBeans (https://netbeans.apache.org/) which bundles both
echo.
echo Then double-click this file again.
echo.
pause
exit /b 1

:launch
echo [INFO] Launching the JavaFX application...
echo [INFO] (Close the application window to exit.)
echo.
call mvn -q javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] The application exited with an error code %ERRORLEVEL%.
    echo See the output above for details.
    echo.
    pause
)

endlocal
