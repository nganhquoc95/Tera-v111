@echo off
@title Lidium Server Console
set CLASSPATH=.;dist\Lidium.jar;lib\*;lib\graaljs\*
echo Starting Lidium Server...
echo Press Ctrl+C to stop the server gracefully
java -server -Dnet.sf.odinms.wzpath=wz\ server.Start
if %errorlevel% neq 0 (
    echo Server stopped with exit code %errorlevel%
) else (
    echo Server stopped normally
)
pause