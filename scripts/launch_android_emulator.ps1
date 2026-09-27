<#
.SYNOPSIS
    Automated launcher for NotesAlltogether Android client on Android Emulator.
.DESCRIPTION
    Checks Android SDK and ADB, starts the Pixel_8_Pro_API_35 AVD if not already running,
    waits for the Android OS to fully boot, builds and installs the debug APK, and launches the app.
#>

[CmdletBinding()]
param(
    [string]$AvdName = "Pixel_8_Pro_API_35",
    [string]$SdkDir = "C:\Users\rokhl\AppData\Local\Android\Sdk",
    [switch]$SkipBuild,
    [switch]$WipeData
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   NotesAlltogether - Android Automated Launch Script      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Resolve Android SDK Paths
$AdbPath = Join-Path $SdkDir "platform-tools\adb.exe"
$EmulatorPath = Join-Path $SdkDir "emulator\emulator.exe"

if (-not (Test-Path $AdbPath)) {
    Write-Error "ADB not found at: $AdbPath. Please verify Android SDK path."
}
if (-not (Test-Path $EmulatorPath)) {
    Write-Error "Emulator binary not found at: $EmulatorPath."
}

Write-Host "[1/5] Checking connected Android devices..." -ForegroundColor Yellow
$devicesOutput = & $AdbPath devices
$runningDevice = $devicesOutput | Where-Object { $_ -match "emulator-\d+\s+device" }

if (-not $runningDevice) {
    Write-Host "[2/5] No running emulator detected. Launching AVD: '$AvdName'..." -ForegroundColor Yellow
    
    $emulatorArgs = @("-avd", $AvdName, "-netdelay", "none", "-netspeed", "full")
    if ($WipeData) {
        $emulatorArgs += "-wipe-data"
    }

    Start-Process -FilePath $EmulatorPath -ArgumentList $emulatorArgs -NoNewWindow
    Write-Host "Emulator process started in background. Waiting for device to appear..." -ForegroundColor Gray
    
    & $AdbPath wait-for-device
    Write-Host "Device detected via ADB. Waiting for system boot to complete..." -ForegroundColor Yellow
    
    $bootCompleted = $false
    $timeoutSeconds = 180
    $startTime = Get-Date

    while (-not $bootCompleted) {
        if (((Get-Date) - $startTime).TotalSeconds -gt $timeoutSeconds) {
            Write-Error "Timed out waiting for emulator to complete boot."
        }
        Start-Sleep -Seconds 3
        $bootProp = (& $AdbPath shell getprop sys.boot_completed) -replace '\s',''
        if ($bootProp -eq "1") {
            $bootCompleted = $true
        } else {
            Write-Host "  ...booting Android system (sys.boot_completed=$bootProp)..." -ForegroundColor Gray
        }
    }
    Write-Host "Android system boot completed successfully!" -ForegroundColor Green
} else {
    Write-Host "[2/5] Active emulator already running: $runningDevice" -ForegroundColor Green
}

# 2. Build and Install APK
$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $ProjectRoot

try {
    if (-not $SkipBuild) {
        Write-Host "[3/5] Building and installing debug APK (:composeApp:installDebug)..." -ForegroundColor Yellow
        $gradlew = Join-Path $ProjectRoot "gradlew.bat"
        & $gradlew :composeApp:installDebug
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Gradle build or installDebug failed with exit code $LASTEXITCODE."
        }
        Write-Host "APK installed successfully." -ForegroundColor Green
    } else {
        Write-Host "[3/5] Skipping build as requested (-SkipBuild)." -ForegroundColor Gray
    }

    # 3. Grant permissions & Launch Main Activity
    Write-Host "[4/5] Launching NotesAlltogether MainActivity..." -ForegroundColor Yellow
    $PackageName = "com.notes.client"
    $MainActivity = "com.notes.client.MainActivity"
    
    & $AdbPath shell am start -n "$PackageName/$MainActivity"
    
    Write-Host "[5/5] Application launched on emulator screen!" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "   App is running on $AvdName                              " -ForegroundColor Cyan
    Write-Host "   Local storage: /data/user/0/$PackageName/files/        " -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan
} finally {
    Pop-Location
}
