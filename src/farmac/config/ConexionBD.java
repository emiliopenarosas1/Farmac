package farmac.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestor de conexión a PostgreSQL.
 */
public class ConexionBD {
    private static String url;
    private static String usuario;
    private static String password;

    static {
        try (InputStream input = ConexionBD.class.getClassLoader().getResourceAsStream("farmac.properties")) {
            Properties prop = new Properties();
            if (input != null) {
                prop.load(input);
            }
            url = prop.getProperty("db.url", "jdbc:postgresql://localhost:5432/farmac_db");
            usuario = prop.getProperty("db.user", "postgres");
            password = prop.getProperty("db.password", "postgres");
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            System.err.println("Advertencia: No se cargó farmac.properties o driver JDBC: " + e.getMessage());
        }
    }

    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(url, usuario, password);
    }
}
