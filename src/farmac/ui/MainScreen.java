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
import farmac.util.PanelFade;
import farmac.util.TemaFarmac;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
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
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla principal del sistema con control de acceso por roles (RNF02),
 * integración de todos los módulos y alertas al inicio (RF10, RF11).
 */
public class MainScreen extends JFrame {

    private final Usuario usuarioActivo;
    private JPanel pnlContenidoCentral;
    private CardLayout cardLayout;
    private List<JButton> botonesMenu;
    private final Map<String, PanelFade> panelesCreados;

    public MainScreen(Usuario usuario) {
        super("Farmac - Sistema de Gestión Farmacéutica y Punto de Venta");
        this.usuarioActivo = usuario;
        this.botonesMenu = new ArrayList<>();
        this.panelesCreados = new HashMap<>();
        Sesion.setUsuarioActivo(usuario);
        Sesion.refrescarTurno();
        configurarVentana();
        inicializarComponentes();

        // Lanzar comprobación de alertas en segundo plano al arrancar
        SwingUtilities.invokeLater(this::comprobarAlertasInicio);
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle limitesPantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setBounds(limitesPantalla);
        setMinimumSize(new Dimension(1020, 620));
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout());

        // Sidebar lateral
        JPanel pnlSidebar = new JPanel();
        pnlSidebar.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlSidebar.setPreferredSize(new Dimension(220, 0));
        pnlSidebar.setLayout(new BorderLayout());

        // Cabecera del Sidebar
        JPanel pnlBrand = new JPanel();
        pnlBrand.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlBrand.setLayout(new BoxLayout(pnlBrand, BoxLayout.Y_AXIS));
        pnlBrand.setBorder(new EmptyBorder(14, 14, 10, 14));

        JLabel lblLogo = new JLabel("FARMAC");
        lblLogo.setFont(TemaFarmac.FUENTE_TITULO);
        lblLogo.setForeground(Color.WHITE);

        JLabel lblSlogan = new JLabel("Panel de Control Principal");
        lblSlogan.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSlogan.setForeground(new Color(0x94, 0xD2, 0xBD));

        pnlBrand.add(lblLogo);
        pnlBrand.add(Box.createRigidArea(new Dimension(0, 1)));
        pnlBrand.add(lblSlogan);
        pnlSidebar.add(pnlBrand, BorderLayout.NORTH);

        // Menú de opciones
        JPanel pnlMenu = new JPanel();
        pnlMenu.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlMenu.setLayout(new BoxLayout(pnlMenu, BoxLayout.Y_AXIS));
        pnlMenu.setBorder(new EmptyBorder(4, 6, 4, 6));

        cardLayout = new CardLayout();
        pnlContenidoCentral = new JPanel(cardLayout);
        pnlContenidoCentral.setBackground(TemaFarmac.GRIS_FONDO);

        boolean esEncargado = "ENCARGADO".equalsIgnoreCase(usuarioActivo.obtenerRol());

        // Módulos compartidos: se construyen de forma diferida al abrir cada pestaña
        agregarOpcionMenu(pnlMenu, "Punto de Venta", "POS", () -> new PuntoVentaScreen(usuarioActivo));
        agregarOpcionMenu(pnlMenu, "Control de Caja", "CAJA", () -> new CorteCajaScreen(usuarioActivo));
        agregarOpcionMenu(pnlMenu, "Historial de Ventas", "HISTORIAL", HistorialVentasScreen::new);

        // Módulos exclusivos de Encargado (RNF02)
        if (esEncargado) {
            agregarOpcionMenu(pnlMenu, "Catálogo Medicamentos", "CATALOGO", () -> new CatalogoProductosScreen(usuarioActivo));
            agregarOpcionMenu(pnlMenu, "Recepción de Lotes", "RECEPCION", RecepcionInventarioScreen::new);
            agregarOpcionMenu(pnlMenu, "Ajustes de Inventario", "AJUSTES", () -> new AjusteInventarioScreen(usuarioActivo));
            agregarOpcionMenu(pnlMenu, "Reporte Existencias", "EXISTENCIAS", ReporteExistenciasScreen::new);
            agregarOpcionMenu(pnlMenu, "Alertas Caducidad", "CADUCIDAD", ReporteCaducidadScreen::new);
            agregarOpcionMenu(pnlMenu, "Proveedores", "PROVEEDORES", ProveedoresScreen::new);
            agregarOpcionMenu(pnlMenu, "Gestión de Usuarios", "USUARIOS", GestionUsuariosScreen::new);

            // Botón de Respaldo de BD (A6, RNF05)
            JButton btnRespaldo = new JButton("Respaldar BD (SQL)");
            btnRespaldo.setFont(TemaFarmac.FUENTE_PEQUENA);
            btnRespaldo.setForeground(new Color(0xD0, 0xE2, 0xEC));
            btnRespaldo.setBackground(TemaFarmac.AZUL_PETROLEO);
            btnRespaldo.setContentAreaFilled(false);
            btnRespaldo.setBorderPainted(false);
            btnRespaldo.setFocusPainted(false);
            btnRespaldo.setHorizontalAlignment(SwingConstants.LEFT);
            btnRespaldo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            btnRespaldo.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnRespaldo.addActionListener(e -> ejecutarRespaldoBD());
            pnlMenu.add(Box.createRigidArea(new Dimension(0, 8)));
            pnlMenu.add(btnRespaldo);
        }

        pnlSidebar.add(pnlMenu, BorderLayout.CENTER);

