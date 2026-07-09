Windows PowerShell
Copyright (C) Microsoft Corporation. Todos los derechos reservados.

Instale la versión más reciente de PowerShell para obtener nuevas características y mejoras. https://aka.ms/PSWindows

PS C:\Users\junortiz> # ========= INVENTARIO COMPLETO WSL + UBUNTU + DOCKER + LOCALSTACK + JAVA + AWSCLI =========
PS C:\Users\junortiz> $ErrorActionPreference = "Continue"
PS C:\Users\junortiz> $distro = "Ubuntu2204"
PS C:\Users\junortiz> $stamp  = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
PS C:\Users\junortiz>
PS C:\Users\junortiz> function Section($title) {
>>   Write-Host ""
>>   Write-Host ("="*110)
>>   Write-Host ("{0}  [{1}]" -f $title, $stamp)
>>   Write-Host ("="*110)
>> }
PS C:\Users\junortiz>
PS C:\Users\junortiz> function Run($label, $scriptBlock) {
>>   Write-Host ""
>>   Write-Host ("--- {0}" -f $label) -ForegroundColor Cyan
>>   try {
>>     & $scriptBlock 2>&1 | ForEach-Object { "$_" }
>>   } catch {
>>     "ERROR: $($_.Exception.Message)"
>>   }
>> }
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "0) CONTEXTO (Este script debe correr en Windows PowerShell, NO dentro de Ubuntu/WSL)"

==============================================================================================================
0) CONTEXTO (Este script debe correr en Windows PowerShell, NO dentro de Ubuntu/WSL)  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "Usuario actual / Host" { whoami; hostname }

--- Usuario actual / Host
bancolombia\junortiz
pb0b0976046
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "1) WSL (version, status, distros)"

==============================================================================================================
1) WSL (version, status, distros)  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "wsl --version" { wsl --version }

--- wsl --version
Versi¾n de WSL: 2.6.3.0

Versi¾n de kernel: 6.6.87.2-1

Versi¾n de WSLg: 1.0.71

Versi¾n de MSRDC: 1.2.6353

Versi¾n de Direct3D: 1.611.1-81528511

Versi¾n de DXCore: 10.0.26100.1-240331-1435.ge-release

Versi¾n de Windows: 10.0.26100.7840


PS C:\Users\junortiz> Run "wsl --status"  { wsl --status }

--- wsl --status
Distribuci¾n predeterminada: Ubuntu2204

Versi¾n predeterminada: 2


PS C:\Users\junortiz> Run "wsl -l -v"     { wsl -l -v }

--- wsl -l -v
  NAME          STATE           VERSION

* Ubuntu2204    Running         2


PS C:\Users\junortiz> Run "wsl --list --running" { wsl --list --running }

--- wsl --list --running
Distribuciones de subsistema de Windows para Linux:

Ubuntu2204 (Predeterminado)


PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "2) Ubuntu (WSL): OS, kernel, IPs (usando distro '$distro')"

==============================================================================================================
2) Ubuntu (WSL): OS, kernel, IPs (usando distro 'Ubuntu2204')  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "WSL: /etc/os-release (head)" { wsl.exe -d $distro -- bash -lc "cat /etc/os-release | head -n 12" }

--- WSL: /etc/os-release (head)
PRETTY_NAME="Ubuntu 22.04.5 LTS"
NAME="Ubuntu"
VERSION_ID="22.04"
VERSION="22.04.5 LTS (Jammy Jellyfish)"
VERSION_CODENAME=jammy
ID=ubuntu
ID_LIKE=debian
HOME_URL="https://www.ubuntu.com/"
SUPPORT_URL="https://help.ubuntu.com/"
BUG_REPORT_URL="https://bugs.launchpad.net/ubuntu/"
PRIVACY_POLICY_URL="https://www.ubuntu.com/legal/terms-and-policies/privacy-policy"
UBUNTU_CODENAME=jammy
PS C:\Users\junortiz> Run "WSL: uname -a"               { wsl.exe -d $distro -- uname -a }

--- WSL: uname -a
Linux pb0b0976046 6.6.87.2-microsoft-standard-WSL2 #1 SMP PREEMPT_DYNAMIC Thu Jun  5 18:30:46 UTC 2025 x86_64 x86_64 x86_64 GNU/Linux
PS C:\Users\junortiz> Run "WSL: hostname -I"            { wsl.exe -d $distro -- hostname -I }

--- WSL: hostname -I
192.168.78.172 100.96.229.196 172.18.0.1 172.17.0.1 fd4f:270b:99c8::433 fd4f:270b:99c8:0:e931:d91f:7370:aee0 fd4f:270b:99c8:0:2f2e:cd0f:cf1e:fdb7 2606:4700:cf1:1000::8753
PS C:\Users\junortiz> Run "WSL: ip addr (eth0) resumen" { wsl.exe -d $distro -- bash -lc "ip -br addr show eth0 || true" }

--- WSL: ip addr (eth0) resumen
eth0             UP             192.168.78.172/24 fd4f:270b:99c8::433/128 fd4f:270b:99c8:0:e931:d91f:7370:aee0/128 fd4f:270b:99c8:0:2f2e:cd0f:cf1e:fdb7/64 fe80::cfda:fcda:a903:334e/64
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "3) Java en Windows (sin depender del PATH) + JAVA_HOME"

==============================================================================================================
3) Java en Windows (sin depender del PATH) + JAVA_HOME  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "JAVA_HOME y java.exe" {
>>   "JAVA_HOME = $env:JAVA_HOME"
>>   if (Test-Path "$env:JAVA_HOME\bin\java.exe") {
>>     & "$env:JAVA_HOME\bin\java.exe" -version
>>   } else {
>>     "NO existe: $env:JAVA_HOME\bin\java.exe"
>>   }
>> }

