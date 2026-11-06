# Documentacion del Proyecto — Farmac

## Identidad del proyecto

**Nombre del proyecto:** Farmac

**Descripcion:** Farmac es un sistema de punto de venta e inventario para farmacia. El sistema permite el registro de medicamentos, proveedores y lotes; la dispensacion mediante punto de venta con validacion de recetas; el control de inventario con criterio de fecha de caducidad; el arqueo de caja y la generacion de reportes.

## Funcionalidades y proposito

### Funcionalidades

- Punto de venta con catalogo, busqueda por codigo de barras y ticket en PDF.
- Recepcion de lotes y ajustes de inventario con movimientos.
- Control de inventario con FEFO (fecha de caducidad mas proxima).
- Validacion de recetas medicas para medicamentos que lo requieren.
- Corte de caja con apertura, cierre, conteo y arqueo.
- Reportes de caducidad, existencias y ventas con receta.
- Bitacora de auditoria y gestion de usuarios con roles.

### Proposito

El proposito del proyecto es cubrir el ciclo completo de la dispensacion de medicamentos en una farmacia: desde el registro del medicamento y su origen, hasta la venta validada por receta, el movimiento de inventario y el cierre del turno de caja.

## Enfoque de la base de datos

La persistencia de Farmac se sustenta en **PostgreSQL**, un sistema de gestion de base de datos relacional de codigo abierto que implementa el modelo relacional y garantiza transacciones con propiedades **ACID**. El acceso a la base de datos se realiza mediante el protocolo JDBC sobre el driver `postgresql-42.5.4`.

El modelo de datos esta normalizado en el sentido funcional de la aplicacion y responde a las areas de negocio: identidad del usuario, auditoria, abastecimiento, inventario, ventas, recetas y caja.

## Modelo de datos

### Alcance del modelo

El modelo de datos contempla **11 tablas**:

1. `usuario`
2. `bitacora`
3. `proveedor`
4. `categoria`
5. `producto`
6. `lote`
7. `movimiento_inventario`
8. `turno_caja`
9. `venta`
10. `detalle_venta`
11. `receta_venta`

### Tablas del modelo

#### 1. Tabla `usuario`

Registra las credenciales y el rol de las personas que interactuan con el sistema.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| nombre_usuario | VARCHAR(50) | NOT NULL, UNIQUE |
| nombre_completo | VARCHAR(100) | NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL |
| rol | VARCHAR(20) | NOT NULL, CHECK (ENCARGADO, EMPLEADO) |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

#### 2. Tabla `bitacora`

Registra las acciones del sistema para auditoria.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| usuario_id | INT | NOT NULL, FK -> usuario(id) |
| accion | VARCHAR(50) | NOT NULL |
| descripcion | VARCHAR(255) | NOT NULL |
| fecha_hora | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP |
| ip | VARCHAR(50) |  |

#### 3. Tabla `proveedor`

Registra a los proveedores de medicamentos.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| razon_social | VARCHAR(150) | NOT NULL |
| contacto | VARCHAR(100) |  |
| telefono | VARCHAR(30) |  |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

#### 4. Tabla `categoria`

Registra las categorias de medicamentos.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| nombre | VARCHAR(100) | NOT NULL UNIQUE |

#### 5. Tabla `producto`

Registra los medicamentos comercializados.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| codigo_barras | VARCHAR(50) | NOT NULL UNIQUE |
| nombre | VARCHAR(150) | NOT NULL |
| categoria_id | INT | NOT NULL FK -> categoria(id) |
| proveedor_id | INT | NOT NULL FK -> proveedor(id) |
| precio_venta | NUMERIC(10,2) | NOT NULL CHECK (precio_venta > 0) |
| requiere_receta | BOOLEAN | NOT NULL DEFAULT FALSE |
| stock_minimo | INT | NOT NULL DEFAULT 5 CHECK (stock_minimo >= 0) |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

#### 6. Tabla `lote`

