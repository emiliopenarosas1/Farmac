package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.LoteDAO;
import farmac.dao.MovimientoDAO;
import farmac.dao.ProductoDAO;
import farmac.dao.ProveedorDAO;
import farmac.dominio.Lote;
import farmac.dominio.MovimientoInventario;
import farmac.dominio.Producto;
import farmac.dominio.Proveedor;
import farmac.util.Sesion;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla para recepción y registro de lotes de medicamentos en PostgreSQL (RF07, RF08).
 */
public class RecepcionInventarioScreen extends JPanel {

    private final ProductoDAO productoDAO;
    private final ProveedorDAO proveedorDAO;
    private final LoteDAO loteDAO;
    private final MovimientoDAO movimientoDAO;

    private JComboBox<Producto> cmbProductos;
    private JComboBox<Proveedor> cmbProveedores;
    private JTextField txtNumLote;
    private JTextField txtCaducidad;
    private JTextField txtCantidad;
    private JTextField txtPrecioCompra;
    private JTextField txtFactura;

    public RecepcionInventarioScreen() {
        this.productoDAO = new ProductoDAO();
        this.proveedorDAO = new ProveedorDAO();
        this.loteDAO = new LoteDAO();
        this.movimientoDAO = new MovimientoDAO();

        inicializarUI();
        cargarCombos();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);

