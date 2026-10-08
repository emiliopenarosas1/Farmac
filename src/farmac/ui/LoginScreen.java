package farmac.ui;

import farmac.dominio.Empleado;
import farmac.dominio.Encargado;
import farmac.dominio.Usuario;
import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Pantalla de inicio de sesion con Switch / Selector interactivo de Rol (Empleado / Encargado).
 */
public class LoginScreen extends JFrame {
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    private JButton btnRolEmpleado;
    private JButton btnRolEncargado;
    private JLabel lblRolDescripcion;
    private boolean esEncargado = true; // Por defecto Encargado

    public LoginScreen() {
        super("Farmac - Acceso al Sistema");
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 600);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void inicializarComponentes() {
        JPanel pnlFondo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TemaFarmac.AZUL_PETROLEO);
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
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(380, 500));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Título y Marca
        JLabel lblTitulo = new JLabel("FARMAC");
        lblTitulo.setFont(TemaFarmac.FUENTE_DISPLAY);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSlogan = new JLabel("Cada lote, bajo control.");
        lblSlogan.setFont(TemaFarmac.FUENTE_REGULAR);
        lblSlogan.setForeground(TemaFarmac.TURQUESA);
        lblSlogan.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Switch / Segmented Control para selección de Rol
        JPanel pnlSwitchContenedor = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        pnlSwitchContenedor.setOpaque(false);
        pnlSwitchContenedor.setLayout(new GridLayout(1, 2, 4, 0));
        pnlSwitchContenedor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        pnlSwitchContenedor.setBorder(new EmptyBorder(3, 3, 3, 3));

        btnRolEmpleado = crearBotonSegmento("Empleado");
        btnRolEncargado = crearBotonSegmento("Encargado");

        btnRolEmpleado.addActionListener(e -> seleccionarRol(false));
        btnRolEncargado.addActionListener(e -> seleccionarRol(true));

        pnlSwitchContenedor.add(btnRolEmpleado);
        pnlSwitchContenedor.add(btnRolEncargado);

        lblRolDescripcion = new JLabel("Modo: Administracion e Inventario Completo", SwingConstants.CENTER);
        lblRolDescripcion.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblRolDescripcion.setForeground(TemaFarmac.TEXTO_MUTED);
        lblRolDescripcion.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Campos de entrada
        JLabel lblUser = new JLabel("Usuario");
        lblUser.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblUser.setForeground(TemaFarmac.TEXTO_AZUL);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtUsuario = TemaFarmac.crearCampoTexto("admin", 20);
        txtUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPass = new JLabel("Contrasena");
        lblPass.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblPass.setForeground(TemaFarmac.TEXTO_AZUL);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = TemaFarmac.crearCampoPassword("123456", 20);
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtPassword.addActionListener(e -> procesarLogin());

        btnIngresar = TemaFarmac.crearBoton("Ingresar al Sistema", TemaFarmac.TURQUESA, Color.WHITE);
        btnIngresar.setFont(TemaFarmac.FUENTE_SUBTITULO);
        btnIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnIngresar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnIngresar.addActionListener(e -> procesarLogin());

        // Ensamblado
        card.add(lblTitulo);
        card.add(Box.createRigidArea(new Dimension(0, 3)));
        card.add(lblSlogan);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        card.add(pnlSwitchContenedor);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(lblRolDescripcion);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        card.add(lblUser);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(txtUsuario);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        card.add(lblPass);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(txtPassword);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        card.add(btnIngresar);

        pnlFondo.add(card);

        // Estado inicial
        actualizarEstiloSwitch();
    }

    private JButton crearBotonSegmento(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(TemaFarmac.FUENTE_NEGRITA);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void seleccionarRol(boolean encargado) {
        this.esEncargado = encargado;
        if (encargado) {
            txtUsuario.setText("admin");
            lblRolDescripcion.setText("Modo: Administracion e Inventario Completo");
        } else {
            txtUsuario.setText("empleado1");
            lblRolDescripcion.setText("Modo: Punto de Venta y Atencion al Cliente");
        }
        actualizarEstiloSwitch();
    }

    private void actualizarEstiloSwitch() {
        if (esEncargado) {
            btnRolEncargado.setOpaque(true);
            btnRolEncargado.setBackground(TemaFarmac.AZUL_PETROLEO);
            btnRolEncargado.setForeground(Color.WHITE);

            btnRolEmpleado.setOpaque(false);
            btnRolEmpleado.setBackground(new Color(0xE2, 0xE8, 0xF0));
            btnRolEmpleado.setForeground(TemaFarmac.TEXTO_MUTED);
        } else {
            btnRolEmpleado.setOpaque(true);
            btnRolEmpleado.setBackground(TemaFarmac.TURQUESA);
            btnRolEmpleado.setForeground(Color.WHITE);

            btnRolEncargado.setOpaque(false);
            btnRolEncargado.setBackground(new Color(0xE2, 0xE8, 0xF0));
            btnRolEncargado.setForeground(TemaFarmac.TEXTO_MUTED);
        }
        btnRolEmpleado.repaint();
        btnRolEncargado.repaint();
    }

    private void procesarLogin() {
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese usuario y contrasena.", "Validacion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Usuario usuarioLogueado;
        if (esEncargado) {
            usuarioLogueado = new Encargado(1, user, "Administrador General", pass, true);
        } else {
            usuarioLogueado = new Empleado(2, user, "Carlos Lopez", pass, true);
        }

        dispose();
        new MainScreen(usuarioLogueado).setVisible(true);
    }
}
