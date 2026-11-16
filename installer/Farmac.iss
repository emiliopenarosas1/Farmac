; ============================================================================
;  Farmac.iss - Script de Inno Setup 6 para el instalador de Farmac
;  ---------------------------------------------------------------------------
;  Genera Farmac-Setup.exe (asistente gráfico de 7 pantallas).
;
;  Requisitos para compilar: Inno Setup 6.3 o superior, JDK 21 y Apache Ant.
;  El script build-installer.ps1 prepara dist\ (JAR) y build\jre\ (JRE mínimo)
;  antes de invocar ISCC.
;
;  IMPORTANTE: este archivo debe guardarse en UTF-8 con BOM para que Inno Setup
;  interprete correctamente los acentos del español.
; ============================================================================

#define MiAppNombre "Farmac"
#define MiAppVersion "1.0"
#define MiAppEditor "UAEMEX - CU Tianguistenco"
#define MiAppExe "Farmac-Setup"

[Setup]
; Identificador fijo de la aplicación (permite actualizaciones y desinstalación).
AppId={{9E7A4C2B-1F3D-4A6E-B8C5-2D7F9A1E4B60}
AppName={#MiAppNombre}
AppVersion={#MiAppVersion}
AppVerName={#MiAppNombre} {#MiAppVersion}
AppPublisher={#MiAppEditor}
VersionInfoCompany={#MiAppEditor}
VersionInfoDescription=Instalador de {#MiAppNombre}
VersionInfoProductName={#MiAppNombre}
VersionInfoVersion=1.0.0.0

; Instalación por usuario (sin UAC) en el perfil local.
DefaultDirName={localappdata}\Programs\Farmac
DisableDirPage=no
DefaultGroupName=Farmac
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0

; Origen de los archivos (raíz del repositorio) y salida.
SourceDir=..
OutputDir=installer\Output
OutputBaseFilename={#MiAppExe}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
DisableWelcomePage=no
UninstallDisplayName={#MiAppNombre}
UninstallDisplayIcon={app}\farmac.ico

; Recursos gráficos del asistente.
SetupIconFile=installer\assets\farmac.ico
WizardImageFile=installer\assets\wizard-image.bmp
WizardSmallImageFile=installer\assets\wizard-small.bmp

; Espacio adicional mostrado en el asistente (180 MB).
ExtraDiskSpaceRequired=188743680

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Files]
; --- Aplicación -------------------------------------------------------------
Source: "dist\Farmac.jar"; DestDir: "{app}"; Flags: ignoreversion; AfterInstall: LogFarmacJar
Source: "dist\lib\*.jar"; DestDir: "{app}\lib"; Flags: ignoreversion

; --- JRE mínimo generado con jlink -----------------------------------------
Source: "build\jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs; AfterInstall: LogJre

; --- Scripts SQL (01 a 08) --------------------------------------------------
Source: "sql\*.sql"; DestDir: "{app}\sql"; Flags: ignoreversion

; --- Helper Java de respaldo (DbSetup) --------------------------------------
Source: "installer\helper\DbSetup.class"; DestDir: "{app}\helper"; Flags: ignoreversion

; --- Icono de la aplicación -------------------------------------------------
Source: "installer\assets\farmac.ico"; DestDir: "{app}"; Flags: ignoreversion

; --- Config por defecto (se conserva en actualizaciones y desinstalación) ---
Source: "dist\config\farmac.properties"; DestDir: "{app}\config"; Flags: onlyifdoesntexist uninsneveruninstall

; --- Copias temporales para la prueba de conexión de la pantalla 4 ----------
;     (se extraen a {tmp} con ExtractTemporaryFile y no se instalan)
Source: "installer\helper\DbSetup.class"; Flags: dontcopy
Source: "dist\lib\postgresql-42.5.4.jar"; Flags: dontcopy

[Icons]
; Accesos directos: config al frente del classpath para que gane sobre el
; farmac.properties empaquetado dentro de Farmac.jar. Directorio de trabajo
; {app} para que la aplicación cree tickets\ y respaldos\ dentro de la app.
Name: "{group}\Farmac"; Filename: "{app}\jre\bin\javaw.exe"; Parameters: "-cp ""{app}\config;{app}\Farmac.jar;{app}\lib\*"" farmac.app.Main"; WorkingDir: "{app}"; IconFilename: "{app}\farmac.ico"; Check: DebeCrearMenuInicio
Name: "{autodesktop}\Farmac"; Filename: "{app}\jre\bin\javaw.exe"; Parameters: "-cp ""{app}\config;{app}\Farmac.jar;{app}\lib\*"" farmac.app.Main"; WorkingDir: "{app}"; IconFilename: "{app}\farmac.ico"; Check: DebeCrearEscritorio

[Run]
Filename: "{app}\jre\bin\javaw.exe"; Parameters: "-cp ""{app}\config;{app}\Farmac.jar;{app}\lib\*"" farmac.app.Main"; WorkingDir: "{app}"; Description: "Iniciar Farmac"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
; Solo se elimina lo generado por el instalador. NUNCA tickets\, respaldos\
; ni config\farmac.properties (marcado uninsneveruninstall).
Type: filesandordirs; Name: "{app}\jre"
Type: filesandordirs; Name: "{app}\lib"
Type: filesandordirs; Name: "{app}\helper"
Type: filesandordirs; Name: "{app}\sql"
Type: files; Name: "{app}\Farmac.jar"
Type: files; Name: "{app}\farmac.ico"
Type: files; Name: "{app}\instalacion.log"
Type: dirifempty; Name: "{app}\config"
Type: dirifempty; Name: "{app}"

[Code]
const
  RUTA_REGISTRO = 'Software\Farmac';
  DIGITOS_HEX = '0123456789abcdef';

var
  PaginaComponentes: TWizardPage;
  ChkFarmac: TNewCheckBox;
  ChkEscritorio: TNewCheckBox;
  ChkMenuInicio: TNewCheckBox;
  LblDescripcion: TNewStaticText;

  PaginaBD: TInputQueryWizardPage;
  ChkCrearBase: TNewCheckBox;
  BtnProbar: TNewButton;
  LblResultado: TNewStaticText;

  LogMemo: TNewMemo;

  PsqlPath: String;
  OrigExiste: Boolean;
  OrigUrl: String;
  OrigUsuario: String;
  OrigPassword: String;
  ConfigCargada: Boolean;
  ConfigContenido: AnsiString;

{ ----------------------------------------------------------------------------
  Utilidades generales
  ---------------------------------------------------------------------------- }

function Hex4(Valor: Integer): String;
begin
  Result :=
    DIGITOS_HEX[((Valor shr 12) and $F) + 1] +
    DIGITOS_HEX[((Valor shr 8) and $F) + 1] +
    DIGITOS_HEX[((Valor shr 4) and $F) + 1] +
    DIGITOS_HEX[(Valor and $F) + 1];
end;

{ Escapa el símbolo % para un archivo .cmd de Windows. }
function EscaparBatch(const Texto: String): String;
var
  T: String;
begin
  T := Texto;
  StringChangeEx(T, '%', '%%', True);
  Result := T;
end;

{ Escapa un valor para un archivo .properties de Java. Los caracteres no ASCII
  se codifican como \uXXXX porque Properties.load() lee en ISO-8859-1. }
function EscaparPropiedad(const Texto: String): String;
var
  I: Integer;
  C: Char;
  R: String;
begin
  R := '';
  for I := 1 to Length(Texto) do begin
    C := Texto[I];
    if C = '\' then
      R := R + '\\'
    else if C = #13 then
      R := R + '\r'
    else if C = #10 then
      R := R + '\n'
    else if C = #9 then
      R := R + '\t'
    else if (Ord(C) < 32) or (Ord(C) > 126) then
      R := R + '\u' + Hex4(Ord(C))
    else
      R := R + C;
  end;
  Result := R;
end;

{ Devuelve el valor (sin desescapar) de una clave de un .properties. }
function LeerPropiedad(const Contenido, Clave: String): String;
var
  Resto, Linea: String;
  P: Integer;
begin
  Result := '';
  Resto := Contenido;
  while Resto <> '' do begin
    P := Pos(#10, Resto);
    if P > 0 then begin
      Linea := Copy(Resto, 1, P - 1);
      Resto := Copy(Resto, P + 1, Length(Resto) - P);
    end else begin
      Linea := Resto;
      Resto := '';
    end;
    if (Length(Linea) > 0) and (Linea[Length(Linea)] = #13) then
      Linea := Copy(Linea, 1, Length(Linea) - 1);
    if Pos(Clave + '=', Linea) = 1 then begin
      P := Pos('=', Linea);
      Result := Copy(Linea, P + 1, Length(Linea) - P);
      Exit;
    end;
  end;
end;

{ Desescapa un valor de .properties (\\, \uXXXX, etc.). }
function DesescaparPropiedad(const Texto: String): String;
var
  I: Integer;
  R: String;
  C: Char;
begin
  R := '';
  I := 1;
  while I <= Length(Texto) do begin
    C := Texto[I];
    if C = '\' then begin
      if I = Length(Texto) then begin
        R := R + '\';
        Break;
      end;
      C := Texto[I + 1];
      if C = 'u' then begin
        R := R + Chr(StrToInt('$' + Copy(Texto, I + 2, 4)));
        I := I + 6;
      end else begin
        if C = 'r' then R := R + #13
        else if C = 'n' then R := R + #10
        else if C = 't' then R := R + #9
        else R := R + C;
        I := I + 2;
      end;
    end else begin
      R := R + C;
      I := I + 1;
    end;
  end;
  Result := R;
end;

{ Extrae host, puerto y base de una URL JDBC de PostgreSQL. }
procedure ParsearUrlJdbc(const Url: String; var Host, Puerto, Base: String);
var
  S, Resto: String;
  P: Integer;
begin
  Host := 'localhost';
  Puerto := '5432';
  Base := 'farmac_db';
  S := Url;
  P := Pos('://', S);
  if P > 0 then
    Resto := Copy(S, P + 3, Length(S) - P - 2)
  else
    Resto := S;
  P := Pos('/', Resto);
  if P > 0 then begin
    Base := Copy(Resto, P + 1, Length(Resto) - P);
    Resto := Copy(Resto, 1, P - 1);
  end;
  P := Pos(':', Resto);
  if P > 0 then begin
    Puerto := Copy(Resto, P + 1, Length(Resto) - P);
    Host := Copy(Resto, 1, P - 1);
  end else begin
    Host := Resto;
  end;
end;

{ ----------------------------------------------------------------------------
  Registro de mensajes de la pantalla "Instalando"
  ---------------------------------------------------------------------------- }

procedure LogLine(const Texto: String);
begin
  if LogMemo <> nil then
    LogMemo.Lines.Add(Texto);
  if WizardForm <> nil then
    WizardForm.StatusLabel.Caption := Texto;
end;

{ Callbacks sin parámetros exigidos por el atributo AfterInstall de [Files]. }
procedure LogFarmacJar();
begin
  LogLine('Extrayendo archivos: Farmac.jar');
end;

procedure LogJre();
begin
  LogLine('Extrayendo archivos: jre\bin\javaw.exe');
end;

procedure RegistrarError(const Texto: String);
var
  Renglon: String;
begin
  Renglon := '[' + GetDateTimeString('yyyy-mm-dd hh:nn:ss', '-', ':') + '] ' + Texto + #13#10;
  SaveStringToFile(ExpandConstant('{app}\instalacion.log'), Renglon, True);
  LogLine('ERROR: ' + Texto);
end;

{ ----------------------------------------------------------------------------
  Localización de psql y de Java
  ---------------------------------------------------------------------------- }

function EncontrarPsqlEnRegistro(const Raiz: Integer): String;
var
  Nombres: TArrayOfString;
  I: Integer;
  Base: String;
begin
  Result := '';
  if RegGetSubkeyNames(Raiz, 'SOFTWARE\PostgreSQL\Installations', Nombres) then
    for I := 0 to GetArrayLength(Nombres) - 1 do
      if RegQueryStringValue(Raiz, 'SOFTWARE\PostgreSQL\Installations\' + Nombres[I], 'Base Directory', Base) then
        if FileExists(AddBackslash(Base) + 'bin\psql.exe') then begin
          Result := AddBackslash(Base) + 'bin\psql.exe';
          Exit;
        end;
end;

function EncontrarPsql(): String;
var
  Res: Integer;
  Busqueda: TFindRec;
  Mejor: String;
  MejorVer: Integer;
  Ver: Integer;
  Ruta: String;
begin
  Result := '';
  { 1) En el PATH }
  if Exec('cmd.exe', '/c psql --version >nul 2>nul', '', SW_HIDE, ewWaitUntilTerminated, Res) and (Res = 0) then begin
    Result := 'psql';
    Exit;
  end;
  { 2) En el registro (64 y 32 bits) }
  Result := EncontrarPsqlEnRegistro(HKLM);
  if Result = '' then
    Result := EncontrarPsqlEnRegistro(HKLM32);
  if Result <> '' then
    Exit;
  { 3) En C:\Program Files\PostgreSQL\<version> (la versión más alta) }
  Mejor := '';
  MejorVer := -1;
  if FindFirst(ExpandConstant('{pf}\PostgreSQL\*'), Busqueda) then begin
    repeat
      if (Busqueda.Attributes and FILE_ATTRIBUTE_DIRECTORY) <> 0 then begin
        Ver := StrToIntDef(Busqueda.Name, -1);
        Ruta := ExpandConstant('{pf}\PostgreSQL\') + Busqueda.Name + '\bin\psql.exe';
        if (Ver > MejorVer) and FileExists(Ruta) then begin
          MejorVer := Ver;
          Mejor := Ruta;
        end;
      end;
    until not FindNext(Busqueda);
    FindClose(Busqueda);
  end;
  Result := Mejor;
end;

function RutaJava(): String;
var
  Res: Integer;
begin
  Result := '';
  if FileExists(ExpandConstant('{app}\jre\bin\java.exe')) then
    Result := ExpandConstant('{app}\jre\bin\java.exe')
  else if Exec('cmd.exe', '/c java -version >nul 2>nul', '', SW_HIDE, ewWaitUntilTerminated, Res) and (Res = 0) then
    Result := 'java';
end;

{ ----------------------------------------------------------------------------
  Ejecución de procesos con salida capturada a archivo
  ---------------------------------------------------------------------------- }

function EjecutarBatch(const Contenido, Salida: String): Integer;
var
  Bat: String;
  Res: Integer;
begin
  Bat := ExpandConstant('{tmp}\farmac-run.cmd');
  SaveStringToFile(Bat, Contenido, False);
  if Exec('cmd.exe', '/c ""' + Bat + '" > "' + Salida + '" 2>&1"', '', SW_HIDE, ewWaitUntilTerminated, Res) then
    Result := Res
  else
    Result := -1;
  DeleteFile(Bat);
end;

function RutaSalida(): String;
begin
  Result := ExpandConstant('{tmp}\farmac-out.txt');
end;

function LeerSalida(): String;
var
  S: AnsiString;
begin
  S := '';
  if FileExists(RutaSalida()) then
    LoadStringFromFile(RutaSalida(), S);
  Result := Trim(String(S));
end;

{ Ejecuta psql con PGPASSWORD en un .cmd temporal (nunca en la línea de comandos
  ni en el registro del instalador). El archivo temporal se borra de inmediato. }
function EjecutarPsql(const Argumentos, Password, Salida: String): Integer;
var
  Bat: String;
begin
  Bat := '@echo off' + #13#10 +
         'set "PGPASSWORD=' + EscaparBatch(Password) + '"' + #13#10 +
         'set "PGCLIENTENCODING=UTF8"' + #13#10 +
         '"' + PsqlPath + '" ' + Argumentos;
  Result := EjecutarBatch(Bat, Salida);
end;

{ ----------------------------------------------------------------------------
  Prueba de conexión (usada por el botón de la pantalla 4)
  ---------------------------------------------------------------------------- }

function ProbarConexion(const Host, Puerto, Base, Usuario, Password: String; var Mensaje: String): Boolean;
var
  Res: Integer;
  Salida: String;
  Java: String;
  Bat: String;
  Args: String;
begin
  Result := False;
  Mensaje := '';
  Salida := RutaSalida();

  if PsqlPath <> '' then begin
    Res := EjecutarPsql('-h ' + Host + ' -p ' + Puerto + ' -U ' + Usuario + ' -d postgres -tAc "SELECT 1"', Password, Salida);
    Salida := LeerSalida();
    if (Res = 0) and (Pos('1', Salida) > 0) then begin
      Result := True;
      Exit;
    end;
    Mensaje := Salida;
    Exit;
  end;

  { Respaldo en Java (sin psql). Usa el helper extraído a la carpeta temporal. }
  Java := RutaJava();
  if Java = '' then begin
    Mensaje := 'No se encontró el cliente psql ni un Java disponible para probar la conexión.';
    Exit;
  end;
  Args := '-cp "' + ExpandConstant('{tmp}') + ';' + ExpandConstant('{tmp}\postgresql-42.5.4.jar') +
          '" DbSetup test ' + Host + ' ' + Puerto + ' ' + Base + ' ' + Usuario;
  Bat := '@echo off' + #13#10 +
         'set "FARMAC_DBPASS=' + EscaparBatch(Password) + '"' + #13#10 +
         '"' + Java + '" ' + Args;
  Res := EjecutarBatch(Bat, Salida);
  Salida := LeerSalida();
  if (Res = 0) and (Pos('OK:', Salida) > 0) then begin
    Result := True;
    Exit;
  end;
  Mensaje := Salida;
end;

{ ----------------------------------------------------------------------------
  Lógica de base de datos (se ejecuta solo en ssPostInstall)
  ---------------------------------------------------------------------------- }

function BaseExiste(const Host, Puerto, Usuario, Password, Base: String): Boolean;
var
  Res: Integer;
  Salida: String;
begin
  { Sin psql no se puede verificar; se asume que existe y se intenta conectar. }
  if PsqlPath = '' then begin
    Result := True;
    Exit;
  end;
  Res := EjecutarPsql('-h ' + Host + ' -p ' + Puerto + ' -U ' + Usuario + ' -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname=''' + Base + '''"', Password, RutaSalida());
  Salida := LeerSalida();
  Result := (Res = 0) and (Pos('1', Salida) > 0);
end;

function CrearBase(const Host, Puerto, Usuario, Password, Base: String; var Mensaje: String): Boolean;
var
  Res: Integer;
begin
  if PsqlPath = '' then begin
    Mensaje := 'No se encontró psql.exe; se requiere para crear la base de datos.';
    Result := False;
    Exit;
  end;
  Res := EjecutarPsql('-h ' + Host + ' -p ' + Puerto + ' -U ' + Usuario + ' -d postgres -c "CREATE DATABASE ' + Base + '"', Password, RutaSalida());
  Mensaje := LeerSalida();
  Result := (Res = 0);
end;

function BaseTieneDatos(const Host, Puerto, Usuario, Password, Base: String): Boolean;
var
  Res: Integer;
  Salida: String;
begin
  if PsqlPath = '' then begin
    Result := False;
    Exit;
  end;
  Res := EjecutarPsql('-h ' + Host + ' -p ' + Puerto + ' -U ' + Usuario + ' -d ' + Base + ' -tAc "SELECT 1 FROM information_schema.tables WHERE table_name=''usuario''"', Password, RutaSalida());
  Salida := LeerSalida();
  if (Res <> 0) or (Pos('1', Salida) = 0) then begin
    Result := False;
    Exit;
  end;
  Res := EjecutarPsql('-h ' + Host + ' -p ' + Puerto + ' -U ' + Usuario + ' -d ' + Base + ' -tAc "SELECT count(*) FROM usuario"', Password, RutaSalida());
  Salida := LeerSalida();
  Result := (Res = 0) and (StrToIntDef(Salida, 0) > 0);
end;

function EjecutarScript(const Host, Puerto, Usuario, Password, Base, Script, Salida: String; var Mensaje: String): Boolean;
var
  Res: Integer;
  Java: String;
  Bat: String;
begin
  if PsqlPath <> '' then begin
    Res := EjecutarPsql('-h ' + Host + ' -p ' + Puerto + ' -U ' + Usuario + ' -d ' + Base + ' -v ON_ERROR_STOP=1 -f "' + Script + '"', Password, Salida);
    Mensaje := LeerSalida();
    Result := (Res = 0);
    Exit;
  end;

  { Respaldo en Java con DbSetup (ya instalado en la aplicación). }
  Java := ExpandConstant('{app}\jre\bin\java.exe');
  if not FileExists(Java) then begin
    Mensaje := 'No se encontró psql.exe ni el JRE instalado para ejecutar los scripts.';
    Result := False;
    Exit;
  end;
  Bat := '@echo off' + #13#10 +
         'set "FARMAC_DBPASS=' + EscaparBatch(Password) + '"' + #13#10 +
         '"' + Java + '" -cp "' + ExpandConstant('{app}\helper') + ';' + ExpandConstant('{app}\lib\postgresql-42.5.4.jar') +
         '" DbSetup run-script ' + Host + ' ' + Puerto + ' ' + Base + ' ' + Usuario + ' "' + Script + '"';
  Res := EjecutarBatch(Bat, Salida);
  Mensaje := LeerSalida();
  Result := (Res = 0) and (Pos('OK:', Mensaje) > 0);
end;

procedure FalloBaseDeDatos(const Detalle: String);
begin
  RegistrarError('No se pudo configurar la base de datos: ' + Detalle);
  RegWriteStringValue(HKCU, RUTA_REGISTRO, 'UltimaBaseDatos', 'error');
  MsgBox('Farmac se instaló, pero no se pudo crear la base de datos: ' + Detalle + #13#10 + #13#10 +
         'Puede crearla después con los pasos de la sección 5 del manual.', mbError, MB_OK);
end;

procedure ConfigurarBaseDeDatos();
var
  Host, Puerto, Base, Usuario, Password: String;
  Mensaje: String;
  Salida: String;
  Scripts: array[0..4] of String;
  I: Integer;
begin
  Host := PaginaBD.Values[0];
  Puerto := PaginaBD.Values[1];
  Base := PaginaBD.Values[2];
  Usuario := PaginaBD.Values[3];
  Password := PaginaBD.Values[4];
  Salida := RutaSalida();
  Scripts[0] := '01_schema.sql';
  Scripts[1] := '02_datos.sql';
  Scripts[2] := '03_inventario.sql';
  Scripts[3] := '04_ventas.sql';
  Scripts[4] := '05_migracion_auditoria.sql';

  WizardForm.ProgressGauge.Style := npbstMarquee;
  LogLine('Verificando la conexión a PostgreSQL ...');
  if not ProbarConexion(Host, Puerto, Base, Usuario, Password, Mensaje) then begin
    FalloBaseDeDatos(Mensaje);
    Exit;
  end;

  if not BaseExiste(Host, Puerto, Usuario, Password, Base) then begin
    LogLine('Creando la base de datos ' + Base + ' ...');
    if not CrearBase(Host, Puerto, Usuario, Password, Base, Mensaje) then begin
      FalloBaseDeDatos(Mensaje);
      Exit;
    end;
  end else if BaseTieneDatos(Host, Puerto, Usuario, Password, Base) then begin
    LogLine('La base ' + Base + ' ya contiene datos. No se modificará.');
    RegWriteStringValue(HKCU, RUTA_REGISTRO, 'UltimaBaseDatos', 'omitida');
    Exit;
  end;

  for I := 0 to 4 do begin
    LogLine('Ejecutando ' + Scripts[I] + ' ...');
    if not EjecutarScript(Host, Puerto, Usuario, Password, Base,
                          ExpandConstant('{app}\sql\') + Scripts[I], Salida, Mensaje) then begin
      FalloBaseDeDatos('Error en ' + Scripts[I] + ': ' + Mensaje);
      Exit;
    end;
  end;

  LogLine('Base de datos configurada correctamente.');
  RegWriteStringValue(HKCU, RUTA_REGISTRO, 'UltimaBaseDatos', 'creada');
end;

{ ----------------------------------------------------------------------------
  Escritura del archivo config\farmac.properties
  ---------------------------------------------------------------------------- }

procedure EscribirConfig();
var
  Host, Puerto, Base, Usuario, Password: String;
  Contenido: String;
begin
  Host := PaginaBD.Values[0];
  Puerto := PaginaBD.Values[1];
  Base := PaginaBD.Values[2];
  Usuario := PaginaBD.Values[3];
  Password := PaginaBD.Values[4];

  Contenido :=
    '# Archivo de configuracion de base de datos Farmac' + #13#10 +
    'db.url=jdbc:postgresql://' + Host + ':' + Puerto + '/' + Base + #13#10 +
    'db.user=' + EscaparPropiedad(Usuario) + #13#10 +
    'db.password=' + EscaparPropiedad(Password) + #13#10 +
    'caducidad.dias_alerta=60' + #13#10;

  ForceDirectories(ExpandConstant('{app}\config'));
  SaveStringToFile(ExpandConstant('{app}\config\farmac.properties'), Contenido, False);
  LogLine('Escribiendo config\farmac.properties');
end;

procedure EscribirConfigSiCambio();
var
  UrlNueva, Usuario, Password: String;
begin
  UrlNueva := 'jdbc:postgresql://' + PaginaBD.Values[0] + ':' + PaginaBD.Values[1] + '/' + PaginaBD.Values[2];
  Usuario := PaginaBD.Values[3];
  Password := PaginaBD.Values[4];
  if OrigExiste and (UrlNueva = OrigUrl) and (Usuario = OrigUsuario) and (Password = OrigPassword) then
    LogLine('Escribiendo config\farmac.properties (sin cambios, se conserva)')
  else
    EscribirConfig();
end;

{ ----------------------------------------------------------------------------
  Validación de la pantalla 4
  ---------------------------------------------------------------------------- }

function ValidarCampos(var Mensaje: String): Boolean;
var
  I: Integer;
begin
  Result := True;
  Mensaje := '';
  for I := 0 to 3 do
    if Trim(PaginaBD.Values[I]) = '' then begin
      Mensaje := 'Ningún campo puede quedar vacío.';
      Result := False;
      Exit;
    end;
  if StrToIntDef(PaginaBD.Values[1], -1) < 0 then begin
    Mensaje := 'El puerto debe ser un número.';
    Result := False;
    Exit;
  end;
  if Trim(PaginaBD.Values[4]) = '' then begin
    Mensaje := 'La contraseña no puede quedar vacía.';
    Result := False;
    Exit;
  end;
end;

procedure BtnProbarClick(Sender: TObject);
var
  Mensaje: String;
begin
  if not ValidarCampos(Mensaje) then begin
    LblResultado.Font.Color := clRed;
    LblResultado.Caption := Mensaje;
    Exit;
  end;
  if ProbarConexion(PaginaBD.Values[0], PaginaBD.Values[1], PaginaBD.Values[2], PaginaBD.Values[3], PaginaBD.Values[4], Mensaje) then begin
    LblResultado.Font.Color := clGreen;
    LblResultado.Caption := 'La conexión es correcta';
  end else begin
    LblResultado.Font.Color := clRed;
    LblResultado.Caption := 'Error: ' + Mensaje;
  end;
end;

{ ----------------------------------------------------------------------------
  Creación de las páginas personalizadas
  ---------------------------------------------------------------------------- }

{ Recupera los valores de una instalación anterior y los precarga en la página.
  Se ejecuta cuando el directorio de instalación ya está definido. Es idempotente. }
procedure CargarConfigPrevia();
var
  Ruta, Host, Puerto, Base: String;
begin
  if ConfigCargada then
    Exit;
  ConfigCargada := True;
  Ruta := ExpandConstant('{app}\config\farmac.properties');
  if not FileExists(Ruta) then
    Exit;
  LoadStringFromFile(Ruta, ConfigContenido);
  OrigUrl := LeerPropiedad(String(ConfigContenido), 'db.url');
  OrigUsuario := DesescaparPropiedad(LeerPropiedad(String(ConfigContenido), 'db.user'));
  OrigPassword := DesescaparPropiedad(LeerPropiedad(String(ConfigContenido), 'db.password'));
  if OrigUrl = '' then
    Exit;
  OrigExiste := True;
  ParsearUrlJdbc(OrigUrl, Host, Puerto, Base);
  PaginaBD.Values[0] := Host;
  PaginaBD.Values[1] := Puerto;
  PaginaBD.Values[2] := Base;
  PaginaBD.Values[3] := OrigUsuario;
  PaginaBD.Values[4] := OrigPassword;
end;

procedure InitializeWizard();
var
  EnvPass: String;
begin
  PsqlPath := EncontrarPsql();

  { Extrae a la carpeta temporal el helper y el driver para la prueba de conexión. }
  ExtractTemporaryFile('DbSetup.class');
  ExtractTemporaryFile('postgresql-42.5.4.jar');

  { --- Pantalla 3: Seleccionar componentes --- }
  PaginaComponentes := CreateCustomPage(wpSelectDir, 'Seleccionar componentes',
    'Seleccione los componentes que desea instalar.');

  ChkFarmac := TNewCheckBox.Create(PaginaComponentes);
  ChkFarmac.Parent := PaginaComponentes.Surface;
  ChkFarmac.Caption := 'Farmac (obligatorio)';
  ChkFarmac.Checked := True;
  ChkFarmac.Enabled := False;
  ChkFarmac.Top := ScaleY(4);
  ChkFarmac.Left := 0;
  ChkFarmac.Width := PaginaComponentes.SurfaceWidth;

  ChkEscritorio := TNewCheckBox.Create(PaginaComponentes);
  ChkEscritorio.Parent := PaginaComponentes.Surface;
  ChkEscritorio.Caption := 'Acceso directo en el escritorio';
  ChkEscritorio.Checked := True;
  ChkEscritorio.Top := ChkFarmac.Top + ChkFarmac.Height + ScaleY(8);
  ChkEscritorio.Left := 0;
  ChkEscritorio.Width := PaginaComponentes.SurfaceWidth;

  ChkMenuInicio := TNewCheckBox.Create(PaginaComponentes);
  ChkMenuInicio.Parent := PaginaComponentes.Surface;
  ChkMenuInicio.Caption := 'Acceso directo en el menú principal';
  ChkMenuInicio.Checked := True;
  ChkMenuInicio.Top := ChkEscritorio.Top + ChkEscritorio.Height + ScaleY(8);
  ChkMenuInicio.Left := 0;
  ChkMenuInicio.Width := PaginaComponentes.SurfaceWidth;

  LblDescripcion := TNewStaticText.Create(PaginaComponentes);
  LblDescripcion.Parent := PaginaComponentes.Surface;
  LblDescripcion.AutoSize := False;
  LblDescripcion.WordWrap := True;
  LblDescripcion.Caption := 'Programa principal, Java incluido y scripts de la base de datos.';
  LblDescripcion.Left := 0;
  LblDescripcion.Width := PaginaComponentes.SurfaceWidth;
  LblDescripcion.Height := ScaleY(46);
  LblDescripcion.Top := PaginaComponentes.SurfaceHeight - LblDescripcion.Height;

  { --- Pantalla 4: Conexión a PostgreSQL --- }
  PaginaBD := CreateInputQueryPage(PaginaComponentes.ID, 'Conexión a PostgreSQL',
    'Indique cómo conectarse a la base de datos de Farmac.',
    'Se instaló PostgreSQL con la configuración por defecto, solo pulse Siguiente.');
  PaginaBD.Add('Servidor:', False);
  PaginaBD.Add('Puerto:', False);
  PaginaBD.Add('Base de datos:', False);
  PaginaBD.Add('Usuario:', False);
  PaginaBD.Add('Contraseña:', True);
  PaginaBD.Values[0] := 'localhost';
  PaginaBD.Values[1] := '5432';
  PaginaBD.Values[2] := 'farmac_db';
  PaginaBD.Values[3] := 'postgres';
  PaginaBD.Values[4] := '';

  { Los valores previos de config\farmac.properties se cargan más tarde, cuando
    el directorio de instalación ya está definido (ver CargarConfigPrevia). }
  OrigExiste := False;
  OrigUrl := '';
  OrigUsuario := '';
  OrigPassword := '';
  ConfigCargada := False;

  { Permite instalaciones desatendidas con la contraseña en el entorno. }
  EnvPass := GetEnv('FARMAC_DBPASS');
  if (PaginaBD.Values[4] = '') and (EnvPass <> '') then
    PaginaBD.Values[4] := EnvPass;

  ChkCrearBase := TNewCheckBox.Create(PaginaBD);
  ChkCrearBase.Parent := PaginaBD.Surface;
  ChkCrearBase.Caption := 'Crear la base de datos y cargar las tablas (recomendado)';
  ChkCrearBase.Checked := True;
  ChkCrearBase.Left := 0;
  ChkCrearBase.Width := PaginaBD.SurfaceWidth;
  ChkCrearBase.Top := PaginaBD.Edits[4].Top + PaginaBD.Edits[4].Height + ScaleY(12);

  BtnProbar := TNewButton.Create(PaginaBD);
  BtnProbar.Parent := PaginaBD.Surface;
  BtnProbar.Caption := 'Probar conexión';
  BtnProbar.OnClick := @BtnProbarClick;
  BtnProbar.Left := 0;
  BtnProbar.Top := ChkCrearBase.Top + ChkCrearBase.Height + ScaleY(10);

  LblResultado := TNewStaticText.Create(PaginaBD);
  LblResultado.Parent := PaginaBD.Surface;
  LblResultado.AutoSize := False;
  LblResultado.WordWrap := True;
  LblResultado.Caption := '';
  LblResultado.Left := BtnProbar.Left + BtnProbar.Width + ScaleX(10);
  LblResultado.Top := BtnProbar.Top + ScaleY(4);
  LblResultado.Width := PaginaBD.SurfaceWidth - LblResultado.Left;
  LblResultado.Height := ScaleY(40);

  { --- Memo visible con el registro de la pantalla "Instalando" --- }
  LogMemo := TNewMemo.Create(WizardForm);
  LogMemo.Parent := WizardForm.InstallingPage;
  LogMemo.Left := WizardForm.ProgressGauge.Left;
  LogMemo.Top := WizardForm.ProgressGauge.Top + WizardForm.ProgressGauge.Height + ScaleY(22);
  LogMemo.Width := WizardForm.ProgressGauge.Width;
  LogMemo.Height := WizardForm.InstallingPage.Height - LogMemo.Top - ScaleY(8);
  LogMemo.ReadOnly := True;
  LogMemo.ScrollBars := ssVertical;
  LogMemo.Color := clWhite;
  LogMemo.Visible := False;
end;

function DebeCrearEscritorio(): Boolean;
begin
  Result := ChkEscritorio.Checked;
end;

function DebeCrearMenuInicio(): Boolean;
begin
  Result := ChkMenuInicio.Checked;
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := (PageID = wpSelectComponents) or (PageID = wpSelectProgramGroup) or (PageID = wpSelectTasks);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  Mensaje: String;
begin
  Result := True;
  if CurPageID = PaginaBD.ID then begin
    Result := ValidarCampos(Mensaje);
    if not Result then
      MsgBox(Mensaje, mbError, MB_OK);
  end;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  if CurPageID = PaginaBD.ID then
    CargarConfigPrevia();
  if CurPageID = wpWelcome then begin
    WizardForm.WelcomeLabel1.Caption := 'Le damos la bienvenida al asistente de instalación de Farmac';
    WizardForm.WelcomeLabel2.Caption :=
      'Este asistente instalará Farmac, el sistema de gestión de farmacia, en este equipo. ' +
      'Antes de continuar, verifique que PostgreSQL 14 o superior esté instalado y en ejecución. ' +
      'Haga clic en Siguiente para continuar.';
  end;
  if CurPageID = wpReady then
    WizardForm.NextButton.Caption := 'Instalar';
  if CurPageID = wpInstalling then begin
    LogMemo.Visible := True;
    LogLine('Esto puede tardar entre 1 y 2 minutos.');
  end;
  if CurPageID = wpFinished then begin
    WizardForm.FinishedHeadingLabel.Caption := 'Instalación completada';
    WizardForm.FinishedLabel.Caption :=
      'Farmac se instaló correctamente en su equipo.' + #13#10 +
      'Usuario inicial: admin / Contraseña inicial: admin123.' + #13#10 +
      'Haga clic en Finalizar para cerrar el asistente.';
  end;
end;

function UpdateReadyMemo(const Space, NewLine, MemoUserInfoInfo, MemoDirInfo,
  MemoTypeInfo, MemoComponentsInfo, MemoGroupInfo, MemoTasksInfo: String): String;
var
  S: String;
begin
  S := '';
  S := S + 'Carpeta de instalación:' + NewLine + Space + WizardDirValue + NewLine + NewLine;
  S := S + 'Componentes elegidos:' + NewLine + Space + 'Farmac' + NewLine;
  if ChkEscritorio.Checked then
    S := S + Space + 'Acceso directo en el escritorio' + NewLine;
  if ChkMenuInicio.Checked then
    S := S + Space + 'Acceso directo en el menú principal' + NewLine;
  S := S + NewLine;
  S := S + 'Base de datos:' + NewLine + Space +
       PaginaBD.Values[3] + '@' + PaginaBD.Values[0] + ':' + PaginaBD.Values[1] + ' / ' + PaginaBD.Values[2] + NewLine;
  if ChkCrearBase.Checked then
    S := S + Space + 'Crear base y tablas: Sí'
  else
    S := S + Space + 'Crear base y tablas: No';
  Result := S;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then begin
    CargarConfigPrevia();
    EscribirConfigSiCambio();
    if ChkCrearBase.Checked then
      ConfigurarBaseDeDatos();
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
    MsgBox('La base de datos, los tickets y los respaldos se conservaron.' + #13#10 +
           'Para borrar la base use: dropdb -U postgres farmac_db', mbInformation, MB_OK);
end;
