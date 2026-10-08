package farmac.ui;

import farmac.dominio.Producto;
import farmac.dominio.RecetaMedica;
import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Ventana modal para captura y validacion de Recetas Medicas obligatorias (COFEPRIS).
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
        super(parent, "Registro Obligatorio de Receta Medica", true);
        this.productoAmparado = producto;
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setSize(460, 440);
        setLocationRelativeTo(getParent());
        setResizable(false);
    }

    private void inicializarComponentes() {
        JPanel pnlFondo = new JPanel(new BorderLayout(12, 12));
        pnlFondo.setBackground(TemaFarmac.FONDO);
        pnlFondo.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(pnlFondo);

        // Cabecera
        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setOpaque(false);

        JLabel lblTit = new JLabel("Control de Medicamento con Receta");
        lblTit.setFont(TemaFarmac.FUENTE_TITULO);
        lblTit.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Este medicamento requiere registro de receta para autorizar la venta.");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        pnlHeader.add(lblTit);
        pnlHeader.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlHeader.add(lblSub);
        pnlFondo.add(pnlHeader, BorderLayout.NORTH);

        // Tarjeta central con formulario
        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblMed = new JLabel("Medicamento:");
        lblMed.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblMed, gbc);

        lblMedicamento = new JLabel(productoAmparado != null ? productoAmparado.getNombre() : "Medicamento");
        lblMedicamento.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblMedicamento.setForeground(TemaFarmac.AZUL_PETROLEO);
        gbc.gridx = 1;
        pnlCard.add(lblMedicamento, gbc);

        gbc.gridy = 1; gbc.gridx = 0;
        JLabel lblM = new JLabel("Nombre del Medico:");
        lblM.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblM, gbc);

        txtMedico = TemaFarmac.crearCampoTexto("Dr. Roberto Garza Morales", 18);
        gbc.gridx = 1;
        pnlCard.add(txtMedico, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        JLabel lblC = new JLabel("Cedula Profesional:");
        lblC.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblC, gbc);

        txtCedula = TemaFarmac.crearCampoTexto("MED-884920", 18);
        gbc.gridx = 1;
        pnlCard.add(txtCedula, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        JLabel lblF = new JLabel("Folio de la Receta:");
        lblF.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblF, gbc);

        txtFolio = TemaFarmac.crearCampoTexto("REC-2026-991", 18);
        gbc.gridx = 1;
        pnlCard.add(txtFolio, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        JLabel lblFecha = new JLabel("Fecha Expedicion (AAAA-MM-DD):");
        lblFecha.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblFecha, gbc);

        txtFechaExpedicion = TemaFarmac.crearCampoTexto(LocalDate.now().toString(), 18);
        gbc.gridx = 1;
        pnlCard.add(txtFechaExpedicion, gbc);

        pnlFondo.add(pnlCard, BorderLayout.CENTER);

        // Botones inferiores
        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBotones.setOpaque(false);

        JButton btnCancelar = TemaFarmac.crearBoton("Cancelar Venta", TemaFarmac.BORDE, TemaFarmac.TEXTO);
        btnCancelar.addActionListener(e -> {
            confirmado = false;
            dispose();
        });

        JButton btnGuardar = TemaFarmac.crearBoton("Validar y Proceder al Cobro", TemaFarmac.VERDE, Color.WHITE);
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
            JOptionPane.showMessageDialog(this, "Todos los datos de la receta medica son obligatorios.", "Receta Incompleta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate fechaExp;
        try {
            fechaExp = LocalDate.parse(fechaStr);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Formato de fecha invalido. Use AAAA-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        recetaRegistrada = new RecetaMedica(1, productoAmparado, medico, cedula, fechaExp, folio);
        if (!recetaRegistrada.validarDatos()) {
            JOptionPane.showMessageDialog(this, "Los datos de la receta no son validos.", "Error", JOptionPane.ERROR_MESSAGE);
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