Registra los lotes de caducidad por medicamento.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| producto_id | INT | NOT NULL FK -> producto(id) |
| numero_lote | VARCHAR(50) | NOT NULL |
| fecha_caducidad | DATE | NOT NULL |
| cantidad_recibida | INT | NOT NULL CHECK (cantidad_recibida > 0) |
| cantidad_actual | INT | NOT NULL CHECK (cantidad_actual >= 0) |
| precio_compra | NUMERIC(10,2) | NOT NULL CHECK (precio_compra > 0) |
| proveedor_id | INT | NOT NULL FK -> proveedor(id) |
| fecha_recepcion | DATE | NOT NULL DEFAULT CURRENT_DATE |
| referencia_factura | VARCHAR(100) |  |
| **Regla** | **UNIQUE (producto_id, numero_lote)** | Restriccion de unicidad |

#### 7. Tabla `movimiento_inventario`

Registra los movimientos sobre los lotes.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| lote_id | INT | NOT NULL FK -> lote(id) |
| tipo | VARCHAR(30) | NOT NULL CHECK (RECEPCION, VENTA, AJUSTE, DEVOLUCION, CANCELACION) |
| cantidad | INT | NOT NULL; signo positivo para entradas y negativo para salidas |
| motivo | VARCHAR(255) | NOT NULL |
| referencia_id | VARCHAR(100) |  |
| usuario_id | INT | NOT NULL FK -> usuario(id) |
| fecha_hora | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP |

#### 8. Tabla `turno_caja`

Registra el ciclo de apertura y cierre de caja.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| usuario_id | INT | NOT NULL FK -> usuario(id) |
| fecha_apertura | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP |
| monto_inicial | NUMERIC(10,2) | NOT NULL CHECK (monto_inicial >= 0) |
| fecha_cierre | TIMESTAMP |  |
| efectivo_contado | NUMERIC(10,2) |  |
| monto_esperado | NUMERIC(10,2) |  |
| diferencia | NUMERIC(10,2) |  |
| estado | VARCHAR(20) | NOT NULL DEFAULT 'ABIERTO', CHECK (ABIERTO, CERRADO) |

#### 9. Tabla `venta`

Registra la venta de un medicamento.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| turno_id | INT | NOT NULL FK -> turno_caja(id) |
| usuario_id | INT | NOT NULL FK -> usuario(id) |
| fecha_hora | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP |
| total | NUMERIC(10,2) | NOT NULL CHECK (total > 0) |
| forma_pago | VARCHAR(20) | NOT NULL CHECK (EFECTIVO, TARJETA, TRANSFERENCIA) |
| estado | VARCHAR(20) | NOT NULL DEFAULT 'COMPLETADA', CHECK (COMPLETADA, CANCELADA) |
| motivo_cancelacion | VARCHAR(255) |  |
| cancelada_en | TIMESTAMP |  |

#### 10. Tabla `detalle_venta`

Detalla los medicamentos vendidos en una venta.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| venta_id | INT | NOT NULL FK -> venta(id) ON DELETE CASCADE |
| producto_id | INT | NOT NULL FK -> producto(id) |
| lote_id | INT | NOT NULL FK -> lote(id) |
| cantidad | INT | NOT NULL CHECK (cantidad > 0) |
| precio_unitario | NUMERIC(10,2) | NOT NULL CHECK (precio_unitario > 0) |
| subtotal | NUMERIC(10,2) | NOT NULL CHECK (subtotal > 0) |

#### 11. Tabla `receta_venta`

Registra la receta asociada a la venta de un medicamento.

| Atributo | Tipo | Restricciones |
|---|---|---|
| id | SERIAL | Clave primaria |
| venta_id | INT | NOT NULL FK -> venta(id) |
| producto_id | INT | NOT NULL FK -> producto(id) |
| nombre_medico | VARCHAR(150) | NOT NULL |
| cedula_profesional | VARCHAR(50) | NOT NULL |
| fecha_expedicion | DATE | NOT NULL |
| folio_receta | VARCHAR(50) | NOT NULL |

### Relaciones entre tablas

Las relaciones del modelo se expresan mediante claves foraneas y se resuelven de la siguiente manera:

