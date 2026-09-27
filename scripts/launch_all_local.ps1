<#
.SYNOPSIS
    Master Automated Orchestration Script for NotesAlltogether Local Deployment.
.DESCRIPTION
    Deploys and tests all system components on the local machine:
    1. Ktor Backend Server (Port 8080)
    2. Android Emulator (Pixel_8_Pro_API_35) with app installation
    3. Web/Wasm Client (Port 8081 / browser)
#>

[CmdletBinding()]
param(
    [ValidateSet("All", "Server", "Android", "Web")]
    [string]$Target = "All",
    [string]$AvdName = "Pixel_8_Pro_API_35"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Magenta
Write-Host "  NotesAlltogether - Master System Deployment Runner      " -ForegroundColor Magenta
Write-Host "==========================================================" -ForegroundColor Magenta

$ClientScripts = $PSScriptRoot
$ServerDir = Resolve-Path (Join-Path $PSScriptRoot "..\..\notesServer")
$ServerScript = Join-Path $ServerDir "scripts\launch_server.ps1"
$AndroidScript = Join-Path $ClientScripts "launch_android_emulator.ps1"
$WebScript = Join-Path $ClientScripts "launch_web_wasm.ps1"

# 1. Start Server
if ($Target -eq "All" -or $Target -eq "Server") {
    Write-Host "`n>>> [Step 1] Starting Ktor Backend Service..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$ServerScript`""
    Write-Host "Ktor server started in a new dedicated terminal window." -ForegroundColor Green
    Start-Sleep -Seconds 3
}

# 2. Start Android Emulator & App
if ($Target -eq "All" -or $Target -eq "Android") {
    Write-Host "`n>>> [Step 2] Deploying Android App to Emulator ($AvdName)..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$AndroidScript`"", "-AvdName", "`"$AvdName`""
    Write-Host "Android deployment process started in a new terminal window." -ForegroundColor Green
}

# 3. Start Web/Wasm Client
if ($Target -eq "All" -or $Target -eq "Web") {
    Write-Host "`n>>> [Step 3] Launching Web/WasmJs Client..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$WebScript`""
    Write-Host "Web client dev server launched in a new terminal window." -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Magenta
Write-Host "  All requested subsystems have been dispatched!         " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Magenta