--- JAVA_HOME y java.exe
JAVA_HOME = C:\Users\junortiz\corretto-17
openjdk version "17.0.17" 2025-10-21 LTS
OpenJDK Runtime Environment Corretto-17.0.17.10.1 (build 17.0.17+10-LTS)
OpenJDK 64-Bit Server VM Corretto-17.0.17.10.1 (build 17.0.17+10-LTS, mixed mode, sharing)
PS C:\Users\junortiz> Run "where.exe java (ojo: puede apuntar a un 'java' suelto)" { where.exe java }

--- where.exe java (ojo: puede apuntar a un 'java' suelto)
C:\Users\junortiz\java
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "4) AWS CLI en Windows (ruta y version)"

==============================================================================================================
4) AWS CLI en Windows (ruta y version)  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "where.exe aws" { where.exe aws }

--- where.exe aws
C:\Program Files\Amazon\AWSCLIV2\aws.exe
PS C:\Users\junortiz> Run "aws --version (directo al exe estándar)" {
>>   if (Test-Path "C:\Program Files\Amazon\AWSCLIV2\aws.exe") {
>>     & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" --version
>>   } else {
>>     "NO existe: C:\Program Files\Amazon\AWSCLIV2\aws.exe"
>>   }
>> }

--- aws --version (directo al exe estándar)
aws-cli/2.13.18 Python/3.11.5 Windows/10 exe/AMD64 prompt/off
PS C:\Users\junortiz> Run "aws configure list-profiles (Windows)" {
>>   if (Test-Path "C:\Program Files\Amazon\AWSCLIV2\aws.exe") {
>>     & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" configure list-profiles
>>   } else {
>>     "AWS CLI v2 no encontrado en ruta estándar"
>>   }
>> }

--- aws configure list-profiles (Windows)
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "5) LocalStack desde Windows: puerto 4566 y health"

==============================================================================================================
5) LocalStack desde Windows: puerto 4566 y health  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "Test-NetConnection localhost:4566" { Test-NetConnection localhost -Port 4566 }

--- Test-NetConnection localhost:4566
TestNetConnectionResult
PS C:\Users\junortiz> Run "netstat -ano | findstr :4566 (PID)" { cmd /c "netstat -ano | findstr :4566" }

--- netstat -ano | findstr :4566 (PID)
  TCP    127.0.0.1:49666        127.0.0.1:4566         TIME_WAIT       0
  TCP    127.0.0.1:59922        127.0.0.1:4566         TIME_WAIT       0
  TCP    127.0.0.1:60636        127.0.0.1:4566         TIME_WAIT       0
  TCP    127.0.0.1:60651        127.0.0.1:4566         TIME_WAIT       0
  TCP    192.168.78.172:59893   192.168.78.172:4566    SYN_SENT        20276
PS C:\Users\junortiz> Run "curl http://localhost:4566/_localstack/health" { curl http://localhost:4566/_localstack/health }

--- curl http://localhost:4566/_localstack/health

Advertencia de seguridad: riesgo de ejecución de script
Invoke-WebRequest analiza el contenido de la página web. El código de script de la página web se puede ejecutar cuando se analiza la página.
      ACCIÓN RECOMENDADA:
      Usa el modificador -UseBasicParsing para evitar la ejecución de código de script.

      ¿Quieres continuar?

[S] Sí  [O] Sí a todo  [N] No  [T] No a todo  [U] Suspender  [?] Ayuda (el valor predeterminado es "N"): O
{"services": {"acm": "disabled", "apigateway": "disabled", "cloudformation": "disabled", "cloudwatch": "disabled", "config": "disabled", "dynamodb": "disabled", "dynamodbstreams": "disabled", "ec2": "disabled", "es": "disabled", "events": "disabled", "firehose": "disabled", "iam": "disabled", "kinesis": "disabled", "kms": "disabled", "lambda": "disabled", "logs": "disabled", "opensearch": "disabled", "redshift": "disabled", "resource-groups": "disabled", "resourcegroupstaggingapi": "disabled", "route53": "disabled", "route53resolver": "disabled", "s3": "running", "s3control": "disabled", "scheduler": "disabled", "secretsmanager": "disabled", "ses": "disabled", "sns": "disabled", "sqs": "disabled", "ssm": "disabled", "stepfunctions": "disabled", "sts": "disabled", "support": "disabled", "swf": "disabled", "transcribe": "disabled"}, "edition": "community", "version": "3.8.1"}
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "6) Docker dentro de WSL (si existe) + estado del daemon"

==============================================================================================================
6) Docker dentro de WSL (si existe) + estado del daemon  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "WSL: docker --version / docker ps" {
>>   wsl.exe -d $distro -- bash -lc @'
>> echo "docker path: $(command -v docker || echo NO_DOCKER)"
>> docker --version 2>/dev/null || true
>> docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}" 2>/dev/null || true
>> '@
>> }

--- WSL: docker --version / docker ps
docker
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "7) LocalStack dentro de WSL: container, health, puerto, quien escucha (docker-proxy)"

==============================================================================================================
7) LocalStack dentro de WSL: container, health, puerto, quien escucha (docker-proxy)  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "WSL: Verificar listener 4566 y health desde WSL" {
>>   wsl.exe -d $distro -- bash -lc @'
>> echo "== ss -lntp | grep :4566 =="
>> (ss -lntp | grep ":4566" || true)
>>
>> echo ""
>> echo "== LocalStack health =="
>> curl -s http://localhost:4566/_localstack/health || true
>> '@
>> }