- `usuario` (1) -> `bitacora` (N): cada registro de auditoria se atribuye a un usuario.
- `usuario` (1) -> `turno_caja` (N): cada turno de caja es operado por un usuario.
- `usuario` (1) -> `movimiento_inventario` (N): cada movimiento registra al usuario responsable.
- `categoria` (N) -> `producto` (1): un producto se clasifica en una categoria.
- `proveedor` (1) -> `producto` (N): un proveedor suministra productos.
- `proveedor` (1) -> `lote` (N): un proveedor emite lotes.
- `producto` (1) -> `lote` (N): un producto puede tener varios lotes.
- `lote` (1) -> `movimiento_inventario` (N): cada movimiento opera sobre un lote.
- `turno_caja` (1) -> `venta` (N): cada venta se realiza dentro de un turno.
- `venta` (1) -> `detalle_venta` (N): los detalles se integran a una venta.
- `venta` (1) -> `receta_venta` (N): la receta queda asociada a la venta.

### Justificacion de las relaciones

- La relacion `usuario -> bitacora` sustenta la trazabilidad de las acciones realizadas en el sistema.
- La relacion `turno_caja -> venta` agrupa las operaciones de venta dentro de un periodo de caja.
- La relacion `producto -> lote` sustenta el control por caducidad.
- La relacion `venta -> detalle_venta` registra que medicamento y lote se despacharon.
- La relacion `venta -> receta_venta` conserva el historial de recetas por venta.

## Scripts de creacion del esquema

A continuacion se presentan los scripts de SQL para la creacion del esquema en base de datos.

### `sql/01_schema.sql`

```sql
-- Schema: Farmac - Punto de Venta e Inventario

CREATE TABLE IF NOT EXISTS usuario (
    id SERIAL PRIMARY KEY,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    nombre_completo VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ENCARGADO', 'EMPLEADO')),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS bitacora (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuario(id),
    accion VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_bitacora_usuario_id ON bitacora(usuario_id);
CREATE INDEX IF NOT EXISTS idx_bitacora_fecha_hora ON bitacora(fecha_hora);

CREATE TABLE IF NOT EXISTS proveedor (
    id SERIAL PRIMARY KEY,
    razon_social VARCHAR(150) NOT NULL,
    contacto VARCHAR(100),
    telefono VARCHAR(30),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS categoria (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS producto (
    id SERIAL PRIMARY KEY,
    codigo_barras VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    categoria_id INT NOT NULL REFERENCES categoria(id),
    proveedor_id INT NOT NULL REFERENCES proveedor(id),
    precio_venta NUMERIC(10,2) NOT NULL CHECK (precio_venta > 0),
    requiere_receta BOOLEAN NOT NULL DEFAULT FALSE,
    stock_minimo INT NOT NULL DEFAULT 5 CHECK (stock_minimo >= 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_producto_nombre ON producto(nombre);
CREATE INDEX IF NOT EXISTS idx_producto_codigo_barras ON producto(codigo_barras);
```

### `sql/02_datos.sql`

