package farmac.ui;

import farmac.dominio.Producto;
import farmac.dominio.RecetaMedica;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * Ventana modal para captura y validación estricta de Recetas Médicas obligatorias (COFEPRIS).
 */
public class RegistroRecetaDialog extends JDialog {

    private JTextField txtMedico;
    private JTextField txtCedula;
    private JTextField txtFolio;
    private JTextField txtFechaExpedicion;
    private JLabel lblMedicamento;
    private boolean confirmado = false;
    private RecetaMedica recetaRegistrada;
    private final Producto productoAmparado;

    public RegistroRecetaDialog(Frame parent, Producto producto) {
        super(parent, "Registro Obligatorio de Receta Médica", true);
        this.productoAmparado = producto;
        configurarVentana();
        inicializarComponentes();
        finalizarVentana();
    }

    private void configurarVentana() {
        setResizable(true);
        Dimension pantalla = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setMinimumSize(new Dimension(
                Math.min(420, Math.max(320, pantalla.width - 40)),
                Math.min(360, Math.max(300, pantalla.height - 80))
        ));
    }

    /**
     * Empaqueta con el tamaño preferido y lo limita al área útil de la pantalla
     * para que también quepa en equipos pequeños.
     */
    private void finalizarVentana() {
        pack();
        Dimension pantalla = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int ancho = Math.min(Math.max(getWidth(), 640), pantalla.width - 40);
        int alto = Math.min(Math.max(getHeight(), 520), pantalla.height - 80);
        setSize(ancho, alto);
        setLocationRelativeTo(getParent());
    }

