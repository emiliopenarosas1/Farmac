package farmac.dao;

import farmac.config.ConexionBD;
import farmac.dominio.TurnoCaja;
import farmac.dominio.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para los turnos y cortes de caja (RF12, RF16).
 */
public class TurnoCajaDAO {

    public TurnoCaja obtenerTurnoAbierto() throws SQLException {
        String sql = "SELECT t.id, t.fecha_apertura, t.monto_inicial, t.fecha_cierre, "
                   + "t.efectivo_contado, t.monto_esperado, t.diferencia, t.estado, "
                   + "u.id AS user_id, u.nombre_usuario, u.nombre_completo, u.rol, u.activo "
                   + "FROM turno_caja t "
                   + "JOIN usuario u ON t.usuario_id = u.id "
                   + "WHERE t.estado = 'ABIERTO' LIMIT 1";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return mapearTurno(rs);
            }
        }
        return null;
    }

    public TurnoCaja abrirTurno(TurnoCaja turno) throws SQLException {
        // Verificar que no haya otro turno abierto
        TurnoCaja abierto = obtenerTurnoAbierto();
        if (abierto != null) {
            throw new SQLException("Ya existe un turno de caja abierto (Turno #" + abierto.getId() + "). Debe cerrarlo antes.");
        }

        String sql = "INSERT INTO turno_caja (usuario_id, fecha_apertura, monto_inicial, estado) "
                   + "VALUES (?, ?, ?, 'ABIERTO')";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, turno.getUsuario().getId());
            ps.setTimestamp(2, Timestamp.valueOf(turno.getFechaApertura() != null ? turno.getFechaApertura() : LocalDateTime.now()));
            ps.setDouble(3, turno.getMontoInicial());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    turno.setId(rs.getInt(1));
                    turno.setEstado("ABIERTO");
                }
            }
        }
        return turno;
    }

    public boolean cerrarTurno(TurnoCaja turno) throws SQLException {
        String sql = "UPDATE turno_caja SET fecha_cierre = ?, efectivo_contado = ?, "
                   + "monto_esperado = ?, diferencia = ?, estado = 'CERRADO' "
                   + "WHERE id = ? AND estado = 'ABIERTO'";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(turno.getFechaCierre() != null ? turno.getFechaCierre() : LocalDateTime.now()));
            ps.setDouble(2, turno.getEfectivoContado());
            ps.setDouble(3, turno.getMontoEsperado());
            ps.setDouble(4, turno.getDiferencia());
            ps.setInt(5, turno.getId());
            int filas = ps.executeUpdate();
            if (filas > 0) {
                turno.setEstado("CERRADO");
                return true;
            }
            return false;
        }
    }

    public double obtenerTotalVentasEfectivoTurno(int turnoId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM venta "
                   + "WHERE turno_id = ? AND forma_pago = 'EFECTIVO' AND estado = 'COMPLETADA'";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turnoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public double obtenerTotalVentasTurnoPorFormaPago(int turnoId, String formaPago) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM venta "
                   + "WHERE turno_id = ? AND forma_pago = ? AND estado = 'COMPLETADA'";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turnoId);
            ps.setString(2, formaPago);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public List<TurnoCaja> listarHistorial(int limite) throws SQLException {
        List<TurnoCaja> lista = new ArrayList<>();
        String sql = "SELECT t.id, t.fecha_apertura, t.monto_inicial, t.fecha_cierre, "
                   + "t.efectivo_contado, t.monto_esperado, t.diferencia, t.estado, "
                   + "u.id AS user_id, u.nombre_usuario, u.nombre_completo, u.rol, u.activo "
                   + "FROM turno_caja t "
                   + "JOIN usuario u ON t.usuario_id = u.id "
                   + "ORDER BY t.id DESC LIMIT ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearTurno(rs));
                }
            }
        }
        return lista;
    }

    private TurnoCaja mapearTurno(ResultSet rs) throws SQLException {
        Usuario u = new UsuarioDAO().buscarPorId(rs.getInt("user_id"));
        TurnoCaja t = new TurnoCaja();
        t.setId(rs.getInt("id"));
        t.setUsuario(u);
        t.setFechaApertura(rs.getTimestamp("fecha_apertura").toLocalDateTime());
        t.setMontoInicial(rs.getDouble("monto_inicial"));
        Timestamp cierre = rs.getTimestamp("fecha_cierre");
        if (cierre != null) {
            t.setFechaCierre(cierre.toLocalDateTime());
        }
        t.setEfectivoContado(rs.getDouble("efectivo_contado"));
        t.setMontoEsperado(rs.getDouble("monto_esperado"));
        t.setDiferencia(rs.getDouble("diferencia"));
        t.setEstado(rs.getString("estado"));
        return t;
    }
}
