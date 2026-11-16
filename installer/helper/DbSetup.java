import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DbSetup: utilidad de respaldo para operaciones de base de datos cuando no
 * hay un cliente psql disponible en el equipo.
 *
 * La contrasena NUNCA se pasa por linea de comandos: se lee de la variable de
 * entorno FARMAC_DBPASS, de modo que no quede expuesta en la lista de procesos
 * ni en los registros del instalador.
 *
 * Comandos:
 *   test        &lt;host&gt; &lt;puerto&gt; &lt;base&gt; &lt;usuario&gt;
 *   run-script  &lt;host&gt; &lt;puerto&gt; &lt;base&gt; &lt;usuario&gt; &lt;archivo.sql&gt;
 *
 * Salida:
 *   OK: &lt;detalle&gt;    -> exito (codigo 0)
 *   ERROR: &lt;detalle&gt; -> fallo (codigo 1)
 *
 * Limitacion conocida: el separador de sentencias SQL es propio (respeta
 * comentarios, cadenas y bloques DO $$ ... $$). No interpreta meta-comandos de
 * psql (lineas que inician con '\'), los ignora. Para scripts muy complejos se
 * recomienda disponer de psql.
 */
public class DbSetup {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                uso();
                System.exit(2);
            }
            String comando = args[0].toLowerCase();
            switch (comando) {
                case "test": {
                    if (args.length < 5) { uso(); System.exit(2); }
                    test(args[1], args[2], args[3], args[4]);
                    break;
                }
                case "run-script": {
                    if (args.length < 6) { uso(); System.exit(2); }
                    ejecutarScript(args[1], args[2], args[3], args[4], args[5]);
                    break;
                }
                default:
                    uso();
                    System.exit(2);
            }
        } catch (Throwable t) {
            System.out.println("ERROR: " + mensaje(t));
            System.exit(1);
        }
    }

    private static void uso() {
        System.out.println("Uso: DbSetup test <host> <puerto> <base> <usuario>");
        System.out.println("     DbSetup run-script <host> <puerto> <base> <usuario> <archivo.sql>");
        System.out.println("La contrasena se lee de la variable de entorno FARMAC_DBPASS.");
    }

    private static String jdbcUrl(String host, String puerto, String base) {
        return "jdbc:postgresql://" + host + ":" + puerto + "/" + base;
    }

    private static Connection conectar(String host, String puerto, String base, String usuario) throws Exception {
        String password = System.getenv("FARMAC_DBPASS");
        if (password == null) {
            throw new IllegalStateException("No se definio la variable de entorno FARMAC_DBPASS.");
        }
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(jdbcUrl(host, puerto, base), usuario, password);
    }

    private static void test(String host, String puerto, String base, String usuario) throws Exception {
        try (Connection cn = conectar(host, puerto, base, usuario);
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            if (rs.next() && rs.getInt(1) == 1) {
                System.out.println("OK: La conexion es correcta");
            } else {
                throw new IllegalStateException("La consulta de prueba no devolvio resultados.");
            }
        }
    }

    private static void ejecutarScript(String host, String puerto, String base, String usuario, String archivo) throws Exception {
        Path ruta = Path.of(archivo);
        if (!Files.isRegularFile(ruta)) {
            throw new IOException("No se encontro el archivo: " + archivo);
        }
        String contenido = Files.readString(ruta, StandardCharsets.UTF_8);
        List<String> sentencias = separarSentencias(contenido);
        try (Connection cn = conectar(host, puerto, base, usuario)) {
            cn.setAutoCommit(true);
            int ejecutadas = 0;
            try (Statement st = cn.createStatement()) {
                for (String sql : sentencias) {
                    String s = sql.trim();
                    if (s.isEmpty() || s.startsWith("\\")) {
                        continue;
                    }
                    st.execute(s);
                    ejecutadas++;
                }
            }
            System.out.println("OK: Script ejecutado (" + ejecutadas + " sentencias): " + ruta.getFileName());
        }
    }

    /**
     * Separa un script SQL en sentencias terminadas en ';' respetando:
     * comentarios de linea, comentarios de bloque, cadenas entre comillas
     * simples (con escape '') y bloques con dollar-quoting ($$ o $etiqueta$).
     */
    static List<String> separarSentencias(String sql) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        int n = sql.length();
        int i = 0;
        while (i < n) {
            char c = sql.charAt(i);

            // Comentario de linea
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                while (i < n && sql.charAt(i) != '\n') i++;
                continue;
            }
            // Comentario de bloque
            if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(sql.charAt(i) == '*' && sql.charAt(i + 1) == '/')) i++;
                i += 2;
                continue;
            }
            // Cadena entre comillas simples
            if (c == '\'') {
                actual.append(c);
                i++;
                while (i < n) {
                    char q = sql.charAt(i);
                    actual.append(q);
                    if (q == '\'') {
                        if (i + 1 < n && sql.charAt(i + 1) == '\'') {
                            actual.append(sql.charAt(i + 1));
                            i += 2;
                            continue;
                        }
                        i++;
                        break;
                    }
                    i++;
                }
                continue;
            }
            // Dollar-quoting: $$ o $etiqueta$
            if (c == '$') {
                String etiqueta = leerEtiquetaDollar(sql, i);
                if (etiqueta != null) {
                    actual.append(etiqueta);
                    i += etiqueta.length();
                    int fin = sql.indexOf(etiqueta, i);
                    if (fin < 0) {
                        actual.append(sql.substring(i));
                        i = n;
                    } else {
                        actual.append(sql, i, fin + etiqueta.length());
                        i = fin + etiqueta.length();
                    }
                    continue;
                }
            }
            // Fin de sentencia
            if (c == ';') {
                String s = actual.toString().trim();
                if (!s.isEmpty()) {
                    sentencias.add(s);
                }
                actual.setLength(0);
                i++;
                continue;
            }
            actual.append(c);
            i++;
        }
        String resto = actual.toString().trim();
        if (!resto.isEmpty()) {
            sentencias.add(resto);
        }
        return sentencias;
    }

    private static String leerEtiquetaDollar(String sql, int inicio) {
        // inicio apunta a '$'. Formato: $etiqueta$ donde etiqueta = [A-Za-z0-9_]*
        int j = inicio + 1;
        while (j < sql.length()) {
            char c = sql.charAt(j);
            if (c == '$') {
                return sql.substring(inicio, j + 1);
            }
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return null;
            }
            j++;
        }
        return null;
    }

    private static String mensaje(Throwable t) {
        String m = t.getMessage();
        if (m == null || m.isBlank()) {
            return t.getClass().getSimpleName();
        }
        return m.replaceAll("\\s+", " ").trim();
    }
}
