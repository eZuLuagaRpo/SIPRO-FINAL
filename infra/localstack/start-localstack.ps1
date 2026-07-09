# ============================================================
#  SIPRO - LocalStack S3 (Persist) - Start
#  Basado en el script probado del proyecto.
#  Uso:
#    .\start-localstack.ps1                # Start interactivo
#    .\start-localstack.ps1 -Mode Start -NoPause  # Para run-fullstack.bat
#    .\start-localstack.ps1 -Mode Recreate         # Limpia y recrea
#    .\start-localstack.ps1 -Mode Status
#    .\start-localstack.ps1 -Mode Stop
# ============================================================
param(
    [ValidateSet("Start","Status","Stop","Recreate")]
    [string]$Mode = "Start",
    [switch]$NoPause
)

Write-Host "S3 Local (LocalStack Persist) - Modo: $Mode" -ForegroundColor Cyan

# Parametros fijos del entorno local soportado por SIPRO.
# Se asume una distro WSL2 conocida, un bucket estable y una carpeta Windows persistente.
$distro    = "Ubuntu2204"
$container = "localstack"
$winDir    = "C:\s3mock2"
$bucket    = "sipro-bucket"
$seedDirInContainer = "/var/lib/localstack/seed/datos_s3"
$keepAlivePidFile = Join-Path $winDir "localstack-keepalive.pid"

# 0) Asegurar carpetas base
New-Item -ItemType Directory -Force -Path $winDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $winDir "init-scripts") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $winDir "seed") | Out-Null

# 1) Windows -> WSL path (/mnt/c/...)
$wslDir = ($winDir -replace '\\','/')
if ($wslDir -match '^([A-Za-z]):') {
    $drive = $Matches[1].ToLower()
    $wslDir = "/mnt/$drive" + $wslDir.Substring(2)
}

# 2) Volumenes
$volData = $wslDir + ":/var/lib/localstack"
$volInit = ($wslDir + "/init-scripts") + ":/etc/localstack/init/ready.d"

function Run-Wsl([string]$bashCmd) {
    # Ejecuta comandos bash dentro de WSL como root para evitar problemas de permisos con Docker.
    & wsl.exe -d $distro -u root -e bash -lc $bashCmd
}

function Run-WslQuiet([string]$bashCmd) {
    # Variante silenciosa para chequeos idempotentes donde el stderr solo agrega ruido.
    & wsl.exe -d $distro -u root -e bash -lc $bashCmd 2>$null | Out-Null
}

function Stop-LocalstackKeepAlive {
    # Limpia el proceso auxiliar que evita que WSL2 pierda el auto-forward a localhost:4566.
    if (Test-Path $keepAlivePidFile) {
        try {
            $pid = Get-Content $keepAlivePidFile -ErrorAction Stop
            if ($pid) {
                Stop-Process -Id ([int]$pid) -Force -ErrorAction SilentlyContinue
            }
        } catch { }
        Remove-Item $keepAlivePidFile -Force -ErrorAction SilentlyContinue
    }
}

function Start-LocalstackKeepAlive {
    Stop-LocalstackKeepAlive

    # Mantiene viva la distro WSL y el auto-forward localhost->WSL.
    # Estrategia dual cada 5s:
    #  1) ping interno en WSL a LocalStack (evita idle de la VM)
    #  2) conexión TCP desde Windows a 127.0.0.1:4566 (mantiene forward)
    $keepAliveCmd = @'
while ($true) {
    try {
        wsl.exe -d Ubuntu2204 -u root -e bash -lc "curl -s --max-time 2 http://localhost:4566/_localstack/health >/dev/null 2>&1 || true" | Out-Null
    } catch { }

    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect("127.0.0.1", 4566)
        $tcp.Close()
    } catch { }
    Start-Sleep -Seconds 5
}
'@

    $proc = Start-Process powershell.exe -WindowStyle Hidden -PassThru -ArgumentList @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-Command", $keepAliveCmd
    )

    Set-Content -Path $keepAlivePidFile -Value $proc.Id -Encoding ASCII
    Write-Host "[OK] KeepAlive Windows+WSL activo (PID=$($proc.Id), cada 5s)." -ForegroundColor Green
}

# 3) Iniciar docker en WSL (solo si no está activo)
if ($Mode -eq "Start" -or $Mode -eq "Recreate") {
    Run-WslQuiet "pgrep -x dockerd >/dev/null 2>&1 || service docker start >/dev/null 2>&1 || true"
}

