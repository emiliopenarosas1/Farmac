# Farmac — Sistema de Gestion de Farmacia

## Descripcion general

Farmac es un sistema de gestion integral para farmacia desarrollado en **Java 21** con interfaz grafica de escritorio basada en **Swing**. El sistema concentra en una sola aplicacion los procesos esenciales de la operacion farmaceutica: el catalogo de medicamentos y proveedores, el punto de venta, el control de inventario bajo criterio de fecha de caducidad mas proxima (FEFO), la captura de recetas medicas para medicamentos de prescripcion controlada, el arqueo de caja y la generacion de reportes.

El proposito del sistema es centralizar y dar orden a la operatividad diaria de una farmacia. En lugar de dispersar las funciones entre herramientas aisladas, Farmac integra la gestion comercial, el control de inventario y el control financiero en un flujo coherente: se identifica el medicamento y su origen mediante proveedores y lotes, se controla su disponibilidad mediante fechas de caducidad, se verifica la prescripcion medica cuando el medico lo exige, se realiza la venta y se emite el comprobante, se registra el movimiento de inventario y se cierra el turno de caja con su arqueo.

El diseno del software se sustenta en una arquitectura por capas: paquetes de presentacion (interfaz grafica Swing), dominio (modelos con logica de negocio), acceso a datos (persistencia mediante DAO sobre PostgreSQL), configuracion y utilidades. Esta separacion permite mantener la interfaz, la logica y la persistencia de forma independiente entre si.

## Funcionalidades

### Punto de venta
- Catalogo de productos con busqueda por nombre y por codigo de barras.
- Integracion con lector de codigo de barras.
- Carrito de ventas y calculo de totales.
- Generacion de ticket en formato PDF al completar la venta.

### Inventario
- Recepcion de lotes con numero de lote, fecha de caducidad, cantidad recibida, precio de compra y referencia de factura.
- Ajustes de inventario con movimiento transaccional y registro en bitacora.
- Algoritmo de seleccion de lotes por fecha de caducidad mas proxima (FEFO).
- Consulta de existencias y reporte de caducidad con semaforo de proximos vencimientos.

### Recetas medicas
- Medicamentos con `requiere_receta` habilitado exigen receta para su venta.
- Validacion de receta vigente.
- Bloqueo de ventas de medicamentos controlados sin receta o con receta vencida.

### Corte de caja
- Apertura y cierre de turno con fondo inicial.
- Calculo del monto esperado, conteo de efectivo y diferencia.
- Historial de turnos con sus arqueos.

### Reportes y auditoria
- Ventas con y sin receta, con filtro de recetas.
- Historial de turnos de caja.
- Bitacora de acciones del sistema.
- Exportacion de reportes de caducidad y existencias.

### Administracion
- Gestion de usuarios con roles (Encargado y Empleado).
- Gestion de proveedores.
- Gestion de categorias y productos.

## Proposito

El proposito del proyecto es disponer de un sistema propio de gestion para una farmacia que cubra el ciclo completo de la dispensacion de medicamentos:

1. Identificacion del medicamento y origen mediante proveedores y lotes.
2. Control de disponibilidad mediante fechas de caducidad.
3. Verificacion de la prescripcion medica cuando el medico lo exige.
4. Realizacion de la venta y emision del comprobante.
5. Registro del movimiento de inventario y de la accion en la bitacora.
6. Cierre del turno de caja y arqueo de la operacion.

El resultado esperado es una farmacia con procesos auditables, inventario controlado y operaciones de caja verificadas.

## Desglose tecnologico y especificaciones

### Lenguaje y plataforma
- Lenguaje: Java 21.
- Interfaz de usuario: Swing con tema FlatLaf y iconos Ikonli.

### Persistencia de datos
- Motor de base de datos: PostgreSQL.
- Acceso mediante JDBC con driver `postgresql-42.5.4`.
- Esquema definido en scripts SQL versionados en `sql/`.
- Transacciones con control ACID y manejo de errores transaccional.

