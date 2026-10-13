# Especificación de Casos de Uso, Modelo de Dominio y Arquitectura POO
## Sistema de Gestión Farmacéutica — Farmac

Documento técnico elaborado para **revisión externa, auditoría académica y validación de diseño de software**. Contiene el desglose formal de requerimientos funcionales, catálogo de actores, matriz de casos de uso, diagrama UML con límites del sistema y relaciones estereotipadas (`<<include>>`, `<<extend>>`, generalizaciones), así como el mapeo estricto contra las clases del dominio en Java 21.

---

## 1. Requerimientos Funcionales del Sistema (RF)

| ID | Nombre | Descripción Técnica |
|---|---|---|
| **RF-01** | **Autenticación Polimórfica y Control de Acceso** | El sistema debe validar credenciales de acceso diferenciando entre perfiles `ENCARGADO` y `EMPLEADO` mediante una clase abstracta base y polimorfismo en tiempo de ejecución. |
| **RF-02** | **Apertura de Turno de Caja** | Registro del fondo fijo inicial antes de iniciar transacciones de cobro en el punto de venta. |
| **RF-03** | **Procesamiento de Venta en Mostrador** | Adición de productos mediante lectura de código de barras o búsqueda manual en catálogo; cálculo dinámico de subtotales y total general. |
| **RF-04** | **Despacho Automatizado según Regla FEFO** | Selección y deducción obligatoria del inventario priorizando los lotes vigentes con fecha de caducidad más cercana (*First Expired, First Out*). |
| **RF-05** | **Captura y Validación de Receta Médica** | Para medicamentos regulados (`requiereReceta == true`), el sistema debe condicionar la finalización de la venta a la captura y validación obligatoria de: médico emisor, cédula profesional, folio de receta y vigencia máxima de 30 días naturales. |
| **RF-06** | **Emisión de Comprobante PDF** | Generación autónoma incondicional de un comprobante de pago en formato PDF estándar archivado en una estructura cronológica de directorios (`tickets/YYYY-MM-DD/`). |
| **RF-07** | **Cancelación de Ventas** | Posibilidad de revertir una transacción completada reintegrando íntegramente las unidades a sus respectivos lotes de origen. |
| **RF-08** | **Corte y Arqueo de Caja** | Cierre de turno calculando la diferencia entre el efectivo físico capturado por el usuario y el saldo contable esperado (Fondo inicial + Ventas en efectivo). |
| **RF-09** | **Recepción y Registro de Lotes** | Registro de nuevas partidas físicas con número de lote, fecha de vencimiento, costo de adquisición y producto asociado. |
| **RF-10** | **Ajustes Manuales de Inventario** | Corrección de existencias por mermas, daños o auditorías físicas. Requiere justificación obligatoria y autorización exclusiva del Encargado. |
| **RF-11** | **Monitoreo de Caducidades y Alertas** | Clasificación visual mediante semáforo de lotes vencidos o próximos a expirar (umbral estándar de 60 días) con capacidad de exportación a CSV. |
| **RF-12** | **Mantenimiento de Catálogo de Productos (CRUD)** | Altas, bajas lógicas, parametrización de stock de seguridad y precio de venta al público. |
| **RF-13** | **Administración de Cuentas de Usuario (CRUD)** | Control de cuentas de empleados, asignación de roles jerárquicos y desactivación de credenciales. |

---

## 2. Identificación y Jerarquía de Actores

En el modelado UML se identifican dos actores humanos primarios con una relación de **generalización / herencia de actores**, más un actor secundario del sistema operativo:

### 2.1. `Usuario` (Actor General / Abstracto)
- **Definición**: Representa a cualquier usuario autenticado en el sistema.
- **Responsabilidades**: Autenticarse en el sistema, gestionar su sesión activa y actualizar su propia contraseña.

