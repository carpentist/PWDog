param(
    [Parameter(Mandatory = $true)][ValidateSet("availability", "verify")][string]$Action,
    [string]$Message = "Unlock PWDog"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Runtime.WindowsRuntime

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq "AsTask" -and
    $_.GetParameters().Count -eq 1 -and
    $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]

function Await-WinRt($WinRtTask, $ResultType) {
    $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)
    $netTask = $asTask.Invoke($null, @($WinRtTask))
    $netTask.Wait(-1) | Out-Null
    $netTask.Result
}

[Windows.Security.Credentials.UI.UserConsentVerifier, Windows.Security.Credentials.UI, ContentType = WindowsRuntime] | Out-Null

if ($Action -eq "availability") {
    $result = Await-WinRt `
        ([Windows.Security.Credentials.UI.UserConsentVerifier]::CheckAvailabilityAsync()) `
        ([Windows.Security.Credentials.UI.UserConsentVerifierAvailability])
    Write-Output $result
    exit 0
}

$result = Await-WinRt `
    ([Windows.Security.Credentials.UI.UserConsentVerifier]::RequestVerificationAsync($Message)) `
    ([Windows.Security.Credentials.UI.UserConsentVerificationResult])

if ($result -eq [Windows.Security.Credentials.UI.UserConsentVerificationResult]::Verified) {
    exit 0
}

exit 2
