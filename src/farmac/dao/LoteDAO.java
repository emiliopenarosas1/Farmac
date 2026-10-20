package farmac.dao;

import farmac.config.ConexionBD;
import farmac.dominio.Lote;
import farmac.dominio.Producto;
import farmac.dominio.Proveedor;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para Lotes y control FEFO (RF07, RF08, RF09, RF10, RF19).
 */
public class LoteDAO {

    private final String SQL_SELECT_BASE =
            "SELECT l.id, l.numero_lote, l.fecha_caducidad, l.cantidad_recibida, l.cantidad_actual, "
          + "l.precio_compra, l.fecha_recepcion, l.referencia_factura, "
          + "p.id AS prod_id, p.codigo_barras, p.nombre AS prod_nombre, c.nombre AS categoria, "
          + "p.precio_venta, p.requiere_receta, p.stock_minimo, p.activo AS prod_activo, "
          + "pr.id AS prov_id, pr.razon_social AS prov_nombre, pr.contacto AS prov_contacto, "
          + "pr.telefono AS prov_telefono, pr.activo AS prov_activo "
          + "FROM lote l "
          + "JOIN producto p ON l.producto_id = p.id "
          + "JOIN categoria c ON p.categoria_id = c.id "
          + "JOIN proveedor pr ON l.proveedor_id = pr.id ";

    public List<Lote> listarTodos() throws SQLException {
        List<Lote> lista = new ArrayList<>();
        String sql = SQL_SELECT_BASE + "ORDER BY l.fecha_caducidad ASC";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapearLote(rs));
            }
        }
        return lista;
    }

    public List<Lote> listarPorProducto(int productoId, boolean soloConStock) throws SQLException {
        List<Lote> lista = new ArrayList<>();
        String sql = SQL_SELECT_BASE + "WHERE l.producto_id = ? "
                   + (soloConStock ? "AND l.cantidad_actual > 0 " : "")
                   + "ORDER BY l.fecha_caducidad ASC";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearLote(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Consulta FEFO: lotes vigentes (no vencidos), con existencias > 0,
     * ordenados estrictamente por fecha de caducidad ascendente (RF08, RF13).
     */
    public List<Lote> listarLotesFEFO(int productoId) throws SQLException {
        List<Lote> lista = new ArrayList<>();
        String sql = SQL_SELECT_BASE + "WHERE l.producto_id = ? "
                   + "AND l.cantidad_actual > 0 "
                   + "AND l.fecha_caducidad >= CURRENT_DATE "
                   + "ORDER BY l.fecha_caducidad ASC";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearLote(rs));
                }
            }
        }
        return lista;
    }

    public Lote buscarPorId(int id) throws SQLException {
        String sql = SQL_SELECT_BASE + "WHERE l.id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearLote(rs);
                }
            }
        }
        return null;
    }

    public List<Lote> listarLotesEnRango(LocalDate desde, LocalDate hasta) throws SQLException {
        List<Lote> lista = new ArrayList<>();
        String sql = SQL_SELECT_BASE + "WHERE l.fecha_caducidad BETWEEN ? AND ? "
                   + "ORDER BY l.fecha_caducidad ASC";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearLote(rs));
                }
            }
        }
        return lista;
    }

    public Lote insertar(Lote lote) throws SQLException {
        String sql = "INSERT INTO lote (producto_id, numero_lote, fecha_caducidad, cantidad_recibida, "
                   + "cantidad_actual, precio_compra, proveedor_id, fecha_recepcion, referencia_factura) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, lote.getProducto().getId());
            ps.setString(2, lote.getNumeroLote().trim());
            ps.setDate(3, Date.valueOf(lote.getFechaCaducidad()));
            ps.setInt(4, lote.getCantidadRecibida() > 0 ? lote.getCantidadRecibida() : lote.getCantidadActual());
            ps.setInt(5, lote.getCantidadActual());
            ps.setDouble(6, lote.getPrecioCompra());
            ps.setInt(7, lote.getProveedor() != null ? lote.getProveedor().getId() : 1);
            ps.setDate(8, Date.valueOf(lote.getFechaRecepcion() != null ? lote.getFechaRecepcion() : LocalDate.now()));
            ps.setString(9, lote.getReferenciaFactura() != null ? lote.getReferenciaFactura().trim() : null);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    lote.setId(rs.getInt(1));
                }
            }
        }
        return lote;
    }

    public boolean actualizarStock(int loteId, int nuevoStock, Connection connExterna) throws SQLException {
        String sql = "UPDATE lote SET cantidad_actual = ? WHERE id = ?";
        boolean cerrarConexion = false;
        Connection conn = connExterna;
        if (conn == null) {
            conn = ConexionBD.obtenerConexion();
            cerrarConexion = true;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setInt(2, loteId);
            return ps.executeUpdate() > 0;
        } finally {
            if (cerrarConexion && conn != null) {
                conn.close();
            }
        }
    }

    private Lote mapearLote(ResultSet rs) throws SQLException {
        Proveedor prov = new Proveedor(
                rs.getInt("prov_id"),
                rs.getString("prov_nombre"),
                rs.getString("prov_contacto"),
                rs.getString("prov_telefono"),
                rs.getBoolean("prov_activo")
        );

        Producto prod = new Producto(
                rs.getInt("prod_id"),
                rs.getString("codigo_barras"),
                rs.getString("prod_nombre"),
                rs.getString("categoria"),
                prov,
                rs.getDouble("precio_venta"),
                rs.getBoolean("requiere_receta"),
                rs.getInt("stock_minimo"),
                rs.getBoolean("prod_activo")
        );

        return new Lote(
                rs.getInt("id"),
                rs.getString("numero_lote"),
                rs.getDate("fecha_caducidad").toLocalDate(),
                rs.getInt("cantidad_recibida"),
                rs.getInt("cantidad_actual"),
                rs.getDouble("precio_compra"),
                prod,
                prov,
                rs.getDate("fecha_recepcion") != null ? rs.getDate("fecha_recepcion").toLocalDate() : LocalDate.now(),
                rs.getString("referencia_factura")
        );
    }
}
