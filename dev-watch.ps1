$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = 'src'
$watcher.IncludeSubdirectories = $true
$watcher.Filter = '*.java'
$watcher.EnableRaisingEvents = $true

$action = {
    $path = $Event.SourceEventArgs.FullPath
    $changeType = $Event.SourceEventArgs.ChangeType
    Write-Host "File $path $changeType, rebuilding..."
    & '.\build.bat' 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host 'Rebuild successful, classes reloaded automatically'
    } else {
        Write-Host 'Rebuild failed'
    }
}

Register-ObjectEvent $watcher 'Changed' -Action $action
Register-ObjectEvent $watcher 'Created' -Action $action
Register-ObjectEvent $watcher 'Deleted' -Action $action
Register-ObjectEvent $watcher 'Renamed' -Action $action

# Start the server in background
$serverProcess = Start-Process -FilePath ".\launch.bat" -ArgumentList "debug" -NoNewWindow -PassThru

Write-Host "Server started in background (PID: $($serverProcess.Id)). Watching for file changes..."
Write-Host "Press Ctrl+C to stop"

# Keep the script running
try {
    while ($true) { Start-Sleep 1 }
} finally {
    Write-Host "Stopping server..."
    Stop-Process -Id $serverProcess.Id -ErrorAction SilentlyContinue
}