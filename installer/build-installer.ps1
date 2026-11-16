# ============================================================================
#  build-installer.ps1 - Compila Farmac-Setup.exe de principio a fin
#  ---------------------------------------------------------------------------
#  Pasos que realiza:
#    1. Compila el JAR con Apache Ant (ant clean dist).
#    2. Calcula los modulos de la JVM con jdeps y genera un JRE minimo con jlink.
#    3. Compila el helper Java de respaldo (DbSetup).
#    4. Genera los recursos graficos del asistente si no existen.
#    5. Ejecuta ISCC.exe (Inno Setup 6) sobre installer\Farmac.iss.
#
#  Resultado: installer\Output\Farmac-Setup.exe
#
#  Requisitos: JDK 21, Apache Ant e Inno Setup 6.
#  Uso:  powershell -ExecutionPolicy Bypass -File installer\build-installer.ps1
# ============================================================================

[CmdletBinding()]
param(
    [switch]$SkipAnt,
    [switch]$SkipJre,
    [switch]$SkipAssets
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$InstallerDir = $PSScriptRoot
$Root = Split-Path -Parent $InstallerDir
Set-Location $Root

function Escribir($mensaje) { Write-Host "==> $mensaje" -ForegroundColor Cyan }
function Fallar($mensaje) { Write-Host "ERROR: $mensaje" -ForegroundColor Red; exit 1 }

# ---------------------------------------------------------------------------
#  Localizacion de herramientas
# ---------------------------------------------------------------------------

function Obtener-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
        return $env:JAVA_HOME
    }
    $candidatos = @(
        'C:\Program Files\Eclipse Adoptium\jdk-21*',
        'C:\Program Files\Eclipse Adoptium\jdk-*',
        'C:\Program Files\Java\jdk-21*',
        'C:\Program Files\Microsoft\jdk-21*'
    )
    foreach ($patron in $candidatos) {
        $dir = Get-ChildItem $patron -Directory -ErrorAction SilentlyContinue |
               Sort-Object Name -Descending | Select-Object -First 1
        if ($dir -and (Test-Path (Join-Path $dir.FullName 'bin\javac.exe'))) {
            return $dir.FullName
        }
    }
    $javac = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javac) { return (Split-Path -Parent (Split-Path -Parent $javac.Source)) }
    Fallar 'No se encontro un JDK 21. Defina JAVA_HOME o instale Temurin 21.'
}

function Obtener-Ant {
    $ant = Get-Command ant.bat -ErrorAction SilentlyContinue
    if (-not $ant) { $ant = Get-Command ant.cmd -ErrorAction SilentlyContinue }
    if (-not $ant) { $ant = Get-Command ant.exe -ErrorAction SilentlyContinue }
    if ($ant) { return $ant.Source }
    $netbeans = Get-ChildItem 'C:\Program Files\NetBeans-*\netbeans\extide\ant\bin\ant.bat' -ErrorAction SilentlyContinue |
                Sort-Object FullName -Descending | Select-Object -First 1
    if ($netbeans) { return $netbeans.FullName }
    $local = Join-Path (Split-Path -Parent $Root) 'ant\bin\ant.bat'
    if (Test-Path $local) { return $local }
    Fallar 'No se encontro Apache Ant. Instalelo o agreguelo al PATH.'
}

function Obtener-Iscc {
    $iscc = Get-Command ISCC.exe -ErrorAction SilentlyContinue
    if ($iscc) { return $iscc.Source }
    $rutas = @(
        'C:\Program Files (x86)\Inno Setup 6\ISCC.exe',
        'C:\Program Files\Inno Setup 6\ISCC.exe'
    )
    foreach ($ruta in $rutas) {
        if (Test-Path $ruta) { return $ruta }
    }
    Fallar 'No se encontro ISCC.exe (Inno Setup 6). Instalelo desde https://jrsoftware.org/isdl.php'
}

$JavaHome = Obtener-JavaHome
$env:JAVA_HOME = $JavaHome
$Ant = Obtener-Ant
$Iscc = Obtener-Iscc

Escribir "JDK:         $JavaHome"
Escribir "Ant:         $Ant"
Escribir "Inno Setup:  $Iscc"

# ---------------------------------------------------------------------------
#  1) Compilar el JAR con Ant
# ---------------------------------------------------------------------------

if (-not $SkipAnt) {
    Escribir 'Compilando dist/Farmac.jar con Ant ...'
    & $Ant -f build.xml clean dist
    if ($LASTEXITCODE -ne 0) { Fallar 'ant clean dist fallo.' }
} else {
    Escribir 'Omitiendo Ant (-SkipAnt).'
}
if (-not (Test-Path 'dist\Farmac.jar')) { Fallar 'No existe dist\Farmac.jar. Ejecute sin -SkipAnt.' }

