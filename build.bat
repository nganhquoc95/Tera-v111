@echo off
echo Building Lidium Server...
if not exist build mkdir build
if not exist dist mkdir dist
if exist tmp/sources.txt del tmp/sources.txt
for /r src %%f in (*.java) do echo %%f >> tmp/sources.txt
javac -cp "lib\*" -d build @tmp/sources.txt
if %errorlevel% neq 0 (
    echo Compilation failed.
    exit /b 1
)
jar cfm dist\Lidium.jar manifest.txt -C build .
if %errorlevel% neq 0 (
    echo Packaging failed.
    exit /b 1
)
echo Build complete.