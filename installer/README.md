# Instalador de Windows de Farmac

Esta carpeta contiene todo lo necesario para generar `Farmac-Setup.exe`, el
asistente gráfico que instala Farmac (la aplicación de escritorio en Java 21 +
PostgreSQL) en equipos Windows 10/11 de 64 bits.

## Contenido

| Archivo | Descripción |
| --- | --- |
| `Farmac.iss` | Script de Inno Setup 6 que define el asistente de 7 pantallas y toda la lógica en Pascal Script. |
| `build-installer.ps1` | Compila el JAR, genera el JRE mínimo, compila el helper y ejecuta ISCC. |
| `helper/DbSetup.java` | Utilidad Java de respaldo para probar la conexión y ejecutar los scripts SQL cuando no hay `psql`. |
| `assets/generate-assets.ps1` | Genera el banner (164x314), el icono pequeño (55x55) y `farmac.ico` con la identidad visual. |
| `assets/wizard-image.bmp`, `assets/wizard-small.bmp`, `assets/farmac.ico` | Recursos gráficos ya generados. |
| `Output/Farmac-Setup.exe` | Instalador resultante (se crea al compilar). |

## Requisitos

- **JDK 21** (Temurin recomendado). Se usan `javac`, `jdeps` y `jlink`.
- **Apache Ant** (o el Ant incluido en NetBeans; el script lo detecta solo).
- **Inno Setup 6** — https://jrsoftware.org/isdl.php (el compilador `ISCC.exe`).
- Windows 10/11 de 64 bits.

Opcional en el equipo de destino: **PostgreSQL 14 o superior** con `psql` en el
PATH o registrado. Si no está, el instalador usa el respaldo en Java.

## Cómo compilar

Desde la raíz del repositorio:

```powershell
powershell -ExecutionPolicy Bypass -File installer\build-installer.ps1
```

El resultado queda en `installer\Output\Farmac-Setup.exe`.

También se puede usar el target de Ant (hace lo mismo):

```powershell
ant installer
```

Opciones del script:

- `-SkipAnt`  usa el `dist\` ya compilado en lugar de ejecutar `ant clean dist`.
- `-SkipJre`  reutiliza `build\jre` si ya existe.
- `-SkipAssets` no regenera las imágenes del asistente.

En integración continua, el flujo `.github/workflows/build-installer.yml`
instala Temurin 21, Ant e Inno Setup (Chocolatey), compila y publica
`Farmac-Setup.exe` como artefacto descargable.

## Las 7 pantallas del asistente

1. **Bienvenida** — aviso sobre PostgreSQL.
2. **Carpeta de destino** — `%LOCALAPPDATA%\Programs\Farmac`, espacio requerido y disponible.
3. **Seleccionar componentes** — Farmac (fijo) y los dos accesos directos.
4. **Conexión a PostgreSQL** — servidor, puerto, base, usuario y contraseña, con botón *Probar conexión* y la casilla para crear la base.
5. **Listo para instalar** — resumen; el botón dice *Instalar*.
6. **Instalando** — barra de progreso y registro de líneas visible.
7. **Instalación completada** — credenciales iniciales y casilla *Iniciar Farmac*.

## Comportamiento de la base de datos

Todo ocurre en la fase posterior a la copia de archivos (`ssPostInstall`) y solo
si la casilla *Crear la base de datos y cargar las tablas* está marcada:

1. Localiza `psql.exe` en el PATH, en el registro y en `C:\Program Files\PostgreSQL\*`.
2. Prueba la conexión a la base `postgres`.
3. Si `farmac_db` no existe, la crea. Si existe y ya tiene datos (tabla `usuario`
   con filas), **no modifica nada**.
4. Ejecuta en orden `01_schema.sql`, `02_datos.sql`, `03_inventario.sql`,
   `04_ventas.sql` y `05_migracion_auditoria.sql`.
5. Si algo falla, la instalación de archivos se conserva; el detalle queda en
   `{app}\instalacion.log` y se muestra un mensaje con los pasos del manual.

La contraseña **nunca** se escribe en la línea de comandos ni en los registros:
se pasa mediante variables de entorno (`PGPASSWORD` / `FARMAC_DBPASS`) dentro de
un archivo `.cmd` temporal que se borra de inmediato.

## Desinstalación

El desinstalador elimina los archivos del programa, `lib\`, `jre\`, `helper\`,
`sql\` y los accesos directos. **Conserva** la base de datos, las carpetas
`tickets\` y `respaldos\`, y `config\farmac.properties`. Al terminar muestra:

> La base de datos, los tickets y los respaldos se conservaron.
> Para borrar la base use: dropdb -U postgres farmac_db

## Lista de verificación de pruebas manuales

Ejecute `installer\Output\Farmac-Setup.exe` en un equipo Windows 10/11 de 64 bits
y confirme, en orden:

- [ ] El asistente muestra **exactamente las 7 pantallas** descritas, en orden.
- [ ] **No** pide permisos de administrador (no aparece el aviso de UAC).
- [ ] Con PostgreSQL por defecto y la contraseña correcta, **Probar conexión**
      responde "La conexión es correcta." y se crean **11 tablas**
      (`psql -U postgres -d farmac_db -c "\dt"` muestra 11), con **2 usuarios**,
      **30 productos** y **6 lotes**.
- [ ] Con una contraseña incorrecta, la prueba de conexión falla con un mensaje
      claro (por ejemplo, `password authentication failed`).
- [ ] La aplicación abre la pantalla de inicio de sesión con el acceso directo y
      `admin / admin123` entra correctamente.
- [ ] Un cambio en la pantalla 4 (por ejemplo, otra base de datos) se refleja de
      verdad en la conexión de la aplicación: `config` va **primero** en el
      classpath (`-cp "config;Farmac.jar;lib\*"`), por lo que gana sobre el
      `farmac.properties` empaquetado dentro de `Farmac.jar`.
- [ ] Reinstalar sobre una base con datos **no modifica** los datos (el log indica
      que la base ya contiene datos y no se modifica).
- [ ] Desinstalar conserva la base, la carpeta `tickets\` y la carpeta `respaldos\`.

### Cómo comprobar el orden del classpath

En el equipo instalado, abra `config\farmac.properties` y cambie `db.url` por una
base inexistente; al abrir Farmac desde el acceso directo, el error de conexión
debe mencionar esa base, lo que demuestra que se está usando `config` y no el
archivo empaquetado en el JAR.

## Limitaciones conocidas

- El respaldo en Java (`DbSetup run-script`) divide el script por `;` respetando
  comentarios, cadenas y bloques `DO $$ ... $$`. No interpreta meta-comandos de
  `psql` (líneas que inician con `\`). Para scripts complejos se recomienda tener
  `psql` disponible; el instalador lo usa de forma prioritaria.
- El espacio requerido se muestra como 180 MB mediante `ExtraDiskSpaceRequired`,
  aunque el tamaño real instalado es menor (el JRE mínimo ronda los 48 MB).
