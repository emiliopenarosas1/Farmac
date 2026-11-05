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
        int ancho = Math.min(Math.max(getWidth(), 460), pantalla.width - 40);
        int alto = Math.min(Math.max(getHeight(), 400), pantalla.height - 80);
        setSize(ancho, alto);
        setLocationRelativeTo(getParent());
    }

    private void inicializarComponentes() {
        JPanel pnlFondo = new JPanel(new BorderLayout(12, 12));
        pnlFondo.setBackground(TemaFarmac.GRIS_FONDO);
        pnlFondo.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(pnlFondo);

        // Cabecera
        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setOpaque(false);

        JLabel lblTit = new JLabel("Control de Medicamento Regulado");
        lblTit.setFont(TemaFarmac.FUENTE_TITULO);
        lblTit.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Este producto requiere receta médica vigente (máx. 30 días de expedición)");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        pnlHeader.add(lblTit);
        pnlHeader.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlHeader.add(lblSub);
        pnlFondo.add(pnlHeader, BorderLayout.NORTH);

        // Formulario central (layout con pesos y scroll vertical)
        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new BorderLayout());

        lblMedicamento = new JLabel(productoAmparado != null ? productoAmparado.getNombre() : "Medicamento Controlado");
        lblMedicamento.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblMedicamento.setForeground(TemaFarmac.AZUL_PETROLEO);

        txtMedico = TemaFarmac.crearCampoTexto(18);
        txtMedico.putClientProperty("JTextField.placeholderText", "Ej. Dr. Alejandro Gómez");

        txtCedula = TemaFarmac.crearCampoTexto(18);
        txtCedula.putClientProperty("JTextField.placeholderText", "Ej. 7481920");

        txtFolio = TemaFarmac.crearCampoTexto(18);
        txtFolio.putClientProperty("JTextField.placeholderText", "Ej. FOL-2026-0045");

        txtFechaExpedicion = TemaFarmac.crearCampoTexto(18);
        txtFechaExpedicion.setText(LocalDate.now().toString());

        JPanel form = TemaFarmac.crearFormulario(
                new String[]{
                    "Medicamento Amparado:",
                    "Nombre Completo del Médico *:",
                    "Cédula Profesional *:",
                    "Folio de Receta *:",
                    "Fecha de Expedición (AAAA-MM-DD) *:"
                },
                new javax.swing.JComponent[]{
                    lblMedicamento, txtMedico, txtCedula, txtFolio, txtFechaExpedicion
                });
        pnlCard.add(form, BorderLayout.CENTER);

        pnlFondo.add(TemaFarmac.crearScrollFormulario(pnlCard), BorderLayout.CENTER);

        // Botones inferiores
        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBotones.setOpaque(false);

        JButton btnCancelar = TemaFarmac.crearBoton("Cancelar Venta del Ítem", TemaFarmac.BORDE, TemaFarmac.TEXTO);
        btnCancelar.addActionListener(e -> {
            confirmado = false;
            dispose();
        });

        JButton btnGuardar = TemaFarmac.crearBoton("Validar y Autorizar Ítem", TemaFarmac.VERDE, Color.WHITE);
        btnGuardar.addActionListener(e -> validarYGuardar());

        pnlBotones.add(btnCancelar);
        pnlBotones.add(btnGuardar);
        pnlFondo.add(pnlBotones, BorderLayout.SOUTH);
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
