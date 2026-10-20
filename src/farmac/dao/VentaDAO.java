package farmac.dao;

import farmac.config.ConexionBD;
import farmac.dominio.DetalleVenta;
import farmac.dominio.Lote;
import farmac.dominio.MovimientoInventario;
import farmac.dominio.Producto;
import farmac.dominio.RecetaMedica;
import farmac.dominio.TurnoCaja;
import farmac.dominio.Usuario;
import farmac.dominio.Venta;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos para Ventas con transaccionalidad ACID completa (A4, RF13, RF14, RF15, RF17).
 */
public class VentaDAO {

    public static class RotacionProducto {
        private int productoId;
        private String codigoBarras;
        private String nombreProducto;
        private int unidadesVendidas;
        private double totalImporte;

        public RotacionProducto(int productoId, String codigoBarras, String nombreProducto, int unidadesVendidas, double totalImporte) {
            this.productoId = productoId;
            this.codigoBarras = codigoBarras;
            this.nombreProducto = nombreProducto;
            this.unidadesVendidas = unidadesVendidas;
            this.totalImporte = totalImporte;
        }

        public int getProductoId() { return productoId; }
        public String getCodigoBarras() { return codigoBarras; }
        public String getNombreProducto() { return nombreProducto; }
        public int getUnidadesVendidas() { return unidadesVendidas; }
        public double getTotalImporte() { return totalImporte; }
    }

