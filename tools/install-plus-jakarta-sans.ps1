# PulihGo / typography asset installer
# Downloads official static TTF fonts to YOUR local repo; the provided ZIP contains no font files.
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$repo = Split-Path -Parent $PSScriptRoot
$destination = Join-Path $repo 'app\src\main\res\font'
$licenseDir = Join-Path $repo 'licenses'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
New-Item -ItemType Directory -Force -Path $licenseDir | Out-Null

$source = 'https://raw.githubusercontent.com/tokotype/PlusJakartaSans/master/fonts/ttf'
$fonts = @(
    @{ Source = 'PlusJakartaSans-Regular.ttf';   Target = 'plus_jakarta_sans_regular.ttf' },
    @{ Source = 'PlusJakartaSans-Medium.ttf';    Target = 'plus_jakarta_sans_medium.ttf' },
    @{ Source = 'PlusJakartaSans-SemiBold.ttf';  Target = 'plus_jakarta_sans_semibold.ttf' },
    @{ Source = 'PlusJakartaSans-Bold.ttf';      Target = 'plus_jakarta_sans_bold.ttf' },
    @{ Source = 'PlusJakartaSans-ExtraBold.ttf'; Target = 'plus_jakarta_sans_extrabold.ttf' }
)

foreach ($font in $fonts) {
    $out = Join-Path $destination $font.Target
    if ((Test-Path $out) -and ((Get-Item $out).Length -gt 10000)) {
        Write-Host "Already available: $($font.Target)"
        continue
    }
    $uri = "$source/$($font.Source)"
    Write-Host "Downloading $($font.Target) ..."
    Invoke-WebRequest -Uri $uri -OutFile $out -UseBasicParsing
    $bytes = [System.IO.File]::ReadAllBytes($out)
    if ($bytes.Length -lt 10000 -or -not (
        ($bytes[0] -eq 0 -and $bytes[1] -eq 1 -and $bytes[2] -eq 0 -and $bytes[3] -eq 0) -or
        ($bytes[0] -eq 79 -and $bytes[1] -eq 84 -and $bytes[2] -eq 84 -and $bytes[3] -eq 79)
    )) {
        Remove-Item -LiteralPath $out -Force -ErrorAction SilentlyContinue
        throw "Invalid TTF/OpenType file downloaded: $($font.Target)"
    }
}

$licensePath = Join-Path $licenseDir 'PlusJakartaSans-OFL.txt'
if (-not (Test-Path $licensePath)) {
    Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/tokotype/PlusJakartaSans/master/OFL.txt' -OutFile $licensePath -UseBasicParsing
}
Write-Host "OK: 5 Plus Jakarta Sans font files downloaded to $destination"
Write-Host "License: $licensePath"
Write-Host 'Now open Android Studio and Gradle Sync / build.'