### Seguridad
- Contraseñas gestionadas con hash BCrypt.
- Sesiones de usuario.
- Bitacora de acciones.

### Generacion de documentos
- Tickets de venta en formato PDF.

### Desarrollo y control de calidad
- Build: Apache Ant (`build.xml`).
- IDE recomendado: NetBeans 21.
- Pruebas: JUnit 4 con Hamcrest.

## Arquitectura del sistema

El sistema se organiza en los siguientes paquetes dentro de `src/farmac`:

- `app`: punto de entrada de la aplicacion.
- `config`: conexion a base de datos.
- `dominio`: clases de negocio y modelos.
- `dao`: acceso a datos con persistencia.
- `ui`: pantallas de la interfaz.
- `util`: componentes transversales (tema, tickets, sesion, respaldo).

## Estructura del proyecto

```
Farmac/
├── src/
│   └── farmac/
│       ├── app/          # Punto de entrada (Main.java)
│       ├── config/       # Conexion a base de datos (ConexionBD.java)
│       ├── dominio/      # Modelos y logica de negocio
│       ├── dao/          # Acceso a datos (DAO)
│       ├── ui/           # Pantallas Swing
│       └── util/         # Utilidades (tema, tickets, sesion, respaldo)
├── sql/                  # Scripts de esquema, datos y migraciones
├── test/                 # Pruebas JUnit
├── lib/                  # Librerias de terceros
├── build.xml             # Build con Apache Ant
├── nbproject/           # Configuracion de NetBeans
└── README.md
```

## Requisitos

- **Java 21**.
- **Apache Ant** para compilar con `build.xml`.
- **NetBeans 21** recomendado (proyecto Ant nativo).
- **PostgreSQL** para persistencia.

## Base de datos

El proyecto utiliza **PostgreSQL**. Para crear la base y aplicar el esquema:

```bash
createdb -U postgres farmac_db

psql -U postgres -d farmac_db -f sql/01_schema.sql
psql -U postgres -d farmac_db -f sql/02_datos.sql
psql -U postgres -d farmac_db -f sql/03_inventario.sql
psql -U postgres -d farmac_db -f sql/04_ventas.sql
psql -U postgres -d farmac_db -f sql/05_migracion_auditoria.sql
```

La conexion se configura en `src/farmac.properties` (`db.url`, `db.user`, `db.password`).

### Datos de prueba integrales

```bash
psql -U postgres -d farmac_db -f sql/08_datos_prueba.sql
```

## Como compilar y ejecutar

```bash
ant clean dist
java -jar dist/Farmac.jar
```

O desde NetBeans: `Run Project (F6)`.

## Consultas de auditoria

La pantalla **Historial de Ventas** incluye:

- Pestaña **Ventas con Receta**: ventas del periodo con receta asociada.
- Pestaña **Historial de Turnos**: apertura y cierre de turnos con arqueos.
- Filtro **Solo ventas con receta**.

## Tickets PDF

Los tickets se generan al completar una venta y se guardan en `tickets/YYYY-MM-DD/`.

## Roles del sistema

| Rol | Acceso |
|---|---|
| **Encargado** | Todos los modulos |
| **Empleado** | Punto de venta y corte de caja |

El login incluye un selector visual Encargado / Empleado. El rol efectivo siempre se toma del usuario almacenado en la base de datos.

## Credenciales de prueba

| Usuario | Password | Rol |
|---|---|---|
| `admin` | `admin123` | Encargado |
| `empleado1` | `caja123` | Empleado |

Los hashes BCrypt correspondientes estan en `sql/02_datos.sql`.

## Documentacion complementaria

- `DIAGRAMA_CLASES.md`: representacion orientada a objetos del dominio.
- `DIAGRAMA_OBJETOS.md`: representacion de objetos y servicios.
- `DIAGRAMA_CASOS_USO_MERMAID.txt` y `DIAGRAMA_CASOS_USO_PERSONITAS.txt`: casos de uso.

## Autores

Proyecto desarrollado para la materia **Programacion Orientada a Objetos** — Ingenieria en Sistemas.