--- WSL: Verificar listener 4566 y health desde WSL
grep: ==
(ss -lntp | grep :4566 || true)
System.Management.Automation.RemoteException
echo
echo ==: No such file or directory
grep: LocalStack: No such file or directory
grep: health: No such file or directory
grep: ==
curl -s http://localhost:4566/_localstack/health || true: No such file or directory
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "8) LocalStack container detalles (si el contenedor se llama 'localstack')"

==============================================================================================================
8) LocalStack container detalles (si el contenedor se llama 'localstack')  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "WSL: docker inspect / logs / puertos / filesystem layout" {
>>   wsl.exe -d $distro -- bash -lc @'
>> set +e
>>
>> echo "== docker ps (localstack) =="
>> docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}" | grep -i localstack || echo "No hay contenedor 'localstack' corriendo"
>>
>> echo ""
>> echo "== docker inspect (resumen) =="
>> docker inspect localstack --format "Name={{.Name}} Image={{.Config.Image}} Status={{.State.Status}} Health={{if .State.Health}}{{.State.Health.Status}}{{else}}(no-health){{end}}" 2>/dev/null || true
>> docker port localstack 2>/dev/null || true
>>
>> echo ""
>> echo "== últimos logs (100) =="
>> docker logs --tail 100 localstack 2>/dev/null || true
>>
>> echo ""
>> echo "== RUTAS dentro del contenedor: /var/lib/localstack =="
>> docker exec -it localstack sh -lc "ls -la /var/lib/localstack; echo; ls -la /var/lib/localstack/logs; echo; ls -la /var/lib/localstack/state" 2>/dev/null || true
>>
>> echo ""
>> echo "== Init hooks disponibles (si existen) =="
>> docker exec -it localstack sh -lc "ls -la /etc/localstack/init 2>/dev/null || true; ls -la /etc/localstack/init/ready.d 2>/dev/null || true" 2>/dev/null || true
>> '@
>> }

--- WSL: docker inspect / logs / puertos / filesystem layout
/bin/bash: -c: line 3: syntax error near unexpected token `('
/bin/bash: -c: line 3: `echo "== docker ps (localstack) =="'
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "9) Java/AWS CLI dentro de Ubuntu (WSL) - confirmar si están instalados"

==============================================================================================================
9) Java/AWS CLI dentro de Ubuntu (WSL) - confirmar si están instalados  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "WSL: java/aws instalados?" {
>>   wsl.exe -d $distro -- bash -lc @'
>> echo "== Java en WSL =="
>> (command -v java && java -version) || echo "JAVA NO instalado en WSL"
>>
>> echo ""
>> echo "== AWS CLI en WSL =="
>> (command -v aws && aws --version) || echo "AWS CLI NO instalado en WSL"
>> '@
>> }

--- WSL: java/aws instalados?
==
PS C:\Users\junortiz>
PS C:\Users\junortiz> Section "10) RESUMEN AUTOMÁTICO (lo que suele causar fallos típicos)"

==============================================================================================================
10) RESUMEN AUTOMÁTICO (lo que suele causar fallos típicos)  [2026-03-04 18:31:47]
==============================================================================================================
PS C:\Users\junortiz> Run "Resumen basado en lo detectado" {
>> @"
>> - Tu WSL y Ubuntu: OK (distro '$distro' existe y corre).
>> - LocalStack: responde en 4566 y muestra edition/version en /_localstack/health.
>> - Si ves 'docker-proxy' escuchando en 4566 dentro de WSL, LocalStack está publicado vía Docker.
>> - Java: instalado en Windows (por JAVA_HOME), pero 'java' puede no estar en PATH (depende de bin\java.exe).
>> - AWS CLI: instalado en Windows (AWSCLIV2\aws.exe); NO está instalado en Ubuntu WSL.
>> - Si un comando falla con 'command not found' dentro de Ubuntu, seguramente era cmdlet de PowerShell (Test-NetConnection/Get-NetTCPConnection/Format-Table).
>> - Si falla con 'WSL_E_DISTRO_NOT_FOUND', el nombre de distro usado no coincide (tu distro real es '$distro', no 'Ubuntu').
>> "@
>> }

--- Resumen basado en lo detectado
- Tu WSL y Ubuntu: OK (distro 'Ubuntu2204' existe y corre).
- LocalStack: responde en 4566 y muestra edition/version en /_localstack/health.
- Si ves 'docker-proxy' escuchando en 4566 dentro de WSL, LocalStack está publicado vía Docker.
- Java: instalado en Windows (por JAVA_HOME), pero 'java' puede no estar en PATH (depende de bin\java.exe).
- AWS CLI: instalado en Windows (AWSCLIV2\aws.exe); NO está instalado en Ubuntu WSL.
- Si un comando falla con 'command not found' dentro de Ubuntu, seguramente era cmdlet de PowerShell (Test-NetConnection/Get-NetTCPConnection/Format-Table).
- Si falla con 'WSL_E_DISTRO_NOT_FOUND', el nombre de distro usado no coincide (tu distro real es 'Ubuntu2204', no 'Ubuntu').
PS C:\Users\junortiz>
PS C:\Users\junortiz> # ========= FIN =========

Windows PowerShell
Copyright (C) Microsoft Corporation. Todos los derechos reservados.

Instale la versión más reciente de PowerShell para obtener nuevas características y mejoras. https://aka.ms/PSWindows

PS C:\Users\junortiz> wsl --version
Versión de WSL: 2.6.3.0
Versión de kernel: 6.6.87.2-1
Versión de WSLg: 1.0.71
Versión de MSRDC: 1.2.6353
Versión de Direct3D: 1.611.1-81528511
Versión de DXCore: 10.0.26100.1-240331-1435.ge-release
Versión de Windows: 10.0.26100.7840
PS C:\Users\junortiz> wsl --status
Distribución predeterminada: Ubuntu2204
Versión predeterminada: 2
PS C:\Users\junortiz>
PS C:\Users\junortiz>
PS C:\Users\junortiz> wsl --list --verbose
  NAME          STATE           VERSION
