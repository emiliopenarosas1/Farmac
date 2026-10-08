package farmac.ui;

import farmac.dominio.Lote;
import farmac.dominio.Producto;
import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

/**
 * Pantalla para recepcion y registro de lotes de medicamentos.
 */
public class RecepcionInventarioScreen extends JPanel {
    private JTextField txtNumLote;
    private JTextField txtCaducidad;
    private JTextField txtCantidad;
    private JTextField txtPrecioCompra;
    private JComboBox<String> cmbProductos;

    public RecepcionInventarioScreen() {
        inicializarUI();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(TemaFarmac.FONDO);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);

        JLabel lblTitulo = new JLabel("Recepcion de Mercancia por Lote");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Ingreso de nuevas partidas con numero de lote, fecha de vencimiento y factura");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        JPanel pnlTit = new JPanel();
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        pnlTit.setOpaque(false);
        pnlTit.add(lblTitulo);
        pnlTit.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlTit.add(lblSub);

        pnlHeader.add(pnlTit, BorderLayout.WEST);
        add(pnlHeader, BorderLayout.NORTH);

        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new GridBagLayout());
        pnlCard.setMaximumSize(new Dimension(650, 440));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblProd = new JLabel("Medicamento / Producto:");
        lblProd.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblProd, gbc);

        cmbProductos = new JComboBox<>(new String[]{"Amoxicilina 500mg", "Paracetamol 500mg", "Ibuprofeno 400mg", "Omeprazol 20mg"});
        cmbProductos.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbProductos.setBackground(Color.WHITE);
        cmbProductos.setPreferredSize(new Dimension(280, 36));
        gbc.gridx = 1;
        pnlCard.add(cmbProductos, gbc);

        gbc.gridy = 1; gbc.gridx = 0;
        JLabel lblLote = new JLabel("Numero de Lote:");
        lblLote.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblLote, gbc);

        txtNumLote = TemaFarmac.crearCampoTexto("LT-2026-N01", 15);
        gbc.gridx = 1;
        pnlCard.add(txtNumLote, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        JLabel lblCad = new JLabel("Fecha de Caducidad (AAAA-MM-DD):");
        lblCad.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblCad, gbc);

        txtCaducidad = TemaFarmac.crearCampoTexto("2027-11-30", 15);
        gbc.gridx = 1;
        pnlCard.add(txtCaducidad, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        JLabel lblCant = new JLabel("Cantidad de Piezas Recibidas:");
        lblCant.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblCant, gbc);

        txtCantidad = TemaFarmac.crearCampoTexto("100", 15);
        gbc.gridx = 1;
        pnlCard.add(txtCantidad, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        JLabel lblPrec = new JLabel("Precio Unitario de Compra:");
        lblPrec.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblPrec, gbc);

        txtPrecioCompra = TemaFarmac.crearCampoTexto("45.00", 15);
        gbc.gridx = 1;
        pnlCard.add(txtPrecioCompra, gbc);

        JButton btnGuardar = TemaFarmac.crearBoton("Guardar Entrada de Lote", TemaFarmac.VERDE, Color.WHITE);
        btnGuardar.setPreferredSize(new Dimension(280, 40));
        btnGuardar.addActionListener(e -> registrar());

        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 12, 8, 12);
        pnlCard.add(btnGuardar, gbc);

        JPanel pnlContenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
        pnlContenedor.setOpaque(false);
        pnlContenedor.add(pnlCard);

        add(pnlContenedor, BorderLayout.CENTER);
    }

    private void registrar() {
        String lote = txtNumLote.getText().trim();
        if (lote.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe especificar el numero de lote.", "Validacion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Producto p = new Producto(1, "75010001", (String) cmbProductos.getSelectedItem(), "General", "Nacional", 85.0, false, 5);
        Lote nuevoLote = new Lote(1, lote, LocalDate.parse(txtCaducidad.getText()), Integer.parseInt(txtCantidad.getText()), Double.parseDouble(txtPrecioCompra.getText()), p);
        JOptionPane.showMessageDialog(this, "Lote " + nuevoLote.getNumeroLote() + " registrado exitosamente en el inventario.", "Operacion Exitosa", JOptionPane.INFORMATION_MESSAGE);
    }
}
