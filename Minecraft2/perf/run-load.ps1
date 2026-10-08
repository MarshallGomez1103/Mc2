# Carga headless de la IA de enemigos (recuperación C2, REC-E4/E8).
# Uso, desde Minecraft2/:  .\perf\run-load.ps1 baseline|stress [minutos reales máx., por defecto 20]
# Protocolo y SLO: docs/recuperacion-c2/carga.md
param(
    [Parameter(Mandatory = $true)][ValidateSet('baseline', 'stress')][string]$Escenario,
    [double]$Minutos = 20
)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$scenario = if ($Escenario -eq 'baseline') { 'REC_BASELINE' } else { 'REC_STRESS' }
$heapFlags = @('-Xms1g', '-Xmx1g')
$sep = [IO.Path]::PathSeparator

# Windows PowerShell 5.1 convierte el stderr de un ejecutable en error: solo se juzga el código de salida.
$ErrorActionPreference = 'Continue'
Write-Host 'Compilando (mvn -q test-compile)...'
& mvn -q test-compile
if ($LASTEXITCODE -ne 0) { throw "mvn test-compile falló ($LASTEXITCODE)" }

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$out = "target/load-results/$($scenario.ToLower())-$stamp"
New-Item -ItemType Directory -Force $out | Out-Null

function Get-Sha256([string]$path) { (Get-FileHash -Algorithm SHA256 $path).Hash.ToLower() }
$javaLines = @(cmd /c 'java -version 2>&1')
$cpu = (Get-CimInstance Win32_Processor | Select-Object -First 1).Name.Trim()
$ram = [math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB, 1).ToString([Globalization.CultureInfo]::InvariantCulture)
$os = (Get-CimInstance Win32_OperatingSystem)
$dirty = @(git status --porcelain -- src test perf).Count
$minutesText = $Minutos.ToString([Globalization.CultureInfo]::InvariantCulture)

@(
    "scenario=$scenario"
    "started_local=$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
    "commit=$(git rev-parse HEAD)"
    "branch=$(git rev-parse --abbrev-ref HEAD)"
    "uncommitted_files_src_test_perf=$dirty"
    "java=$($javaLines[0])"
    "java_runtime=$($javaLines[1])"
    "jvm_flags=$($heapFlags -join ' ')"
    "wall_limit_minutes=$minutesText"
    "os=$($os.Caption) $($os.Version)"
    "cpu=$cpu"
    "logical_cpus=$([Environment]::ProcessorCount)"
    "ram_gb=$ram"
    "sha256_EnemyLoadHarness.java=$(Get-Sha256 'test/loadtest/EnemyLoadHarness.java')"
    "sha256_run-load.sh=$(Get-Sha256 'perf/run-load.sh')"
    "sha256_run-load.ps1=$(Get-Sha256 'perf/run-load.ps1')"
) | Set-Content -Encoding utf8 "$out/manifest.txt"

$javaArgs = $heapFlags + @('-Dsun.stdout.encoding=UTF-8', "-Dmc2.load.out=$out",
    '-cp', "target/classes${sep}target/test-classes", 'loadtest.EnemyLoadHarness', $scenario, $minutesText)
& java @javaArgs 2>&1 | ForEach-Object { "$_" } | Tee-Object -FilePath "$out/console.txt"
if ($LASTEXITCODE -ne 0) { throw "El harness terminó con código $LASTEXITCODE" }

Add-Content -Encoding utf8 "$out/manifest.txt" "finished_local=$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
Write-Host "Resultados en $out (copiar los seleccionados a perf/results/ tras revisarlos)"
