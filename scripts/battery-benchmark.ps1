param(
    [ValidateSet("baseline", "optimized")]
    [string] $RunName = "optimized",
    [int] $Minutes = 60,
    [string] $PackageName = "com.android.stepsync"
)

$ErrorActionPreference = "Stop"

$outputDir = Join-Path $PSScriptRoot "..\docs\battery-results"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$prefix = Join-Path $outputDir "$RunName-$timestamp"

adb shell dumpsys batterystats --reset
adb shell dumpsys battery unplug

Write-Host "Battery stats reset. Start StepSync tracking now."
Read-Host "Press Enter after tracking has started"
Write-Host "Waiting $Minutes minute(s) for the $RunName run..."
Start-Sleep -Seconds ($Minutes * 60)

adb shell dumpsys battery | Out-File "$prefix-battery.txt" -Encoding utf8
adb shell dumpsys batterystats $PackageName | Out-File "$prefix-batterystats.txt" -Encoding utf8
adb shell run-as $PackageName cat shared_prefs/step_sync_prefs.xml | Out-File "$prefix-step-sync-prefs.xml" -Encoding utf8
adb shell dumpsys battery reset

Write-Host "Saved results:"
Write-Host "$prefix-battery.txt"
Write-Host "$prefix-batterystats.txt"
Write-Host "$prefix-step-sync-prefs.xml"
