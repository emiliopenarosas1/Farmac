package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.UsuarioDAO;
import farmac.dominio.Usuario;
import farmac.util.Sesion;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla de inicio de sesión con autenticación real en base de datos y BCrypt.
 */
public class LoginScreen extends JFrame {

    private final UsuarioDAO usuarioDAO;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    private JButton btnVerPassword;
    private boolean passwordVisible = false;
    private JButton btnRolEmpleado;
    private JButton btnRolEncargado;
    private JLabel lblRolDescripcion;
    private boolean esEncargado = true;

    public LoginScreen() {
        super("Farmac - Acceso al Sistema");
        this.usuarioDAO = new UsuarioDAO();
        setIconImages(TemaFarmac.iconosApp());
        configurarVentana();
        inicializarComponentes();
        finalizarVentana();
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        setMinimumSize(new Dimension(
                Math.min(400, Math.max(320, pantalla.width - 40)),
                Math.min(540, Math.max(420, pantalla.height - 80))
        ));
    }

    /**
     * Empaqueta la ventana con su tamaño preferido y la limita al área útil
     * de la pantalla, para que también funcione en equipos pequeños.
     */
    private void finalizarVentana() {
        pack();
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int ancho = Math.min(Math.max(getWidth(), 400), pantalla.width - 40);
        int alto = Math.min(Math.max(getHeight(), 540), pantalla.height - 80);
        setSize(ancho, alto);
        setLocationRelativeTo(null);
    }

    private void inicializarComponentes() {
        JPanel pnlFondo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TemaFarmac.FONDO);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        pnlFondo.setLayout(new GridBagLayout());
        setContentPane(pnlFondo);

