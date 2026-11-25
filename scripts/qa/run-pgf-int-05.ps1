param(
    [switch]$SkipAndroid,
    [switch]$SkipWeb
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).ProviderPath
$gradle = Join-Path $repoRoot 'gradlew.bat'
$web = Join-Path $repoRoot 'therapist-web'
$vitest = Join-Path $web 'node_modules\.bin\vitest.cmd'
$npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
try {
    if (-not $SkipAndroid) {
        if (-not (Test-Path -LiteralPath $gradle)) { throw 'Missing gradlew.bat' }
        Push-Location $repoRoot
        try {
            Write-Host '[PGF-INT-05] Running Android JVM tests and debug build...'
            & $gradle :app:testDebugUnitTest :app:assembleDebug
            if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
        } finally { Pop-Location }
    }
    if (-not $SkipWeb) {
        if (-not (Test-Path -LiteralPath $vitest)) { throw 'therapist-web/node_modules missing. Run npm ci inside therapist-web first.' }
        if ($null -eq $npm) { throw 'npm.cmd not found in PATH.' }
        Push-Location $web
        try {
            Write-Host '[PGF-INT-05] Running THR-05 scoped web tests...'
            & $vitest run src/lib/therapyPlan.test.ts src/lib/therapyPlanApi.test.ts
            if ($LASTEXITCODE -ne 0) { throw "Vitest failed with exit code $LASTEXITCODE" }
            Write-Host '[PGF-INT-05] Building therapist web...'
            & $npm.Source run build
            if ($LASTEXITCODE -ne 0) { throw "Web build failed with exit code $LASTEXITCODE" }
        } finally { Pop-Location }
    }
    Write-Host '[PGF-INT-05] ALL SELECTED AUTOMATED CHECKS PASSED.' -ForegroundColor Green
} catch {
    Write-Error $_
    exit 1
}