```sql
INSERT INTO usuario (nombre_usuario, nombre_completo, password_hash, rol, activo) VALUES
('admin', 'Carlos Ramirez (Encargado)', '$2a$10$fm2dGYB.bd80nsqMQ00waePHAiyCiZdOj8RlXKOVT3uzR7N/SX5/O', 'ENCARGADO', TRUE),
('empleado1', 'Laura Gomez (Empleado)', '$2a$10$0UvIkCceCpYYbajeb42PVO8NyJznYD/lw1PpgcMOlJhdmpISMnjym', 'EMPLEADO', TRUE)
ON CONFLICT (nombre_usuario) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    rol = EXCLUDED.rol,
    activo = EXCLUDED.activo;

INSERT INTO proveedor (razon_social, contacto, telefono, activo)
SELECT v.razon_social, v.contacto, v.telefono, TRUE
FROM (VALUES
    ('Distribuidora Farmacéutica Nacional S.A.', 'Roberto Mendez', '555-0101'),
    ('Laboratorios Médicos del Centro', 'Patricia Valenzuela', '555-0102'),
    ('Suministros Hospitalarios y Genéricos', 'Esteban Silva', '555-0103')
) AS v(razon_social, contacto, telefono)
WHERE NOT EXISTS (
    SELECT 1 FROM proveedor p WHERE p.razon_social = v.razon_social
);

INSERT INTO categoria (nombre) VALUES
('Analgésicos y Antiinflamatorios'),
('Antibióticos y Antivirales'),
('Salud Digestiva'),
('Cardiovascular y Presión'),
('Cuidado Personal y Primeros Auxilios')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO producto (codigo_barras, nombre, categoria_id, proveedor_id, precio_venta, requiere_receta, stock_minimo, activo)
SELECT v.codigo_barras, v.nombre, c.id, pr.id, v.precio_venta, v.requiere_receta, v.stock_minimo, TRUE
FROM (VALUES
    ('75010001', 'Paracetamol 500mg (20 tabletas)', 'Analgésicos y Antiinflamatorios', 'Distribuidora Farmacéutica Nacional S.A.', 35.50, FALSE, 10),
    ('75010002', 'Ibuprofeno 400mg (10 cápsulas)', 'Analgésicos y Antiinflamatorios', 'Distribuidora Farmacéutica Nacional S.A.', 48.00, FALSE, 8),
    ('75010003', 'Naproxeno 250mg (20 tabletas)', 'Analgésicos y Antiinflamatorios', 'Laboratorios Médicos del Centro', 62.00, FALSE, 6),
    ('75010004', 'Ketorolaco 10mg (10 tabletas sublinguales)', 'Analgésicos y Antiinflamatorios', 'Laboratorios Médicos del Centro', 75.00, TRUE, 5),
    ('75010005', 'Diclofenaco 100mg Liberación Prolongada', 'Analgésicos y Antiinflamatorios', 'Distribuidora Farmacéutica Nacional S.A.', 85.00, FALSE, 6),
    ('75010006', 'Aspirina Protect 100mg (28 tabletas)', 'Analgésicos y Antiinflamatorios', 'Suministros Hospitalarios y Genéricos', 95.00, FALSE, 12),
    ('75010007', 'Celecoxib 200mg (10 cápsulas)', 'Analgésicos y Antiinflamatorios', 'Laboratorios Médicos del Centro', 140.00, TRUE, 4),
    ('75010008', 'Amoxicilina 500mg (12 cápsulas)', 'Antibióticos y Antivirales', 'Distribuidora Farmacéutica Nacional S.A.', 85.00, TRUE, 8),
    ('75010009', 'Azitromicina 500mg (3 tabletas)', 'Antibióticos y Antivirales', 'Distribuidora Farmacéutica Nacional S.A.', 120.00, TRUE, 5),
    ('75010010', 'Ciprofloxacino 500mg (14 tabletas)', 'Antibióticos y Antivirales', 'Laboratorios Médicos del Centro', 110.00, TRUE, 6),
    ('75010011', 'Cefalexina 500mg (20 cápsulas)', 'Antibióticos y Antivirales', 'Laboratorios Médicos del Centro', 98.00, TRUE, 6),
    ('75010012', 'Aciclovir 400mg (35 tabletas)', 'Antibióticos y Antivirales', 'Suministros Hospitalarios y Genéricos', 160.00, TRUE, 4),
    ('75010013', 'Trimetoprima y Sulfametoxazol 80/400mg', 'Antibióticos y Antivirales', 'Suministros Hospitalarios y Genéricos', 65.00, TRUE, 5),
    ('75010014', 'Omeprazol 20mg (14 cápsulas)', 'Salud Digestiva', 'Distribuidora Farmacéutica Nacional S.A.', 42.00, FALSE, 15),
    ('75010015', 'Pantoprazol 40mg (7 tabletas)', 'Salud Digestiva', 'Laboratorios Médicos del Centro', 115.00, FALSE, 8),
    ('75010016', 'Ranitidina Generica 150mg', 'Salud Digestiva', 'Distribuidora Farmacéutica Nacional S.A.', 30.00, FALSE, 5),
    ('75010017', 'Subsalicilato de Bismuto 262mg Suspensión', 'Salud Digestiva', 'Suministros Hospitalarios y Genéricos', 58.00, FALSE, 7),
    ('75010018', 'Loperamida 2mg (12 tabletas)', 'Salud Digestiva', 'Distribuidora Farmacéutica Nacional S.A.', 25.00, FALSE, 10),
    ('75010019', 'Metoclopramida 10mg (20 tabletas)', 'Salud Digestiva', 'Laboratorios Médicos del Centro', 38.00, FALSE, 6),
    ('75010020', 'Losartán 50mg (30 grageas)', 'Cardiovascular y Presión', 'Distribuidora Farmacéutica Nacional S.A.', 75.00, TRUE, 12),
    ('75010021', 'Enalapril 10mg (30 tabletas)', 'Cardiovascular y Presión', 'Laboratorios Médicos del Centro', 50.00, TRUE, 10),
    ('75010022', 'Amlodipino 5mg (30 tabletas)', 'Cardiovascular y Presión', 'Distribuidora Farmacéutica Nacional S.A.', 68.00, TRUE, 8),
    ('75010023', 'Metoprolol 100mg (20 tabletas)', 'Cardiovascular y Presión', 'Laboratorios Médicos del Centro', 82.00, TRUE, 6),
    ('75010024', 'Atorvastatina 20mg (30 tabletas)', 'Cardiovascular y Presión', 'Suministros Hospitalarios y Genéricos', 130.00, TRUE, 10),
    ('75010025', 'Hidroclorotiazida 25mg (30 tabletas)', 'Cardiovascular y Presión', 'Distribuidora Farmacéutica Nacional S.A.', 45.00, TRUE, 7),
    ('75010026', 'Alcohol Etílico 70% Desnaturalizado 500ml', 'Cuidado Personal y Primeros Auxilios', 'Suministros Hospitalarios y Genéricos', 32.00, FALSE, 15),
    ('75010027', 'Gasas Esterilizadas 10x10cm (10 piezas)', 'Cuidado Personal y Primeros Auxilios', 'Suministros Hospitalarios y Genéricos', 22.00, FALSE, 20),
    ('75010028', 'Venda Elástica 10cm x 5m', 'Cuidado Personal y Primeros Auxilios', 'Suministros Hospitalarios y Genéricos', 28.00, FALSE, 10),
    ('75010029', 'Solución Antiséptica Iodopovidona 120ml', 'Cuidado Personal y Primeros Auxilios', 'Suministros Hospitalarios y Genéricos', 49.00, FALSE, 8),
    ('75010030', 'Termómetro Clínico Digital Infrarrojo', 'Cuidado Personal y Primeros Auxilios', 'Laboratorios Médicos del Centro', 195.00, FALSE, 4)
) AS v(codigo_barras, nombre, categoria, proveedor, precio_venta, requiere_receta, stock_minimo)
JOIN categoria c ON c.nombre = v.categoria
JOIN proveedor pr ON pr.razon_social = v.proveedor
ON CONFLICT (codigo_barras) DO NOTHING;
```

