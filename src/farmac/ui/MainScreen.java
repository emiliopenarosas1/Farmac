package farmac.ui;

import farmac.dominio.Usuario;
import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal con ajuste automatico al area visible de la pantalla (sin tapar la barra de tareas).
 */
public class MainScreen extends JFrame {
    private final Usuario usuarioActivo;
    private JPanel pnlContenidoCentral;
    private CardLayout cardLayout;
    private List<JButton> botonesMenu;

    public MainScreen(Usuario usuario) {
        super("Farmac - Sistema de Gestion de Farmacia y Punto de Venta");
        this.usuarioActivo = usuario;
        this.botonesMenu = new ArrayList<>();
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Obtener limites seguros de la pantalla (descontando la barra de tareas de Windows)
        Rectangle limitesPantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setBounds(limitesPantalla);
        setMinimumSize(new Dimension(980, 580));
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout());

        // Sidebar lateral compacto
        JPanel pnlSidebar = new JPanel();
        pnlSidebar.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlSidebar.setPreferredSize(new Dimension(200, 0));
        pnlSidebar.setLayout(new BorderLayout());

        // Cabecera del Sidebar
        JPanel pnlBrand = new JPanel();
        pnlBrand.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlBrand.setLayout(new BoxLayout(pnlBrand, BoxLayout.Y_AXIS));
        pnlBrand.setBorder(new EmptyBorder(14, 14, 10, 14));

        JLabel lblLogo = new JLabel("FARMAC");
        lblLogo.setFont(TemaFarmac.FUENTE_TITULO);
        lblLogo.setForeground(Color.WHITE);

        JLabel lblSlogan = new JLabel("Panel de Control");
        lblSlogan.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSlogan.setForeground(new Color(0x94, 0xD2, 0xBD));

        pnlBrand.add(lblLogo);
        pnlBrand.add(Box.createRigidArea(new Dimension(0, 1)));
        pnlBrand.add(lblSlogan);
        pnlSidebar.add(pnlBrand, BorderLayout.NORTH);

        // Menu de opciones
        JPanel pnlMenu = new JPanel();
        pnlMenu.setBackground(TemaFarmac.AZUL_PETROLEO);
        pnlMenu.setLayout(new BoxLayout(pnlMenu, BoxLayout.Y_AXIS));
        pnlMenu.setBorder(new EmptyBorder(4, 6, 4, 6));

        cardLayout = new CardLayout();
        pnlContenidoCentral = new JPanel(cardLayout);
        pnlContenidoCentral.setBackground(TemaFarmac.FONDO);

        agregarOpcionMenu(pnlMenu, "Punto de Venta", "POS", new PuntoVentaScreen(usuarioActivo));
        agregarOpcionMenu(pnlMenu, "Control de Caja", "CAJA", new CorteCajaScreen(usuarioActivo));
        agregarOpcionMenu(pnlMenu, "Catalogo Medicamentos", "CATALOGO", new CatalogoProductosScreen(usuarioActivo));
        agregarOpcionMenu(pnlMenu, "Recepcion de Lotes", "RECEPCION", new RecepcionInventarioScreen());
        agregarOpcionMenu(pnlMenu, "Alertas de Caducidad", "CADUCIDAD", new ReporteCaducidadScreen());

        if ("ENCARGADO".equalsIgnoreCase(usuarioActivo.obtenerRol())) {
            agregarOpcionMenu(pnlMenu, "Ajustes de Inventario", "AJUSTES", new AjusteInventarioScreen(usuarioActivo));
            agregarOpcionMenu(pnlMenu, "Gestion de Usuarios", "USUARIOS", new GestionUsuariosScreen());
        }

        pnlSidebar.add(pnlMenu, BorderLayout.CENTER);

        // Pie de usuario compacto
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

        JButton btnSalir = TemaFarmac.crearBoton("Cerrar Sesion", TemaFarmac.ROJO, Color.WHITE);
        btnSalir.setFont(TemaFarmac.FUENTE_PEQUENA);
        btnSalir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btnSalir.addActionListener(e -> {
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

    private void agregarOpcionMenu(JPanel contenedorMenu, String texto, String claveCard, JPanel panelVista) {
        JButton btn = new JButton(texto);
        btn.setFont(TemaFarmac.FUENTE_NEGRITA);
        btn.setForeground(new Color(0xD0, 0xE2, 0xEC));
        btn.setBackground(TemaFarmac.AZUL_PETROLEO);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 8, 6, 8));

        btn.addActionListener(e -> {
            for (JButton b : botonesMenu) {
                b.setOpaque(false);
                b.setBackground(TemaFarmac.AZUL_PETROLEO);
                b.setForeground(new Color(0xD0, 0xE2, 0xEC));
            }
            btn.setOpaque(true);
            btn.setBackground(TemaFarmac.TURQUESA);
            btn.setForeground(Color.WHITE);
            cardLayout.show(pnlContenidoCentral, claveCard);
        });

        botonesMenu.add(btn);
        contenedorMenu.add(btn);
        contenedorMenu.add(Box.createRigidArea(new Dimension(0, 2)));

        pnlContenidoCentral.add(panelVista, claveCard);
    }
}
