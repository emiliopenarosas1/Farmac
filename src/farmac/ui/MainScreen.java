package farmac.ui;

import farmac.dao.LoteDAO;
import farmac.dao.ProductoDAO;
import farmac.dominio.Lote;
import farmac.dominio.OperacionesInventario;
import farmac.dominio.Producto;
import farmac.dominio.Usuario;
import farmac.util.RespaldoUtil;
import farmac.util.Sesion;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla principal del sistema con control de acceso por roles,
 * integración de todos los módulos y alertas al inicio.
 */
public class MainScreen extends JFrame {

    private final Usuario usuarioActivo;
    private JPanel pnlContenidoCentral;
    private CardLayout cardLayout;
    private List<JButton> botonesMenu;
    private final Map<String, JPanel> panelesCreados;
    private boolean alertasCaducidadPendientes;
    private List<Lote> alertasLotes = new ArrayList<>();
    private List<Producto> alertasStock = new ArrayList<>();
    private List<Lote> alertasTodosLotes = new ArrayList<>();
    private JLabel lblEstadoTurno;
    private JLabel lblFechaHora;
    private javax.swing.Timer relojTurno;

    public MainScreen(Usuario usuario) {
        super("Farmac - Sistema de Gestión Farmacéutica y Punto de Venta");
        this.usuarioActivo = usuario;
        this.botonesMenu = new ArrayList<>();
        this.panelesCreados = new HashMap<>();

        Sesion.setUsuarioActivo(usuario);
        setIconImages(TemaFarmac.iconosApp());
        configurarVentana();
        inicializarComponentes();

        // Lanzar comprobación de alertas en segundo plano al arrancar
        SwingUtilities.invokeLater(this::comprobarAlertasInicio);
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Tamaño mínimo seguro: si la pantalla física es más pequeña, el mínimo
        // se ajusta para que la ventana nunca quede fuera del escritorio.
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int minAncho = Math.min(1024, Math.max(720, pantalla.width - 40));
        int minAlto = Math.min(640, Math.max(520, pantalla.height - 80));
        setMinimumSize(new Dimension(minAncho, minAlto));

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout());

        // Sidebar lateral
        JPanel pnlSidebar = new JPanel(new BorderLayout());
        pnlSidebar.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlSidebar.setPreferredSize(new Dimension(254, 0));

        // Marca
        JPanel pnlBrand = new JPanel(new BorderLayout());
        pnlBrand.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlBrand.setBorder(new EmptyBorder(18, 20, 14, 14));
        pnlBrand.add(TemaFarmac.crearLogo(Color.WHITE, 26), BorderLayout.WEST);
        pnlSidebar.add(pnlBrand, BorderLayout.NORTH);

        // Menú de opciones (con scroll vertical por si la pantalla es baja)
        JPanel pnlMenu = new JPanel();
        pnlMenu.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlMenu.setLayout(new BoxLayout(pnlMenu, BoxLayout.Y_AXIS));
        pnlMenu.setBorder(new EmptyBorder(4, 10, 4, 10));

        cardLayout = new CardLayout();
        pnlContenidoCentral = new JPanel(cardLayout);
        pnlContenidoCentral.setBackground(TemaFarmac.GRIS_FONDO);

        boolean esEncargado = "ENCARGADO".equalsIgnoreCase(usuarioActivo.obtenerRol());

        // Módulos compartidos: se construyen de forma diferida al abrir cada pestaña
        agregarOpcionMenu(pnlMenu, "Punto de Venta", "POS", () -> new PuntoVentaScreen(usuarioActivo));
        agregarOpcionMenu(pnlMenu, "Control de caja", "CAJA", () -> new CorteCajaScreen(usuarioActivo));
        agregarOpcionMenu(pnlMenu, "Historial de Ventas", "HISTORIAL", HistorialVentasScreen::new);