# ── STOP ──────────────────────────────────────────────────────
if ($Mode -eq "Stop") {
    Write-Host "Deteniendo contenedor..." -ForegroundColor Yellow
    Run-WslQuiet "docker stop $container >/dev/null 2>&1 || true"
    Stop-LocalstackKeepAlive
    Write-Host "[OK] KeepAlive Windows detenido." -ForegroundColor Green
    Write-Host "[OK] Contenedor detenido." -ForegroundColor Green
    if (-not $NoPause) { Read-Host "Presiona Enter para cerrar" | Out-Null }
    exit 0
}

# ── STATUS ────────────────────────────────────────────────────
if ($Mode -eq "Status") {
    Write-Host "Estado del contenedor:" -ForegroundColor Yellow
    Run-Wsl "docker ps -a --filter name=$container --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"
    Write-Host "Health (parcial):" -ForegroundColor Yellow
    Run-Wsl "curl -s --max-time 2 http://localhost:4566/_localstack/health | head -c 200; echo"
    Write-Host "Init hooks (parcial):" -ForegroundColor Yellow
    Run-Wsl "curl -s --max-time 2 http://localhost:4566/_localstack/init | head -c 200; echo"
    Write-Host "Bucket (conteo objetos):" -ForegroundColor Yellow
    Run-Wsl "docker exec $container awslocal s3 ls s3://$bucket/ --recursive 2>/dev/null | wc -l || true"

    if (Test-Path $keepAlivePidFile) {
        $kp = (Get-Content $keepAlivePidFile -ErrorAction SilentlyContinue)
        $running = $false
        if ($kp) {
            $running = [bool](Get-Process -Id ([int]$kp) -ErrorAction SilentlyContinue)
        }
        if ($running) {
            Write-Host "KeepAlive Windows: ACTIVO (PID=$kp)" -ForegroundColor Green
        } else {
            Write-Host "KeepAlive Windows: PID huérfano (limpiando archivo)." -ForegroundColor Yellow
            Remove-Item $keepAlivePidFile -Force -ErrorAction SilentlyContinue
        }
    } else {
        Write-Host "KeepAlive Windows: INACTIVO" -ForegroundColor Yellow
    }

    if (-not $NoPause) { Read-Host "Presiona Enter para cerrar" | Out-Null }
    exit 0
}

# ── RECREATE ──────────────────────────────────────────────────
if ($Mode -eq "Recreate") {
    Write-Host "Recreate: eliminando contenedor..." -ForegroundColor Yellow
    Run-WslQuiet "docker rm -f $container >/dev/null 2>&1 || true"
    Stop-LocalstackKeepAlive

    # Se preservan init-scripts y seed porque representan la base reproducible del mock S3.
    Write-Host "Limpiando datos persistentes antiguos (evita crash loop)..." -ForegroundColor Yellow
    if (Test-Path $winDir) {
        Get-ChildItem -Path $winDir | Where-Object { $_.Name -notin "init-scripts", "seed" } | ForEach-Object {
            Write-Host "  -> Borrando: $($_.Name)" -ForegroundColor DarkGray
            Remove-Item -Path $_.FullName -Recurse -Force -ErrorAction SilentlyContinue | Out-Null
        }
    }
}

# ── START: Crear o arrancar contenedor ────────────────────────

# Verificar que puerto 4566 no esté ocupado por otro container
$portConflict = ("$(Run-Wsl "docker ps --filter 'publish=4566' --format '{{.Names}}' 2>/dev/null")").Trim()
if ($portConflict -and $portConflict -ne $container) {
    Write-Host "Puerto 4566 ocupado por container '$portConflict'. Deteniendolo..." -ForegroundColor Yellow
    Run-WslQuiet "docker stop $portConflict >/dev/null 2>&1 || true"
    Start-Sleep -Seconds 2
}

$exists = ("$(Run-Wsl "docker ps -a --filter 'name=^/$container$' --format '{{.Names}}'")").Trim()

