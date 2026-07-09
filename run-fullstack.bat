@echo off
setlocal EnableDelayedExpansion

REM ── Detectar raiz del repo (donde vive este .bat) ─────────
set "REPO_ROOT=%~dp0"
if "%REPO_ROOT:~-1%"=="\" set "REPO_ROOT=%REPO_ROOT:~0,-1%"

REM ── Rutas derivadas (todo relativo al repo) ────────────────
set "BACKEND_DIR=%REPO_ROOT%\backend"
set "SERVICE_DIR=%BACKEND_DIR%\services\validation-service"
set "FRONTEND_DIR=%REPO_ROOT%\frontend"
set "GRADLEW=%BACKEND_DIR%\gradlew.bat"

REM ── JAVA_HOME dinamico por usuario ────────────────────────
REM    %USERPROFILE% resuelve a C:\Users\<usuario> automaticamente
REM ──────────────────────────────────────────────────────────
if not defined JAVA_HOME (
    set "JAVA_HOME=%USERPROFILE%\corretto-17"
    echo [JAVA] JAVA_HOME auto-detectado: !JAVA_HOME!
) else (
    echo [JAVA] JAVA_HOME ya definido: !JAVA_HOME!
)

if not exist "!JAVA_HOME!\bin\java.exe" (
    echo.
    echo [ERROR] No se encontro java.exe en: !JAVA_HOME!\bin\
    echo         Instala Amazon Corretto 17 en: %USERPROFILE%\corretto-17
    echo         O define JAVA_HOME manualmente.
    pause
    exit /b 1
)

REM ── Auto-deteccion de puerto PostgreSQL ────────────────────
REM    junortiz -> 5432   |   todos los demas -> 5433
REM    Esto alimenta JDBC_URL para que Spring Boot se conecte
REM    al puerto correcto sin tocar application.yml
REM ──────────────────────────────────────────────────────────
set "DB_PORT=5433"
if /i "%USERNAME%"=="junortiz" set "DB_PORT=5432"

REM ── JDBC_URL dinamico (sobreescribe el default del YML) ────
if not defined JDBC_URL (
    set "JDBC_URL=jdbc:postgresql://localhost:!DB_PORT!/appdb?characterEncoding=UTF-8"
    echo [DB] JDBC_URL auto-generado con puerto !DB_PORT!
) else (
    echo [DB] JDBC_URL ya definido externamente: !JDBC_URL!
)

echo.
echo ========================================
echo      SIPRO - Full Stack Startup
echo ========================================
echo Repo      : %REPO_ROOT%
echo Usuario   : %USERNAME%
echo JAVA_HOME : !JAVA_HOME!
echo JDBC_URL  : !JDBC_URL!
echo Backend   : %SERVICE_DIR%
echo Frontend  : %FRONTEND_DIR%
echo ========================================
echo.

REM ── Validar que las carpetas existan ───────────────────────
set "ABORT=0"
if not exist "%SERVICE_DIR%\" (
    echo [ERROR] No existe: %SERVICE_DIR%
    set "ABORT=1"
)
if not exist "%FRONTEND_DIR%\" (
    echo [ERROR] No existe: %FRONTEND_DIR%
    set "ABORT=1"
)
if not exist "%GRADLEW%" (
    echo [ERROR] No existe: %GRADLEW%
    set "ABORT=1"
)
if "%ABORT%"=="1" (
    echo.
    echo Verifica que este .bat este en la raiz del repositorio SIPRO.
    pause
    exit /b 1
)

REM ── PATH temporal (se pierde al cerrar) ────────────────────
set "PATH=%REPO_ROOT%;%BACKEND_DIR%;%SERVICE_DIR%;%FRONTEND_DIR%;%PATH%"

REM ── Liberar puertos ocupados ───────────────────────────────
echo [0/2] Liberando puertos 8080 y 4200...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
    echo   Matando PID %%p en puerto 8080...
    taskkill /PID %%p /F >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":4200 " ^| findstr "LISTENING"') do (
    echo   Matando PID %%p en puerto 4200...
    taskkill /PID %%p /F >nul 2>&1
)
echo   Puertos liberados.
echo.

REM ── Credenciales LZ (Impala) ───────────────────────────────
set "LZ_DEV_PASSWORD=%PASSWORD%"
if not defined LZ_DEV_PASSWORD (
    echo [AVISO] Variable PASSWORD no encontrada. El backend usara Secrets Manager.
) else (
    echo [LZ-AUTH] Credencial LZ leida. Bypass DEV activo.
)
echo.

REM ── Paso 1: Backend (Spring Boot) ──────────────────────────
REM    JAVA_HOME y JDBC_URL ya estan en el entorno.
REM    Las ventanas hijas heredan todas las variables automaticamente.
echo [1/2] Iniciando Backend (Java/Spring Boot)...
start "SIPRO Backend" cmd /k "cd /d "%SERVICE_DIR%" && "%GRADLEW%" bootRun"

echo Esperando 15 segundos para que el backend inicie...
timeout /t 15 /nobreak

REM ── Paso 2: Frontend (Angular) ─────────────────────────────
echo.
echo [2/2] Iniciando Frontend (Angular)...
start "SIPRO Frontend" cmd /k "cd /d "%FRONTEND_DIR%" && npm start"

echo.
echo ========================================
echo   Servicios iniciados correctamente
echo ========================================
echo Repo:      %REPO_ROOT%
echo Usuario:   %USERNAME%
echo JAVA_HOME: !JAVA_HOME!
echo JDBC_URL:  !JDBC_URL!
echo Storage:   Local (C:\s3mock2\sipro-local-storage)
echo Backend:   http://localhost:8080
echo Frontend:  http://localhost:4200
echo ========================================
echo.
echo Presiona cualquier tecla para salir...
pause > nul

endlocal
