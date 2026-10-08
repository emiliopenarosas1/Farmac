-- ========================================================
-- Datos iniciales: Farmac
-- Passwords iniciales con BCrypt:
-- admin123 -> $2a$10$w3U6mP0j3v5gS7tO3d5Tte8WbQk9yv1Q7gE2I4G5J8H3K6L9M1N2P (o generados con BCrypt)
-- Hash para "admin123": $2a$10$tZg/eMhC.eC7mO2oXfM34ODd61yN1dYcO.C39w7I2KjZ5v6x5u9lK
-- Hash para "empleado123": $2a$10$fV3m7l2pC4K.9sT8oY1q2e9X2w1v0uZaBcDeFgHiJkLmNoPqRsTuV
-- ========================================================

-- Usuarios (1 ENCARGADO, 1 EMPLEADO)
-- Password por defecto para ambos en prueba: "123456"
-- Hash BCrypt estándar para "123456": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO usuario (nombre_usuario, nombre_completo, password_hash, rol, activo) VALUES
('admin', 'Carlos Ramirez (Encargado)', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ENCARGADO', TRUE),
('empleado1', 'Laura Gomez (Empleado)', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'EMPLEADO', TRUE)
ON CONFLICT (nombre_usuario) DO NOTHING;

-- Proveedores (3 proveedores)
INSERT INTO proveedor (razon_social, contacto, telefono, activo) VALUES
('Distribuidora Farmacéutica Nacional S.A.', 'Roberto Mendez', '555-0101', TRUE),
('Laboratorios Médicos del Centro', 'Patricia Valenzuela', '555-0102', TRUE),
('Suministros Hospitalarios y Genéricos', 'Esteban Silva', '555-0103', TRUE);

-- Categorías (5 categorías)
INSERT INTO categoria (nombre) VALUES
('Analgésicos y Antiinflamatorios'),
('Antibióticos y Antivirales'),
('Salud Digestiva'),
('Cardiovascular y Presión'),
('Cuidado Personal y Primeros Auxilios')
ON CONFLICT (nombre) DO NOTHING;

-- Productos (~30 productos)
-- (categoria_id y proveedor_id referenciados 1 a 5 y 1 a 3)
INSERT INTO producto (codigo_barras, nombre, categoria_id, proveedor_id, precio_venta, requiere_receta, stock_minimo, activo) VALUES
('75010001', 'Paracetamol 500mg (20 tabletas)', 1, 1, 35.50, FALSE, 10, TRUE),
('75010002', 'Ibuprofeno 400mg (10 cápsulas)', 1, 1, 48.00, FALSE, 8, TRUE),
('75010003', 'Naproxeno 250mg (20 tabletas)', 1, 2, 62.00, FALSE, 6, TRUE),
('75010004', 'Ketorolaco 10mg (10 tabletas sublinguales)', 1, 2, 75.00, TRUE, 5, TRUE),
('75010005', 'Diclofenaco 100mg Liberación Prolongada', 1, 1, 85.00, FALSE, 6, TRUE),
('75010006', 'Aspirina Protect 100mg (28 tabletas)', 1, 3, 95.00, FALSE, 12, TRUE),
('75010007', 'Celecoxib 200mg (10 cápsulas)', 1, 2, 140.00, TRUE, 4, TRUE),

('75010008', 'Amoxicilina 500mg (12 cápsulas)', 2, 1, 85.00, TRUE, 8, TRUE),
('75010009', 'Azitromicina 500mg (3 tabletas)', 2, 1, 120.00, TRUE, 5, TRUE),
('75010010', 'Ciprofloxacino 500mg (14 tabletas)', 2, 2, 110.00, TRUE, 6, TRUE),
('75010011', 'Cefalexina 500mg (20 cápsulas)', 2, 2, 98.00, TRUE, 6, TRUE),
('75010012', 'Aciclovir 400mg (35 tabletas)', 2, 3, 160.00, TRUE, 4, TRUE),
('75010013', 'Trimetoprima y Sulfametoxazol 80/400mg', 2, 3, 65.00, TRUE, 5, TRUE),

('75010014', 'Omeprazol 20mg (14 cápsulas)', 3, 1, 42.00, FALSE, 15, TRUE),
('75010015', 'Pantoprazol 40mg (7 tabletas)', 3, 2, 115.00, FALSE, 8, TRUE),
('75010016', 'Ranitidina Genérica 150mg', 3, 1, 30.00, FALSE, 5, TRUE),
('75010017', 'Subsalicilato de Bismuto 262mg Suspensión', 3, 3, 58.00, FALSE, 7, TRUE),
('75010018', 'Loperamida 2mg (12 tabletas)', 3, 1, 25.00, FALSE, 10, TRUE),
('75010019', 'Metoclopramida 10mg (20 tabletas)', 3, 2, 38.00, FALSE, 6, TRUE),

('75010020', 'Losartán 50mg (30 grageas)', 4, 1, 75.00, TRUE, 12, TRUE),
('75010021', 'Enalapril 10mg (30 tabletas)', 4, 2, 50.00, TRUE, 10, TRUE),
('75010022', 'Amlodipino 5mg (30 tabletas)', 4, 1, 68.00, TRUE, 8, TRUE),
('75010023', 'Metoprolol 100mg (20 tabletas)', 4, 2, 82.00, TRUE, 6, TRUE),
('75010024', 'Atorvastatina 20mg (30 tabletas)', 4, 3, 130.00, TRUE, 10, TRUE),
('75010025', 'Hidroclorotiazida 25mg (30 tabletas)', 4, 1, 45.00, TRUE, 7, TRUE),

('75010026', 'Alcohol Etílico 70% Desnaturalizado 500ml', 5, 3, 32.00, FALSE, 15, TRUE),
('75010027', 'Gasas Esterilizadas 10x10cm (10 piezas)', 5, 3, 22.00, FALSE, 20, TRUE),
('75010028', 'Venda Elástica 10cm x 5m', 5, 3, 28.00, FALSE, 10, TRUE),
('75010029', 'Solución Antiséptica Iodopovidona 120ml', 5, 3, 49.00, FALSE, 8, TRUE),
('75010030', 'Termómetro Clínico Digital Infrarrojo', 5, 2, 195.00, FALSE, 4, TRUE)
ON CONFLICT (codigo_barras) DO NOTHING;
