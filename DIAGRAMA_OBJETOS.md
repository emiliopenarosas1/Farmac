# Diagrama de Objetos — Farmac

Escenario de ejecucion: **venta de un antibiotico con receta medica** atendida por el cajero "Luis Ramos" en turno abierto por el encargado.

---

## Escenario representado

1. El encargado `ana_garcia` tiene un turno de caja abierto con fondo inicial de \$500.
2. El cajero `luis_ramos` atiende la venta.
3. Se vende 1 caja de Amoxicilina (requiere receta) del lote L-2024-001.
4. El sistema registra la receta del Dr. Martinez.
5. La venta queda completada con total de \$89.50.

---

```mermaid
classDiagram
    class encargado1 {
        id = 1
        nombreUsuario = ana_garcia
        nombreCompleto = Ana Garcia Lopez
        activo = true
        rol = ENCARGADO
    }

    class empleado1 {
        id = 2
        nombreUsuario = luis_ramos
        nombreCompleto = Luis Ramos Vega
        activo = true
        rol = EMPLEADO
    }

    class turno1 {
        id = 101
        montoInicial = 500.00
        estado = ABIERTO
        fechaApertura = 2024-10-08T08-00
        usuario = ana_garcia
    }

    class amoxicilina {
        id = 5
        codigoBarras = 7501234567890
        nombre = Amoxicilina 500mg
        categoria = Antibiotico
        precioVenta = 89.50
        requiereReceta = true
        stockMinimo = 10
    }

    class loteA {
        id = 201
        numeroLote = L-2024-001
        fechaCaducidad = 2025-06-30
        cantidadActual = 48
        precioCompra = 45.00
        producto = amoxicilina
    }

    class venta1 {
        id = 3001
        fechaHora = 2024-10-08T10-35
        total = 89.50
        formaPago = EFECTIVO
        estado = COMPLETADA
    }

    class detalle1 {
        id = 1
        cantidad = 1
        precioUnitario = 89.50
        subtotal = 89.50
        lote = loteA
    }

    class receta1 {
        id = 401
        nombreMedico = Dr. Carlos Martinez
        cedulaProfesional = 8812345
        fechaExpedicion = 2024-10-01
        folioReceta = REC-2024-9981
        producto = amoxicilina
    }

    class inventario1 {
        diasAlertaCaducidad = 60
    }

    turno1 --> encargado1 : asignado a
    venta1 --> turno1 : registrada en
    venta1 --> empleado1 : atendida por
    venta1 *-- detalle1 : composicion
    venta1 o-- receta1 : soporta
    detalle1 --> loteA : descuenta de
    receta1 --> amoxicilina : ampara
    loteA --> amoxicilina : es lote de
    inventario1 o-- loteA : gestiona
```

---

## Notas del escenario

| Objeto | Clase | Descripcion |
|---|---|---|
| `encargado1` | `Encargado` | Abre el turno, autoriza ajustes de inventario |
| `empleado1` | `Empleado` | Atiende la venta en caja |
| `turno1` | `TurnoCaja` | Turno ABIERTO con fondo de \$500 |
| `amoxicilina` | `Producto` | Medicamento controlado, requiere receta |
| `loteA` | `Lote` | Partida fisica con caducidad 2025-06-30 (FEFO vigente) |
| `venta1` | `Venta` | Transaccion completada por \$89.50 |
| `detalle1` | `DetalleVenta` | Renglon de la venta, compuesto dentro de `venta1` |
| `receta1` | `RecetaMedica` | Capturada en el dialogo de receta, vigente 30 dias |
| `inventario1` | `OperacionesInventario` | Gestiona el lote con alerta de 60 dias |