if ($exists -eq $container) {
    $running = ("$(Run-Wsl "docker inspect $container --format '{{.State.Running}}'")").Trim()
    if ($running -ne "true") {
        Write-Host "Iniciando contenedor existente..." -ForegroundColor Yellow
        Run-WslQuiet "docker start $container >/dev/null"
    } else {
        Write-Host "Contenedor ya estaba corriendo." -ForegroundColor Green
    }
}
else {
    Write-Host "Creando contenedor LocalStack Persist..." -ForegroundColor Yellow
    # Se monta el volumen persistente completo y el directorio de hooks ready.d para auto-init.
    $dockerCmd = "docker run -d --name $container -p 4566:4566 -e LOCALSTACK_SERVICES=s3 -e DEBUG=1 --restart unless-stopped -v '$volData' -v '$volInit' localstack/localstack:3"
    Run-Wsl $dockerCmd
}

# ── Esperar HEALTH estable ────────────────────────────────────
Write-Host "Esperando health estable..." -ForegroundColor White
$ok = 0
$stable = $false

# Se exige estabilidad y no solo un health aislado para evitar arrancar el backend contra un LocalStack a medio inicializar.
for ($i=1; $i -le 120; $i++) {
    $res = ("$(Run-Wsl "curl -s --max-time 2 http://localhost:4566/_localstack/health >/dev/null 2>&1; echo `$?")").Trim()

    if ($res -eq "0") {
        $ok++
        Write-Host "." -NoNewline -ForegroundColor Green
        if ($ok -ge 3) {
            Write-Host ""
            $stable = $true
            break
        }
    }
    else {
        $ok = 0
        Write-Host "." -NoNewline -ForegroundColor DarkGray
    }
    Start-Sleep -Seconds 1
}
Write-Host ""

if (-not $stable) {
    Write-Host "[ERROR] Health no se estabiliza en 120s. Logs recientes:" -ForegroundColor Red
    Run-Wsl "docker logs --tail 200 $container"
    if (-not $NoPause) { Read-Host "Presiona Enter para cerrar" | Out-Null }
    exit 1
}

# ── Validaciones y auto-restore ───────────────────────────────
Write-Host "Health (parcial):" -ForegroundColor White
Run-Wsl "curl -s http://localhost:4566/_localstack/health | head -c 200; echo"

Write-Host "Init hooks (parcial):" -ForegroundColor White
Run-Wsl "curl -s http://localhost:4566/_localstack/init | head -c 200; echo"

# Esperar init hooks READY
Write-Host "Esperando init hooks READY..." -ForegroundColor White
$initReady = $false
for ($j=1; $j -le 30; $j++) {
    $initJson = ("$(Run-Wsl "curl -s http://localhost:4566/_localstack/init")").Trim()
    if ($initJson -match '"READY"\s*:\s*true' -and $initJson -match '"state"\s*:\s*"SUCCESSFUL"') {
        $initReady = $true
        break
    }
    Start-Sleep -Seconds 1
}

# Asegurar bucket exista
$bucketExists = ("$(Run-Wsl "docker exec $container awslocal s3api head-bucket --bucket $bucket >/dev/null 2>&1; echo `$?")").Trim()
if ($bucketExists -ne "0") {
    Run-WslQuiet "docker exec $container awslocal s3 mb s3://$bucket"
}

# Conteo de objetos con reintentos.
# LocalStack puede responder antes de listar correctamente el contenido restaurado del bucket.
$objCount = -1
for ($t=1; $t -le 5; $t++) {
    try {
        $rawList = Run-Wsl "docker exec $container awslocal s3 ls s3://$bucket/ --recursive 2>/dev/null"
        $rawText = (@($rawList) -join "`n")
        $lines = @($rawText -split "`r?`n") | Where-Object { $_.Trim() -ne "" }
        $objCount = $lines.Count
        if ($objCount -gt 0) { break }
    } catch {
        $objCount = -1
    }
    Start-Sleep -Seconds 2
}

$hasSeed = ("$(Run-Wsl "docker exec $container awslocal s3api head-object --bucket $bucket --key 'LEEME_ESTRUCTURA.txt' >/dev/null 2>&1; echo `$?")").Trim()

# Si ya existe el seed, reintentar conteo
if ($objCount -eq 0 -and $hasSeed -eq "0") {
    for ($t=1; $t -le 5; $t++) {
        $rawList = Run-Wsl "docker exec $container awslocal s3 ls s3://$bucket/ --recursive 2>/dev/null"
        $rawText = (@($rawList) -join "`n")
        $lines = @($rawText -split "`r?`n") | Where-Object { $_.Trim() -ne "" }
        $objCount = $lines.Count
        if ($objCount -gt 0) { break }
        Start-Sleep -Seconds 2
    }
}

