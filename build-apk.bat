@echo off
setlocal
title MarinaraAndroid APK Builder

echo ==========================================
echo       MarinaraAndroid APK Builder
echo ==========================================
echo.

set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "ANDROID_SDK=C:\Users\danilov.sv\AppData\Local\Android\Sdk"
set "GRADLE_HOME=D:\gradle-8.10.2"

echo Java:
echo %JAVA_HOME%
echo.
echo Android SDK:
echo %ANDROID_SDK%
echo.
echo Gradle:
echo %GRADLE_HOME%
echo.

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java 17 not found:
    echo "%JAVA_HOME%\bin\java.exe"
    pause
    exit /b 1
)

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    echo ERROR: Gradle not found:
    echo "%GRADLE_HOME%\bin\gradle.bat"
    pause
    exit /b 1
)

if not exist "%ANDROID_SDK%" (
    echo ERROR: Android SDK not found:
    echo "%ANDROID_SDK%"
    pause
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%ANDROID_SDK%\platform-tools;%GRADLE_HOME%\bin;%PATH%"
set "ANDROID_HOME=%ANDROID_SDK%"
set "ANDROID_SDK_ROOT=%ANDROID_SDK%"

echo Java version:
"%JAVA_HOME%\bin\java.exe" -version
echo.

echo ==========================================
echo           BUILDING APK
echo ==========================================
echo.

call "%GRADLE_HOME%\bin\gradle.bat" :app:assembleDebug --no-daemon --refresh-dependencies

if errorlevel 1 (
    echo.
    echo ==========================================
    echo             BUILD FAILED
    echo ==========================================
    echo.
    pause
    exit /b 1
)

set "APK=%~dp0app\build\outputs\apk\debug\app-debug.apk"

echo.
echo ==========================================
echo          BUILD SUCCESSFUL!
echo ==========================================
echo.

if exist "%APK%" (
    echo APK:
    echo %APK%
    echo.
    explorer "%~dp0app\build\outputs\apk\debug"
) else (
    echo ERROR: APK was not created.
)

echo.
pause