* Ubuntu2204    Stopped         2
PS C:\Users\junortiz> # o corto:
PS C:\Users\junortiz> wsl -l -v
  NAME          STATE           VERSION
* Ubuntu2204    Stopped         2
PS C:\Users\junortiz> wsl --list --running
No hay distribuciones en ejecución.
PS C:\Users\junortiz> notepad $env:USERPROFILE\.wslconfig
PS C:\Users\junortiz>
PS C:\Users\junortiz> [wsl2]
No se encuentra el tipo [wsl2].
En línea: 1 Carácter: 1
+ [wsl2]
+ ~~~~~~
    + CategoryInfo          : InvalidOperation: (wsl2:TypeName) [], RuntimeException
    + FullyQualifiedErrorId : TypeNotFound

PS C:\Users\junortiz> networkingMode=mirrored
networkingMode=mirrored : El término 'networkingMode=mirrored' no se reconoce como nombre de un cmdlet, función, archivo de script o programa ejecutable.
Compruebe si escribió correctamente el nombre o, si incluyó una ruta de acceso, compruebe que dicha ruta es correcta e inténtelo de nuevo.
En línea: 1 Carácter: 1
+ networkingMode=mirrored
+ ~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : ObjectNotFound: (networkingMode=mirrored:String) [], CommandNotFoundException
    + FullyQualifiedErrorId : CommandNotFoundException

PS C:\Users\junortiz> notepad $env:USERPROFILE\.wslconfig
PS C:\Users\junortiz> wsl.exe -d Ubuntu -- cat /etc/os-release
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
PS C:\Users\junortiz> wsl.exe -d Ubuntu -- lsb_release -a
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
PS C:\Users\junortiz> wsl.exe -d Ubuntu -- uname -a
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
PS C:\Users\junortiz> wsl.exe -d Ubuntu -- hostname -I
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
PS C:\Users\junortiz> java -version
java : El término 'java' no se reconoce como nombre de un cmdlet, función, archivo de script o programa ejecutable. Compruebe si escribió correctamente el
nombre o, si incluyó una ruta de acceso, compruebe que dicha ruta es correcta e inténtelo de nuevo.
En línea: 1 Carácter: 1
+ java -version
+ ~~~~
    + CategoryInfo          : ObjectNotFound: (java:String) [], CommandNotFoundException
    + FullyQualifiedErrorId : CommandNotFoundException


Suggestion [3,General]: No se encontró el comando java, pero existe en la ubicación actual. Windows PowerShell no carga comandos de la ubicación actual de forma predeterminada. Si confía en este comando, escriba ".\java". Vea "get-help about_Command_Precedence" para obtener información más detallada.
PS C:\Users\junortiz> where.exe java
C:\Users\junortiz\java
PS C:\Users\junortiz> Get-Command java | Format-List *
Get-Command : El término 'java' no se reconoce como nombre de un cmdlet, función, archivo de script o programa ejecutable. Compruebe si escribió
correctamente el nombre o, si incluyó una ruta de acceso, compruebe que dicha ruta es correcta e inténtelo de nuevo.
En línea: 1 Carácter: 1
+ Get-Command java | Format-List *
+ ~~~~~~~~~~~~~~~~
    + CategoryInfo          : ObjectNotFound: (java:String) [Get-Command], CommandNotFoundException
    + FullyQualifiedErrorId : CommandNotFoundException,Microsoft.PowerShell.Commands.GetCommandCommand


Suggestion [3,General]: No se encontró el comando java, pero existe en la ubicación actual. Windows PowerShell no carga comandos de la ubicación actual de forma predeterminada. Si confía en este comando, escriba ".\java". Vea "get-help about_Command_Precedence" para obtener información más detallada.
PS C:\Users\junortiz> echo $env:JAVA_HOME
C:\Users\junortiz\corretto-17
PS C:\Users\junortiz> echo $env:PATH
C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.3\bin;C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.3\libnvvp;C:\WINDOWS\system32;C:\WINDOWS;C:\WINDOWS\System32\Wbem;C:\WINDOWS\System32\WindowsPowerShell\v1.0\;C:\WINDOWS\System32\OpenSSH\;C:\Program Files (x86)\Microsoft SQL Server\150\DTS\Binn\;C:\Program Files\Azure Data Studio\bin;C:\Program Files\NVIDIA Corporation\Nsight Compute 2023.3.1\;C:\Program Files (x86)\NVIDIA Corporation\PhysX\Common;C:\Program Files\Cloudflare\Cloudflare WARP\;C:\Program Files\Amazon\AWSCLIV2\;C:\PythonEVN\AllEvn\Scripts;C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.3\bin;C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.3\libnvvp;C:\WINDOWS\system32;C:\WINDOWS;C:\WINDOWS\System32\Wbem;C:\WINDOWS\System32\WindowsPowerShell\v1.0\;C:\WINDOWS\System32\OpenSSH\;C:\Program Files (x86)\Microsoft SQL Server\150\DTS\Binn\;C:\Program Files\Azure Data Studio\bin;C:\Program Files\Cloudflare\Cloudflare WARP\;C:\Program Files\NVIDIA Corporation\Nsight Compute 2023.3.1\;C:\Program Files (x86)\NVIDIA Corporation\PhysX\Common;C:\Users\junortiz\scoop\shims;C:\Users\junortiz\AppData\Local\Programs\Python\Python39\Scripts\;C:\Users\junortiz\AppData\Local\Programs\Python\Python39\;C:\Users\junortiz\AppData\Local\Microsoft\WindowsApps;C:\Users\junortiz\AppData\Local\Programs\Microsoft VS Code\bin;C:\Users\junortiz\AppData\Local\Programs\Git\cmd;C:\Users\junortiz\AppData\Local\Programs\Ollama;C:\Users\junortiz\corretto-17;C:\Users\junortiz\Apps\node-v20.20.0-win-x64;
PS C:\Users\junortiz> wsl.exe -d Ubuntu -- java -version
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
PS C:\Users\junortiz> wsl
Welcome to Ubuntu 22.04.5 LTS (GNU/Linux 6.6.87.2-microsoft-standard-WSL2 x86_64)

 * Documentation:  https://help.ubuntu.com
 * Management:     https://landscape.canonical.com
 * Support:        https://ubuntu.com/pro

 System information as of Wed Mar  4 18:24:31 -05 2026

  System load:           0.07
  Usage of /:            0.7% of 1006.85GB
  Memory usage:          4%
  Swap usage:            0%
  Processes:             79
  Users logged in:       0
  IPv4 address for eth0: 192.168.78.172
  IPv6 address for eth0: fd4f:270b:99c8::433
  IPv6 address for eth0: fd4f:270b:99c8:0:e931:d91f:7370:aee0
  IPv6 address for eth0: fd4f:270b:99c8:0:2f2e:cd0f:cf1e:fdb7

 * Strictly confined Kubernetes makes edge and IoT secure. Learn how MicroK8s
   just raised the bar for easy, resilient and secure K8s cluster deployment.

   https://ubuntu.com/engage/secure-kubernetes-at-the-edge

