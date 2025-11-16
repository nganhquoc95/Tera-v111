@echo off
@title Lidium Server Console

if "%1"=="debug" (
    echo Starting Lidium Server in DEBUG mode...
    echo HotSwap enabled - attach debugger to port 5005
    echo Automatic hot reload enabled for class file changes
    echo Press Ctrl+C to stop the server gracefully
    set CLASSPATH=.;build;lib\*;lib\graaljs\*
    java -server -javaagent:lib/hotswap-agent.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Dnet.sf.odinms.wzpath=wz\ server.Start
) else (
    echo Starting Lidium Server...
    echo Use 'launch.bat debug' for hot-reload development mode
    echo Press Ctrl+C to stop the server gracefully
    set CLASSPATH=.;dist\Lidium.jar;lib\*;lib\graaljs\*
    java -server -Dnet.sf.odinms.wzpath=wz\ server.Start
)

if %errorlevel% neq 0 (
    echo Server stopped with exit code %errorlevel%
) else (
    echo Server stopped normally
)
pause