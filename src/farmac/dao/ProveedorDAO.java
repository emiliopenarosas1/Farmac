package farmac.dao;

import farmac.config.ConexionBD;
import farmac.dominio.Proveedor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la entidad Proveedor (RF20).
 */
public class ProveedorDAO {

    public List<Proveedor> listarTodos(boolean soloActivos) throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT id, razon_social, contacto, telefono, activo FROM proveedor "
                   + (soloActivos ? "WHERE activo = TRUE " : "")
                   + "ORDER BY razon_social ASC";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Proveedor(
                        rs.getInt("id"),
                        rs.getString("razon_social"),
                        rs.getString("contacto"),
                        rs.getString("telefono"),
                        rs.getBoolean("activo")
                ));
            }
        }
        return lista;
    }

    public Proveedor buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, razon_social, contacto, telefono, activo FROM proveedor WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Proveedor(
                            rs.getInt("id"),
                            rs.getString("razon_social"),
                            rs.getString("contacto"),
                            rs.getString("telefono"),
                            rs.getBoolean("activo")
                    );
                }
            }
        }
        return null;
    }

    public Proveedor insertar(Proveedor p) throws SQLException {
        String sql = "INSERT INTO proveedor (razon_social, contacto, telefono, activo) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getRazonSocial().trim());
            ps.setString(2, p.getContacto() != null ? p.getContacto().trim() : null);
            ps.setString(3, p.getTelefono() != null ? p.getTelefono().trim() : null);
            ps.setBoolean(4, p.isActivo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
        }
        return p;
    }

    public boolean actualizar(Proveedor p) throws SQLException {
        String sql = "UPDATE proveedor SET razon_social = ?, contacto = ?, telefono = ?, activo = ? WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getRazonSocial().trim());
            ps.setString(2, p.getContacto() != null ? p.getContacto().trim() : null);
            ps.setString(3, p.getTelefono() != null ? p.getTelefono().trim() : null);
            ps.setBoolean(4, p.isActivo());
            ps.setInt(5, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int id, boolean nuevoEstado) throws SQLException {
        String sql = "UPDATE proveedor SET activo = ? WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, nuevoEstado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }
}
