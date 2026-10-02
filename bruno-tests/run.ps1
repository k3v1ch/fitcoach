param(
    [ValidateSet('public', 'e2e')][string]$Suite = 'public',
    [string]$Environment = 'Local'
)
$ErrorActionPreference = 'Stop'
$collectionName = if ($Suite -eq 'public') { 'automatic-public' } else { 'automatic-e2e' }
$collectionPath = Join-Path $PSScriptRoot $collectionName
$environmentPath = Join-Path $collectionPath "environments\$Environment.bru"
if (-not (Test-Path -LiteralPath $environmentPath)) {
    throw "Окружение не найдено: $environmentPath. Скопируйте Local.bru в Private.bru и заполните значения."
}
$localCli = Join-Path $PSScriptRoot 'node_modules\@usebruno\cli\bin\bru.js'
$runArguments = @('run', 'requests', '--env', $Environment, '--bail',
    '--reporter-junit', "reports\$Suite.xml", '--reporter-html', "reports\$Suite.html",
    '--reporter-skip-all-headers', '--reporter-skip-body')
Push-Location $collectionPath
try {
    New-Item -ItemType Directory -Force -Path reports | Out-Null
    if (Test-Path -LiteralPath $localCli) { & node $localCli @runArguments }
    elseif (Get-Command bru -ErrorAction SilentlyContinue) { & bru @runArguments }
    else { throw 'Bruno CLI не найден. Выполните npm install в папке bruno.' }
    $runExitCode = $LASTEXITCODE
} finally { Pop-Location }
exit $runExitCode
