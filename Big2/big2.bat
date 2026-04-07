@echo off
chcp 65001 >nul
title Big Two

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo Java not found. Please install Java 11 or later.
    echo Download: https://adoptium.net/
    pause
    exit /b 1
)

java -jar "%~dp0big2.jar"
if %errorlevel% neq 0 (
    echo Failed to launch. Make sure Java 11+ is installed.
    pause
)
