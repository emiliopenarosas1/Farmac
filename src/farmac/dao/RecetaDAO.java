package farmac.dao;

import farmac.config.ConexionBD;
import farmac.dominio.Producto;
import farmac.dominio.RecetaMedica;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para recetas médicas asociadas a ventas (RF14, RNF09).
 */
public class RecetaDAO {

    public RecetaMedica insertar(RecetaMedica receta, int ventaId, Connection connExterna) throws SQLException {
        String sql = "INSERT INTO receta_venta (venta_id, producto_id, nombre_medico, cedula_profesional, fecha_expedicion, folio_receta) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        boolean cerrar = false;
        Connection conn = connExterna;
        if (conn == null) {
            conn = ConexionBD.obtenerConexion();
            cerrar = true;
        }

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ventaId);
            ps.setInt(2, receta.getProducto() != null ? receta.getProducto().getId() : 1);
            ps.setString(3, receta.getNombreMedico().trim());
            ps.setString(4, receta.getCedulaProfesional().trim());
            ps.setDate(5, Date.valueOf(receta.getFechaExpedicion()));
            ps.setString(6, receta.getFolioReceta().trim());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    receta.setId(rs.getInt(1));
                }
            }
        } finally {
            if (cerrar && conn != null) {
                conn.close();
            }
        }
        return receta;
    }

    public List<RecetaMedica> listarPorVenta(int ventaId) throws SQLException {
        List<RecetaMedica> lista = new ArrayList<>();
        String sql = "SELECT rv.id, rv.nombre_medico, rv.cedula_profesional, rv.fecha_expedicion, rv.folio_receta, "
                   + "p.id AS prod_id, p.nombre AS prod_nombre, p.codigo_barras "
                   + "FROM receta_venta rv "
                   + "JOIN producto p ON rv.producto_id = p.id "
                   + "WHERE rv.venta_id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto();
                    p.setId(rs.getInt("prod_id"));
                    p.setNombre(rs.getString("prod_nombre"));
                    p.setCodigoBarras(rs.getString("codigo_barras"));

                    lista.add(new RecetaMedica(
                            rs.getInt("id"),
                            p,
                            rs.getString("nombre_medico"),
                            rs.getString("cedula_profesional"),
                            rs.getDate("fecha_expedicion").toLocalDate(),
                            rs.getString("folio_receta")
                    ));
                }
            }
        }
        return lista;
    }
}
