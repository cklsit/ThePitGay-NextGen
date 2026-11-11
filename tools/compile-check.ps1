<#
.SYNOPSIS
    Full Java compile check without Gradle locking.

.DESCRIPTION
    Compiles every Java source in core/src/main/java with javac directly, using the
    compile classpath resolved once by Gradle (cached in .gradle/compile-classpath.txt).
    Diagnostics are written to build/<LogName> (no console line wrapping) and summarised
    per file. Safe to run while other agents edit sources: it does not touch Gradle.

    -XDshould-stop.ifError=GENERATE is passed on the real command line: with the default
    policy javac skips annotation processing as soon as any unit has an ENTER phase error,
    which disables Lombok and produces hundreds of bogus "cannot find symbol getXxx()".
    With GENERATE the compiler keeps going, so reported errors are real.

.PARAMETER Refresh
    Re-resolve the compile classpath through Gradle before compiling.

.PARAMETER Filter
    Only show errors whose file path matches this regex.

.PARAMETER Top
    How many files to show in the summary (default 25).

.PARAMETER LogName
    Log file name under build/. Use a distinct name per agent to avoid clobbering.

.EXAMPLE
    pwsh -File tools/compile-check.ps1 -Filter "npclib"
#>
param(
    [switch]$Refresh,
    [string]$Filter = "",
    [int]$Top = 25,
    [string]$LogName = "compile-check.log"
)

$ErrorActionPreference = "Continue"
$repo = Split-Path -Parent $PSScriptRoot
$classpathFile = Join-Path $repo ".gradle/compile-classpath.txt"
$initScript = Join-Path $PSScriptRoot "print-classpath.gradle"
$logFile = Join-Path $repo ("build/" + $LogName)

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
    foreach ($candidate in $candidates) {
        if (-not (Test-Path $candidate)) { continue }
        $version = (& $candidate -version 2>&1 | Out-String)
        if ($version -match '(\d+)\.\d+') {
            if ([int]$matches[1] -ge 21) { return $candidate }
        }
    }
    throw "Cannot locate a JDK 21 $Name. Install a JDK 21 and set JAVA_HOME."
}

if ($Refresh -or -not (Test-Path $classpathFile)) {
    Write-Output "resolving compile classpath via Gradle..."
    Push-Location $repo
    & (Join-Path $repo "gradlew.bat") "-I" $initScript ":core:printCompileCp" "-q" "--console=plain" 2>&1 | Out-Null
    $generated = Join-Path $repo "cp-list.txt"
    if (-not (Test-Path $generated)) { throw "Gradle did not produce cp-list.txt" }
    New-Item -ItemType Directory -Force (Split-Path $classpathFile) | Out-Null
    Move-Item -Force $generated $classpathFile
    Pop-Location
}

$javac = Find-JdkTool "javac"
$classpath = @((Get-Content $classpathFile))
$classpath += (Join-Path $repo "core/build/classes/kotlin/main")
$sourcePath = @(
    (Join-Path $repo "core/src/main/java"),
    (Join-Path $repo "vendor/mythic-framework/core/src/main/java"),
    (Join-Path $repo "vendor/mythic-framework/bukkit/src/main/java")
) -join ";"

$sources = Get-ChildItem (Join-Path $repo "core/src/main/java") -Recurse -Filter *.java |
    ForEach-Object { $_.FullName }
$outputDir = Join-Path ([System.IO.Path]::GetTempPath()) ("pit-compile-check-" + ($LogName -replace '[^a-zA-Z0-9]', '-'))
if (Test-Path $outputDir) { Remove-Item -Recurse -Force $outputDir }
New-Item -ItemType Directory -Force $outputDir | Out-Null
New-Item -ItemType Directory -Force (Split-Path $logFile) | Out-Null

# Windows caps the command line near 32k characters and the compile classpath alone is
# bigger than that, so every javac argument goes into an @argfile.
$argFile = Join-Path ([System.IO.Path]::GetTempPath()) ("pit-compile-args-" + ($LogName -replace '[^a-zA-Z0-9]', '-') + ".txt")
$argLines = New-Object System.Collections.Generic.List[string]
$argLines.Add("-encoding"); $argLines.Add("UTF-8")
$argLines.Add("-proc:full")
$argLines.Add("-Xmaxerrs"); $argLines.Add("5000")
$argLines.Add("-nowarn")
$argLines.Add("-d"); $argLines.Add($outputDir)
$argLines.Add("-cp"); $argLines.Add(($classpath -join ";"))
$argLines.Add("-sourcepath"); $argLines.Add($sourcePath)
$sources | ForEach-Object { $argLines.Add($_) }
[System.IO.File]::WriteAllLines($argFile, [string[]]$argLines, (New-Object System.Text.UTF8Encoding($false)))

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $javac
$psi.Arguments = @(
    "-J-Duser.language=en",
    "-J-Duser.country=US",
    "-XDshould-stop.ifError=GENERATE",
    "@$argFile"
) -join ' '
$psi.UseShellExecute = $false
$psi.RedirectStandardError = $true
$psi.RedirectStandardOutput = $true
$psi.CreateNoWindow = $true
$process = [System.Diagnostics.Process]::Start($psi)
$stderrTask = $process.StandardError.ReadToEndAsync()
$stdout = $process.StandardOutput.ReadToEnd()
$stderr = $stderrTask.Result
$process.WaitForExit()
if ($process.ExitCode -eq 2) {
    Write-Output "javac rejected the arguments:"
    Write-Output $stdout
}
[System.IO.File]::WriteAllText($logFile, $stderr, (New-Object System.Text.UTF8Encoding($false)))

$prefix = [regex]::Escape($repo + "\core\src\main\java\")
$counts = @{}
$total = 0
foreach ($line in ($stderr -split "`n")) {
    if ($line -match ("^" + $prefix + "(.+?\.java):\d+: error:")) {
        $rel = $matches[1].Replace('\', '/')
        if (-not $counts.ContainsKey($rel)) { $counts[$rel] = 0 }
        $counts[$rel]++
        $total++
    }
}

Write-Output ("javac exit: " + $process.ExitCode)
Write-Output ("errors: " + $total + "  (log: " + $logFile + ")")
if ($Filter) {
    Write-Output "--- matching files ---"
    $counts.Keys | Where-Object { $_ -match $Filter } | Sort-Object { -$counts[$_] } |
        ForEach-Object { "{0,5}  {1}" -f $counts[$_], $_ }
} else {
    Write-Output "--- top files ---"
    $counts.Keys | Sort-Object { -$counts[$_] } | Select-Object -First $Top |
        ForEach-Object { "{0,5}  {1}" -f $counts[$_], $_ }
}
