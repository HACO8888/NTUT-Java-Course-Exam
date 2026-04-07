@echo off
title 大老二 - Big Two
java -jar "%~dp0big2.jar"
if %errorlevel% neq 0 (
    echo.
    echo [錯誤] 請確認已安裝 Java 11 或以上版本
    echo 下載網址: https://adoptium.net/
    pause
)
