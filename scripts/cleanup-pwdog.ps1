param(
    [switch]$DeleteData,
    [switch]$Yes
)

$ErrorActionPreference = "Stop"

$runKey = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run"
$runValue = Get-ItemProperty -Path $runKey -Name PWDog -ErrorAction SilentlyContinue
if ($runValue) {
    Remove-ItemProperty -Path $runKey -Name PWDog
    Write-Host "Removed PWDog autostart entry."
}

$dataDir = if ($env:PWDOG_DATA_DIR) {
    $env:PWDOG_DATA_DIR
} else {
    Join-Path $env:LOCALAPPDATA "PWDogData"
}

if (-not (Test-Path -LiteralPath $dataDir)) {
    Write-Host "PWDog data directory not found: $dataDir"
    exit 0
}

if (-not $DeleteData) {
    Write-Host "PWDog data kept at: $dataDir"
    Write-Host "Run again with -DeleteData to remove it permanently."
    exit 0
}

$target = (Resolve-Path -LiteralPath $dataDir).Path
$root = [System.IO.Path]::GetPathRoot($target)
$homeDir = [System.IO.Path]::GetFullPath($HOME)
if (-not $target -or $target -eq $root -or $target -eq $homeDir) {
    throw "Refusing to delete unsafe path: $target"
}

if (-not $Yes) {
    $answer = Read-Host "This permanently deletes all PWDog accounts and vaults at $target. Type DELETE to continue"
    if ($answer -ne "DELETE") {
        Write-Host "Cancelled."
        exit 1
    }
}

Remove-Item -LiteralPath $target -Recurse -Force
Write-Host "Deleted PWDog data: $target"