### 2.2. `Empleado` (Cajero / Operador de Mostrador)
- **Especialización**: Hereda todas las capacidades del actor `Usuario`.
- **Ámbito Operativo**: Operaciones rutinarias de mostrador en caja: apertura y cierre de su turno, registro de ventas, validación de recetas, consulta básica de catálogo y generación de tickets.

### 2.3. `Encargado` (Administrador / Farmacéutico Titular)
- **Especialización**: Hereda de `Usuario` (y por extensibilidad operativa puede desempeñar todas las funciones de `Empleado`).
- **Ámbito Operativo**: Operaciones privilegiadas: autorización de ajustes de existencias, alta de recepciones de lotes, administración del catálogo de medicamentos, supervisión de usuarios y auditoría de caducidades y cortes de caja.

### 2.4. `Sistema de Archivos del SO` (Actor Secundario / Soporte)
- **Definición**: Subsistema encargado del almacenamiento no volátil de tickets en PDF y reportes tabulares en CSV.

```
       [ Usuario ] (Abstracto)
           ▲
      ┌────┴────┐
      │         │
 [ Empleado ]  [ Encargado ]
```

---

## 3. Matriz de Casos de Uso por Actor

| Actor | Código | Caso de Uso | Relaciones Estereotipadas |
|---|---|---|---|
| **Usuario** | `CU-01` | Iniciar Sesión | Caso de uso base |
| **Usuario** | `CU-02` | Cambiar Contraseña | Caso de uso base |
| **Empleado** | `CU-03` | Abrir Turno de Caja | Caso de uso base |
| **Empleado** | `CU-04` | Registrar Venta de Mostrador | `<<include>>` CU-05, CU-06 <br> `<<extend>>` CU-07 |
| **Sistema** | `CU-05` | Descontar Stock por Regla FEFO | Invocado vía `<<include>>` desde CU-04 |
| **Sistema** | `CU-06` | Generar Comprobante PDF de Venta | Invocado vía `<<include>>` desde CU-04 |
| **Empleado** | `CU-07` | Registrar Datos de Receta Médica | Extiende a CU-04 bajo condición de medicamento controlado |
| **Empleado** | `CU-08` | Cancelar Venta | Reintegra existencias al inventario |
| **Empleado** | `CU-09` | Realizar Corte de Caja (Arqueo) | `<<include>>` CU-10 |
| **Sistema** | `CU-10` | Calcular Discrepancia Contable | Invocado vía `<<include>>` desde CU-09 |
| **Empleado / Encargado** | `CU-11` | Consultar Catálogo y Existencias | Caso de uso de consulta |
| **Encargado** | `CU-12` | Registrar Entrada de Lotes | Caso de uso administrativo |
| **Encargado** | `CU-13` | Autorizar Ajuste de Inventario | Requiere motivo y rol Encargado |
| **Encargado** | `CU-14` | Consultar Alertas de Caducidad | `<<extend>>` CU-15 |
| **Encargado** | `CU-15` | Exportar Alertas a Archivo CSV | Extensión opcional de CU-14 |
| **Encargado** | `CU-16` | Administrar Catálogo de Productos | CRUD de medicamentos |
| **Encargado** | `CU-17` | Administrar Cuentas de Usuario | CRUD de operadores y roles |

---

## 4. Diagrama de Casos de Uso (UML)

El diagrama delimita el **Sistema Farmac** con su frontera perimetral, muestra la herencia entre actores y las dependencias `<<include>>` y `<<extend>>`.

