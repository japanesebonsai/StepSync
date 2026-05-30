param(
    [int] $Minutes = 60,
    [string] $PackageName = "com.android.stepsync",
    [string] $BaselineApk = "C:\Users\Jacob\Documents\Codex\2026-05-30\my-project-stepsync-is-this-true\work\stepsync-baseline\app\build\outputs\apk\debug\app-debug.apk",
    [string] $OptimizedApk = "C:\Users\Jacob\code\stepsync\app\build\outputs\apk\debug\app-debug.apk"
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Run-StepSyncBatteryPass {
    param(
        [string] $RunName,
        [string] $ApkPath
    )

    if (!(Test-Path $ApkPath)) {
        throw "APK not found: $ApkPath"
    }

    Write-Host ""
    Write-Host "Installing $RunName APK..."
    adb install -r $ApkPath

    Write-Host "Opening StepSync. Sign in if needed, then start a recording."
    adb shell monkey -p $PackageName 1 | Out-Null

    & "$scriptDir\battery-benchmark.ps1" -RunName $RunName -Minutes $Minutes -PackageName $PackageName

    Write-Host "Stop and save the activity in StepSync before continuing."
    Read-Host "Press Enter when ready for the next run"
}

adb devices

Run-StepSyncBatteryPass -RunName "baseline" -ApkPath $BaselineApk
Run-StepSyncBatteryPass -RunName "optimized" -ApkPath $OptimizedApk

Write-Host ""
Write-Host "Comparison runs complete. Results are in docs\battery-results."
