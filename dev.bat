@echo off
@title Lidium Server Development Watcher

echo Starting Lidium Server with automatic hot reload...
echo Watching for source file changes in src\ directory
echo Press Ctrl+C to stop

powershell -ExecutionPolicy Bypass -File dev-watch.ps1