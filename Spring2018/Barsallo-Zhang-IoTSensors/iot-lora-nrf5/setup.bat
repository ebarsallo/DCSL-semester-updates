@echo off
echo Creating symlinks 
echo.

:: Cread junction or handlinks to SDK folders. The SDK path must be accepted as an argument
:: USAGE: setup.bat path/to/sdk

set SDK_PATH=%1

:: Check if the %SDK_PATH% was set
IF "%SDK_PATH%"=="" GOTO end

:: Check if the directory exits
IF NOT EXIST %SDK_PATH% GOTO end

:links
MKLINK /j sdk_components "%SDK_PATH%\components"
MKLINK /j sdk_external "%SDK_PATH%\external"
MKLINK /j sdk_svd "%SDK_PATH%\svd"

echo Symlinks created!
goto:eof

:end
echo ERROR: 
echo    The path was not set or does not exists. 
echo USAGE: 
echo    setup.bat path/to/sdk
goto:eof