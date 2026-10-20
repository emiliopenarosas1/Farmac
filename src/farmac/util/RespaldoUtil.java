package farmac.util;

import farmac.config.ConexionBD;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Utilidad de respaldo de base de datos PostgreSQL (A6, RNF05).
 * Genera volcados en la carpeta respaldos/ mediante pg_dump o exportador SQL nativo.
 */
public class RespaldoUtil {

    public static File generarRespaldo() throws Exception {
        File dir = new File("respaldos");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File archivoSalida = new File(dir, "respaldo_farmac_" + timeStamp + ".sql");

        // Intentar primero con pg_dump
        boolean pgDumpExitoso = false;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", "localhost",
                    "-p", "5432",
                    "-U", "postgres",
                    "-F", "p", // Plain text SQL
                    "-f", archivoSalida.getAbsolutePath(),
                    "farmac_db"
            );
            pb.environment().put("PGPASSWORD", "postgres");
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode == 0 && archivoSalida.exists() && archivoSalida.length() > 0) {
                pgDumpExitoso = true;
            }
        } catch (Exception e) {
            // Si pg_dump no está en el PATH, usamos el generador SQL interno
        }

        if (!pgDumpExitoso) {
            generarRespaldoNativo(archivoSalida);
        }

        return archivoSalida;
    }

    private static void generarRespaldoNativo(File archivo) throws Exception {
        try (Connection conn = ConexionBD.obtenerConexion();
             PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {

            pw.println("-- ========================================================");
            pw.println("-- RESPALDO FARMAC - GENERADO: " + new Date());
            pw.println("-- ========================================================");
            pw.println();

            String[] tablas = {
                "categoria", "proveedor", "usuario", "producto", "lote",
                "movimiento_inventario", "turno_caja", "venta", "detalle_venta",
                "receta_venta", "bitacora"
            };

            for (String tabla : tablas) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    int cols = rs.getMetaData().getColumnCount();
                    pw.println("-- Datos de tabla: " + tabla);
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            Object val = rs.getObject(i);
                            if (val == null) {
                                sb.append("NULL");
                            } else if (val instanceof Number || val instanceof Boolean) {
                                sb.append(val);
                            } else {
                                sb.append("'").append(val.toString().replace("'", "''")).append("'");
                            }
                            if (i < cols) sb.append(", ");
                        }
                        sb.append(");");
                        pw.println(sb.toString());
                    }
                    pw.println();
                } catch (Exception ex) {
                    pw.println("-- Error al volcar " + tabla + ": " + ex.getMessage());
                }
            }
        }
    }
}
