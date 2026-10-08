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
│       ├── dominio/      # 10 clases de dominio
│       │   ├── Usuario.java              (abstracta)
│       │   ├── Encargado.java
│       │   ├── Empleado.java
│       │   ├── Producto.java
│       │   ├── Lote.java
│       │   ├── TurnoCaja.java
│       │   ├── Venta.java
│       │   ├── DetalleVenta.java
│       │   ├── RecetaMedica.java
│       │   └── OperacionesInventario.java
│       ├── ui/           # 10 pantallas Swing + 1 dialogo
│       └── util/         # TemaFarmac.java, TicketGenerator.java
├── test/                 # Pruebas JUnit 4
├── lib/                  # junit-4.13.2.jar, hamcrest-core-1.3.jar
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

## Como compilar y ejecutar

```bash
# Compilar
ant clean dist

# Ejecutar JAR generado
java -jar dist/Farmac.jar
```

O directamente desde NetBeans: `Run Project (F6)`.

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
| **Encargado** | Todos los modulos: catalogo, inventario, ajustes, usuarios, reportes, corte |
| **Empleado** | Punto de venta y corte de caja unicamente |

El login incluye un selector Encargado / Empleado con credenciales de prueba precargadas.

---

## Credenciales de prueba

| Usuario | Password | Rol |
|---|---|---|
| `admin` | `admin123` | Encargado |
| `cajero1` | `caja123` | Empleado |

---

## Autores

Proyecto desarrollado para la materia **Programacion Orientada a Objetos** — Ingenieria en Sistemas.