```mermaid
flowchart LR
    %% Actores
    subgraph Actores["Actores del Sistema"]
        Usuario["fa:fa-user Usuario\n(Abstracto)"]
        Empleado["fa:fa-user-nurse Empleado\n(Cajero)"]
        Encargado["fa:fa-user-tie Encargado\n(Administrador)"]
        SistemaArchivos["fa:fa-file-pdf Sistema de Archivos\n(Actor Secundario)"]
    end

    Empleado --|> Usuario
    Encargado --|> Usuario

    %% Límite del Sistema
    subgraph SistemaFarmac ["LÍMITE DEL SISTEMA: Sistema de Gestión Farmacéutica (Farmac)"]
        
        %% Módulo de Sesión
        CU01(["CU-01: Iniciar Sesión"])
        CU02(["CU-02: Cambiar Contraseña"])
        
        %% Módulo de Ventas y Caja
        CU03(["CU-03: Abrir Turno de Caja"])
        CU04(["CU-04: Registrar Venta de Mostrador"])
        CU05(["CU-05: Descontar Lotes FEFO"])
        CU06(["CU-06: Generar Ticket PDF"])
        CU07(["CU-07: Registrar Receta Médica"])
        CU08(["CU-08: Cancelar Venta"])
        CU09(["CU-09: Realizar Corte de Caja"])
        CU10(["CU-10: Calcular Discrepancia Arqueo"])
        
        %% Módulo de Inventarios
        CU11(["CU-11: Consultar Existencias"])
        CU12(["CU-12: Registrar Entrada de Lote"])
        CU13(["CU-13: Autorizar Ajuste de Stock"])
        CU14(["CU-14: Consultar Alertas de Caducidad"])
        CU15(["CU-15: Exportar Alertas a CSV"])
        
        %% Módulo de Administración
        CU16(["CU-16: Gestionar Catálogo Productos"])
        CU17(["CU-17: Gestionar Cuentas Usuarios"])

        %% Relaciones Include
        CU04 -.->|"<<include>>"| CU05
        CU04 -.->|"<<include>>"| CU06
        CU09 -.->|"<<include>>"| CU10

        %% Relaciones Extend
        CU07 -.->|"<<extend>>\n[Requiere Receta]"| CU04
        CU15 -.->|"<<extend>>\n[Solicitud de archivo]"| CU14
    end

    %% Asociaciones Actor-Caso de Uso
    Usuario --- CU01
    Usuario --- CU02

    Empleado --- CU03
    Empleado --- CU04
    Empleado --- CU08
    Empleado --- CU09
    Empleado --- CU11

    Encargado --- CU12
    Encargado --- CU13
    Encargado --- CU14
    Encargado --- CU16
    Encargado --- CU17
    Encargado --- CU11

    CU06 --- SistemaArchivos
    CU15 --- SistemaArchivos
```

---

## 5. Justificación Técnica de Relaciones UML

### 5.1. Generalización de Actores
- **`Empleado --|> Usuario`** y **`Encargado --|> Usuario`**:
  - Ambas figuras heredan la capacidad de autenticarse y operar con credenciales cifradas.
  - El Encargado cuenta con polimorfismo en sus métodos de negocio (`autorizarAjuste()` y `realizarCorte()`), mientras que el Empleado inicializa turnos comerciales (`iniciarTurno()`).

### 5.2. Relaciones `<<include>>` (Inclusión Obligatoria)
Una relación `<<include>>` modela un comportamiento que se ejecuta **siempre y obligatoriamente** como parte del flujo base:
1. **`Registrar Venta` -- `<<include>>` --> `Descontar Lotes FEFO`**:
   - Una venta no puede considerarse válida sin la deducción inmediata del inventario físico en la partida correspondiente según el algoritmo FEFO.
2. **`Registrar Venta` -- `<<include>>` --> `Generar Ticket PDF`**:
   - Al completarse la transacción financiera, el subsistema `TicketGenerator` genera sin excepción el documento binario PDF en disco para entrega al cliente.
3. **`Realizar Corte de Caja` -- `<<include>>` --> `Calcular Discrepancia Arqueo`**:
   - El arqueo físico incluye forzosamente la fórmula matemática `Diferencia = EfectivoContado - (FondoInicial + VentasEfectivo)`.

