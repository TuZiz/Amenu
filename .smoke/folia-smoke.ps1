param(
    [Parameter(Mandatory = $true)]
    [string]$ServerDir,
    [Parameter(Mandatory = $true)]
    [string]$ServerJar,
    [string]$JavaHome = (Join-Path $PSScriptRoot "..\.tools\jdk21"),
    [string]$PluginJar = (Join-Path $PSScriptRoot "..\target\amenu-1.0.0-SNAPSHOT-shaded.jar"),
    [int]$StartupTimeoutSeconds = 90
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-RequiredPath {
    param([string]$Path)
    return (Resolve-Path $Path).Path
}

function Invoke-CompatibilitySmoke {
    param(
        [string]$ResolvedServerDir,
        [string]$ResolvedServerJar,
        [string]$ResolvedJavaHome,
        [string]$ResolvedPluginJar,
        [int]$TimeoutSeconds,
        [string]$PlatformName
    )

    $pluginsDir = Join-Path $ResolvedServerDir "plugins"
    $logsDir = Join-Path $ResolvedServerDir "logs"
    $latestLog = Join-Path $logsDir "latest.log"
    $pluginTarget = Join-Path $pluginsDir "AMenu.jar"

    New-Item -ItemType Directory -Force -Path $pluginsDir | Out-Null
    New-Item -ItemType Directory -Force -Path $logsDir | Out-Null
    Copy-Item -LiteralPath $ResolvedPluginJar -Destination $pluginTarget -Force

    Push-Location $ResolvedServerDir
    try {
        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = New-Object System.Diagnostics.ProcessStartInfo
        $process.StartInfo.FileName = Join-Path $ResolvedJavaHome "bin\java.exe"
        $process.StartInfo.Arguments = "-jar `"$ResolvedServerJar`" nogui"
        $process.StartInfo.WorkingDirectory = $ResolvedServerDir
        $process.StartInfo.RedirectStandardInput = $true
        $process.StartInfo.RedirectStandardOutput = $true
        $process.StartInfo.RedirectStandardError = $true
        $process.StartInfo.UseShellExecute = $false
        $process.StartInfo.CreateNoWindow = $true

        [void]$process.Start()
        $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
        do {
            Start-Sleep -Seconds 2
            if (Test-Path $latestLog) {
                $logText = Get-Content $latestLog -Raw
                if ($logText -match 'Done \(') {
                    break
                }
            }
        } while ((Get-Date) -lt $deadline -and -not $process.HasExited)

        if (-not (Test-Path $latestLog)) {
            throw "$PlatformName smoke did not produce logs/latest.log."
        }

        $logText = Get-Content $latestLog -Raw
        if ($logText -notmatch 'Done \(') {
            throw "$PlatformName smoke did not reach server startup readiness within $TimeoutSeconds seconds."
        }
        if ($logText -notmatch 'Enabling AMenu') {
            throw "$PlatformName smoke did not show AMenu enablement."
        }

        $forbiddenPatterns = @(
            'NoClassDefFoundError',
            'MiniMessage',
            'scheduler reflection',
            'prompt cleanup',
            'ClassNotFoundException'
        )
        foreach ($pattern in $forbiddenPatterns) {
            if ($logText -match $pattern) {
                throw "$PlatformName smoke detected forbidden log pattern: $pattern"
            }
        }

        $process.StandardInput.WriteLine("stop")
        $process.StandardInput.Flush()
        if (-not $process.WaitForExit(30000)) {
            $process.Kill()
            throw "$PlatformName smoke server did not stop cleanly after log inspection."
        }
    } finally {
        Pop-Location
    }
}

$resolvedServerDir = Resolve-RequiredPath $ServerDir
$resolvedServerJar = Resolve-RequiredPath (Join-Path $resolvedServerDir $ServerJar)
$resolvedJavaHome = Resolve-RequiredPath $JavaHome
$resolvedPluginJar = Resolve-RequiredPath $PluginJar

Invoke-CompatibilitySmoke `
    -ResolvedServerDir $resolvedServerDir `
    -ResolvedServerJar $resolvedServerJar `
    -ResolvedJavaHome $resolvedJavaHome `
    -ResolvedPluginJar $resolvedPluginJar `
    -TimeoutSeconds $StartupTimeoutSeconds `
    -PlatformName "Folia"