This message is shown once a day. To disable it please create the
/root/.hushlogin file.
root@pb0b0976046:/mnt/c/Users/junortiz# wsl.exe -d Ubuntu -- java -version
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
root@pb0b0976046:/mnt/c/Users/junortiz# wsl.exe -d Ubuntu -- bash -lc 'echo $JAVA_HOME'
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
root@pb0b0976046:/mnt/c/Users/junortiz# aws --version
Command 'aws' not found, but can be installed with:
snap install aws-cli  # version 1.44.50, or
apt  install awscli   # version 1.22.34-1
See 'snap info aws-cli' for additional versions.
root@pb0b0976046:/mnt/c/Users/junortiz#
where.exe aws
Get-Command aws | Format-List *
C:\Program Files\Amazon\AWSCLIV2\aws.exe
Get-Command: command not found
Format-List: command not found
root@pb0b0976046:/mnt/c/Users/junortiz# aws configure list-profiles
Command 'aws' not found, but can be installed with:
snap install aws-cli  # version 1.44.50, or
apt  install awscli   # version 1.22.34-1
See 'snap info aws-cli' for additional versions.
root@pb0b0976046:/mnt/c/Users/junortiz# aws configure list
Command 'aws' not found, but can be installed with:
snap install aws-cli  # version 1.44.50, or
apt  install awscli   # version 1.22.34-1
See 'snap info aws-cli' for additional versions.
root@pb0b0976046:/mnt/c/Users/junortiz#
wsl.exe -d Ubuntu -- aws --version
wsl.exe -d Ubuntu -- aws configure list-profiles
wsl.exe -d Ubuntu -- aws configure list
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
root@pb0b0976046:/mnt/c/Users/junortiz# Test-NetConnection -ComputerName localhost -Port 4566
Test-NetConnection: command not found
root@pb0b0976046:/mnt/c/Users/junortiz# Get-NetTCPConnection -LocalPort 4566 -State Listen | Format-Table -AutoSize
Format-Table: command not found
Get-NetTCPConnection: command not found
root@pb0b0976046:/mnt/c/Users/junortiz#
netstat -ano | findstr ":4566"
# luego con el PID:
# tasklist /fi "PID eq <PID>"
Command 'netstat' not found, but can be installed with:
apt install net-tools
findstr: command not found
root@pb0b0976046:/mnt/c/Users/junortiz#
curl http://localhost:4566/_localstack/health
# o también suele existir:
curl http://localhost:4566/health
{"services": {"acm": "disabled", "apigateway": "disabled", "cloudformation": "disabled", "cloudwatch": "disabled", "config": "disabled", "dynamodb": "disabled", "dynamodbstreams": "disabled", "ec2": "disabled", "es": "disabled", "events": "disabled", "firehose": "disabled", "iam": "disabled", "kinesis": "disabled", "kms": "disabled", "lambda": "disabled", "logs": "disabled", "opensearch": "disabled", "redshift": "disabled", "resource-groups": "disabled", "resourcegroupstaggingapi": "disabled", "route53": "disabled", "route53resolver": "disabled", "s3": "running", "s3control": "disabled", "scheduler": "disabled", "secretsmanager": "disabled", "ses": "disabled", "sns": "disabled", "sqs": "disabled", "ssm": "disabled", "stepfunctions": "disabled", "sts": "disabled", "support": "disabled", "swf": "disabled", "transcribe": "disabled"}, "edition": "community", "version": "3.8.1"}<?xml version='1.0' encoding='utf-8'?>
<Error><Code>NoSuchBucket</Code><Message>The specified bucket does not exist</Message><RequestId>f1794b77-0ffa-45ce-a6b4-57aedc57cd2b</RequestId><BucketName>health</BucketName></Error>root@pb0b097wsl.exe -d Ubuntu -- curl -s http://localhost:4566/_localstack/healthhost:4566/_localstack/health
No hay ninguna distribución con el nombre proporcionado.
Código de error: Wsl/Service/WSL_E_DISTRO_NOT_FOUND
root@pb0b0976046:/mnt/c/Users/junortiz#

