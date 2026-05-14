@echo off
setlocal

cd /d "%~dp0.."

call compile.bat
if errorlevel 1 exit /b 1

call one.bat -b 1 settings\eetle_ratio_00_60.txt
if errorlevel 1 exit /b 1

call one.bat -b 1 settings\eetle_ratio_10_60.txt
if errorlevel 1 exit /b 1

call one.bat -b 1 settings\eetle_ratio_20_60.txt
if errorlevel 1 exit /b 1

call one.bat -b 1 settings\eetle_ratio_30_60.txt
if errorlevel 1 exit /b 1

call one.bat -b 1 settings\eetle_ratio_40_60.txt
if errorlevel 1 exit /b 1

endlocal