        JLabel lblTitulo = new JLabel("Recepción de Mercancía por Lote");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Registro formal de partidas con validación de caducidad futura, proveedor y remisión (RF07, RF08)");
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
        pnlCard.setPreferredSize(new Dimension(680, 480));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        // 1. Producto
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblProd = new JLabel("Medicamento / Producto *:");
        lblProd.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblProd, gbc);

        cmbProductos = new JComboBox<>();
        cmbProductos.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbProductos.setBackground(Color.WHITE);
        cmbProductos.setPreferredSize(new Dimension(320, 36));
        gbc.gridx = 1;
        pnlCard.add(cmbProductos, gbc);

        // 2. Proveedor
        gbc.gridy = 1; gbc.gridx = 0;
        JLabel lblProv = new JLabel("Proveedor / Distribuidor *:");
        lblProv.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblProv, gbc);

        cmbProveedores = new JComboBox<>();
        cmbProveedores.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbProveedores.setBackground(Color.WHITE);
        cmbProveedores.setPreferredSize(new Dimension(320, 36));
        gbc.gridx = 1;
        pnlCard.add(cmbProveedores, gbc);

        // 3. Número de Lote
        gbc.gridy = 2; gbc.gridx = 0;
        JLabel lblLote = new JLabel("Número de Lote *:");
        lblLote.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblLote, gbc);

        txtNumLote = TemaFarmac.crearCampoTexto(18);
        gbc.gridx = 1;
        pnlCard.add(txtNumLote, gbc);

        // 4. Caducidad
        gbc.gridy = 3; gbc.gridx = 0;
        JLabel lblCad = new JLabel("Fecha de Caducidad (AAAA-MM-DD) *:");
        lblCad.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblCad, gbc);

        txtCaducidad = TemaFarmac.crearCampoTexto(18);
        txtCaducidad.putClientProperty("JTextField.placeholderText", "Ej. 2027-12-31");
        gbc.gridx = 1;
        pnlCard.add(txtCaducidad, gbc);

        // 5. Cantidad
        gbc.gridy = 4; gbc.gridx = 0;
        JLabel lblCant = new JLabel("Piezas Recibidas *:");
        lblCant.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblCant, gbc);

        txtCantidad = TemaFarmac.crearCampoTexto(18);
        gbc.gridx = 1;
        pnlCard.add(txtCantidad, gbc);

        // 6. Precio Compra
        gbc.gridy = 5; gbc.gridx = 0;
        JLabel lblPrec = new JLabel("Precio Unitario Compra ($) *:");
        lblPrec.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblPrec, gbc);

        txtPrecioCompra = TemaFarmac.crearCampoTexto(18);
        gbc.gridx = 1;
        pnlCard.add(txtPrecioCompra, gbc);

        // 7. Factura / Remisión
        gbc.gridy = 6; gbc.gridx = 0;
        JLabel lblFact = new JLabel("Factura / Remisión:");
        lblFact.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblFact, gbc);

        txtFactura = TemaFarmac.crearCampoTexto(18);
        gbc.gridx = 1;
        pnlCard.add(txtFactura, gbc);

        // Botón Guardar
        JButton btnGuardar = TemaFarmac.crearBoton("Guardar Entrada en Inventario", TemaFarmac.VERDE, Color.WHITE);
        btnGuardar.setPreferredSize(new Dimension(320, 42));
        btnGuardar.addActionListener(e -> registrar());

        gbc.gridy = 7; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 12, 8, 12);
        pnlCard.add(btnGuardar, gbc);

        JPanel pnlContenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
        pnlContenedor.setOpaque(false);
        pnlContenedor.add(pnlCard);

        add(pnlContenedor, BorderLayout.CENTER);
    }

    public void cargarCombos() {
        try {
            List<Producto> prods = productoDAO.listarTodos(true);
            cmbProductos.removeAllItems();
            for (Producto p : prods) {
                cmbProductos.addItem(p);
            }

            List<Proveedor> provs = proveedorDAO.listarTodos(true);
            cmbProveedores.removeAllItems();
            for (Proveedor pr : provs) {
                cmbProveedores.addItem(pr);
            }
        } catch (SQLException e) {
DialogoFarmac.mostrar(this, "Error", "Error al cargar catálogos: " + e.getMessage(), DialogoFarmac.Tipo.ERROR);
        }
    }

    private void registrar() {
        Producto p = (Producto) cmbProductos.getSelectedItem();
        Proveedor prov = (Proveedor) cmbProveedores.getSelectedItem();
        String lote = txtNumLote.getText().trim();
        String cadStr = txtCaducidad.getText().trim();
        String cantStr = txtCantidad.getText().trim();
        String precioStr = txtPrecioCompra.getText().trim();
        String factura = txtFactura.getText().trim();

        if (p == null || prov == null || lote.isEmpty() || cadStr.isEmpty() || cantStr.isEmpty() || precioStr.isEmpty()) {
            DialogoFarmac.mostrarJO(this, "Por favor complete todos los campos obligatorios (*).", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate caducidad;
        try {
            caducidad = LocalDate.parse(cadStr);
            if (!caducidad.isAfter(LocalDate.now())) {
                DialogoFarmac.mostrarJO(this, "La fecha de caducidad debe ser futura a la fecha actual.", "Caducidad Inválida", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception e) {
            DialogoFarmac.mostrarJO(this, "Formato de fecha inválido. Utilice el formato AAAA-MM-DD (ej. 2027-10-15).", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantStr);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            DialogoFarmac.mostrarJO(this, "La cantidad de piezas debe ser un número entero mayor a 0.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double precioCompra;
        try {
            precioCompra = Double.parseDouble(precioStr);
            if (precioCompra <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            DialogoFarmac.mostrarJO(this, "El precio de compra unitario debe ser mayor a 0.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Lote nuevoLote = new Lote(0, lote, caducidad, cantidad, cantidad, precioCompra, p, prov, LocalDate.now(), factura);
            loteDAO.insertar(nuevoLote);

            // Registrar movimiento de auditoría
            MovimientoInventario mov = new MovimientoInventario(
                    0, nuevoLote, "RECEPCION", cantidad, "Entrada de mercancía / factura: " + factura,
                    factura.isEmpty() ? "REC-" + nuevoLote.getId() : factura,
                    Sesion.getUsuarioActivo(), LocalDateTime.now()
            );
            movimientoDAO.insertar(mov, null);

            BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "INVENTARIO_RECEPCION",
                    "Recepción de lote " + lote + " (" + cantidad + " pzas) para " + p.getNombre());

            DialogoFarmac.mostrarJO(this,
                    "Lote '" + lote + "' registrado exitosamente en el inventario con " + cantidad + " piezas.",
                    "Recepción Completada", JOptionPane.INFORMATION_MESSAGE);

            // Limpiar campos
            txtNumLote.setText("");
            txtCaducidad.setText("");
            txtCantidad.setText("");
            txtPrecioCompra.setText("");
            txtFactura.setText("");

        } catch (SQLException ex) {
            DialogoFarmac.mostrarJO(this, "Error al guardar lote en base de datos: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }
}
