@echo off
REM ============================================================
REM DRS-Enhanced - Server Launcher (Windows)
REM
REM Double-click this file to start the DRS server.
REM No NetBeans, no PowerShell, no typing commands required.
REM
REM Prerequisites:
REM   * A running MySQL 8 server. Connection settings come from
REM     db.properties (edit db.user / db.password to match your
REM     install) or from the DB_URL / DB_USER / DB_PASSWORD
REM     environment variables, which take precedence.
REM   * ONE of:
REM       - JDK 17+ and Maven on PATH (standard developer setup), OR
REM       - Apache NetBeans installed at the default location
REM         (C:\Program Files\Apache NetBeans), which bundles both.
REM
REM The server applies the database schema and seed, creates the
REM default admin and the demo accounts (see README), and listens
REM on port 5599. Keep this window open; close it (or Ctrl+C) to
REM stop the server. Start clients with run-app.bat.
REM
REM Author: Fabian Roberto Guzman (12287570)
REM Unit:   COIT20258 Software Engineering - Assessment 3
REM ============================================================

setlocal

REM Move to the project folder (where this .bat lives)
cd /d "%~dp0"

echo ============================================================
echo  DRS-Enhanced - Server
echo  Fabian Roberto Guzman (12287570) - COIT20258 Assessment 3
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
echo [INFO] Starting the DRS server (port 5599)...
echo [INFO] Keep this window open while clients are connected.
echo.
call mvn -q compile exec:java -Dexec.mainClass=edu.cqu.drs.server.DrsServerLauncher

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] The server exited with an error code %ERRORLEVEL%.
    echo If the message above mentions 'Access denied' or a connection
    echo failure, check that MySQL is running and that db.properties
    echo ^(or the DB_USER / DB_PASSWORD environment variables^) carry
    echo valid credentials.
    echo.
    pause
)

endlocal
