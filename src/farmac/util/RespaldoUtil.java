package farmac.util;

import farmac.config.ConexionBD;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

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

        // Intentar primero con pg_dump usando la misma configuración de la aplicación
        boolean pgDumpExitoso = false;
        try {
            String[] conexion = parsearUrlJdbc(ConexionBD.getUrl());
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", conexion[0],
                    "-p", conexion[1],
                    "-U", ConexionBD.getUsuario(),
                    "-F", "p", // Plain text SQL
                    "-f", archivoSalida.getAbsolutePath(),
                    conexion[2]
            );
            pb.environment().put("PGPASSWORD", ConexionBD.getPassword());
            // Descartar la salida del proceso: si se dejara sin consumir, un volcado
            // grande llenaría el buffer del pipe y bloquearía pg_dump.
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
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
                    String columnas = "";
                    for (int i = 1; i <= cols; i++) {
                        columnas = i == 1 ? "\"" + rs.getMetaData().getColumnLabel(i) + "\"" : columnas + ", \"" + rs.getMetaData().getColumnLabel(i) + "\"";
                    }
                    pw.println("-- Datos de tabla: " + tabla);
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " (" + columnas + ") VALUES (");
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

    /**
     * Extrae host, puerto y nombre de base desde una URL JDBC de PostgreSQL.
     * Formato esperado: jdbc:postgresql://host:puerto/base?parametros
     * Si la URL no trae algún dato se usan los valores por defecto de la aplicación
     * (localhost, 5432, farmac_db).
     */
    static String[] parsearUrlJdbc(String jdbcUrl) {
        String host = "localhost";
        String puerto = "5432";
        String base = "farmac_db";

        if (jdbcUrl != null && !jdbcUrl.trim().isEmpty()) {
            String resto = jdbcUrl.trim();

            int idx = resto.indexOf("//");
            if (idx >= 0) {
                resto = resto.substring(idx + 2);
            } else {
                String prefijo = "jdbc:postgresql:";
                if (resto.startsWith(prefijo)) {
                    resto = resto.substring(prefijo.length());
                }
            }

            int corte = resto.indexOf('?');
            if (corte >= 0) {
                resto = resto.substring(0, corte);
            }

            int barra = resto.indexOf('/');
            String autoridad = barra >= 0 ? resto.substring(0, barra) : resto;
            String baseCruda = barra >= 0 ? resto.substring(barra + 1) : "";
            if (!baseCruda.trim().isEmpty()) {
                base = baseCruda.trim();
            }

            if (!autoridad.trim().isEmpty()) {
                int dosPuntos = autoridad.lastIndexOf(':');
                if (dosPuntos > 0 && dosPuntos < autoridad.length() - 1) {
                    host = autoridad.substring(0, dosPuntos).trim();
                    puerto = autoridad.substring(dosPuntos + 1).trim();
                } else {
                    host = autoridad.trim();
                }
            }
        }

        return new String[]{host, puerto, base};
    }
}