### `sql/03_inventario.sql`

```sql
CREATE TABLE IF NOT EXISTS lote (
    id SERIAL PRIMARY KEY,
    producto_id INT NOT NULL REFERENCES producto(id),
    numero_lote VARCHAR(50) NOT NULL,
    fecha_caducidad DATE NOT NULL,
    cantidad_recibida INT NOT NULL CHECK (cantidad_recibida > 0),
    cantidad_actual INT NOT NULL CHECK (cantidad_actual >= 0),
    precio_compra NUMERIC(10,2) NOT NULL CHECK (precio_compra > 0),
    proveedor_id INT NOT NULL REFERENCES proveedor(id),
    fecha_recepcion DATE NOT NULL DEFAULT CURRENT_DATE,
    referencia_factura VARCHAR(100),
    CONSTRAINT uk_producto_lote UNIQUE (producto_id, numero_lote)
);

CREATE TABLE IF NOT EXISTS movimiento_inventario (
    id SERIAL PRIMARY KEY,
    lote_id INT NOT NULL REFERENCES lote(id),
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('RECEPCION', 'VENTA', 'AJUSTE', 'DEVOLUCION', 'CANCELACION')),
    cantidad INT NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    referencia_id VARCHAR(100),
    usuario_id INT NOT NULL REFERENCES usuario(id),
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lote_fecha_caducidad ON lote(fecha_caducidad);
CREATE INDEX IF NOT EXISTS idx_lote_producto_id ON lote(producto_id);
```