### 5.3. Relaciones `<<extend>>` (Extensión Condicional)
Una relación `<<extend>>` modela un comportamiento optativo o condicional que se inserta únicamente en un **Punto de Extensión** bajo circunstancias específicas:
1. **`Registrar Receta Médica` -- `<<extend>>` --> `Registrar Venta`**:
   - **Punto de Extensión**: *Pre-Cobro de Producto Controlado*.
   - **Condición**: Se activa si y solo si al menos un ítem agregado al carrito tiene el atributo booleano `requiereReceta == true`. Si el cliente solo adquiere analgésicos de libre venta (OTC), este caso de uso **no** se ejecuta.
2. **`Exportar Alertas a CSV` -- `<<extend>>` --> `Consultar Alertas de Caducidad`**:
   - **Punto de Extensión**: *Generación de Respaldo Externo*.
   - **Condición**: Se activa cuando el Encargado presiona expresamente la opción de descarga de datos.

---

## 6. Mapeo Arquitectónico contra Clases del Dominio POO

A continuación se detalla la congruencia entre los casos de uso descritos y las clases implementadas en el paquete `farmac.dominio`:

```
┌─────────────────────────────────┐           ┌─────────────────────────────────┐
│        <<Abstract>>             │           │           Encargado             │
│           Usuario               │◄──────────┤   - autorizarAjuste(...)        │
│  - autenticar(password)         │           │   - realizarCorte(...)          │
│  - obtenerRol()*                │           │   - obtenerRol() = "ENCARGADO"  │
└────────────────┬────────────────┘           └─────────────────────────────────┘
                 ▲
                 │                            ┌─────────────────────────────────┐
                 │                            │           Empleado              │
                 └────────────────────────────┤   - iniciarTurno(...)           │
                                              │   - obtenerRol() = "EMPLEADO"   │
                                              └─────────────────────────────────┘
```

### 6.1. Especificación de Clases de Dominio

#### 1. `Usuario` (Clase Abstracta)
- **Patrón/Concepto**: Abstracción y Encapsulamiento.
- **Atributos**: `id: int`, `nombreUsuario: String`, `nombreCompleto: String`, `password: String`, `activo: boolean`.
- **Métodos Clave**:
  - `autenticar(String passwordIngresada): boolean` — Valida estado activo y contraseña.
  - `obtenerRol(): String` — Método abstracto polimórfico implementado en cada rol.
- **Soporte a Casos de Uso**: `CU-01`, `CU-02`.

#### 2. `Encargado` (Subclase de `Usuario`)
- **Patrón/Concepto**: Herencia y Especialización.
- **Métodos Clave**:
  - `autorizarAjuste(Lote lote, int cantidad, String motivo): boolean` — Valida y aplica correcciones al stock.
  - `realizarCorte(TurnoCaja turno, double efectivoContado, double ventasEfectivo): double` — Calcula y registra arqueo.
  - `obtenerRol(): String` — Retorna `"ENCARGADO"`.
- **Soporte a Casos de Uso**: `CU-12`, `CU-13`, `CU-14`, `CU-16`, `CU-17`.

#### 3. `Empleado` (Subclase de `Usuario`)
- **Patrón/Concepto**: Herencia y Polimorfismo.
- **Métodos Clave**:
  - `iniciarTurno(double montoInicial): TurnoCaja` — Inicializa el turno del cajero.
  - `obtenerRol(): String` — Retorna `"EMPLEADO"`.
- **Soporte a Casos de Uso**: `CU-03`, `CU-04`, `CU-08`, `CU-09`.

#### 4. `Producto`
- **Patrón/Concepto**: Entidad de catálogo y regla de control médico.
- **Atributos**: `id: int`, `codigoBarras: String`, `nombre: String`, `categoria: String`, `proveedor: String`, `precioVenta: double`, `requiereReceta: boolean`, `stockMinimo: int`.
- **Métodos Clave**:
  - `calcularStockTotal(List<Lote> lotes): int` — Sumatoria de cantidades de todos sus lotes activos.
  - `esBajoStock(List<Lote> lotes): boolean` — Evalúa si el stock consolidado es menor o igual al umbral mínimo.