    /**
     * Registra una venta completa en una sola transacción ACID (A4, RF13, RF14, RNF05).
     * Si falla cualquier deducción de stock o inserción, se ejecuta rollback automático.
     */
    public Venta registrarVentaTransaccional(Venta venta) throws SQLException {
        if (venta == null || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new SQLException("La venta no contiene productos.");
        }
        if (venta.getTurnoCaja() == null || !venta.getTurnoCaja().estaAbierto()) {
            throw new SQLException("No hay un turno de caja abierto para registrar la venta (RF12).");
        }

        Connection conn = null;
        try {
            conn = ConexionBD.obtenerConexion();
            conn.setAutoCommit(false); // INICIO TRANSACCIÓN (A4)

            // 1. Insertar Venta
            String sqlVenta = "INSERT INTO venta (turno_id, usuario_id, fecha_hora, total, forma_pago, estado) "
                            + "VALUES (?, ?, ?, ?, ?, 'COMPLETADA')";
            try (PreparedStatement psVenta = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                psVenta.setInt(1, venta.getTurnoCaja().getId());
                psVenta.setInt(2, venta.getUsuario() != null ? venta.getUsuario().getId() : 1);
                psVenta.setTimestamp(3, Timestamp.valueOf(venta.getFechaHora() != null ? venta.getFechaHora() : LocalDateTime.now()));
                psVenta.setDouble(4, venta.getTotal());
                psVenta.setString(5, venta.getFormaPago() != null ? venta.getFormaPago() : "EFECTIVO");
                psVenta.executeUpdate();

                try (ResultSet rs = psVenta.getGeneratedKeys()) {
                    if (rs.next()) {
                        venta.setId(rs.getInt(1));
                    }
                }
            }

            // 2. Insertar Detalles y Descontar Stock con validación de existencia
            String sqlDetalle = "INSERT INTO detalle_venta (venta_id, producto_id, lote_id, cantidad, precio_unitario, subtotal) "
                              + "VALUES (?, ?, ?, ?, ?, ?)";
            String sqlDescontar = "UPDATE lote SET cantidad_actual = cantidad_actual - ? "
                                + "WHERE id = ? AND cantidad_actual >= ?";
            String sqlMov = "INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora) "
                          + "VALUES (?, 'VENTA', ?, ?, ?, ?, ?)";

            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psDescontar = conn.prepareStatement(sqlDescontar);
                 PreparedStatement psMov = conn.prepareStatement(sqlMov)) {

                for (DetalleVenta d : venta.getDetalles()) {
                    Lote lote = d.getLote();
                    if (lote == null) {
                        throw new SQLException("Detalle sin lote asignado.");
                    }
                    int productoId = (lote.getProducto() != null) ? lote.getProducto().getId() : 1;

                    // Insertar detalle
                    psDetalle.setInt(1, venta.getId());
                    psDetalle.setInt(2, productoId);
                    psDetalle.setInt(3, lote.getId());
                    psDetalle.setInt(4, d.getCantidad());
                    psDetalle.setDouble(5, d.getPrecioUnitario());
                    psDetalle.setDouble(6, d.getSubtotal());
                    psDetalle.executeUpdate();

                    try (ResultSet rsD = psDetalle.getGeneratedKeys()) {
                        if (rsD.next()) {
                            d.setId(rsD.getInt(1));
                        }
                    }

                    // Descontar stock del lote garantizando no-negatividad
                    psDescontar.setInt(1, d.getCantidad());
                    psDescontar.setInt(2, lote.getId());
                    psDescontar.setInt(3, d.getCantidad());
                    int filasAfectadas = psDescontar.executeUpdate();
                    if (filasAfectadas == 0) {
                        throw new SQLException("Existencias insuficientes en el lote " + lote.getNumeroLote()
                                + " para surtir " + d.getCantidad() + " unidades.");
                    }
                    lote.descontarStock(d.getCantidad());

                    // Registrar movimiento de auditoría en inventario
                    psMov.setInt(1, lote.getId());
                    psMov.setInt(2, -d.getCantidad());
                    psMov.setString(3, "Venta mostrador #" + venta.getId());
                    psMov.setString(4, "VENTA-" + venta.getId());
                    psMov.setInt(5, venta.getUsuario() != null ? venta.getUsuario().getId() : 1);
                    psMov.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    psMov.executeUpdate();
                }
            }

            // 3. Guardar Recetas Médicas asociadas (RF14, RNF09)
            if (venta.getRecetas() != null && !venta.getRecetas().isEmpty()) {
                String sqlReceta = "INSERT INTO receta_venta (venta_id, producto_id, nombre_medico, cedula_profesional, fecha_expedicion, folio_receta) "
                                 + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psReceta = conn.prepareStatement(sqlReceta, Statement.RETURN_GENERATED_KEYS)) {
                    for (RecetaMedica r : venta.getRecetas()) {
                        psReceta.setInt(1, venta.getId());
                        psReceta.setInt(2, r.getProducto() != null ? r.getProducto().getId() : 1);
                        psReceta.setString(3, r.getNombreMedico());
                        psReceta.setString(4, r.getCedulaProfesional());
                        psReceta.setDate(5, Date.valueOf(r.getFechaExpedicion()));
                        psReceta.setString(6, r.getFolioReceta());
                        psReceta.executeUpdate();

                        try (ResultSet rsR = psReceta.getGeneratedKeys()) {
                            if (rsR.next()) {
                                r.setId(rsR.getInt(1));
                            }
                        }
                    }
                }
            }

