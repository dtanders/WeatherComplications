# Gradle wrapper for PowerShell
# Reads gradle/wrapper/gradle-wrapper.properties to locate the correct Gradle version.

$ErrorActionPreference = 'Stop'
$propsPath = Join-Path $PSScriptRoot 'gradle\wrapper\gradle-wrapper.properties'

$props = @{}
foreach ($line in Get-Content $propsPath) {
    if ($line -match '^([^#=]+)=(.*)$') {
        $props[$Matches[1].Trim()] = $Matches[2].Trim() -replace '\\:', ':'
    }
}

$distUrl  = $props['distributionUrl']
$distName = [IO.Path]::GetFileNameWithoutExtension($distUrl.Split('/')[-1])  # e.g. gradle-9.4.1-bin
$gradleUserHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
$distsRoot = Join-Path $gradleUserHome 'wrapper\dists' $distName

$hashDir    = Get-ChildItem -Path $distsRoot -Directory -ErrorAction Stop | Select-Object -First 1
$innerName  = $distName -replace '-(bin|all)$', ''   # gradle-9.4.1-bin -> gradle-9.4.1
$gradleExe  = Join-Path $hashDir.FullName "$innerName\bin\gradle.bat"
if (-not (Test-Path $gradleExe)) {
    $gradleExe = Join-Path $hashDir.FullName "$innerName\bin\gradle"
}

& $gradleExe @args
exit $LASTEXITCODE