if ($objCount -eq 0 -and $hasSeed -ne "0") {
    Write-Host "Bucket vacio: restaurando desde seed ($seedDirInContainer)..." -ForegroundColor Yellow
    Run-WslQuiet "docker exec $container awslocal s3 sync $seedDirInContainer s3://$bucket/"

    # Recalcular
    $rawList = Run-Wsl "docker exec $container awslocal s3 ls s3://$bucket/ --recursive 2>/dev/null"
    $rawText = (@($rawList) -join "`n")
    $lines = @($rawText -split "`r?`n") | Where-Object { $_.Trim() -ne "" }
    $objCount = $lines.Count
}

Write-Host ""
Write-Host "[OK] LISTO: http://localhost:4566" -ForegroundColor Green
Write-Host "Bucket: $bucket | Objetos: $objCount" -ForegroundColor Green
Write-Host "Persistencia: $winDir (seed + hooks incluidos)" -ForegroundColor Green

# ── Warm-Up: Activar port-forwarding WSL2 → Windows ─────────────
# WSL2 auto-forward es vago: a veces no se activa hasta que alguien
# intenta conectar. Hacemos 10 intentos con 2s de espera para
# "despertar" el forwarding ANTES de que el backend Java arranque.
Write-Host ""
Write-Host "Verificando conectividad Windows -> localhost:4566..." -ForegroundColor White

$winReachable = $false
for ($warmup = 1; $warmup -le 10; $warmup++) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect("127.0.0.1", 4566)
        $tcp.Close()
        $winReachable = $true
        Write-Host "[OK] localhost:4566 alcanzable desde Windows (intento $warmup/10)." -ForegroundColor Green
        break
    } catch { }
    Write-Host "  Intento $warmup/10: no alcanzable. Esperando..." -ForegroundColor DarkGray
    Start-Sleep -Seconds 2
}

if (-not $winReachable) {
    # Intentar netsh portproxy como fallback cuando el auto-forward de WSL2 no aparece.
    # Esto no sustituye el comportamiento normal; solo reduce fallas intermitentes en Windows.
    $wsl2Ip = $null
    try { $wsl2Ip = (wsl -d $distro hostname -I 2>$null).Trim().Split(" ")[0] } catch { }

    if ($wsl2Ip) {
        Write-Host "Auto-forward inactivo. Intentando netsh portproxy a $wsl2Ip..." -ForegroundColor Yellow
        try {
            netsh interface portproxy delete v4tov4 listenport=4566 listenaddress=127.0.0.1 2>$null | Out-Null
            netsh interface portproxy add v4tov4 listenport=4566 listenaddress=127.0.0.1 connectport=4566 connectaddress=$wsl2Ip 2>$null | Out-Null
            Start-Sleep -Seconds 1
            try {
                $tcp2 = New-Object System.Net.Sockets.TcpClient
                $tcp2.Connect("127.0.0.1", 4566)
                $tcp2.Close()
                $winReachable = $true
                Write-Host "[OK] Port-proxy configurado: localhost:4566 -> ${wsl2Ip}:4566" -ForegroundColor Green
            } catch { }
        } catch { }
    }

    if (-not $winReachable) {
        Write-Host "[WARN] localhost:4566 NO alcanzable desde Windows." -ForegroundColor Red
        Write-Host "       S3Config del backend reintentara la conexion automaticamente." -ForegroundColor Yellow
        Write-Host "       Si persiste, ejecute como Admin:" -ForegroundColor Yellow
        if ($wsl2Ip) {
            Write-Host "       netsh interface portproxy add v4tov4 listenport=4566 listenaddress=127.0.0.1 connectport=4566 connectaddress=$wsl2Ip" -ForegroundColor DarkYellow
        }
    }
}

# KeepAlive persistente en Windows para evitar caída de localhost:4566 por idle.
# Se deja al final, cuando ya confirmamos que el endpoint local responde.
Start-LocalstackKeepAlive

Write-Host ""
Write-Host "Muestra contenido (10 primeras):" -ForegroundColor Cyan
Run-Wsl "docker exec $container awslocal s3 ls s3://$bucket/ --recursive 2>/dev/null | head -n 10 || true"

if (-not $NoPause) {
    Write-Host ""
    Write-Host "NOTA: El contenedor sigue corriendo en segundo plano (Docker)." -ForegroundColor Gray
    Write-Host "Presiona Enter para finalizar."
    $null = Read-Host
}
