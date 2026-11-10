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
 * Pantalla para recepción y registro de lotes de medicamentos en PostgreSQL.
 */
public class RecepcionInventarioScreen extends JPanel {

    private final ProductoDAO productoDAO;
    private final ProveedorDAO proveedorDAO;
    private final LoteDAO loteDAO;
    private final MovimientoDAO movimientoDAO;

    private JComboBox<Producto> cmbProductos;
    private JComboBox<Proveedor> cmbProveedores;
    private JTextField txtNumLote;
    private farmac.util.SelectorFecha selCaducidad;
    private JTextField txtCantidad;
    private JTextField txtPrecioCompra;
    private JTextField txtFactura;

    public RecepcionInventarioScreen() {
        this.productoDAO = new ProductoDAO();
        this.proveedorDAO = new ProveedorDAO();
        this.loteDAO = new LoteDAO();
        this.movimientoDAO = new MovimientoDAO();

        inicializarUI();
        // Cargar después de pintar la pantalla, sin bloquear la navegación
        javax.swing.SwingUtilities.invokeLater(this::cargarCombos);
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 12));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(20, 26, 20, 26));

        JLabel lblTitulo = new JLabel("Recepción de mercancía por lote");
        lblTitulo.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 20));
        lblTitulo.setForeground(TemaFarmac.TEXTO_AZUL);
        add(lblTitulo, BorderLayout.NORTH);

        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new BorderLayout(0, 16));
        pnlCard.setBorder(new EmptyBorder(18, 18, 18, 18));

        cmbProductos = new JComboBox<>();
        TemaFarmac.estilizarComboBox(cmbProductos);
        cmbProductos.setPreferredSize(new Dimension(200, 38));

        cmbProveedores = new JComboBox<>();
        TemaFarmac.estilizarComboBox(cmbProveedores);
        cmbProveedores.setPreferredSize(new Dimension(200, 38));

        txtNumLote = TemaFarmac.crearCampoTexto(18);
        selCaducidad = new farmac.util.SelectorFecha(LocalDate.now());
        selCaducidad.setPreferredSize(new Dimension(200, 38));
        txtCantidad = TemaFarmac.crearCampoTexto(18);
        txtPrecioCompra = TemaFarmac.crearCampoTexto(18);
        txtFactura = TemaFarmac.crearCampoTexto(18);

        JPanel form = TemaFarmac.crearFormularioGrid(
                new String[]{
                    "Medicamento / producto *",
                    "Proveedor / distribuidor *",
                    "Número de lote *",
                    "Fecha de caducidad *",
                    "Piezas recibidas *",
                    "Precio unitario de compra ($) *",
                    "Factura / remisión"
                },
                new javax.swing.JComponent[]{
                    cmbProductos, cmbProveedores, txtNumLote, selCaducidad,
                    txtCantidad, txtPrecioCompra, txtFactura
                },
                new int[]{1, 1, 1, 1, 1, 1, 2});
        pnlCard.add(form, BorderLayout.CENTER);

        JButton btnGuardar = TemaFarmac.crearBoton("Guardar entrada en inventario", TemaFarmac.TURQUESA, Color.WHITE);
        btnGuardar.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 13));
        btnGuardar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.BOX, 13, Color.WHITE));
        btnGuardar.setIconTextGap(8);
        btnGuardar.setPreferredSize(new Dimension(200, 46));
        btnGuardar.addActionListener(e -> registrar());
        pnlCard.add(btnGuardar, BorderLayout.SOUTH);

        // Navegación con Enter: cada campo pasa al siguiente y el último guarda
        TemaFarmac.enterComoTab(this::registrar, cmbProductos, cmbProveedores, txtNumLote, selCaducidad.getCampoTexto(),
                txtCantidad, txtPrecioCompra, txtFactura);

        // La tarjeta ocupa ~60% del ancho, alineada a la izquierda
        pnlCard.setPreferredSize(new Dimension(800, pnlCard.getPreferredSize().height));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(pnlCard, BorderLayout.WEST);
        add(TemaFarmac.crearScrollResponsivo(wrap), BorderLayout.CENTER);
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
        String cadStr = selCaducidad.getFecha().toString();
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
            selCaducidad.setFecha(LocalDate.now());
            txtCantidad.setText("");
            txtPrecioCompra.setText("");
            txtFactura.setText("");

        } catch (SQLException ex) {
            DialogoFarmac.mostrarJO(this, "Error al guardar lote en base de datos: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }
}