    private void inicializarComponentes() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Color.WHITE);
        setContentPane(raiz);

        // Cabecera
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TemaFarmac.BORDE),
                new EmptyBorder(14, 22, 12, 22)));
        JPanel pnlTit = new JPanel();
        pnlTit.setOpaque(false);
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        JLabel lblTit = new JLabel("Registro obligatorio de receta médica");
        lblTit.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 17));
        lblTit.setForeground(TemaFarmac.TEXTO_AZUL);
        JLabel lblSub = new JLabel("Control de medicamento regulado");
        lblSub.setFont(TemaFarmac.FUENTE_REGULAR);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);
        pnlTit.add(lblTit);
        pnlTit.add(lblSub);
        JLabel lblEtiqueta = new JLabel("Medicamento controlado");
        lblEtiqueta.setFont(TemaFarmac.FUENTE_REGULAR);
        lblEtiqueta.setForeground(TemaFarmac.AMBAR);
        lblEtiqueta.setIcon(new javax.swing.Icon() {
            @Override public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TemaFarmac.AMBAR);
                g2.fillOval(x, y, 8, 8);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 8; }
            @Override public int getIconHeight() { return 8; }
        });
        lblEtiqueta.setIconTextGap(6);
        pnlHeader.add(pnlTit, BorderLayout.WEST);
        pnlHeader.add(lblEtiqueta, BorderLayout.EAST);
        raiz.add(pnlHeader, BorderLayout.NORTH);

        // Campos
        txtMedico = TemaFarmac.crearCampoTexto(18);
        txtMedico.putClientProperty("JTextField.placeholderText", "Ej. Dr. Alejandro Gómez");
        txtCedula = TemaFarmac.crearCampoTexto(18);
        txtCedula.putClientProperty("JTextField.placeholderText", "Ej. 7481920");
        txtFolio = TemaFarmac.crearCampoTexto(18);
        txtFolio.putClientProperty("JTextField.placeholderText", "Ej. FOL-2026-0045");
        txtFechaExpedicion = TemaFarmac.crearCampoTexto(18);
        txtFechaExpedicion.setText(LocalDate.now().toString());

        // Medicamento amparado (solo lectura)
        lblMedicamento = new JLabel(productoAmparado != null ? productoAmparado.getNombre() : "Medicamento controlado");
        lblMedicamento.setFont(TemaFarmac.FUENTE_REGULAR);
        lblMedicamento.setForeground(TemaFarmac.TEXTO_MUTED);
        JPanel pnlMed = new JPanel(new BorderLayout());
        pnlMed.setBackground(new Color(0xE3, 0xE4, 0xE5));
        pnlMed.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(new Color(0xB0, 0xB4, 0xB8), 8, 1), new EmptyBorder(8, 12, 8, 12)));
        pnlMed.setPreferredSize(new Dimension(200, 38));
        pnlMed.add(lblMedicamento, BorderLayout.CENTER);

        JPanel form = TemaFarmac.crearFormularioGrid(
                new String[]{
                    "Medicamento amparado",
                    "Nombre completo del médico *",
                    "Cédula profesional *",
                    "Folio de receta *",
                    "Fecha de expedición (AAAA-MM-DD) *"
                },
                new javax.swing.JComponent[]{pnlMed, txtMedico, txtCedula, txtFolio, txtFechaExpedicion},
                new int[]{2, 2, 1, 1, 1});

        JPanel pnlCuerpo = new JPanel();
        pnlCuerpo.setOpaque(false);
        pnlCuerpo.setLayout(new BoxLayout(pnlCuerpo, BoxLayout.Y_AXIS));
        pnlCuerpo.setBorder(new EmptyBorder(18, 22, 14, 22));
        JPanel aviso = TemaFarmac.crearAviso("Este producto requiere receta médica vigente (máx. 30 días de expedición).",
                TemaFarmac.AMBAR_BG, TemaFarmac.AMBAR);
        aviso.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        aviso.setMaximumSize(new Dimension(Integer.MAX_VALUE, aviso.getPreferredSize().height));
        form.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel lblLey = new JLabel("Todos los datos de la receta médica son obligatorios por ley (COFEPRIS).");
        lblLey.setFont(TemaFarmac.FUENTE_REGULAR);
        lblLey.setForeground(TemaFarmac.TEXTO_MUTED);
        lblLey.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pnlCuerpo.add(aviso);
        pnlCuerpo.add(Box.createRigidArea(new Dimension(0, 14)));
        pnlCuerpo.add(form);
        pnlCuerpo.add(Box.createRigidArea(new Dimension(0, 12)));
        pnlCuerpo.add(lblLey);
        raiz.add(TemaFarmac.crearScrollResponsivo(pnlCuerpo), BorderLayout.CENTER);

        // Botones inferiores
        JPanel pnlBotones = new JPanel(new BorderLayout());
        pnlBotones.setOpaque(false);
        pnlBotones.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, TemaFarmac.BORDE),
                new EmptyBorder(14, 22, 14, 22)));

        JButton btnCancelar = TemaFarmac.crearBotonPeligro("Cancelar venta");
        btnCancelar.addActionListener(e -> {
            confirmado = false;
            dispose();
        });

        JButton btnGuardar = TemaFarmac.crearBoton("Validar y autorizar", TemaFarmac.TURQUESA, Color.WHITE);
        btnGuardar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHECK, 12, Color.WHITE));
        btnGuardar.setIconTextGap(8);
        btnGuardar.addActionListener(e -> validarYGuardar());

        pnlBotones.add(btnCancelar, BorderLayout.WEST);
        pnlBotones.add(btnGuardar, BorderLayout.EAST);
        raiz.add(pnlBotones, BorderLayout.SOUTH);

        // Navegación con Enter: médico -> cédula -> folio -> fecha -> validar y autorizar
        TemaFarmac.enterComoTab(btnGuardar::doClick, txtMedico, txtCedula, txtFolio, txtFechaExpedicion);
        getRootPane().registerKeyboardAction(e -> {
            confirmado = false;
            dispose();
        }, javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.SwingUtilities.invokeLater(() -> txtMedico.requestFocusInWindow());
    }

    private void validarYGuardar() {
        String medico = txtMedico.getText().trim();
        String cedula = txtCedula.getText().trim();
        String folio = txtFolio.getText().trim();
        String fechaStr = txtFechaExpedicion.getText().trim();

        if (medico.isEmpty() || cedula.isEmpty() || folio.isEmpty() || fechaStr.isEmpty()) {
            DialogoFarmac.mostrarJO(this, "Todos los datos de la receta médica son obligatorios por ley (COFEPRIS).", "Receta Incompleta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate fechaExp;
        try {
            fechaExp = LocalDate.parse(fechaStr);
        } catch (DateTimeParseException ex) {
            DialogoFarmac.mostrarJO(this, "Formato de fecha inválido. Utilice el formato AAAA-MM-DD.", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate hoy = LocalDate.now();
        if (fechaExp.isAfter(hoy)) {
            DialogoFarmac.mostrarJO(this, "La fecha de expedición no puede ser una fecha futura.", "Fecha Inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        recetaRegistrada = new RecetaMedica(0, productoAmparado, medico, cedula, fechaExp, folio);

        // Validación de vigencia de 30 días (COFEPRIS)
        if (!recetaRegistrada.estaVigente(hoy)) {
            DialogoFarmac.mostrarJO(this,
                    "La receta médica está VENCIDA.\nHan transcurrido más de 30 días naturales desde su expedición (" + fechaExp + ").\n"
                    + "Por disposición legal no se puede surtir este medicamento.",
                    "Receta Vencida", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!recetaRegistrada.validarDatos()) {
            DialogoFarmac.mostrarJO(this, "Los datos capturados no cumplen los requisitos de validez.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public RecetaMedica getRecetaRegistrada() {
        return recetaRegistrada;
    }
}
