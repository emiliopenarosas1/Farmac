package farmac.dao;

import farmac.config.ConexionBD;
import farmac.dominio.Producto;
import farmac.dominio.Proveedor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la entidad Producto (RF03, RF04, RF05, RF06).
 */
public class ProductoDAO {

    public List<Producto> listarTodos(boolean soloActivos) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.id, p.codigo_barras, p.nombre, c.nombre AS categoria, "
                   + "pr.id AS prov_id, pr.razon_social AS prov_nombre, pr.contacto AS prov_contacto, "
                   + "pr.telefono AS prov_telefono, pr.activo AS prov_activo, "
                   + "p.precio_venta, p.requiere_receta, p.stock_minimo, p.activo "
                   + "FROM producto p "
                   + "JOIN categoria c ON p.categoria_id = c.id "
                   + "JOIN proveedor pr ON p.proveedor_id = pr.id "
                   + (soloActivos ? "WHERE p.activo = TRUE " : "")
                   + "ORDER BY p.nombre ASC";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }
        }
        return lista;
    }

    public Producto buscarPorId(int id) throws SQLException {
        String sql = "SELECT p.id, p.codigo_barras, p.nombre, c.nombre AS categoria, "
                   + "pr.id AS prov_id, pr.razon_social AS prov_nombre, pr.contacto AS prov_contacto, "
                   + "pr.telefono AS prov_telefono, pr.activo AS prov_activo, "
                   + "p.precio_venta, p.requiere_receta, p.stock_minimo, p.activo "
                   + "FROM producto p "
                   + "JOIN categoria c ON p.categoria_id = c.id "
                   + "JOIN proveedor pr ON p.proveedor_id = pr.id "
                   + "WHERE p.id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearProducto(rs);
                }
            }
        }
        return null;
    }

    public Producto buscarPorCodigoBarras(String codigoBarras) throws SQLException {
        if (codigoBarras == null || codigoBarras.trim().isEmpty()) return null;
        String sql = "SELECT p.id, p.codigo_barras, p.nombre, c.nombre AS categoria, "
                   + "pr.id AS prov_id, pr.razon_social AS prov_nombre, pr.contacto AS prov_contacto, "
                   + "pr.telefono AS prov_telefono, pr.activo AS prov_activo, "
                   + "p.precio_venta, p.requiere_receta, p.stock_minimo, p.activo "
                   + "FROM producto p "
                   + "JOIN categoria c ON p.categoria_id = c.id "
                   + "JOIN proveedor pr ON p.proveedor_id = pr.id "
                   + "WHERE LOWER(p.codigo_barras) = LOWER(?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoBarras.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearProducto(rs);
                }
            }
        }
        return null;
    }

    public List<Producto> buscarPorFiltro(String texto, String categoria, boolean soloActivos) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id, p.codigo_barras, p.nombre, c.nombre AS categoria, ")
           .append("pr.id AS prov_id, pr.razon_social AS prov_nombre, pr.contacto AS prov_contacto, ")
           .append("pr.telefono AS prov_telefono, pr.activo AS prov_activo, ")
           .append("p.precio_venta, p.requiere_receta, p.stock_minimo, p.activo ")
           .append("FROM producto p ")
           .append("JOIN categoria c ON p.categoria_id = c.id ")
           .append("JOIN proveedor pr ON p.proveedor_id = pr.id ")
           .append("WHERE 1=1 ");

        if (soloActivos) {
            sql.append("AND p.activo = TRUE ");
        }
        if (categoria != null && !categoria.trim().isEmpty() && !categoria.equalsIgnoreCase("TODAS")) {
            sql.append("AND LOWER(c.nombre) = LOWER(?) ");
        }
        if (texto != null && !texto.trim().isEmpty()) {
            sql.append("AND (LOWER(p.nombre) LIKE ? OR LOWER(p.codigo_barras) LIKE ?) ");
        }
        sql.append("ORDER BY p.nombre ASC");

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (categoria != null && !categoria.trim().isEmpty() && !categoria.equalsIgnoreCase("TODAS")) {
                ps.setString(paramIdx++, categoria.trim());
            }
            if (texto != null && !texto.trim().isEmpty()) {
                String term = "%" + texto.trim().toLowerCase() + "%";
                ps.setString(paramIdx++, term);
                ps.setString(paramIdx++, term);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearProducto(rs));
                }
            }
        }
        return lista;
    }

    public List<String> listarCategorias() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM categoria ORDER BY nombre ASC";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }
        }
        return lista;
    }

    public boolean existeCodigoBarras(String codigo, int idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM producto WHERE LOWER(codigo_barras) = LOWER(?) AND id <> ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo.trim());
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public Producto insertar(Producto p) throws SQLException {
        int catId = obtenerOCrearCategoria(p.getCategoria());
        int provId = p.getProveedorObj() != null ? p.getProveedorObj().getId() : obtenerIdProveedorPorNombre(p.getProveedor());

        String sql = "INSERT INTO producto (codigo_barras, nombre, categoria_id, proveedor_id, precio_venta, requiere_receta, stock_minimo, activo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getCodigoBarras().trim());
            ps.setString(2, p.getNombre().trim());
            ps.setInt(3, catId);
            ps.setInt(4, provId);
            ps.setDouble(5, p.getPrecioVenta());
            ps.setBoolean(6, p.isRequiereReceta());
            ps.setInt(7, p.getStockMinimo());
            ps.setBoolean(8, p.isActivo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
        }
        return p;
    }

    public boolean actualizar(Producto p) throws SQLException {
        int catId = obtenerOCrearCategoria(p.getCategoria());
        int provId = p.getProveedorObj() != null ? p.getProveedorObj().getId() : obtenerIdProveedorPorNombre(p.getProveedor());

        String sql = "UPDATE producto SET codigo_barras = ?, nombre = ?, categoria_id = ?, proveedor_id = ?, "
                   + "precio_venta = ?, requiere_receta = ?, stock_minimo = ?, activo = ? WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getCodigoBarras().trim());
            ps.setString(2, p.getNombre().trim());
            ps.setInt(3, catId);
            ps.setInt(4, provId);
            ps.setDouble(5, p.getPrecioVenta());
            ps.setBoolean(6, p.isRequiereReceta());
            ps.setInt(7, p.getStockMinimo());
            ps.setBoolean(8, p.isActivo());
            ps.setInt(9, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean darDeBajaLogica(int id) throws SQLException {
        String sql = "UPDATE producto SET activo = FALSE WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private int obtenerOCrearCategoria(String nombreCat) throws SQLException {
        if (nombreCat == null || nombreCat.trim().isEmpty()) {
            nombreCat = "General";
        }
        nombreCat = nombreCat.trim();
        String sqlSelect = "SELECT id FROM categoria WHERE LOWER(nombre) = LOWER(?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setString(1, nombreCat);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String sqlInsert = "INSERT INTO categoria (nombre) VALUES (?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombreCat);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo registrar la categoría '" + nombreCat + "'.");
    }

    private int obtenerIdProveedorPorNombre(String razonSocial) throws SQLException {
        if (razonSocial == null || razonSocial.trim().isEmpty()) {
            throw new SQLException("Debe seleccionar un proveedor válido para el producto.");
        }
        String sql = "SELECT id FROM proveedor WHERE LOWER(razon_social) = LOWER(?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, razonSocial.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("El proveedor '" + razonSocial.trim() + "' no existe.");
    }

    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Proveedor prov = new Proveedor(
                rs.getInt("prov_id"),
                rs.getString("prov_nombre"),
                rs.getString("prov_contacto"),
                rs.getString("prov_telefono"),
                rs.getBoolean("prov_activo")
        );

        return new Producto(
                rs.getInt("id"),
                rs.getString("codigo_barras"),
                rs.getString("nombre"),
                rs.getString("categoria"),
                prov,
                rs.getDouble("precio_venta"),
                rs.getBoolean("requiere_receta"),
                rs.getInt("stock_minimo"),
                rs.getBoolean("activo")
        );
    }
}