- **Soporte a Casos de Uso**: `CU-04`, `CU-07`, `CU-11`, `CU-16`.

#### 5. `Lote`
- **Patrón/Concepto**: Agregación con `Producto` (`Producto o-- Lote`).
- **Atributos**: `id: int`, `numeroLote: String`, `fechaCaducidad: LocalDate`, `cantidadActual: int`, `precioCompra: double`, `producto: Producto`.
- **Métodos Clave**:
  - `descontarStock(int cantidad): boolean` — Reduce existencias si hay disponibilidad.
  - `incrementarStock(int cantidad): void` — Aumenta existencias (ingresos o cancelaciones).
  - `estaVencido(LocalDate fecha): boolean` — Verifica si la caducidad antecede a la fecha actual.
  - `diasParaVencer(LocalDate fecha): long` — Diferencia cronológica en días para alertas.
- **Soporte a Casos de Uso**: `CU-04`, `CU-05`, `CU-08`, `CU-12`, `CU-13`.

#### 6. `TurnoCaja`
- **Patrón/Concepto**: Asociación con `Usuario`.
- **Atributos**: `id: int`, `fechaApertura: LocalDateTime`, `montoInicial: double`, `fechaCierre: LocalDateTime`, `efectivoContado: double`, `montoEsperado: double`, `estado: String`, `usuario: Usuario`.
- **Métodos Clave**:
  - `cerrarTurno(double efectivoContado, double totalVentasEfectivo): double` — Cierra el turno y computa la diferencia.
  - `calcularDiferencia(): double` — Fórmula `efectivoContado - montoEsperado`.
- **Soporte a Casos de Uso**: `CU-03`, `CU-09`, `CU-10`.

#### 7. `Venta`
- **Patrón/Concepto**: Composición Fuerte con `DetalleVenta` (`Venta *-- DetalleVenta`), Asociación con `TurnoCaja` y `Usuario`, Agregación con `RecetaMedica`.
- **Atributos**: `id: int`, `fechaHora: LocalDateTime`, `total: double`, `formaPago: String`, `estado: String`, `motivoCancelacion: String`, `turnoCaja: TurnoCaja`, `usuario: Usuario`, `detalles: List<DetalleVenta>`, `receta: RecetaMedica`.
- **Métodos Clave**:
  - `agregarDetalle(Lote lote, int cantidad, double precio): DetalleVenta` — Crea e incorpora una línea de venta.
  - `asignarReceta(RecetaMedica receta): void` — Asocia la receta médica validada.
  - `calcularTotal(): double` — Totaliza los subtotales de sus detalles.
  - `cancelar(String motivo): boolean` — Cambia estado y reintegra stock a cada lote asociado.
- **Soporte a Casos de Uso**: `CU-04`, `CU-06`, `CU-07`, `CU-08`.

#### 8. `DetalleVenta`
- **Patrón/Concepto**: Componente de composición de `Venta`. No tiene ciclo de vida independiente.
- **Atributos**: `id: int`, `cantidad: int`, `precioUnitario: double`, `subtotal: double`, `lote: Lote`.
- **Métodos Clave**:
  - `calcularSubtotal(): double` — Retorna `cantidad * precioUnitario`.
- **Soporte a Casos de Uso**: `CU-04`, `CU-05`, `CU-08`.

#### 9. `RecetaMedica`
- **Patrón/Concepto**: Asociación con `Producto` y soporte a `Venta`.
- **Atributos**: `id: int`, `nombreMedico: String`, `cedulaProfesional: String`, `fechaExpedicion: LocalDate`, `folioReceta: String`, `producto: Producto`.
- **Métodos Clave**:
  - `validarDatos(): boolean` — Verifica completitud de folios, cédula y nombres.
  - `estaVigente(LocalDate fecha): boolean` — Verifica que no hayan transcurrido más de 30 días naturales desde su expedición.
