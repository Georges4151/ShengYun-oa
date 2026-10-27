::[Bat To Exe Converter]
::
::YAwzoRdxOk+EWAjk
::fBw5plQjdCqDJF+F4kszJxVrRQu+OmqsD7YT5qby7OXn
::YAwzuBVtJxjWCl3EqQJgSA==
::ZR4luwNxJguZRRnk
::Yhs/ulQjdF+5
::cxAkpRVqdFKZSzk=
::cBs/ulQjdF+5
::ZR41oxFsdFKZSDk=
::eBoioBt6dFKZSDk=
::cRo6pxp7LAbNWATEpCI=
::egkzugNsPRvcWATEpCI=
::dAsiuh18IRvcCxnZtBJQ
::cRYluBh/LU+EWAnk
::YxY4rhs+aU+IeA==
::cxY6rQJ7JhzQF1fEqQJhZksaHErXXA==
::ZQ05rAF9IBncCkqN+0xwdVsFAlTMbCXvZg==
::ZQ05rAF9IAHYFVzEqQK30+Dds9EH57E9y0R8
::eg0/rx1wNQPfEVWB+kM9LVsJDGQ=
::fBEirQZwNQPfEVWB+kM9LVsJDGQ=
::cRolqwZ3JBvQF1fEqQJQ
::dhA7uBVwLU+EWDk=
::YQ03rBFzNR3SWATElA==
::dhAmsQZ3MwfNWATEcZ7woM+Oy+BUXA==
::ZQ0/vhVqMQ3MEVWAtB9wSA==
::Zg8zqx1/OA3MEVWAtB9wSA==
::dhA7pRFwIByZRRnk
::Zh4grVQjdCqDJEqc9VEjIQhAcIN5x+JF0zDHMm82HqSCukh9
::YB416Ek+ZG8=
::
::
::978f952a14a936cc963da21a135fa983
@echo off
:: Set encoding to UTF-8
chcp 65001 > nul 2>&1

:: ======================== Configuration ========================
set JAR_FILE=app\ceams-1.0-SNAPSHOT.jar
set JAVA_OPTS=-Xms512m -Xmx1024m
:: set JAVA_HOME=C:\Program Files\Java\jdk17
:: ===============================================================

if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java.exe
)

if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found [%JAR_FILE%]
    pause
    exit /b 1
)

:: Start in background without console window
echo Starting [%JAR_FILE%] in background...
start "" /b "%JAVA_EXE%" %JAVA_OPTS% -jar "%JAR_FILE%"

:: Optional: Output startup logs to file
:: start "" /b "%JAVA_EXE%" %JAVA_OPTS% -jar "%JAR_FILE%" > app.log 2>&1

echo Startup completed!
timeout /t 2 /nobreak > nul