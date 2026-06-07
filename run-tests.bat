@echo off
REM ============================================================
REM DRS-Initial - JUnit Test Suite Runner (Windows)
REM
REM Double-click this file to run all 95 unit tests.
REM Expected outcome:
REM   Tests run: 95, Failures: 0, Errors: 0, Skipped: 0
REM   BUILD SUCCESS
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

cd /d "%~dp0"

echo ============================================================
echo  DRS-Initial - JUnit Test Suite
echo  Fabian Roberto Guzman (12287570) - COIT20258 Assessment 2
echo ============================================================
echo.

REM 1) Try Maven from PATH first
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Using Maven from PATH.
    goto run
)

REM 2) Fall back to Maven bundled with Apache NetBeans
set "NB_HOME=C:\Program Files\Apache NetBeans"
set "NB_MAVEN=%NB_HOME%\java\maven\bin\mvn.cmd"
set "NB_JDK=%NB_HOME%\jdk"

if exist "%NB_MAVEN%" (
    echo [INFO] Using Maven bundled with Apache NetBeans.
    set "PATH=%NB_HOME%\java\maven\bin;%NB_JDK%\bin;%PATH%"
    if exist "%NB_JDK%\bin\java.exe" set "JAVA_HOME=%NB_JDK%"
    goto run
)

echo [ERROR] Could not find Maven or a JDK on this machine.
echo.
echo Please install ONE of:
echo   - Apache Maven (https://maven.apache.org/) + JDK 17+ on PATH
echo   - Apache NetBeans (https://netbeans.apache.org/)
echo.
pause
exit /b 1

:run
echo [INFO] Running the JUnit test suite...
echo [INFO] 14 *Spec.java classes, 83 @Test + 2 @ParameterizedTest = 95 executions
echo.
call mvn clean test

echo.
echo ============================================================
echo  Test run complete. Press any key to close this window.
echo ============================================================
pause >nul

endlocal
