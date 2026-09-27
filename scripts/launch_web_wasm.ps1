<#
.SYNOPSIS
    Automated launcher for NotesAlltogether Compose Multiplatform Web/WasmJs app.
.DESCRIPTION
    Launches the WasmJs browser development server via Gradle and opens the default web browser.
#>

[CmdletBinding()]
param(
    [int]$Port = 8081,
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   NotesAlltogether - Web/Wasm Automated Launch Script    " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $ProjectRoot

try {
    Write-Host "[1/2] Launching Web/WasmJs Development Server (:composeApp:wasmJsBrowserDevelopmentRun)..." -ForegroundColor Yellow
    Write-Host "Press Ctrl+C to stop the development server when finished." -ForegroundColor Gray
    
    $gradlew = Join-Path $ProjectRoot "gradlew.bat"
    
    if (-not $NoBrowser) {
        # Open browser in parallel after brief delay
        Start-Job -ScriptBlock {
            param($url)
            Start-Sleep -Seconds 8
            Start-Process $url
        } -ArgumentList "http://localhost:$Port" | Out-Null
    }

    & $gradlew :composeApp:wasmJsBrowserDevelopmentRun
} finally {
    Pop-Location
}