Windows PowerShell
Copyright (C) Microsoft Corporation. Todos los derechos reservados.

Instale la versión más reciente de PowerShell para obtener nuevas características y mejoras. https://aka.ms/PSWindows

PS C:\Users\junortiz> wsl --version
Versión de WSL: 2.6.3.0
Versión de kernel: 6.6.87.2-1
Versión de WSLg: 1.0.71
Versión de MSRDC: 1.2.6353
Versión de Direct3D: 1.611.1-81528511
Versión de DXCore: 10.0.26100.1-240331-1435.ge-release
Versión de Windows: 10.0.26100.7840
PS C:\Users\junortiz> wsl --status
Distribución predeterminada: Ubuntu2204
Versión predeterminada: 2
PS C:\Users\junortiz> wsl -l -v
  NAME          STATE           VERSION
* Ubuntu2204    Running         2
PS C:\Users\junortiz> wsl --list --running
Distribuciones de subsistema de Windows para Linux:
Ubuntu2204 (Predeterminado)
PS C:\Users\junortiz> wsl -d Ubuntu2204
root@pb0b0976046:/mnt/c/Users/junortiz# wsl -d Ubuntu2204
Command 'wsl' not found, but can be installed with:
apt install wsl
root@pb0b0976046:/mnt/c/Users/junortiz# wsl.exe -d Ubuntu2204 -- cat /etc/os-release
wsl.exe -d Ubuntu2204 -- uname -a
wsl.exe -d Ubuntu2204 -- hostname -I
PRETTY_NAME="Ubuntu 22.04.5 LTS"
NAME="Ubuntu"
VERSION_ID="22.04"
VERSION="22.04.5 LTS (Jammy Jellyfish)"
VERSION_CODENAME=jammy
ID=ubuntu
ID_LIKE=debian
HOME_URL="https://www.ubuntu.com/"
SUPPORT_URL="https://help.ubuntu.com/"
BUG_REPORT_URL="https://bugs.launchpad.net/ubuntu/"
PRIVACY_POLICY_URL="https://www.ubuntu.com/legal/terms-and-policies/privacy-policy"
UBUNTU_CODENAME=jammy
Linux pb0b0976046 6.6.87.2-microsoft-standard-WSL2 #1 SMP PREEMPT_DYNAMIC Thu Jun  5 18:30:46 UTC 2025 x86_64 x86_64 x86_64 GNU/Linux
192.168.78.172 100.96.229.196 172.18.0.1 172.17.0.1 fd4f:270b:99c8::433 fd4f:270b:99c8:0:e931:d91f:7370:aee0 fd4f:270b:99c8:0:2f2e:cd0f:cf1e:fdb7 2606:4700:cf1:1000::8753
root@pb0b0976046:/mnt/c/Users/junortiz# & "$env:JAVA_HOME\bin\java.exe" -version
-bash: syntax error near unexpected token `&'
root@pb0b0976046:/mnt/c/Users/junortiz# where.exe java
Get-ChildItem "$env:JAVA_HOME\bin\java.exe"
C:\Users\junortiz\java
Get-ChildItem: command not found
root@pb0b0976046:/mnt/c/Users/junortiz# aws --version
where.exe aws
Command 'aws' not found, but can be installed with:
snap install aws-cli  # version 1.44.50, or
apt  install awscli   # version 1.22.34-1
See 'snap info aws-cli' for additional versions.
C:\Program Files\Amazon\AWSCLIV2\aws.exe
root@pb0b0976046:/mnt/c/Users/junortiz# & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" --version
-bash: syntax error near unexpected token `&'
root@pb0b0976046:/mnt/c/Users/junortiz# Test-NetConnection -ComputerName localhost -Port 4566
Test-NetConnection: command not found
root@pb0b0976046:/mnt/c/Users/junortiz# netstat -ano | findstr ":4566"
findstr: command not found
Command 'netstat' not found, but can be installed with:
apt install net-tools
root@pb0b0976046:/mnt/c/Users/junortiz# curl http://localhost:4566/_localstack/health
{"services": {"acm": "disabled", "apigateway": "disabled", "cloudformation": "disabled", "cloudwatch": "disabled", "config": "disabled", "dynamodb": "disabled", "dynamodbstreams": "disabled", "ec2": "disabled", "es": "disabled", "events": "disabled", "firehose": "disabled", "iam": "disabled", "kinesis": "disabled", "kms": "disabled", "lambda": "disabled", "logs": "disabled", "opensearch": "disabled", "redshift": "disabled", "resource-groups": "disabled", "resourcegroupstaggingapi": "disabled", "route53": "disabled", "route53resolver": "disabled", "s3": "running", "s3control": "disabled", "scheduler": "disabled", "secretsmanager": "disabled", "ses": "disabled", "sns": "disabled", "sqs": "disabled", "ssm": "disabled", "stepfunctions": "disabled", "sts": "disabled", "support": "disabled", "swf": "disabled", "transcribe": "disabled"}, "edition": "community", "version": "3.8.1"}root@pb0b0976046:/mnt/c/Users/junortiz#
root@pb0b0976046:/mnt/c/Users/junortiz# cat /etc/os-release
uname -a
hostname -I
PRETTY_NAME="Ubuntu 22.04.5 LTS"
NAME="Ubuntu"
VERSION_ID="22.04"
VERSION="22.04.5 LTS (Jammy Jellyfish)"
VERSION_CODENAME=jammy
ID=ubuntu
ID_LIKE=debian
HOME_URL="https://www.ubuntu.com/"
SUPPORT_URL="https://help.ubuntu.com/"
BUG_REPORT_URL="https://bugs.launchpad.net/ubuntu/"
PRIVACY_POLICY_URL="https://www.ubuntu.com/legal/terms-and-policies/privacy-policy"
UBUNTU_CODENAME=jammy
Linux pb0b0976046 6.6.87.2-microsoft-standard-WSL2 #1 SMP PREEMPT_DYNAMIC Thu Jun  5 18:30:46 UTC 2025 x86_64 x86_64 x86_64 GNU/Linux
192.168.78.172 100.96.229.196 172.18.0.1 172.17.0.1 fd4f:270b:99c8::433 fd4f:270b:99c8:0:e931:d91f:7370:aee0 fd4f:270b:99c8:0:2f2e:cd0f:cf1e:fdb7 2606:4700:cf1:1000::8753
root@pb0b0976046:/mnt/c/Users/junortiz# java -version
which java
echo $JAVA_HOME
Command 'java' not found, but can be installed with:
apt install default-jre              # version 2:1.11-72build2, or
apt install openjdk-11-jre-headless  # version 11.0.30+7-1ubuntu1~22.04
apt install openjdk-17-jre-headless  # version 17.0.18+8-1~22.04.1
apt install openjdk-18-jre-headless  # version 18.0.2+9-2~22.04
apt install openjdk-21-jre-headless  # version 21.0.10+7-1~22.04
apt install openjdk-25-jre-headless  # version 25.0.2+10-1~22.04
apt install openjdk-8-jre-headless   # version 8u482-ga~us1-0ubuntu1~22.04