        // Pie de usuario
        JPanel pnlUserFooter = new JPanel();
        pnlUserFooter.setBackground(TemaFarmac.AZUL_OSCURO);
        pnlUserFooter.setLayout(new BoxLayout(pnlUserFooter, BoxLayout.Y_AXIS));
        pnlUserFooter.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel lblUserName = new JLabel(usuarioActivo.getNombreCompleto());
        lblUserName.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblUserName.setForeground(Color.WHITE);

        JLabel lblUserRole = new JLabel("Rol: " + usuarioActivo.obtenerRol());
        lblUserRole.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblUserRole.setForeground(new Color(0x94, 0xD2, 0xBD));

        JButton btnSalir = TemaFarmac.crearBoton(
"Cerrar Sesión", TemaFarmac.ROJO, Color.WHITE);
        btnSalir.setFont(TemaFarmac.FUENTE_PEQUENA);
        btnSalir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btnSalir.addActionListener(e -> {
            Sesion.cerrarSesion();
            dispose();
            new LoginScreen().setVisible(true);
        });

        pnlUserFooter.add(lblUserName);
        pnlUserFooter.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlUserFooter.add(lblUserRole);
        pnlUserFooter.add(Box.createRigidArea(new Dimension(0, 6)));
        pnlUserFooter.add(btnSalir);

        pnlSidebar.add(pnlUserFooter, BorderLayout.SOUTH);

        add(pnlSidebar, BorderLayout.WEST);
        add(pnlContenidoCentral, BorderLayout.CENTER);

        if (!botonesMenu.isEmpty()) {
            botonesMenu.get(0).doClick();
        }
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
        JButton btn = new JButton(texto);
        btn.setFont(TemaFarmac.FUENTE_NEGRITA);
        btn.setForeground(new Color(0xD0, 0xE2, 0xEC));
        btn.setBackground(TemaFarmac.AZUL_PETROLEO);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 8, 5, 8));
        btn.setIcon(FontIcon.of(iconoPorClave(claveCard), 13, new Color(0xD0, 0xE2, 0xEC)));
        btn.setIconTextGap(8);

        btn.addActionListener(e -> {
            for (JButton b : botonesMenu) {
                b.setOpaque(false);
                b.setBackground(TemaFarmac.AZUL_PETROLEO);
                b.setForeground(new Color(0xD0, 0xE2, 0xEC));
                if (b.getIcon() instanceof FontIcon) {
                    ((FontIcon) b.getIcon()).setIconColor(new Color(0xD0, 0xE2, 0xEC));
                }
            }
            btn.setOpaque(true);
            btn.setBackground(TemaFarmac.TURQUESA);
            btn.setForeground(Color.WHITE);
            if (btn.getIcon() instanceof FontIcon) {
                ((FontIcon) btn.getIcon()).setIconColor(Color.WHITE);
            }

            // Construcción diferida: cada pantalla se crea solo la primera vez que se abre.
            if (!panelesCreados.containsKey(claveCard)) {
                try {
                    JPanel panelVista = fabricaPanel.get();
                    PanelFade wrapper = new PanelFade(panelVista);
                    panelesCreados.put(claveCard, wrapper);
                    pnlContenidoCentral.add(wrapper, claveCard);
                } catch (RuntimeException ex) {
                    DialogoFarmac.mostrarJO(this,
                            "No se pudo abrir el módulo '" + texto + "': " + ex.getMessage(),
                            "Error al abrir módulo", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            cardLayout.show(pnlContenidoCentral, claveCard);
            PanelFade pw = panelesCreados.get(claveCard);
            if (pw != null) pw.fadeIn();
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
                            "Respaldo Exitoso (RNF05)", JOptionPane.INFORMATION_MESSAGE);
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
        try {
            LoteDAO loteDAO = new LoteDAO();
            List<Lote> lotes = loteDAO.listarTodos();
            OperacionesInventario op = new OperacionesInventario(60);
            op.setInventario(lotes);

            List<Lote> alertasCad = op.obtenerLotesEnAlerta();

            StringBuilder msj = new StringBuilder();

            // Alerta de Caducidad (RF10)
            if (!alertasCad.isEmpty()) {
                long vencidos = alertasCad.stream().filter(l -> l.estaVencido(java.time.LocalDate.now())).count();
                long porVencer = alertasCad.size() - vencidos;
                msj.append("ALERTA DE CADUCIDADES (RF10):\n")
                   .append("• ").append(vencidos).append(" lote(s) VENCIDOS.\n")
                   .append("• ").append(porVencer).append(" lote(s) por vencer en los próximos 60 días.\n\n");
            }

            // Alerta de Stock Mínimo para Encargado (RF11)
            if (Sesion.esEncargado()) {
                ProductoDAO prodDAO = new ProductoDAO();
                List<Producto> prods = prodDAO.listarTodos(true);
                List<Producto> bajos = op.productosBajoStock(prods);
                if (!bajos.isEmpty()) {
                    msj.append("ALERTA DE DESABASTO / STOCK MÍNIMO (RF11):\n")
                       .append("• ").append(bajos.size()).append(" producto(s) en o por debajo de su stock de seguridad:\n");
                    for (int i = 0; i < Math.min(bajos.size(), 4); i++) {
                        msj.append("   - ").append(bajos.get(i).getNombre()).append("\n");
                    }
                    if (bajos.size() > 4) {
                        msj.append("   ... y ").append(bajos.size() - 4).append(" más.\n");
                    }
                }
            }

            if (msj.length() > 0) {
                DialogoFarmac.mostrarJO(this, msj.toString(), "Avisos del Sistema al Iniciar", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception e) {
            System.err.println("Error al comprobar alertas: " + e.getMessage());
        }
    }
}
