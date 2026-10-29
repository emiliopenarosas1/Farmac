package farmac.dao;

import farmac.config.ConexionBD;
import farmac.dominio.Bitacora;
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
 * Acceso a datos para la bitácora de auditoría del sistema (A7).
 * Registra login, ajustes, cancelaciones y cortes.
 */
public class BitacoraDAO {

    public static void registrar(Usuario u, String accion, String descripcion) {
        String sql = "INSERT INTO bitacora (usuario_id, accion, descripcion, fecha_hora, ip) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (u != null && u.getId() > 0) {
                ps.setInt(1, u.getId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, accion);
            ps.setString(3, descripcion);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, "127.0.0.1");
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Advertencia al registrar en bitácora: " + e.getMessage());
        }
    }

    public List<Bitacora> listarRecientes(int limite) throws SQLException {
        List<Bitacora> lista = new ArrayList<>();
        String sql = "SELECT b.id, b.usuario_id, b.accion, b.descripcion, b.fecha_hora, b.ip, "
                   + "u.nombre_usuario, u.nombre_completo, u.password_hash, u.rol, u.activo "
                   + "FROM bitacora b "
                   + "LEFT JOIN usuario u ON b.usuario_id = u.id "
                   + "ORDER BY b.id DESC LIMIT ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario u = null;
                    if (rs.getInt("usuario_id") > 0) {
                        u = UsuarioDAO.construirUsuario(
                                rs.getInt("usuario_id"),
                                rs.getString("nombre_usuario"),
                                rs.getString("nombre_completo"),
                                rs.getString("password_hash"),
                                rs.getString("rol"),
                                rs.getBoolean("activo")
                        );
                    }
                    lista.add(new Bitacora(
                            rs.getInt("id"),
                            u,
                            rs.getString("accion"),
                            rs.getString("descripcion"),
                            rs.getTimestamp("fecha_hora").toLocalDateTime(),
                            rs.getString("ip")
                    ));
                }
            }
        }
        return lista;
    }
}
