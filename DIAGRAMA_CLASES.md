# Diagrama de Clases — Farmac

Diagrama UML de clases del sistema, representando herencia, composicion, agregacion y asociaciones entre las 10 clases del dominio.

---

## Diagrama 1 — Jerarquia de Usuarios y Turno de Caja

```mermaid
classDiagram
    Usuario <|-- Encargado
    Usuario <|-- Empleado
    TurnoCaja --> Usuario : asignado a

    class Usuario {
        <<Abstract>>
        #int id
        #String nombreUsuario
        #String nombreCompleto
        #String password
        #boolean activo
        +autenticar(String password) boolean
        +cambiarPassword(String nuevaPassword) void
        +obtenerRol() String
        +getId() int
        +getNombreUsuario() String
        +getNombreCompleto() String
        +isActivo() boolean
        +setActivo(boolean activo) void
    }

    class Encargado {
        +autorizarAjuste(Lote lote, int cantidad, String motivo) boolean
        +realizarCorte(TurnoCaja turno, double efectivo, double ventas) double
        +obtenerRol() String
    }

    class Empleado {
        +iniciarTurno(double montoInicial) TurnoCaja
        +obtenerRol() String
    }

    class TurnoCaja {
        -int id
        -LocalDateTime fechaApertura
        -double montoInicial
        -LocalDateTime fechaCierre
        -double efectivoContado
        -double montoEsperado
        -String estado
        -Usuario usuario
        +cerrarTurno(double efectivo, double totalVentas) double
        +calcularDiferencia() double
        +getId() int
        +getMontoInicial() double
        +getEstado() String
        +getUsuario() Usuario
    }
```

---

## Diagrama 2 — Inventario

```mermaid
classDiagram
    Producto o-- Lote : contiene
    OperacionesInventario o-- Lote : gestiona

    class Producto {
        -int id
        -String codigoBarras
        -String nombre
        -String categoria
        -String proveedor
        -double precioVenta
        -boolean requiereReceta
        -int stockMinimo
        +calcularStockTotal(List lotes) int
        +esBajoStock(List lotes) boolean
        +getId() int
        +getCodigoBarras() String
        +getNombre() String
        +getPrecioVenta() double
        +isRequiereReceta() boolean
        +getStockMinimo() int
    }

    class Lote {
        -int id
        -String numeroLote
        -LocalDate fechaCaducidad
        -int cantidadActual
        -double precioCompra
        -Producto producto
        +descontarStock(int cantidad) boolean
        +incrementarStock(int cantidad) void
        +estaVencido(LocalDate fechaActual) boolean
        +diasParaVencer(LocalDate fechaActual) long
        +getId() int
        +getNumeroLote() String
        +getFechaCaducidad() LocalDate
        +getCantidadActual() int
        +getProducto() Producto
    }

    class OperacionesInventario {
        -List inventario
        -int diasAlertaCaducidad
        +registrarEntrada(Lote nuevoLote) void
        +obtenerLotesFEFO(Producto producto, int cantidad) List
        +obtenerLotesEnAlerta() List
        +ajustarStockLote(Lote lote, int nuevaCantidad, String motivo) boolean
    }
```

---

## Diagrama 3 — Ventas y Recetas

```mermaid
classDiagram
    Venta --> TurnoCaja : registrada en
    Venta --> Usuario : atendida por
    Venta *-- DetalleVenta : composicion
    Venta o-- RecetaMedica : soporta
    DetalleVenta --> Lote : descuenta de
    RecetaMedica --> Producto : ampara

    class Venta {
        -int id
        -LocalDateTime fechaHora
        -double total
        -String formaPago
        -String estado
        -String motivoCancelacion
        +agregarDetalle(Lote lote, int cantidad, double precio) DetalleVenta
        +asignarReceta(RecetaMedica receta) void
        +calcularTotal() double
        +cancelar(String motivo) boolean
        +getId() int
        +getTotal() double
        +getEstado() String
    }

    class DetalleVenta {
        -int id
        -int cantidad
        -double precioUnitario
        -double subtotal
        -Lote lote
        +calcularSubtotal() double
        +getId() int
        +getCantidad() int
        +getPrecioUnitario() double
        +getSubtotal() double
        +getLote() Lote
    }

    class RecetaMedica {
        -int id
        -String nombreMedico
        -String cedulaProfesional
        -LocalDate fechaExpedicion
        -String folioReceta
        -Producto producto
        +validarDatos() boolean
        +estaVigente(LocalDate fechaActual) boolean
        +getId() int
        +getNombreMedico() String
        +getCedulaProfesional() String
        +getFolioReceta() String
    }

    class Lote {
        -int id
        -String numeroLote
        -LocalDate fechaCaducidad
        -int cantidadActual
        +descontarStock(int cantidad) boolean
        +getCantidadActual() int
    }

    class Producto {
        -int id
        -String nombre
        -boolean requiereReceta
        +getNombre() String
        +isRequiereReceta() boolean
    }

    class TurnoCaja {
        -int id
        -String estado
        +getEstado() String
    }

    class Usuario {
        <<Abstract>>
        #int id
        #String nombreCompleto
        +obtenerRol() String
    }
```

---

> Los diagramas tambien estan disponibles en texto plano en `DIAGRAMA_1_USUARIOS.txt`, `DIAGRAMA_2_INVENTARIO.txt` y `DIAGRAMA_3_VENTAS.txt` para pegar directamente en [mermaid.live](https://mermaid.live/edit).
