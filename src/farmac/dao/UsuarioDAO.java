package farmac.dao;

import farmac.config.ConexionBD;
import farmac.dominio.Empleado;
import farmac.dominio.Encargado;
import farmac.dominio.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Acceso a datos para la entidad Usuario (A2, RF01, RF02).
 */
public class UsuarioDAO {

    public static Usuario buscarPorLogin(String login) throws SQLException {
        return new UsuarioDAO().buscarPorNombreUsuario(login);
    }

    public Usuario buscarPorNombreUsuario(String nombreUsuario) throws SQLException {
        String sql = "SELECT id, nombre_usuario, nombre_completo, password_hash, rol, activo "
                   + "FROM usuario WHERE LOWER(nombre_usuario) = LOWER(?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    public Usuario buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre_usuario, nombre_completo, password_hash, rol, activo "
                   + "FROM usuario WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id, nombre_usuario, nombre_completo, password_hash, rol, activo "
                   + "FROM usuario ORDER BY id ASC";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        }
        return lista;
    }

    public boolean existeLogin(String login, int idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE LOWER(nombre_usuario) = LOWER(?) AND id <> ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login.trim());
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public int contarEncargadosActivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE rol = 'ENCARGADO' AND activo = TRUE";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public Usuario insertar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (nombre_usuario, nombre_completo, password_hash, rol, activo) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombreUsuario().trim());
            ps.setString(2, u.getNombreCompleto().trim());

            // Garantizar hash BCrypt
            String passHash = u.getPassword();
            if (passHash == null || (!passHash.startsWith("$2a$") && !passHash.startsWith("$2b$") && !passHash.startsWith("$2y$"))) {
                passHash = BCrypt.hashpw(passHash != null ? passHash : "123456", BCrypt.gensalt());
            }
            ps.setString(3, passHash);
            ps.setString(4, u.obtenerRol());
            ps.setBoolean(5, u.isActivo());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setId(rs.getInt(1));
                    u.setPassword(passHash);
                }
            }
        }
        return u;
    }

    public boolean actualizar(Usuario u, boolean actualizarPassword) throws SQLException {
        String sql;
        if (actualizarPassword) {
            sql = "UPDATE usuario SET nombre_usuario = ?, nombre_completo = ?, password_hash = ?, rol = ?, activo = ? "
                + "WHERE id = ?";
        } else {
            sql = "UPDATE usuario SET nombre_usuario = ?, nombre_completo = ?, rol = ?, activo = ? "
                + "WHERE id = ?";
        }
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNombreUsuario().trim());
            ps.setString(2, u.getNombreCompleto().trim());
            if (actualizarPassword) {
                String passHash = u.getPassword();
                if (passHash == null || (!passHash.startsWith("$2a$") && !passHash.startsWith("$2b$") && !passHash.startsWith("$2y$"))) {
                    passHash = BCrypt.hashpw(passHash != null ? passHash : "123456", BCrypt.gensalt());
                }
                ps.setString(3, passHash);
                ps.setString(4, u.obtenerRol());
                ps.setBoolean(5, u.isActivo());
                ps.setInt(6, u.getId());
            } else {
                ps.setString(3, u.obtenerRol());
                ps.setBoolean(4, u.isActivo());
                ps.setInt(5, u.getId());
            }
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int id, boolean nuevoEstado) throws SQLException {
        String sql = "UPDATE usuario SET activo = ? WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, nuevoEstado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String login = rs.getString("nombre_usuario");
        String nombre = rs.getString("nombre_completo");
        String hash = rs.getString("password_hash");
        String rol = rs.getString("rol");
        boolean activo = rs.getBoolean("activo");

        if ("ENCARGADO".equalsIgnoreCase(rol)) {
            return new Encargado(id, login, nombre, hash, activo);
        } else {
            return new Empleado(id, login, nombre, hash, activo);
        }
    }
}
