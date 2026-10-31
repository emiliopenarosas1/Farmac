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
 * Ventana modal para captura y validaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n estricta de Recetas MÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©dicas obligatorias (COFEPRIS, RF14, RNF09).
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
        super(parent, "Registro Obligatorio de Receta MÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©dica", true);
        this.productoAmparado = producto;
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setSize(480, 460);
        setLocationRelativeTo(getParent());
        setResizable(false);
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

        JLabel lblSub = new JLabel("Este producto requiere receta mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©dica vigente (mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡x. 30 dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­as de expediciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n) - RF14");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        pnlHeader.add(lblTit);
        pnlHeader.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlHeader.add(lblSub);
        pnlFondo.add(pnlHeader, BorderLayout.NORTH);

        // Formulario central
        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        // Medicamento
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblMed = new JLabel("Medicamento Amparado:");
        lblMed.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblMed, gbc);

        lblMedicamento = new JLabel(productoAmparado != null ? productoAmparado.getNombre() : "Medicamento Controlado");
        lblMedicamento.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblMedicamento.setForeground(TemaFarmac.AZUL_PETROLEO);
        gbc.gridx = 1;
        pnlCard.add(lblMedicamento, gbc);

        // MÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©dico
        gbc.gridy = 1; gbc.gridx = 0;
        JLabel lblM = new JLabel("Nombre Completo del MÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©dico *:");
        lblM.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblM, gbc);

        txtMedico = TemaFarmac.crearCampoTexto(18);
        txtMedico.putClientProperty("JTextField.placeholderText", "Ej. Dr. Alejandro GÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³mez");
        gbc.gridx = 1;
        pnlCard.add(txtMedico, gbc);

        // CÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©dula
        gbc.gridy = 2; gbc.gridx = 0;
        JLabel lblC = new JLabel("CÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©dula Profesional *:");
        lblC.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblC, gbc);

        txtCedula = TemaFarmac.crearCampoTexto(18);
        txtCedula.putClientProperty("JTextField.placeholderText", "Ej. 7481920");
        gbc.gridx = 1;
        pnlCard.add(txtCedula, gbc);

        // Folio
        gbc.gridy = 3; gbc.gridx = 0;
        JLabel lblF = new JLabel("Folio de Receta *:");
        lblF.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblF, gbc);

        txtFolio = TemaFarmac.crearCampoTexto(18);
        txtFolio.putClientProperty("JTextField.placeholderText", "Ej. FOL-2026-0045");
        gbc.gridx = 1;
        pnlCard.add(txtFolio, gbc);

        // Fecha de ExpediciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n
        gbc.gridy = 4; gbc.gridx = 0;
        JLabel lblFecha = new JLabel("Fecha de ExpediciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n (AAAA-MM-DD) *:");
        lblFecha.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblFecha, gbc);

        txtFechaExpedicion = TemaFarmac.crearCampoTexto(18);
        txtFechaExpedicion.setText(LocalDate.now().toString());
        gbc.gridx = 1;
        pnlCard.add(txtFechaExpedicion, gbc);

        pnlFondo.add(pnlCard, BorderLayout.CENTER);

        // Botones inferiores
        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBotones.setOpaque(false);

        JButton btnCancelar = TemaFarmac.crearBoton("Cancelar Venta del ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âtem", TemaFarmac.BORDE, TemaFarmac.TEXTO);
        btnCancelar.addActionListener(e -> {
            confirmado = false;
            dispose();
        });

        JButton btnGuardar = TemaFarmac.crearBoton("Validar y Autorizar ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âtem", TemaFarmac.VERDE, Color.WHITE);
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
            DialogoFarmac.mostrarJO(this, "Todos los datos de la receta mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©dica son obligatorios por ley (COFEPRIS).", "Receta Incompleta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate fechaExp;
        try {
            fechaExp = LocalDate.parse(fechaStr);
        } catch (DateTimeParseException ex) {
            DialogoFarmac.mostrarJO(this, "Formato de fecha invÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡lido. Utilice el formato AAAA-MM-DD.", "Formato InvÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡lido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate hoy = LocalDate.now();
        if (fechaExp.isAfter(hoy)) {
            DialogoFarmac.mostrarJO(this, "La fecha de expediciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n no puede ser una fecha futura.", "Fecha InvÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡lida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        recetaRegistrada = new RecetaMedica(0, productoAmparado, medico, cedula, fechaExp, folio);

        // ValidaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n de vigencia de 30 dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­as (COFEPRIS / RF14)
        if (!recetaRegistrada.estaVigente(hoy)) {
            DialogoFarmac.mostrarJO(this,
                    "La receta mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©dica estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ VENCIDA.\nHan transcurrido mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡s de 30 dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­as naturales desde su expediciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n (" + fechaExp + ").\n"
                    + "Por disposiciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n legal no se puede surtir este medicamento.",
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
