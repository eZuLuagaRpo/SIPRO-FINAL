@echo off
echo ========================================
echo   Instalando Dependencias de Angular
echo ========================================
cd sipro-angular-frontend
call npm install
if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   Dependencias instaladas correctamente
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   Error al instalar dependencias
    echo   Verifica que Node.js y npm estén instalados
    echo ========================================
)
pause
