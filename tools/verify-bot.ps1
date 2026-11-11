<#
.SYNOPSIS
    Standalone compile check for the bot module (cn.mythicland.npclib.bot.* + BotManager).

.DESCRIPTION
    The full :core:compileJava stops on unrelated, still-broken modules, so this script
    compiles only the bot sources against the Paper server jar to get a clean signal.

    Requires the local NMS compile classpath (see docs/BUILDING.md):
        <repo>/.gradle/paper-nms/versions/*/paper-*.jar
        <repo>/.gradle/paper-nms/libraries/**/*.jar

.PARAMETER Out
    Output directory for compiled classes. Defaults to a temp folder.

.PARAMETER PaperNmsRoot
    Directory holding versions/ and libraries/. Defaults to <repo>/.gradle/paper-nms.

.EXAMPLE
    pwsh -File tools/verify-bot.ps1
#>
param(
    [string]$Out = "",
    [string]$PaperNmsRoot = ""
)

$ErrorActionPreference = "Continue"
$repo = Split-Path -Parent $PSScriptRoot
if (-not $PaperNmsRoot) { $PaperNmsRoot = Join-Path $repo ".gradle/paper-nms" }

function Find-JdkTool([string]$Name) {
    $candidates = @()
    $onPath = Get-Command $Name -ErrorAction SilentlyContinue
    if ($onPath) { $candidates += $onPath.Source }
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME "bin/$Name.exe") }
    $gradleJdks = Join-Path $env:USERPROFILE ".gradle/jdks"
    if (Test-Path $gradleJdks) {
        $candidates += Get-ChildItem $gradleJdks -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { Join-Path $_.FullName "bin/$Name.exe" }
    }
    # Paper 1.21.8 class files are Java 21 (major 65); older JDKs cannot read them.
    foreach ($candidate in $candidates) {
        if (-not (Test-Path $candidate)) { continue }
        $version = (& $candidate -version 2>&1 | Out-String)
        if ($version -match '(\d+)\.\d+') {
            if ([int]$matches[1] -ge 21) { return $candidate }
        }
    }
    throw "Cannot locate a JDK 21 $Name. Install a JDK 21 and set JAVA_HOME."
}

$javac = Find-JdkTool "javac"

$serverJars = @(Get-ChildItem $PaperNmsRoot -Filter "*.jar" -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match "[\\/]versions[\\/]" } | ForEach-Object { $_.FullName })
if ($serverJars.Count -eq 0) {
    throw "No server jar under $PaperNmsRoot\versions. See docs/BUILDING.md (NMS compile classpath)."
}

$classpath = New-Object System.Collections.Generic.List[string]
$serverJars | ForEach-Object { $classpath.Add($_) }
Get-ChildItem (Join-Path $PaperNmsRoot "libraries") -Filter "*.jar" -Recurse -ErrorAction SilentlyContinue |
    ForEach-Object { $classpath.Add($_.FullName) }
Get-ChildItem (Join-Path $repo "libs") -Filter "*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch "^(citizens|Citizens)" } |
    ForEach-Object { $classpath.Add($_.FullName) }
Get-ChildItem (Join-Path $repo "packLib") -Filter "*.jar" -ErrorAction SilentlyContinue |
    ForEach-Object { $classpath.Add($_.FullName) }

$sourcePath = @(
    (Join-Path $repo "core/src/main/java"),
    (Join-Path $repo "vendor/mythic-framework/core/src/main/java"),
    (Join-Path $repo "vendor/mythic-framework/bukkit/src/main/java")
) -join ";"

$sources = @()
$sources += Get-ChildItem (Join-Path $repo "core/src/main/java/cn/mythicland/npclib/bot") -Recurse -Filter *.java |
    ForEach-Object { $_.FullName }
$sources += Join-Path $repo "core/src/main/java/cn/mythicland/npclib/BotManager.java"

if (-not $Out) { $Out = Join-Path ([System.IO.Path]::GetTempPath()) "pit-bot-verify" }
if (Test-Path $Out) { Remove-Item -Recurse -Force $Out }
New-Item -ItemType Directory -Force -Path $Out | Out-Null
$argFile = Join-Path $Out "sources.txt"
[System.IO.File]::WriteAllLines($argFile, [string[]]$sources, (New-Object System.Text.UTF8Encoding($false)))

Write-Output ("bot sources: " + $sources.Count)
& $javac "-J-Duser.language=en" "-J-Duser.country=US" -encoding UTF-8 -proc:none -d $Out `
    -cp ($classpath -join ";") -sourcepath $sourcePath "@$argFile"
$exit = $LASTEXITCODE
$classes = @(Get-ChildItem -Recurse $Out -Filter *.class -ErrorAction SilentlyContinue).Count
Write-Output ("javac exit: " + $exit)
Write-Output ("classes: " + $classes)
if ($exit -ne 0) { exit $exit }