root@pb0b0976046:/mnt/c/Users/junortiz# command -v aws && aws --version || echo "AWS CLI NO está instalado en Ubuntu (WSL)"
AWS CLI NO está instalado en Ubuntu (WSL)
root@pb0b0976046:/mnt/c/Users/junortiz# curl -s http://localhost:4566/_localstack/health
{"services": {"acm": "disabled", "apigateway": "disabled", "cloudformation": "disabled", "cloudwatch": "disabled", "config": "disabled", "dynamodb": "disabled", "dynamodbstreams": "disabled", "ec2": "disabled", "es": "disabled", "events": "disabled", "firehose": "disabled", "iam": "disabled", "kinesis": "disabled", "kms": "disabled", "lambda": "disabled", "logs": "disabled", "opensearch": "disabled", "redshift": "disabled", "resource-groups": "disabled", "resourcegroupstaggingapi": "disabled", "route53": "disabled", "route53resolver": "disabled", "s3": "running", "s3control": "disabled", "scheduler": "disabled", "secretsmanager": "disabled", "ses": "disabled", "sns": "disabled", "sqs": "disabled", "ssm": "disabled", "stepfunctions": "disabled", "sts": "disabled", "support": "disabled", "swf": "disabled", "transcribe": "disabled"}, "edition": "community", "version": "3.8.1"}root@pb0b0976046:/mnt/c/Users/junortiz#
root@pb0b0976046:/mnt/c/Users/junortiz# ss -lntp | grep ':4566' || echo "Nada escuchando en 4566"
LISTEN 0      4096          0.0.0.0:4566      0.0.0.0:*    users:(("docker-proxy",pid=609,fd=7))
LISTEN 0      4096             [::]:4566         [::]:*    users:(("docker-proxy",pid=615,fd=7))
root@pb0b0976046:/mnt/c/Users/junortiz# notepad $env:USERPROFILE\.wslconfig
Command 'notepad' not found, did you mean:
  command 'notepod' from snap notepod (0.3.0)
See 'snap info <snapname>' for additional versions.
root@pb0b0976046:/mnt/c/Users/junortiz#
Windows PowerShell
Copyright (C) Microsoft Corporation. Todos los derechos reservados.

Instale la versión más reciente de PowerShell para obtener nuevas características y mejoras. https://aka.ms/PSWindows

PS C:\Users\junortiz> $distro = "Ubuntu2204"
PS C:\Users\junortiz>
PS C:\Users\junortiz> Write-Host "=== WSL ==="
=== WSL ===
PS C:\Users\junortiz> wsl --version
Versión de WSL: 2.6.3.0
Versión de kernel: 6.6.87.2-1
Versión de WSLg: 1.0.71
Versión de MSRDC: 1.2.6353
Versión de Direct3D: 1.611.1-81528511
Versión de DXCore: 10.0.26100.1-240331-1435.ge-release
Versión de Windows: 10.0.26100.7840
PS C:\Users\junortiz> wsl --status
Distribución predeterminada: Ubuntu2204
Versión predeterminada: 2
PS C:\Users\junortiz> wsl -l -v
  NAME          STATE           VERSION
* Ubuntu2204    Running         2
PS C:\Users\junortiz>
PS C:\Users\junortiz> Write-Host "`n=== INICIANDO DISTRO (si está apagada) ==="

=== INICIANDO DISTRO (si está apagada) ===
PS C:\Users\junortiz> wsl -d $distro -e bash -lc "echo 'WSL OK: ' && cat /etc/os-release | head -n 3"
WSL OK:
PRETTY_NAME="Ubuntu 22.04.5 LTS"
NAME="Ubuntu"
VERSION_ID="22.04"
PS C:\Users\junortiz>
PS C:\Users\junortiz> Write-Host "`n=== UBUNTU (WSL) ==="