- **Soporte a Casos de Uso**: `CU-07`.

#### 10. `OperacionesInventario`
- **Patrón/Concepto**: Controlador de Reglas de Negocio / Algoritmo FEFO.
- **Atributos**: `inventario: List<Lote>`, `diasAlertaCaducidad: int`.
- **Métodos Clave**:
  - `obtenerLotesFEFO(Producto producto, int cantidadRequerida): List<Lote>` — Filtra lotes no caducados con existencia y los ordena cronológicamente por `fechaCaducidad` ascendente.
  - `obtenerLotesEnAlerta(): List<Lote>` — Filtra lotes vencidos o con vigencia menor a `diasAlertaCaducidad`.
  - `ajustarStockLote(Lote lote, int nuevaCantidad, String motivo): boolean` — Aplica y audita ajustes manuales.
- **Soporte a Casos de Uso**: `CU-05`, `CU-13`, `CU-14`.

---

## 7. Fichas Descriptivas de Casos de Uso Críticos

### Ficha 1: CU-04 Registrar Venta de Mostrador
- **Actor Principal**: Empleado (Cajero)
- **Precondiciones**: El empleado tiene un turno de caja abierto (`TurnoCaja.estado == "ABIERTO"`).
- **Flujo Principal**:
  1. El cajero escanea el código de barras o selecciona un producto del catálogo.
  2. El sistema consulta las existencias mediante `OperacionesInventario.obtenerLotesFEFO()`.
  3. El sistema añade el producto al carrito creando una instancia de `DetalleVenta` asociada al lote seleccionado.
  4. Si algún producto del carrito requiere receta médica, se ejecuta la extensión **CU-07 (Registrar Receta Médica)**.
  5. El cajero selecciona el método de pago y confirma la operación.
  6. El sistema descuenta el stock del lote (`<<include>> CU-05`).
  7. El sistema invoca al generador de PDF para emitir el ticket (`<<include>> CU-06`).
  8. El sistema guarda la venta en estado `"COMPLETADA"`.
- **Postcondiciones**: Stock físico disminuido, dinero sumado a las ventas del turno, comprobante PDF generado.

### Ficha 2: CU-07 Registrar Receta Médica (`<<extend>>`)
- **Actor Principal**: Empleado (Cajero)
- **Punto de Extensión**: Pre-confirmación de venta en CU-04 cuando `Producto.requiereReceta == true`.
- **Flujo Principal**:
  1. El sistema detecta que el medicamento agregado está catalogado como controlado o antibiótico.
  2. El sistema despliega de forma modal la ventana `RegistroRecetaDialog`.
  3. El cajero ingresa: Nombre del médico, Cédula profesional, Folio de receta y Fecha de expedición.
  4. El sistema ejecuta `RecetaMedica.validarDatos()` y `RecetaMedica.estaVigente()`.
  5. Si los datos son válidos y la receta tiene menos de 30 días de expedición, se asocia a la `Venta` (`venta.asignarReceta(receta)`).
  6. Se reanuda el flujo principal de venta.
- **Flujo Alternativo**:
  - Si la receta está vencida o faltan datos obligatorios, el sistema bloquea la venta del medicamento hasta corregir o cancelar el ítem.

---

## 8. Conclusiones para Revisión Externa

1. **Alineación POO**: Existe correspondencia biunívoca entre los Casos de Uso analizados y los métodos implementados en el código fuente de `farmac.dominio`.
2. **Cumplimiento Normativo (COFEPRIS)**: La separación y extensión de `CU-07 Registrar Receta Médica` garantiza el control legal obligatorio para antibióticos y controlados sin entorpecer la venta ágil de medicamentos OTC.
3. **Robustez Transaccional**: Las relaciones `<<include>>` hacia el algoritmo FEFO y la generación incondicional del PDF garantizan trazabilidad física y fiscal de cada operación.