### `sql/04_ventas.sql`

```sql
CREATE TABLE IF NOT EXISTS turno_caja (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL REFERENCES usuario(id),
    fecha_apertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto_inicial NUMERIC(10,2) NOT NULL CHECK (monto_inicial >= 0),
    fecha_cierre TIMESTAMP,
    efectivo_contado NUMERIC(10,2),
    monto_esperado NUMERIC(10,2),
    diferencia NUMERIC(10,2),
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'CERRADO'))
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_unico_turno_abierto
ON turno_caja (estado)
WHERE estado = 'ABIERTO';

CREATE TABLE IF NOT EXISTS venta (
    id SERIAL PRIMARY KEY,
    turno_id INT NOT NULL REFERENCES turno_caja(id),
    usuario_id INT NOT NULL REFERENCES usuario(id),
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total NUMERIC(10,2) NOT NULL CHECK (total > 0),
    forma_pago VARCHAR(20) NOT NULL CHECK (forma_pago IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    estado VARCHAR(20) NOT NULL DEFAULT 'COMPLETADA' CHECK (estado IN ('COMPLETADA', 'CANCELADA')),
    motivo_cancelacion VARCHAR(255),
    cancelada_en TIMESTAMP
);

CREATE TABLE IF NOT EXISTS detalle_venta (
    id SERIAL PRIMARY KEY,
    venta_id INT NOT NULL REFERENCES venta(id) ON DELETE CASCADE,
    producto_id INT NOT NULL REFERENCES producto(id),
    lote_id INT NOT NULL REFERENCES lote(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10,2) NOT NULL CHECK (precio_unitario > 0),
    subtotal NUMERIC(10,2) NOT NULL CHECK (subtotal > 0)
);

CREATE TABLE IF NOT EXISTS receta_venta (
    id SERIAL PRIMARY KEY,
    venta_id INT NOT NULL REFERENCES venta(id),
    producto_id INT NOT NULL REFERENCES producto(id),
    nombre_medico VARCHAR(150) NOT NULL,
    cedula_profesional VARCHAR(50) NOT NULL,
    fecha_expedicion DATE NOT NULL,
    folio_receta VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_venta_turno_id ON venta(turno_id);
CREATE INDEX IF NOT EXISTS idx_detalle_venta_venta_id ON detalle_venta(venta_id);
CREATE INDEX IF NOT EXISTS idx_receta_venta_venta_id ON receta_venta(venta_id);
```

### `sql/05_migracion_auditoria.sql`

```sql
CREATE TABLE IF NOT EXISTS bitacora (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuario(id),
    accion VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_bitacora_usuario_id ON bitacora(usuario_id);
CREATE INDEX IF NOT EXISTS idx_bitacora_fecha_hora ON bitacora(fecha_hora);

ALTER TABLE movimiento_inventario
    DROP CONSTRAINT IF EXISTS movimiento_inventario_tipo_check;

ALTER TABLE movimiento_inventario
    ADD CONSTRAINT movimiento_inventario_tipo_check
    CHECK (tipo IN ('RECEPCION', 'VENTA', 'AJUSTE', 'DEVOLUCION', 'CANCELACION'));
```

---

## Resumen del ciclo de auditoria

Durante la auditoria del proyecto se aplicaron las correcciones correspondientes, entre las que se encuentran: la persistencia de recetas en ventas con transaccion ACID, el control de inventario sin lotes vencidos mediante FEFO, la auditoria con tabla `bitacora`, la migracion que incorpora el tipo de movimiento `CANCELACION`, el respaldo con `pg_dump` y las consultas de historial de ventas y turnos.

---

## Autores

Proyecto desarrollado para la materia **Programacion Orientada a Objetos** — Ingenieria en Sistemas.
