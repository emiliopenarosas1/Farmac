-- ========================================================
-- Schema: Farmac - Punto de Venta e Inventario
-- ========================================================

-- Tabla: usuario
CREATE TABLE IF NOT EXISTS usuario (
    id SERIAL PRIMARY KEY,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    nombre_completo VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ENCARGADO', 'EMPLEADO')),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabla: bitacora (auditoria del sistema, A7)
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

-- Tabla: proveedor
CREATE TABLE IF NOT EXISTS proveedor (
    id SERIAL PRIMARY KEY,
    razon_social VARCHAR(150) NOT NULL,
    contacto VARCHAR(100),
    telefono VARCHAR(30),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabla: categoria
CREATE TABLE IF NOT EXISTS categoria (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

-- Tabla: producto
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

-- Índices solicitados
CREATE INDEX IF NOT EXISTS idx_producto_nombre ON producto(nombre);
CREATE INDEX IF NOT EXISTS idx_producto_codigo_barras ON producto(codigo_barras);