        // Tarjeta blanca central
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TemaFarmac.BLANCO);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.setColor(TemaFarmac.BORDE);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        // La tarjeta se adapta al alto útil de la pantalla para no recortarse
        // en equipos con resoluciones pequeñas.
        Dimension pantallaLogin = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        card.setPreferredSize(new Dimension(
                Math.min(500, Math.max(340, pantallaLogin.width - 80)),
                Math.min(560, Math.max(440, pantallaLogin.height - 140))));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(34, 48, 34, 48));

        // Título y Marca
        JComponent lblTitulo = TemaFarmac.crearLogo(new Color(0x1B, 0x3D, 0x7A), 44);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSlogan = new JLabel("Cada lote, bajo control.");
        lblSlogan.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 14));
        lblSlogan.setForeground(TemaFarmac.TEXTO_AZUL);
        lblSlogan.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Switch / Segmented Control como ayuda visual
        JPanel pnlSwitchContenedor = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xA9, 0xAC, 0xA9));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        pnlSwitchContenedor.setOpaque(false);
        pnlSwitchContenedor.setLayout(new GridLayout(1, 2, 4, 0));
        pnlSwitchContenedor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        pnlSwitchContenedor.setBorder(new EmptyBorder(4, 4, 4, 4));

        btnRolEmpleado = crearBotonSegmento("Empleado");
        btnRolEncargado = crearBotonSegmento("Encargado");

        btnRolEmpleado.addActionListener(e -> seleccionarRol(false));
        btnRolEncargado.addActionListener(e -> seleccionarRol(true));

        pnlSwitchContenedor.add(btnRolEmpleado);
        pnlSwitchContenedor.add(btnRolEncargado);

        lblRolDescripcion = new JLabel("Autenticación con permisos de Encargado", SwingConstants.CENTER);
        lblRolDescripcion.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblRolDescripcion.setForeground(TemaFarmac.TEXTO_AZUL);
        lblRolDescripcion.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Campos de entrada (sin valores precargados fijos)
        JLabel lblUser = new JLabel("Nombre de Usuario");
        lblUser.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblUser.setForeground(TemaFarmac.TEXTO_AZUL);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtUsuario = TemaFarmac.crearCampoTexto(20);
        txtUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtUsuario.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblPass = new JLabel("Contraseña");
        lblPass.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblPass.setForeground(TemaFarmac.TEXTO_AZUL);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = TemaFarmac.crearCampoPassword(20);
        txtPassword.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        txtPassword.addActionListener(e -> procesarLogin());

        btnVerPassword = new JButton();
        btnVerPassword.setIcon(FontIcon.of(FontAwesomeSolid.EYE_SLASH, 15, TemaFarmac.AZUL_PETROLEO));
        btnVerPassword.setContentAreaFilled(false);
        btnVerPassword.setBorderPainted(false);
        btnVerPassword.setFocusPainted(false);
        btnVerPassword.setFocusable(false);
        btnVerPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerPassword.setBorder(new EmptyBorder(0, 8, 0, 10));
        btnVerPassword.addActionListener(e -> alternarVerPassword());

        // Campo con borde redondeado que contiene el botón del ojo
        JPanel pnlPassword = new JPanel(new BorderLayout(0, 0));
        pnlPassword.setBackground(Color.WHITE);
        pnlPassword.setBorder(new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE_CAMPO, 8, 1));
        pnlPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        pnlPassword.setPreferredSize(new Dimension(300, 40));
        pnlPassword.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlPassword.add(txtPassword, BorderLayout.CENTER);
        pnlPassword.add(btnVerPassword, BorderLayout.EAST);

        btnIngresar = TemaFarmac.crearBoton("Ingresar al sistema", TemaFarmac.AZUL_ACCION, Color.WHITE);
        btnIngresar.setIcon(FontIcon.of(FontAwesomeSolid.SIGN_IN_ALT, 16, Color.WHITE));
        btnIngresar.setIconTextGap(8);
        btnIngresar.setFont(TemaFarmac.FUENTE_SUBTITULO);
        btnIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnIngresar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnIngresar.addActionListener(e -> procesarLogin());

        // Ensamblado
        card.add(lblTitulo);
        card.add(Box.createRigidArea(new Dimension(0, 2)));
        card.add(lblSlogan);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        card.add(pnlSwitchContenedor);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(lblRolDescripcion);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        card.add(envolverEtiqueta(lblUser));
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(txtUsuario);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        card.add(envolverEtiqueta(lblPass));
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(pnlPassword);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        card.add(btnIngresar);

        pnlFondo.add(card);

        // Navegación con Enter: usuario -> contraseña -> ingresar
        TemaFarmac.enterComoTab(this::procesarLogin, txtUsuario, txtPassword);

        // Estado inicial
        seleccionarRol(true);
        alternarVerPassword();
        alternarVerPassword();
    }

    /** Coloca una etiqueta alineada a la izquierda dentro de una fila de ancho completo. */
    private JPanel envolverEtiqueta(JLabel lbl) {
        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(false);
        fila.setAlignmentX(Component.CENTER_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        fila.add(lbl, BorderLayout.WEST);
        return fila;
    }

    private JButton crearBotonSegmento(String texto) {
        return new TemaFarmac.BotonSegmento(texto);
    }

    private void seleccionarRol(boolean encargado) {
        this.esEncargado = encargado;
        if (encargado) {
            txtUsuario.setText("admin");
            txtPassword.setText("admin123");
            lblRolDescripcion.setText("Autenticación con permisos de Encargado");
        } else {
            txtUsuario.setText("empleado1");
            txtPassword.setText("caja123");
            lblRolDescripcion.setText("Autenticación con permisos de Empleado");
        }
        actualizarEstiloSwitch();
    }

    private void actualizarEstiloSwitch() {
        ((TemaFarmac.BotonSegmento) btnRolEncargado).setActivo(esEncargado);
        ((TemaFarmac.BotonSegmento) btnRolEmpleado).setActivo(!esEncargado);
    }

    private void alternarVerPassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            txtPassword.setEchoChar((char) 0);
            btnVerPassword.setIcon(FontIcon.of(FontAwesomeSolid.EYE, 15, TemaFarmac.AZUL_PETROLEO));
        } else {
            txtPassword.setEchoChar('\u2022');
            btnVerPassword.setIcon(FontIcon.of(FontAwesomeSolid.EYE_SLASH, 15, TemaFarmac.AZUL_PETROLEO));
        }
    }

    private void procesarLogin() {
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            DialogoFarmac.mostrarJO(this, "Por favor ingrese usuario y contraseña.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Usuario u = usuarioDAO.buscarPorNombreUsuario(user);
            if (u == null) {
                DialogoFarmac.mostrarJO(this, "El usuario '" + user + "' no se encuentra registrado.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!u.isActivo()) {
                DialogoFarmac.mostrarJO(this, "La cuenta del usuario '" + user + "' está INACTIVA.\nContacte al Encargado para reactivarla.", "Cuenta Inactiva", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!u.autenticar(pass)) {
                DialogoFarmac.mostrarJO(this, "Contraseña incorrecta. Verifique sus credenciales.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Inicio de sesión exitoso
            Sesion.setUsuarioActivo(u);
            Sesion.refrescarTurno();

            // Construir la ventana principal antes de cerrar el login: si falla,
            // el usuario conserva la pantalla de acceso para reintentar.
            MainScreen principal = new MainScreen(u);
            principal.setVisible(true);
            dispose();

            BitacoraDAO.registrar(u, "LOGIN", "Inicio de sesión exitoso como " + u.obtenerRol());

        } catch (Exception ex) {
            DialogoFarmac.mostrarJO(this,
                    "No fue posible iniciar sesión: " + ex.getMessage()
                    + "\nVerifique que PostgreSQL esté activo en localhost:5432 y la configuración de la base.",
                    "Error al Iniciar Sesión", JOptionPane.ERROR_MESSAGE);
        }
    }
}