        // Módulos exclusivos de Encargado
        if (esEncargado) {
            agregarOpcionMenu(pnlMenu, "Catálogo de Medicamentos", "CATALOGO", () -> new CatalogoProductosScreen(usuarioActivo));
            agregarOpcionMenu(pnlMenu, "Recepción de Lotes", "RECEPCION", RecepcionInventarioScreen::new);
            agregarOpcionMenu(pnlMenu, "Ajustes de Inventario", "AJUSTES", () -> new AjusteInventarioScreen(usuarioActivo));
            agregarOpcionMenu(pnlMenu, "Reporte de Existencias", "EXISTENCIAS", ReporteExistenciasScreen::new);
            agregarOpcionMenu(pnlMenu, "Alerta de caducidad", "CADUCIDAD", ReporteCaducidadScreen::new);
            agregarOpcionMenu(pnlMenu, "Proveedores", "PROVEEDORES", ProveedoresScreen::new);
            agregarOpcionMenu(pnlMenu, "Gestión de Usuarios", "USUARIOS", GestionUsuariosScreen::new);
        }

        JScrollPane scrollMenu = new JScrollPane(pnlMenu,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMenu.setBorder(null);
        scrollMenu.getViewport().setBackground(TemaFarmac.AZUL_PETROLEO);
        scrollMenu.getVerticalScrollBar().setUnitIncrement(16);
        pnlSidebar.add(scrollMenu, BorderLayout.CENTER);

        // Pie: respaldo (solo Encargado) y cerrar sesión
        JPanel pnlPie = new JPanel();
        pnlPie.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlPie.setLayout(new BoxLayout(pnlPie, BoxLayout.Y_AXIS));
        pnlPie.setBorder(new EmptyBorder(8, 10, 18, 10));

        if (esEncargado) {
            JButton btnRespaldo = crearBotonMenu("Respaldar BD (SQL)", FontAwesomeSolid.DATABASE);
            btnRespaldo.addActionListener(e -> ejecutarRespaldoBD());
            pnlPie.add(btnRespaldo);
            pnlPie.add(Box.createRigidArea(new Dimension(0, 2)));
        }

        JButton btnSalir = crearBotonMenu("Cerrar sesión", FontAwesomeSolid.SIGN_OUT_ALT);
        btnSalir.addActionListener(e -> {
            if (relojTurno != null) {
                relojTurno.stop();
            }
            Sesion.cerrarSesion();
            dispose();
            new LoginScreen().setVisible(true);
        });
        pnlPie.add(btnSalir);
        pnlSidebar.add(pnlPie, BorderLayout.SOUTH);

        // Área derecha: barra superior + contenido
        JPanel pnlDerecha = new JPanel(new BorderLayout());
        pnlDerecha.add(crearBarraSuperior(), BorderLayout.NORTH);
        pnlDerecha.add(pnlContenidoCentral, BorderLayout.CENTER);

        add(pnlSidebar, BorderLayout.WEST);
        add(pnlDerecha, BorderLayout.CENTER);

        if (!botonesMenu.isEmpty()) {
            botonesMenu.get(0).doClick();
        }
    }

    private static final Color COLOR_MENU = Color.WHITE;