=== UBUNTU (WSL) ===
PS C:\Users\junortiz> wsl.exe -d $distro -- uname -a
Linux pb0b0976046 6.6.87.2-microsoft-standard-WSL2 #1 SMP PREEMPT_DYNAMIC Thu Jun  5 18:30:46 UTC 2025 x86_64 x86_64 x86_64 GNU/Linux
PS C:\Users\junortiz> wsl.exe -d $distro -- hostname -I
192.168.78.172 100.96.229.196 172.18.0.1 172.17.0.1 fd4f:270b:99c8::433 fd4f:270b:99c8:0:e931:d91f:7370:aee0 fd4f:270b:99c8:0:2f2e:cd0f:cf1e:fdb7 2606:4700:cf1:1000::8753
PS C:\Users\junortiz>
PS C:\Users\junortiz> Write-Host "`n=== JAVA (Windows) ==="

=== JAVA (Windows) ===
PS C:\Users\junortiz> if (Test-Path "$env:JAVA_HOME\bin\java.exe") {
>>   & "$env:JAVA_HOME\bin\java.exe" -version
>> } else {
>>   Write-Host "No encuentro java.exe en $env:JAVA_HOME\bin"
>> }
openjdk version "17.0.17" 2025-10-21 LTS
OpenJDK Runtime Environment Corretto-17.0.17.10.1 (build 17.0.17+10-LTS)
OpenJDK 64-Bit Server VM Corretto-17.0.17.10.1 (build 17.0.17+10-LTS, mixed mode, sharing)
PS C:\Users\junortiz> where.exe java
C:\Users\junortiz\java
PS C:\Users\junortiz>
PS C:\Users\junortiz> Write-Host "`n=== AWS CLI (Windows) ==="

=== AWS CLI (Windows) ===
PS C:\Users\junortiz> & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" --version
aws-cli/2.13.18 Python/3.11.5 Windows/10 exe/AMD64 prompt/off
PS C:\Users\junortiz> where.exe aws
C:\Program Files\Amazon\AWSCLIV2\aws.exe
PS C:\Users\junortiz>
PS C:\Users\junortiz> Write-Host "`n=== LOCALSTACK 4566 (Windows) ==="

=== LOCALSTACK 4566 (Windows) ===
PS C:\Users\junortiz> Test-NetConnection localhost -Port 4566


ComputerName     : localhost
RemoteAddress    : 127.0.0.1
RemotePort       : 4566
InterfaceAlias   : Loopback Pseudo-Interface 1
SourceAddress    : 127.0.0.1
TcpTestSucceeded : True



PS C:\Users\junortiz>
root@pb0b0976046:/mnt/c/Users/junortiz#
root@pb0b0976046:/mnt/c/Users/junortiz# # 1) ¿Existe el CLI "localstack" (instalación nativa)?
command -v localstack && localstack --version || echo "localstack CLI no instalado en WSL"

# 2) Si está instalado por pip, muestra paquete y versión
python3 -m pip show localstack 2>/dev/null || echo "localstack no está instalado via pip"

# 3) ¿Hay Docker en WSL? (si existe, LocalStack podría ser contenedor)
command -v docker && docker --version || echo "docker no está instalado en WSL"

# 4) Si hay Docker: ¿existe contenedor LocalStack?
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}" 2>/dev/null | grep -i localstack || true
localstack CLI no instalado en WSL
localstack no está instalado via pip
/usr/bin/docker
Docker version 29.1.5, build 0e6fee6
localstack   localstack/localstack:3   4510-4559/tcp, 5678/tcp, 0.0.0.0:4566->4566/tcp, [::]:4566->4566/tcp   Up 4 minutes (healthy)
root@pb0b0976046:/mnt/c/Users/junortiz# docker exec -it localstack ls -la /var/lib/localstackdocker exec -it localstack ls -la /var/lib/localstack/logsdocker exec -it localstack ls -la /var/lib/localstack/state
ls: cannot access '/var/lib/localstackdocker': No such file or directory
ls: cannot access 'exec': No such file or directory
ls: cannot access 'localstack': No such file or directory
ls: cannot access 'ls': No such file or directory
ls: cannot access '/var/lib/localstack/logsdocker': No such file or directory
ls: cannot access 'exec': No such file or directory
ls: cannot access 'localstack': No such file or directory
ls: cannot access 'ls': No such file or directory
/var/lib/localstack/state:
total 0
 3096224744891622 drwxrwxrwx 1 root root 4096 Mar  4 23:22 ..
34621422135853182 drwxrwxrwx 1 root root 4096 Mar  4 22:41 .
root@pb0b0976046:/mnt/c/Users/junortiz#
docker exec -it localstack ls -la /var/lib/localstack
docker exec -it localstack ls -la /var/lib/localstack/logs
docker exec -it localstack ls -la /var/lib/localstack/state
total 4
drwxrwxrwx 1 root root 4096 Mar  4 23:22 .
drwxr-xr-x 1 root root 4096 Oct  8  2024 ..
drwxrwxrwx 1 root root 4096 Mar  4 22:41 cache
drwxrwxrwx 1 root root 4096 Feb  4 23:11 init-scripts
drwxrwxrwx 1 root root 4096 Mar  4 22:41 lib
drwxrwxrwx 1 root root 4096 Mar  4 22:41 logs
drwxrwxrwx 1 root root 4096 Mar  4 23:22 minio-data
drwxrwxrwx 1 root root 4096 Mar  4 22:55 seed
drwxrwxrwx 1 root root 4096 Mar  4 22:41 state
drwxrwxrwx 1 root root 4096 Mar  4 22:41 tmp
total 0
drwxrwxrwx 1 root root 4096 Mar  4 22:41 .
drwxrwxrwx 1 root root 4096 Mar  4 23:22 ..
total 0
drwxrwxrwx 1 root root 4096 Mar  4 22:41 .
drwxrwxrwx 1 root root 4096 Mar  4 23:22 ..
root@pb0b0976046:/mnt/c/Users/junortiz#