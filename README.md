# Farmac — Sistema de Gestion de Farmacia

Sistema de escritorio desarrollado en **Java 21** con **Swing** para la gestion integral de una farmacia: punto de venta, inventario con control FEFO, corte de caja, reportes de caducidad y administracion de usuarios.

---

## Caracteristicas principales

| Modulo | Descripcion |
|---|---|
| **Punto de Venta** | Catalogo con busqueda, lector de codigo de barras, carrito, ticket PDF |
| **Inventario** | Recepcion de lotes, ajustes autorizados, algoritmo FEFO |
| **Corte de Caja** | Arqueo, diferencia automatica, historial de turnos |
| **Reporte Caducidad** | Semaforo de lotes proximos a vencer, exportacion CSV |
| **Gestion Usuarios** | Alta, edicion y activacion/desactivacion de Encargados y Empleados |
| **Catalogo Productos** | CRUD completo, busqueda por codigo de barras |
| **Recetas Medicas** | Captura obligatoria para medicamentos controlados (COFEPRIS) |

---

## Estructura del proyecto

```
Farmac/
├── src/
│   └── farmac/
│       ├── app/          # Punto de entrada (Main.java)
│       ├── config/       # Conexion a base de datos (ConexionBD.java)
│       ├── dominio/      # 13 clases de dominio
│       │   ├── Usuario.java              (abstracta)
│       │   ├── Encargado.java
│       │   ├── Empleado.java
│       │   ├── Producto.java
│       │   ├── Proveedor.java
│       │   ├── Lote.java
│       │   ├── TurnoCaja.java
│       │   ├── Venta.java
│       │   ├── DetalleVenta.java
│       │   ├── RecetaMedica.java
│       │   ├── MovimientoInventario.java
│       │   ├── Bitacora.java
│       │   └── OperacionesInventario.java
│       ├── ui/           # 12 pantallas Swing + 1 dialogo
│       └── util/         # TemaFarmac, TicketGenerator, Sesion, RespaldoUtil
├── sql/                  # Scripts de esquema, datos y migraciones
├── test/                 # Pruebas JUnit 4 (opcional)
├── lib/                  # jbcrypt, postgresql, junit, hamcrest
├── build.xml             # Build con Apache Ant
├── README.md
├── DIAGRAMA_CLASES.md
└── DIAGRAMA_OBJETOS.md
```

---

## Conceptos POO aplicados

| Concepto | Implementacion |
|---|---|
| **Herencia** | `Encargado` y `Empleado` extienden la clase abstracta `Usuario` |
| **Polimorfismo** | `obtenerRol()` sobreescrito en cada subclase |
| **Abstraccion** | `Usuario` es abstracta; no se puede instanciar directamente |
| **Encapsulamiento** | Todos los atributos son `private`/`protected` con getters y setters |
| **Composicion** | `Venta *-- DetalleVenta`: los detalles no existen sin su venta |
| **Agregacion** | `Producto o-- Lote`: un Lote referencia a un Producto independiente |
| **Algoritmo FEFO** | `OperacionesInventario.obtenerLotesFEFO()` ordena por caducidad ascendente |

---

## Requisitos

- **Java 21** (Temurin / OpenJDK)
- **Apache Ant** (para compilar con `build.xml`)
- **NetBeans 21** (recomendado, proyecto Ant nativo)

---

## Base de datos

El proyecto usa PostgreSQL. Cree la base y ejecute los scripts en orden:

```bash
createdb -U postgres farmac_db

psql -U postgres -d farmac_db -f sql/01_schema.sql
psql -U postgres -d farmac_db -f sql/02_datos.sql
psql -U postgres -d farmac_db -f sql/03_inventario.sql
psql -U postgres -d farmac_db -f sql/04_ventas.sql
```

Si la base ya existia antes de esta version, aplique tambien la migracion
que agrega la bitacora y el tipo de movimiento `CANCELACION`:

```bash
psql -U postgres -d farmac_db -f sql/05_migracion_auditoria.sql
```

La conexion se configura en `config/farmac.properties` (`db.url`, `db.user`,
`db.password`).

---

## Como compilar y ejecutar

```bash
# Compilar
ant clean dist

# Ejecutar JAR generado
java -jar dist/Farmac.jar
```

O directamente desde NetBeans: `Run Project (F6)`.

---

## Consultas de Auditoría (Historial de Ventas y Turnos)

La pantalla **Historial de Ventas** (acceso del Encargado) incluye dos consultas
adicionales de auditoría:

- **Pestaña "Ventas con Receta"**: lista las ventas del periodo filtrado que
  contaron con receta médica asociada (folio de receta, médico, producto y estado).
- **Pestaña "Historial de Turnos"**: registra la apertura y el cierre de cada
  turno de caja, con fondos iniciales, montos esperados, efectivo contado y diferencia.
- **Filtro "Solo ventas con receta"** en la barra de filtros.

---

## Tickets PDF

Los tickets se generan automaticamente al completar una venta.  
Se guardan en:

```
tickets/
└── YYYY-MM-DD/
    └── ticket_00001_HHmmss.pdf
```

---

## Roles del sistema

| Rol | Acceso |
|---|---|
| **Encargado** | Todos los modulos: catalogo, inventario, ajustes, proveedores, usuarios, reportes y corte |
| **Empleado** | Punto de venta y corte de caja unicamente |

El login incluye un selector visual Encargado / Empleado. El rol efectivo siempre
se toma del usuario almacenado en la base de datos.

---

## Credenciales de prueba

| Usuario | Password | Rol |
|---|---|---|
| `admin` | `admin123` | Encargado |
| `empleado1` | `caja123` | Empleado |

Los hashes BCrypt correspondientes estan en `sql/02_datos.sql`. Si la base ya
tenia usuarios con hashes invalidos, actualicelos con:

```sql
UPDATE usuario SET password_hash = '$2a$10$fm2dGYB.bd80nsqMQ00waePHAiyCiZdOj8RlXKOVT3uzR7N/SX5/O'
WHERE nombre_usuario = 'admin';
UPDATE usuario SET password_hash = '$2a$10$0UvIkCceCpYYbajeb42PVO8NyJznYD/lw1PpgcMOlJhdmpISMnjym'
WHERE nombre_usuario = 'empleado1';
```

---

## Autores

Proyecto desarrollado para la materia **Programacion Orientada a Objetos** — Ingenieria en Sistemas.
