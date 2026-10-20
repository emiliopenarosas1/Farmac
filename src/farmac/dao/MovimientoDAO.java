package farmac.dao;

import farmac.config.ConexionBD;
import farmac.dominio.Lote;
import farmac.dominio.MovimientoInventario;
import farmac.dominio.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para los movimientos de inventario (RF08, RF09).
 */
public class MovimientoDAO {

    public MovimientoInventario insertar(MovimientoInventario mov, Connection connExterna) throws SQLException {
        String sql = "INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        boolean cerrar = false;
        Connection conn = connExterna;
        if (conn == null) {
            conn = ConexionBD.obtenerConexion();
            cerrar = true;
        }

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, mov.getLote().getId());
            ps.setString(2, mov.getTipo());
            ps.setInt(3, mov.getCantidad());
            ps.setString(4, mov.getMotivo());
            ps.setString(5, mov.getReferenciaId());
            ps.setInt(6, mov.getUsuario() != null ? mov.getUsuario().getId() : 1);
            ps.setTimestamp(7, Timestamp.valueOf(mov.getFechaHora()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    mov.setId(rs.getInt(1));
                }
            }
        } finally {
            if (cerrar && conn != null) {
                conn.close();
            }
        }
        return mov;
    }

    public List<MovimientoInventario> listarPorLote(int loteId) throws SQLException {
        List<MovimientoInventario> lista = new ArrayList<>();
        String sql = "SELECT id, lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora "
                   + "FROM movimiento_inventario WHERE lote_id = ? ORDER BY fecha_hora DESC";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovimientoInventario m = new MovimientoInventario();
                    m.setId(rs.getInt("id"));
                    m.setTipo(rs.getString("tipo"));
                    m.setCantidad(rs.getInt("cantidad"));
                    m.setMotivo(rs.getString("motivo"));
                    m.setReferenciaId(rs.getString("referencia_id"));
                    m.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
                    lista.add(m);
                }
            }
        }
        return lista;
    }
}