            conn.commit(); // COMMIT EXITOSO (A4)
            return venta;
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback(); // ROLLBACK EN CASO DE ERROR (A4)
                } catch (SQLException rollbackEx) {
                    System.err.println("Error al hacer rollback: " + rollbackEx.getMessage());
                }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // Cierre seguro
                }
            }
        }
    }

    /**
     * Cancela una venta, validando que su turno siga abierto y reintegrando el stock a los lotes (RF15).
     */
    public boolean cancelarVentaTransaccional(int ventaId, String motivo, Usuario usuario) throws SQLException {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new SQLException("Debe especificar un motivo válido de cancelación.");
        }

        Connection conn = null;
        try {
            conn = ConexionBD.obtenerConexion();
            conn.setAutoCommit(false); // Transacción para cancelar

            // 1. Consultar estado y turno de la venta
            String sqlCheck = "SELECT v.estado, v.turno_id, t.estado AS turno_estado "
                            + "FROM venta v "
                            + "JOIN turno_caja t ON v.turno_id = t.id "
                            + "WHERE v.id = ?";
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                psCheck.setInt(1, ventaId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("La venta #" + ventaId + " no existe.");
                    }
                    String estadoVenta = rs.getString("estado");
                    String estadoTurno = rs.getString("turno_estado");

                    if ("CANCELADA".equalsIgnoreCase(estadoVenta)) {
                        throw new SQLException("La venta #" + ventaId + " ya está cancelada.");
                    }
                    if (!"ABIERTO".equalsIgnoreCase(estadoTurno)) {
                        throw new SQLException("No se puede cancelar la venta: el turno de caja #"
                                + rs.getInt("turno_id") + " ya tuvo corte y está CERRADO (RF15).");
                    }
                }
            }

            // 2. Marcar venta como cancelada
            String sqlUpdateVenta = "UPDATE venta SET estado = 'CANCELADA', motivo_cancelacion = ?, cancelada_en = NOW() WHERE id = ?";
            try (PreparedStatement psUp = conn.prepareStatement(sqlUpdateVenta)) {
                psUp.setString(1, motivo.trim());
                psUp.setInt(2, ventaId);
                psUp.executeUpdate();
            }

            // 3. Reintegrar existencias a cada lote de los detalles
            String sqlDetalles = "SELECT lote_id, cantidad FROM detalle_venta WHERE venta_id = ?";
            String sqlReintegrar = "UPDATE lote SET cantidad_actual = cantidad_actual + ? WHERE id = ?";
            String sqlMov = "INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora) "
                          + "VALUES (?, 'CANCELACION', ?, ?, ?, ?, ?)";

            try (PreparedStatement psD = conn.prepareStatement(sqlDetalles);
                 PreparedStatement psReint = conn.prepareStatement(sqlReintegrar);
                 PreparedStatement psMov = conn.prepareStatement(sqlMov)) {

                psD.setInt(1, ventaId);
                try (ResultSet rsD = psD.executeQuery()) {
                    while (rsD.next()) {
                        int loteId = rsD.getInt("lote_id");
                        int cantidad = rsD.getInt("cantidad");

                        // Reintegrar al lote
                        psReint.setInt(1, cantidad);
                        psReint.setInt(2, loteId);
                        psReint.executeUpdate();

                        // Registrar movimiento de auditoría
                        psMov.setInt(1, loteId);
                        psMov.setInt(2, cantidad);
                        psMov.setString(3, "Cancelación de Venta #" + ventaId + " - Motivo: " + motivo);
                        psMov.setString(4, "CANC-" + ventaId);
                        psMov.setInt(5, usuario != null ? usuario.getId() : 1);
                        psMov.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                        psMov.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e) {}
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {}
            }
        }
    }

    public List<Venta> listarVentas(LocalDate desde, LocalDate hasta, String filtroEstado) throws SQLException {
        List<Venta> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT v.id, v.turno_id, v.usuario_id, v.fecha_hora, v.total, v.forma_pago, ")
           .append("v.estado, v.motivo_cancelacion, ")
           .append("u.nombre_usuario, u.nombre_completo, u.rol, ")
           .append("t.estado AS turno_estado, t.monto_inicial ")
           .append("FROM venta v ")
           .append("JOIN usuario u ON v.usuario_id = u.id ")
           .append("JOIN turno_caja t ON v.turno_id = t.id ")
           .append("WHERE 1=1 ");

        if (desde != null) {
            sql.append("AND DATE(v.fecha_hora) >= ? ");
        }
        if (hasta != null) {
            sql.append("AND DATE(v.fecha_hora) <= ? ");
        }
        if (filtroEstado != null && !filtroEstado.equalsIgnoreCase("TODAS")) {
            sql.append("AND v.estado = ? ");
        }
        sql.append("ORDER BY v.id DESC");

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (desde != null) {
                ps.setDate(idx++, Date.valueOf(desde));
            }
            if (hasta != null) {
                ps.setDate(idx++, Date.valueOf(hasta));
            }
            if (filtroEstado != null && !filtroEstado.equalsIgnoreCase("TODAS")) {
                ps.setString(idx++, filtroEstado.toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Venta v = new Venta();
                    v.setId(rs.getInt("id"));
                    v.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
                    v.setTotal(rs.getDouble("total"));
                    v.setFormaPago(rs.getString("forma_pago"));
                    v.setEstado(rs.getString("estado"));
                    v.setMotivoCancelacion(rs.getString("motivo_cancelacion"));

                    TurnoCaja t = new TurnoCaja();
                    t.setId(rs.getInt("turno_id"));
                    t.setEstado(rs.getString("turno_estado"));
                    v.setTurnoCaja(t);

                    Usuario u = new UsuarioDAO().buscarPorId(rs.getInt("usuario_id"));
                    v.setUsuario(u);

                    lista.add(v);
                }
            }
        }
        return lista;
    }

    /**
     * Ranking de rotación de productos (más y menos vendidos) para RF17.
     */
    public List<RotacionProducto> obtenerRankingRotacion(LocalDate desde, LocalDate hasta) throws SQLException {
        List<RotacionProducto> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id, p.codigo_barras, p.nombre, ")
           .append("COALESCE(SUM(dv.cantidad), 0) AS unidades, ")
           .append("COALESCE(SUM(dv.subtotal), 0) AS importe ")
           .append("FROM producto p ")
           .append("LEFT JOIN detalle_venta dv ON p.id = dv.producto_id ")
           .append("LEFT JOIN venta v ON dv.venta_id = v.id AND v.estado = 'COMPLETADA' ");

        if (desde != null || hasta != null) {
            sql.append("AND 1=1 ");
            if (desde != null) sql.append("AND DATE(v.fecha_hora) >= '").append(Date.valueOf(desde)).append("' ");
            if (hasta != null) sql.append("AND DATE(v.fecha_hora) <= '").append(Date.valueOf(hasta)).append("' ");
        }

        sql.append("WHERE p.activo = TRUE ")
           .append("GROUP BY p.id, p.codigo_barras, p.nombre ")
           .append("ORDER BY unidades DESC, importe DESC");

        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                lista.add(new RotacionProducto(
                        rs.getInt("id"),
                        rs.getString("codigo_barras"),
                        rs.getString("nombre"),
                        rs.getInt("unidades"),
                        rs.getDouble("importe")
                ));
            }
        }
        return lista;
    }

    public List<DetalleVenta> listarDetallesPorVenta(int ventaId) throws SQLException {
        List<DetalleVenta> lista = new ArrayList<>();
        String sql = "SELECT dv.id, dv.cantidad, dv.precio_unitario, dv.subtotal, "
                   + "l.id AS lote_id, l.numero_lote, l.fecha_caducidad, "
                   + "p.id AS prod_id, p.nombre AS prod_nombre, p.codigo_barras "
                   + "FROM detalle_venta dv "
                   + "JOIN lote l ON dv.lote_id = l.id "
                   + "JOIN producto p ON dv.producto_id = p.id "
                   + "WHERE dv.venta_id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto();
                    p.setId(rs.getInt("prod_id"));
                    p.setNombre(rs.getString("prod_nombre"));
                    p.setCodigoBarras(rs.getString("codigo_barras"));

                    Lote l = new Lote();
                    l.setId(rs.getInt("lote_id"));
                    l.setNumeroLote(rs.getString("numero_lote"));
                    l.setFechaCaducidad(rs.getDate("fecha_caducidad").toLocalDate());
                    l.setProducto(p);

                    DetalleVenta d = new DetalleVenta(
                            rs.getInt("id"),
                            l,
                            rs.getInt("cantidad"),
                            rs.getDouble("precio_unitario")
                    );
                    lista.add(d);
                }
            }
        }
        return lista;
    }
}