    private JButton crearBotonMenu(String texto, FontAwesomeSolid icono) {
        boolean largo = texto.length() > 22;
        String etiqueta = largo ? "<html>" + texto.replaceFirst(" de ", " de<br>") + "</html>" : texto;
        JButton btn = new JButton(etiqueta) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                if (Boolean.TRUE.equals(getClientProperty("farmac.activo"))) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0x3A, 0x66, 0xB5));
                    g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 15));
        btn.setForeground(COLOR_MENU);
        btn.setBackground(TemaFarmac.AZUL_PETROLEO);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, largo ? 52 : 44));
        btn.setPreferredSize(new Dimension(220, largo ? 52 : 44));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 14, 5, 8));
        btn.setIcon(FontIcon.of(icono, 15, COLOR_MENU));
        btn.setIconTextGap(14);
        return btn;
    }

    private JPanel crearBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Color.WHITE);
        barra.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, TemaFarmac.BORDE),
                new EmptyBorder(10, 26, 10, 26)));

        // Avatar + nombre + rol
        JPanel izq = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 0));
        izq.setOpaque(false);
        JLabel avatar = new JLabel(FontIcon.of(FontAwesomeSolid.USER, 16, Color.WHITE), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TemaFarmac.AZUL_PETROLEO);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setPreferredSize(new Dimension(38, 38));
        JLabel lblNombre = new JLabel(usuarioActivo.getNombreCompleto().replaceAll("\\s*\\([^)]*\\)\\s*$", ""));
        lblNombre.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 16));
        lblNombre.setForeground(TemaFarmac.TEXTO);
        JLabel lblSep = new JLabel("|");
        lblSep.setForeground(TemaFarmac.BORDE);
        JLabel lblRol = new JLabel("<html>Rol: <b>" + usuarioActivo.obtenerRol() + "</b></html>");
        lblRol.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 16));
        lblRol.setForeground(TemaFarmac.TEXTO_AZUL);
        izq.add(avatar);
        izq.add(lblNombre);
        izq.add(lblSep);
        izq.add(lblRol);

        // Estado de turno + fecha/hora
        JPanel der = new JPanel();
        der.setOpaque(false);
        der.setLayout(new BoxLayout(der, BoxLayout.X_AXIS));
        lblEstadoTurno = new JLabel();
        lblEstadoTurno.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 15));
        lblFechaHora = new JLabel();
        lblFechaHora.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 15));
        lblFechaHora.setForeground(TemaFarmac.TEXTO_MUTED);
        lblEstadoTurno.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        lblFechaHora.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
        der.add(lblEstadoTurno);
        der.add(Box.createRigidArea(new Dimension(24, 0)));
        der.add(lblFechaHora);

        barra.add(izq, BorderLayout.WEST);
        barra.add(der, BorderLayout.EAST);

        actualizarBarraEstado();
        relojTurno = new javax.swing.Timer(15000, e -> actualizarBarraEstado());
        relojTurno.start();
        return barra;
    }

    private void actualizarBarraEstado() {
        boolean abierto = Sesion.hayTurnoAbierto();
        Color c = abierto ? TemaFarmac.VERDE : TemaFarmac.AMBAR;
        lblEstadoTurno.setText(abierto ? "Turno abierto" : "Sin turno abierto");
        lblEstadoTurno.setForeground(c);
        lblEstadoTurno.setIcon(new javax.swing.Icon() {
            @Override public void paintIcon(java.awt.Component comp, java.awt.Graphics g, int x, int y) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(x, y, 8, 8);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 8; }
            @Override public int getIconHeight() { return 8; }
        });
        lblEstadoTurno.setIconTextGap(6);
        lblFechaHora.setText(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }

    private FontAwesomeSolid iconoPorClave(String clave) {
        switch (clave) {
            case "POS": return FontAwesomeSolid.SHOPPING_CART;
            case "CAJA": return FontAwesomeSolid.CASH_REGISTER;
            case "HISTORIAL": return FontAwesomeSolid.HISTORY;
            case "CATALOGO": return FontAwesomeSolid.PILLS;
            case "RECEPCION": return FontAwesomeSolid.TRUCK_LOADING;
            case "AJUSTES": return FontAwesomeSolid.SLIDERS_H;
            case "EXISTENCIAS": return FontAwesomeSolid.BOXES;
            case "CADUCIDAD": return FontAwesomeSolid.EXCLAMATION_TRIANGLE;
            case "PROVEEDORES": return FontAwesomeSolid.TRUCK;
            case "USUARIOS": return FontAwesomeSolid.USERS;
            default: return FontAwesomeSolid.CIRCLE;
        }
    }

    private void agregarOpcionMenu(JPanel contenedorMenu, String texto, String claveCard, Supplier<JPanel> fabricaPanel) {
        JButton btn = crearBotonMenu(texto, iconoPorClave(claveCard));

        btn.addActionListener(e -> {
            for (JButton b : botonesMenu) {
                b.putClientProperty("farmac.activo", Boolean.FALSE);
                b.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 15));
                b.repaint();
            }
            btn.putClientProperty("farmac.activo", Boolean.TRUE);
            btn.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 15));
            btn.repaint();

            // Construcción diferida: cada pantalla se crea solo la primera vez que se abre.
            if (!panelesCreados.containsKey(claveCard)) {
                try {
                    JPanel panelVista = fabricaPanel.get();
                    panelesCreados.put(claveCard, null);
                    pnlContenidoCentral.add(panelVista, claveCard);
                } catch (RuntimeException ex) {
                    DialogoFarmac.mostrarJO(this,
                            "No se pudo abrir el módulo '" + texto + "': " + ex.getMessage(),
                            "Error al abrir módulo", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            cardLayout.show(pnlContenidoCentral, claveCard);
            actualizarBarraEstado();
        });

        botonesMenu.add(btn);
        contenedorMenu.add(btn);
        contenedorMenu.add(Box.createRigidArea(new Dimension(0, 2)));
    }

    private void ejecutarRespaldoBD() {
        // pg_dump puede tardar: se ejecuta fuera del EDT para no congelar la interfaz.
        SwingWorker<File, Void> worker = new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                return RespaldoUtil.generarRespaldo();
            }

            @Override
            protected void done() {
                try {
                    File f = get();
                    DialogoFarmac.mostrarJO(MainScreen.this,
                            "Respaldo de Base de Datos generado exitosamente:\n" + f.getAbsolutePath(),
                            "Respaldo Exitoso", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    DialogoFarmac.mostrarJO(MainScreen.this,
                            "Error al generar respaldo: " + causa.getMessage(),
                            "Error de Respaldo", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void comprobarAlertasInicio() {
        // Consulta la BD en segundo plano y muestra el diálogo en el EDT
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return construirMensajeAlertas();
            }

            @Override
            protected void done() {
                try {
                    String msj = get();
                    if (msj != null && !msj.isEmpty()) {
                        mostrarDialogoAlertas(msj);
                    }
                } catch (Exception e) {
                    System.err.println("Error al comprobar alertas: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Consulta la base de datos y arma el texto de alertas. No toca componentes
     * Swing: se ejecuta fuera del EDT.
     */
    private String construirMensajeAlertas() {
        alertasCaducidadPendientes = false;
        try {
            LoteDAO loteDAO = new LoteDAO();
            List<Lote> lotes = loteDAO.listarTodos();
            OperacionesInventario op = new OperacionesInventario(60);
            op.setInventario(lotes);

            List<Lote> alertasCad = op.obtenerLotesEnAlerta();
            alertasTodosLotes = lotes;
            alertasLotes = new ArrayList<>(alertasCad);
            alertasStock = new ArrayList<>();

            StringBuilder msj = new StringBuilder();

            // Alerta de Caducidad
            if (!alertasCad.isEmpty()) {
                alertasCaducidadPendientes = true;
                long vencidos = 0;
                long porVencer = 0;
                for (Lote l : alertasCad) {
                    if (l.estaVencido(java.time.LocalDate.now())) {
                        vencidos++;
                    } else {
                        porVencer++;
                    }
                }
                msj.append("ALERTA DE CADUCIDADES:\n")
                   .append("• ").append(vencidos).append(" lote(s) VENCIDOS.\n")
                   .append("• ").append(porVencer).append(" lote(s) por vencer en los próximos 60 días.\n");

                msj.append("\nDetalle:\n");
                int max = Math.min(alertasCad.size(), 6);
                for (int i = 0; i < max; i++) {
                    Lote l = alertasCad.get(i);
                    String estado = l.estaVencido(java.time.LocalDate.now()) ? "VENCIDO" : "POR VENCER";
                    msj.append("   - ").append(l.getNumeroLote())
                       .append(" (").append(l.getProducto() != null ? l.getProducto().getNombre() : "Producto")
                       .append(") - ").append(estado)
                       .append(" - caduca: ").append(l.getFechaCaducidad()).append("\n");
                }
                if (alertasCad.size() > max) {
                    msj.append("   ... y ").append(alertasCad.size() - max).append(" lote(s) más.\n");
                }
            }

            // Alerta de Stock Mínimo para Encargado
            if (Sesion.esEncargado()) {
                ProductoDAO prodDAO = new ProductoDAO();
                List<Producto> prods = prodDAO.listarTodos(true);
                List<Producto> bajos = op.productosBajoStock(prods);
                alertasStock = new ArrayList<>(bajos);
                if (!bajos.isEmpty()) {
                    msj.append("\n\nALERTA DE DESABASTO / STOCK MÍNIMO:\n")
                       .append("• ").append(bajos.size()).append(" producto(s) en o por debajo de su stock de seguridad:\n");
                    int maxP = Math.min(bajos.size(), 6);
                    for (int i = 0; i < maxP; i++) {
                        msj.append("   - ").append(bajos.get(i).getNombre()).append("\n");
                    }
                    if (bajos.size() > maxP) {
                        msj.append("   ... y ").append(bajos.size() - maxP).append(" más.\n");
                    }
                }
            }

            return msj.toString();
        } catch (Exception e) {
            System.err.println("Error al comprobar alertas: " + e.getMessage());
            return "";
        }
    }

    /** Muestra el aviso al usuario con secciones de caducidad y desabasto. Se invoca siempre desde el EDT. */
    private void mostrarDialogoAlertas(String mensaje) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        javax.swing.JDialog dlg = new javax.swing.JDialog(this, "Aviso del sistema", true);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Color.WHITE);

        // Cabecera
        JPanel cab = new JPanel(new BorderLayout(14, 0));
        cab.setOpaque(false);
        cab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TemaFarmac.BORDE), new EmptyBorder(16, 22, 14, 22)));
        JPanel tit = new JPanel();
        tit.setOpaque(false);
        tit.setLayout(new BoxLayout(tit, BoxLayout.Y_AXIS));
        JLabel lblT = new JLabel("Aviso del sistema");
        lblT.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 17));
        lblT.setForeground(TemaFarmac.TEXTO_AZUL);
        JLabel lblS = new JLabel("Revisión automática de inventario");
        lblS.setFont(TemaFarmac.FUENTE_REGULAR);
        lblS.setForeground(TemaFarmac.TEXTO_MUTED);
        tit.add(lblT);
        tit.add(lblS);
        cab.add(new JLabel(FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 24, TemaFarmac.AMBAR)), BorderLayout.WEST);
        cab.add(tit, BorderLayout.CENTER);
        raiz.add(cab, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(new EmptyBorder(16, 22, 8, 22));

        // Sección de caducidades
        if (!alertasLotes.isEmpty()) {
            long vencidos = alertasLotes.stream().filter(l -> l.estaVencido(hoy)).count();
            long porVencer = alertasLotes.size() - vencidos;
            JPanel sec = crearSeccionAlerta("Alerta de caducidades",
                    "<html><b>" + vencidos + "</b> vencido \u00B7 <b>" + porVencer + "</b> por vencer en 60 d\u00EDas</html>",
                    TemaFarmac.ROJO, TemaFarmac.ROJO_BG, new Color(0xEE, 0xB4, 0xB4));
            JPanel lista = (JPanel) sec.getClientProperty("lista");
            for (Lote l : alertasLotes) {
                boolean venc = l.estaVencido(hoy);
                String nombre = (l.getProducto() != null ? l.getProducto().getNombre() : "Producto") + " | " + l.getNumeroLote();
                JPanel fila = new JPanel(new BorderLayout(12, 0));
                fila.setBackground(Color.WHITE);
                fila.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xF0, 0xD5, 0xD5)), new EmptyBorder(8, 14, 8, 14)));
                JLabel ln = new JLabel("<html><div style='width:260px'>" + nombre + "</div></html>");
                ln.setFont(TemaFarmac.FUENTE_REGULAR);
                ln.setForeground(TemaFarmac.TEXTO_AZUL);
                JLabel lf = new JLabel("caduca " + l.getFechaCaducidad().format(fmt));
                lf.setFont(TemaFarmac.FUENTE_PEQUENA);
                lf.setForeground(TemaFarmac.TEXTO_MUTED);
                JLabel le = new JLabel(venc ? "VENCIDO" : "POR VENCER");
                le.setFont(TemaFarmac.FUENTE_REGULAR);
                le.setForeground(venc ? TemaFarmac.ROJO : TemaFarmac.AMBAR);
                le.setIcon(puntoColor(venc ? TemaFarmac.ROJO : TemaFarmac.AMBAR));
                le.setIconTextGap(6);
                le.setPreferredSize(new Dimension(110, 18));
                JPanel der = new JPanel(new BorderLayout(14, 0));
                der.setOpaque(false);
                der.add(lf, BorderLayout.WEST);
                der.add(le, BorderLayout.EAST);
                fila.add(ln, BorderLayout.CENTER);
                fila.add(der, BorderLayout.EAST);
                lista.add(fila);
            }
            sec.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            cuerpo.add(sec);
            cuerpo.add(Box.createRigidArea(new Dimension(0, 14)));
        }

        // Sección de desabasto
        if (!alertasStock.isEmpty()) {
            JPanel sec = crearSeccionAlerta("Alerta de desabasto / stock m\u00EDnimo",
                    "<html><b>" + alertasStock.size() + "</b> productos en o bajo su stock de seguridad</html>",
                    new Color(0x8A, 0x5A, 0x0A), TemaFarmac.AMBAR_BG, new Color(0xE8, 0xCF, 0x8E));
            JPanel lista = (JPanel) sec.getClientProperty("lista");
            List<Lote> todos = alertasTodosLotes;
            for (Producto pr : alertasStock) {
                JPanel fila = new JPanel(new BorderLayout(12, 0));
                fila.setBackground(Color.WHITE);
                fila.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xF2, 0xE6, 0xC4)), new EmptyBorder(8, 14, 8, 14)));
                JLabel ln = new JLabel(pr.getNombre());
                ln.setFont(TemaFarmac.FUENTE_REGULAR);
                ln.setForeground(TemaFarmac.TEXTO_AZUL);
                JLabel lm = new JLabel("m\u00EDnimo " + pr.getStockMinimo());
                lm.setFont(TemaFarmac.FUENTE_PEQUENA);
                lm.setForeground(TemaFarmac.TEXTO_MUTED);
                JLabel lp = new JLabel(pr.calcularStockTotal(todos) + " pzas");
                lp.setFont(TemaFarmac.FUENTE_NEGRITA);
                lp.setForeground(TemaFarmac.ROJO);
                lp.setPreferredSize(new Dimension(70, 18));
                lp.setHorizontalAlignment(SwingConstants.RIGHT);
                JPanel der = new JPanel(new BorderLayout(18, 0));
                der.setOpaque(false);
                der.add(lm, BorderLayout.WEST);
                der.add(lp, BorderLayout.EAST);
                fila.add(ln, BorderLayout.CENTER);
                fila.add(der, BorderLayout.EAST);
                lista.add(fila);
            }
            sec.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            cuerpo.add(sec);
        }

        JScrollPane scroll = new JScrollPane(cuerpo, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        raiz.add(scroll, BorderLayout.CENTER);

        // Pie
        JPanel pie = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, TemaFarmac.BORDE), new EmptyBorder(14, 12, 14, 12)));
        JButton btnOk = TemaFarmac.crearBoton("Enterado", TemaFarmac.TURQUESA, Color.WHITE);
        btnOk.addActionListener(e -> dlg.dispose());
        pie.add(btnOk);
        raiz.add(pie, BorderLayout.SOUTH);

        dlg.getRootPane().setDefaultButton(btnOk);
        dlg.setContentPane(raiz);
        dlg.pack();
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(Math.min(Math.max(dlg.getWidth(), 700), pantalla.width - 60),
                Math.min(dlg.getHeight(), pantalla.height - 100));
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);

    }

    private javax.swing.Icon puntoColor(Color c) {
        return new javax.swing.Icon() {
            @Override public void paintIcon(java.awt.Component comp, java.awt.Graphics g, int x, int y) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(x, y, 8, 8);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 8; }
            @Override public int getIconHeight() { return 8; }
        };
    }

    /** Sección con encabezado de color y un panel "lista" (guardado como propiedad) donde se agregan las filas. */
    private JPanel crearSeccionAlerta(String titulo, String resumen, Color colorTitulo, Color fondo, Color borde) {
        JPanel sec = new JPanel(new BorderLayout());
        sec.setBackground(Color.WHITE);
        sec.setBorder(new TemaFarmac.BordeRedondeado(borde, 8, 1));
        JPanel cab = new JPanel(new BorderLayout());
        cab.setBackground(fondo);
        cab.setBorder(new EmptyBorder(11, 14, 11, 14));
        JLabel lt = new JLabel(titulo);
        lt.setFont(TemaFarmac.FUENTE_NEGRITA);
        lt.setForeground(colorTitulo);
        JLabel lr = new JLabel(resumen);
        lr.setFont(TemaFarmac.FUENTE_REGULAR);
        lr.setForeground(colorTitulo);
        cab.add(lt, BorderLayout.WEST);
        cab.add(lr, BorderLayout.EAST);
        JPanel lista = new JPanel();
        lista.setBackground(Color.WHITE);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        sec.add(cab, BorderLayout.NORTH);
        sec.add(lista, BorderLayout.CENTER);
        sec.putClientProperty("lista", lista);
        return sec;
    }
}