# ---------------------------------------------------------------------------
#  2) JRE minimo con jdeps + jlink
# ---------------------------------------------------------------------------

if (-not $SkipJre) {
    $jdeps = Join-Path $JavaHome 'bin\jdeps.exe'
    $jlink = Join-Path $JavaHome 'bin\jlink.exe'

    $jars = @(Get-ChildItem 'dist\lib\*.jar' | ForEach-Object { $_.FullName })

    Escribir 'Calculando modulos con jdeps ...'
    $modulos = New-Object 'System.Collections.Generic.HashSet[string]'
    $entradas = @((Resolve-Path 'dist\Farmac.jar').Path) + $jars
    # Se analiza cada JAR por separado: algunos (Ikonli) son modulares y jdeps
    # falla al resolverlos si se combinan con -cp. Se toleran los fallos.
    $eapPrevio = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    foreach ($entrada in $entradas) {
        $salida = & $jdeps --print-module-deps --ignore-missing-deps --multi-release 21 $entrada 2>$null
        foreach ($m in (($salida -join ',') -split ',')) {
            $t = $m.Trim()
            if ($t -ne '') { [void]$modulos.Add($t) }
        }
    }
    $ErrorActionPreference = $eapPrevio
    # Modulos que el driver PostgreSQL usa en tiempo de ejecucion y que jdeps
    # sobre la aplicacion no detecta siempre (java.management, java.naming, etc.).
    foreach ($m in @('java.base','java.desktop','java.sql','java.management',
                     'java.naming','java.logging','java.security.jgss','java.security.sasl')) {
        [void]$modulos.Add($m)
    }
    $listaModulos = ($modulos | Sort-Object) -join ','
    Escribir "Modulos: $listaModulos"

    Escribir 'Generando JRE minimo con jlink ...'
    if (Test-Path 'build\jre') { Remove-Item -Recurse -Force 'build\jre' }
    & $jlink --add-modules $listaModulos --strip-debug --no-header-files --no-man-pages `
             --compress=zip-6 --output 'build\jre'
    if ($LASTEXITCODE -ne 0) { Fallar 'jlink fallo.' }
    if (-not (Test-Path 'build\jre\bin\javaw.exe')) { Fallar 'El JRE generado no contiene javaw.exe.' }
    $tamJre = (Get-ChildItem 'build\jre' -Recurse -File | Measure-Object Length -Sum).Sum
    Escribir ("JRE minimo: {0:N1} MB" -f ($tamJre / 1MB))
} else {
    Escribir 'Omitiendo jlink (-SkipJre).'
}
if (-not (Test-Path 'build\jre\bin\javaw.exe')) { Fallar 'No existe build\jre. Ejecute sin -SkipJre.' }

# ---------------------------------------------------------------------------
#  3) Helper Java de respaldo (DbSetup)
# ---------------------------------------------------------------------------

Escribir 'Compilando el helper DbSetup ...'
& (Join-Path $JavaHome 'bin\javac.exe') -encoding UTF-8 `
    -cp 'lib\postgresql-42.5.4.jar' -d 'installer\helper' 'installer\helper\DbSetup.java'
if ($LASTEXITCODE -ne 0) { Fallar 'No se pudo compilar installer\helper\DbSetup.java.' }

# ---------------------------------------------------------------------------
#  4) Recursos graficos del asistente
# ---------------------------------------------------------------------------

if (-not $SkipAssets) {
    if (-not (Test-Path 'installer\assets\wizard-image.bmp') -or
        -not (Test-Path 'installer\assets\wizard-small.bmp') -or
        -not (Test-Path 'installer\assets\farmac.ico')) {
        Escribir 'Generando recursos graficos del asistente ...'
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File 'installer\assets\generate-assets.ps1'
        if ($LASTEXITCODE -ne 0) { Fallar 'No se pudieron generar los recursos graficos.' }
    } else {
        Escribir 'Recursos graficos ya presentes.'
    }
}

# ---------------------------------------------------------------------------
#  5) Compilar el instalador con Inno Setup
# ---------------------------------------------------------------------------

Escribir 'Compilando el instalador con Inno Setup ...'
& $Iscc 'installer\Farmac.iss'
if ($LASTEXITCODE -ne 0) { Fallar 'ISCC fallo al compilar installer\Farmac.iss.' }

$exe = Join-Path $InstallerDir 'Output\Farmac-Setup.exe'
if (-not (Test-Path $exe)) { Fallar 'No se genero Farmac-Setup.exe.' }

$tam = (Get-Item $exe).Length / 1MB
Escribir ("Instalador generado: {0} ({1:N1} MB)" -f $exe, $tam)
