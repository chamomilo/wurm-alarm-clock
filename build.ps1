param(
    [string]$UpdaterReference = (Join-Path $PSScriptRoot '..\updater'),
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$ClientLibDir,
    [string]$JavaFxJar = $env:WURM_JFX,
    [string]$UiKitReference = 'C:\projects\interface items'
)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) { throw 'Set JAVA_HOME to a JDK 8 installation or pass -JavaHome.' }
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
if ($JavaFxJar) { $env:WURM_JFX = $JavaFxJar }
$referenceRoot = (Resolve-Path -LiteralPath $UpdaterReference).Path
$arguments = @('--offline','--no-daemon',"-PchamomiloUpdaterReference=$referenceRoot",'clean','build','dist')
$arguments += "-PupdaterBuildDirectory=$(Join-Path $PSScriptRoot 'build\updater-reference')"
$arguments += "-PupdaterDistributionDirectory=$(Join-Path $PSScriptRoot 'build\updater-reference\dist')"
if ($ClientLibDir) { $arguments += "-PwurmClientLibDir=$ClientLibDir" }
if ($JavaFxJar) { $arguments += "-PwurmJavaFxJar=$JavaFxJar" }
Push-Location $PSScriptRoot
try {
    & (Join-Path $referenceRoot 'gradlew.bat') @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Alarm Clock build failed' }
    $uiVerifier = Join-Path $UiKitReference 'tools\Verify-UiKitEmbedding.ps1'
    if (Test-Path -LiteralPath $uiVerifier) {
        $uiKitJar = Join-Path $referenceRoot 'dependencies\chamomilo-ui-0.4.11.jar'
        & $uiVerifier -ModJar (Join-Path $PSScriptRoot 'build\libs\alarmclock.jar') -ReferenceJar $uiKitJar
    }
    $modVersion = (Select-String -LiteralPath (Join-Path $PSScriptRoot 'mods\alarmclock.properties') -Pattern '^version=(.+)$').Matches.Groups[1].Value
    & (Join-Path $referenceRoot 'tools\Verify-ModUpdater.ps1') -ModZip (Join-Path $PSScriptRoot "dist\alarmclock-$modVersion.zip") -ReferenceRoot $referenceRoot
} finally { Pop-Location }
