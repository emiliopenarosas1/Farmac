package farmac.ui;

import farmac.dominio.*;
import farmac.util.TemaFarmac;
import farmac.util.TicketGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de Punto de Venta con validacion de Receta Medica en ventana dedicada y tickets PDF.
 */
public class PuntoVentaScreen extends JPanel {
    private final Usuario usuario;
    private JTextField txtEscaner;
    private JTable tblCatalogo;
    private DefaultTableModel modeloCatalogo;
    private JTable tblCarrito;
    private DefaultTableModel modeloCarrito;
    private JLabel lblTotal;
    private JLabel lblSubtotal;
    private JComboBox<String> cmbFormaPago;
    private JButton btnCobrar;
    private JButton btnVaciar;
    private List<Producto> catalogo;
    private List<Lote> inventarioLotes;
    private Venta ventaActual;
    private TurnoCaja turnoCaja;
    private int contadorVentas = 1;

    public PuntoVentaScreen(Usuario usuario) {
        this.usuario = usuario;
        this.turnoCaja = new TurnoCaja(1, usuario, 500.0);
        this.ventaActual = new Venta(contadorVentas, turnoCaja, usuario, "EFECTIVO");
        inicializarDatosDemo();
        inicializarUI();
    }

    private void inicializarDatosDemo() {
        catalogo = new ArrayList<>();
        inventarioLotes = new ArrayList<>();

        Producto p1 = new Producto(1, "75010001", "Amoxicilina 500mg (c/12)", "Antibioticos", "Farmaceutica Nal", 85.00, true, 5);
        Producto p2 = new Producto(2, "75010002", "Paracetamol 500mg (c/20)", "Analgesicos", "Laboratorios Alfa", 25.00, false, 10);
        Producto p3 = new Producto(3, "75010003", "Ibuprofeno 400mg (c/10)", "Antiinflamatorios", "Genericos MX", 45.00, false, 8);
        Producto p4 = new Producto(4, "75010004", "Omeprazol 20mg (c/14)", "Gastroenterologia", "Laboratorios Beta", 60.00, false, 6);
        Producto p5 = new Producto(5, "75010005", "Azitromicina 500mg (c/3)", "Antibioticos", "Farmaceutica Nal", 145.00, true, 4);
        Producto p6 = new Producto(6, "75010006", "Loratadina 10mg (c/10)", "Antihistaminicos", "Laboratorios Alfa", 35.00, false, 5);

        catalogo.add(p1); catalogo.add(p2); catalogo.add(p3);
        catalogo.add(p4); catalogo.add(p5); catalogo.add(p6);

        inventarioLotes.add(new Lote(1, "LT-AMX-2026", LocalDate.now().plusMonths(8), 50, 40.0, p1));
        inventarioLotes.add(new Lote(2, "LT-PAR-2027", LocalDate.now().plusMonths(14), 100, 12.0, p2));
        inventarioLotes.add(new Lote(3, "LT-IBU-2026", LocalDate.now().plusMonths(6), 40, 20.0, p3));
        inventarioLotes.add(new Lote(4, "LT-OME-2026", LocalDate.now().plusMonths(10), 60, 28.0, p4));
        inventarioLotes.add(new Lote(5, "LT-AZI-2027", LocalDate.now().plusMonths(12), 30, 70.0, p5));
        inventarioLotes.add(new Lote(6, "LT-LOR-2027", LocalDate.now().plusMonths(18), 80, 15.0, p6));
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(8, 8));
        setBackground(TemaFarmac.FONDO);
        setBorder(new EmptyBorder(10, 12, 10, 12));

        // 1. Cabecera con Buscador / Escaner
        JPanel pnlHeader = new JPanel(new BorderLayout(6, 6));
        pnlHeader.setOpaque(false);

        JPanel pnlTit = new JPanel();
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        pnlTit.setOpaque(false);

        JLabel lblTitulo = new JLabel("Punto de Venta y Facturacion");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Escanee el codigo de barras o agregue productos desde la tabla");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        pnlTit.add(lblTitulo);
        pnlTit.add(lblSub);

        JPanel pnlEscaner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlEscaner.setOpaque(false);

        JLabel lblLector = new JLabel("Lector / Codigo:");
        lblLector.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblLector.setForeground(TemaFarmac.TEXTO_AZUL);

        txtEscaner = TemaFarmac.crearCampoTexto("", 14);
        txtEscaner.setPreferredSize(new Dimension(160, 30));
        txtEscaner.addActionListener(e -> procesarDisparoEscaner());

        JButton btnBuscar = TemaFarmac.crearBoton("Buscar", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
        btnBuscar.addActionListener(e -> procesarDisparoEscaner());

        pnlEscaner.add(lblLector);
        pnlEscaner.add(txtEscaner);
        pnlEscaner.add(btnBuscar);

        pnlHeader.add(pnlTit, BorderLayout.WEST);
        pnlHeader.add(pnlEscaner, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Division central
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(560);
        splitPane.setResizeWeight(0.55);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        // LADO IZQUIERDO: Catalogo disponible con boton "Agregar"
        JPanel pnlCardCatalogo = TemaFarmac.crearCard();
        pnlCardCatalogo.setLayout(new BorderLayout(4, 4));

        JLabel lblTitCat = new JLabel("Medicamentos en Existencia");
        lblTitCat.setFont(TemaFarmac.FUENTE_SUBTITULO);
        lblTitCat.setForeground(TemaFarmac.AZUL_PETROLEO);
        pnlCardCatalogo.add(lblTitCat, BorderLayout.NORTH);

        String[] colsCat = {"Codigo", "Medicamento", "Precio", "Stock", "Accion"};
        modeloCatalogo = new DefaultTableModel(colsCat, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 4;
            }
        };
        tblCatalogo = new JTable(modeloCatalogo);
        TemaFarmac.estilizarTabla(tblCatalogo);

        tblCatalogo.getColumnModel().getColumn(0).setPreferredWidth(75);
        tblCatalogo.getColumnModel().getColumn(1).setPreferredWidth(210);
        tblCatalogo.getColumnModel().getColumn(2).setPreferredWidth(65);
        tblCatalogo.getColumnModel().getColumn(3).setPreferredWidth(65);
        tblCatalogo.getColumnModel().getColumn(4).setPreferredWidth(75);

        tblCatalogo.getColumnModel().getColumn(4).setCellRenderer(
                new TemaFarmac.BotonTablaRenderer("Agregar", TemaFarmac.TURQUESA, Color.WHITE)
        );
        tblCatalogo.getColumnModel().getColumn(4).setCellEditor(
                new TemaFarmac.BotonTablaEditor("Agregar", TemaFarmac.TURQUESA, Color.WHITE, this::agregarProductoPorIndice)
        );

        JScrollPane scrollCat = new JScrollPane(tblCatalogo);
        scrollCat.setBorder(BorderFactory.createLineBorder(TemaFarmac.BORDE));
        scrollCat.getViewport().setBackground(Color.WHITE);
        pnlCardCatalogo.add(scrollCat, BorderLayout.CENTER);

        // LADO DERECHO: Carrito y Cobro
        JPanel pnlCardCarrito = TemaFarmac.crearCard();
        pnlCardCarrito.setLayout(new BorderLayout(6, 6));

        JLabel lblTitCar = new JLabel("Carrito de la Venta");
        lblTitCar.setFont(TemaFarmac.FUENTE_SUBTITULO);
        lblTitCar.setForeground(TemaFarmac.AZUL_PETROLEO);
        pnlCardCarrito.add(lblTitCar, BorderLayout.NORTH);

        String[] colsCar = {"Lote", "Medicamento", "Cant.", "P. Unit.", "Subtotal"};
        modeloCarrito = new DefaultTableModel(colsCar, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblCarrito = new JTable(modeloCarrito);
        TemaFarmac.estilizarTabla(tblCarrito);

        tblCarrito.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblCarrito.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblCarrito.getColumnModel().getColumn(2).setPreferredWidth(45);
        tblCarrito.getColumnModel().getColumn(3).setPreferredWidth(65);
        tblCarrito.getColumnModel().getColumn(4).setPreferredWidth(65);

        JScrollPane scrollCar = new JScrollPane(tblCarrito);
        scrollCar.setBorder(BorderFactory.createLineBorder(TemaFarmac.BORDE));
        scrollCar.getViewport().setBackground(Color.WHITE);
        pnlCardCarrito.add(scrollCar, BorderLayout.CENTER);

        // Panel inferior de totales compacto
        JPanel pnlTotales = new JPanel();
        pnlTotales.setLayout(new BoxLayout(pnlTotales, BoxLayout.Y_AXIS));
        pnlTotales.setOpaque(false);
        pnlTotales.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel pnlMontos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlMontos.setOpaque(false);
        lblSubtotal = new JLabel("Subtotal: $0.00");
        lblSubtotal.setFont(TemaFarmac.FUENTE_REGULAR);
        lblSubtotal.setForeground(TemaFarmac.TEXTO_MUTED);

        lblTotal = new JLabel("$0.00");
        lblTotal.setFont(TemaFarmac.FUENTE_DISPLAY);
        lblTotal.setForeground(TemaFarmac.AZUL_PETROLEO);

        pnlMontos.add(lblSubtotal);
        pnlMontos.add(lblTotal);

        JPanel pnlPagoAccion = new JPanel(new BorderLayout(6, 0));
        pnlPagoAccion.setOpaque(false);

        JPanel pnlPago = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlPago.setOpaque(false);
        JLabel lblForma = new JLabel("Pago:");
        lblForma.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblForma.setForeground(TemaFarmac.TEXTO_AZUL);
        cmbFormaPago = new JComboBox<>(new String[]{"EFECTIVO", "TARJETA", "TRANSFERENCIA"});
        cmbFormaPago.setBackground(Color.WHITE);
        pnlPago.add(lblForma);
        pnlPago.add(cmbFormaPago);

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlBotones.setOpaque(false);

        btnVaciar = TemaFarmac.crearBoton("Vaciar", TemaFarmac.BORDE, TemaFarmac.TEXTO);
        btnVaciar.addActionListener(e -> limpiarCarrito());

        btnCobrar = TemaFarmac.crearBoton("Cobrar y Ticket (PDF)", TemaFarmac.VERDE, Color.WHITE);
        btnCobrar.addActionListener(e -> finalizarCobro());

        pnlBotones.add(btnVaciar);
        pnlBotones.add(btnCobrar);

        pnlPagoAccion.add(pnlPago, BorderLayout.WEST);
        pnlPagoAccion.add(pnlBotones, BorderLayout.EAST);

        pnlTotales.add(pnlMontos);
        pnlTotales.add(Box.createRigidArea(new Dimension(0, 4)));
        pnlTotales.add(pnlPagoAccion);

        pnlCardCarrito.add(pnlTotales, BorderLayout.SOUTH);

        splitPane.setLeftComponent(pnlCardCatalogo);
        splitPane.setRightComponent(pnlCardCarrito);
        add(splitPane, BorderLayout.CENTER);

        llenarTablaCatalogo();
    }

    private void llenarTablaCatalogo() {
        modeloCatalogo.setRowCount(0);
        for (Producto p : catalogo) {
            int stock = p.calcularStockTotal(inventarioLotes);
            modeloCatalogo.addRow(new Object[]{
                    p.getCodigoBarras(),
                    p.getNombre(),
                    String.format("$%.2f", p.getPrecioVenta()),
                    stock + " pzas",
                    "Agregar"
            });
        }
    }

    private void procesarDisparoEscaner() {
        String texto = txtEscaner.getText().trim();
        if (texto.isEmpty()) return;

        Producto encontrado = null;
        for (Producto p : catalogo) {
            if (p.getCodigoBarras().equalsIgnoreCase(texto) || p.getNombre().toLowerCase().contains(texto.toLowerCase())) {
                encontrado = p;
                break;
            }
        }

        if (encontrado != null) {
            agregarProductoAlCarrito(encontrado);
            txtEscaner.setText("");
            txtEscaner.requestFocusInWindow();
        } else {
            JOptionPane.showMessageDialog(this, "Medicamento no encontrado para el codigo: " + texto, "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void agregarProductoPorIndice(int fila) {
        if (fila >= 0 && fila < catalogo.size()) {
            Producto p = catalogo.get(fila);
            agregarProductoAlCarrito(p);
        }
    }

    private void agregarProductoAlCarrito(Producto prod) {
        Lote loteAsignado = null;
        for (Lote l : inventarioLotes) {
            if (l.getProducto() != null && l.getProducto().getId() == prod.getId() && l.getCantidadActual() > 0) {
                loteAsignado = l;
                break;
            }
        }

        if (loteAsignado == null) {
            loteAsignado = new Lote(99, "LT-" + prod.getCodigoBarras(), LocalDate.now().plusMonths(12), 50, 30.0, prod);
            inventarioLotes.add(loteAsignado);
        }

        DetalleVenta d = ventaActual.agregarDetalle(loteAsignado, 1, prod.getPrecioVenta());
        modeloCarrito.addRow(new Object[]{
                loteAsignado.getNumeroLote(),
                prod.getNombre(),
                d.getCantidad(),
                String.format("$%.2f", d.getPrecioUnitario()),
                String.format("$%.2f", d.getSubtotal())
        });
        actualizarTotales();
    }

    private void actualizarTotales() {
        lblSubtotal.setText(String.format("Subtotal: $%.2f", ventaActual.getTotal()));
        lblTotal.setText(String.format("$%.2f", ventaActual.getTotal()));
    }

    private void limpiarCarrito() {
        modeloCarrito.setRowCount(0);
        contadorVentas++;
        ventaActual = new Venta(contadorVentas, turnoCaja, usuario, (String) cmbFormaPago.getSelectedItem());
        actualizarTotales();
    }

    private void finalizarCobro() {
        if (ventaActual.getDetalles().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito esta vacio. Agregue medicamentos.", "Carrito Vacio", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Detectar si algun producto del carrito requiere receta médica
        Producto prodReceta = null;
        for (DetalleVenta d : ventaActual.getDetalles()) {
            if (d.getLote() != null && d.getLote().getProducto() != null && d.getLote().getProducto().isRequiereReceta()) {
                prodReceta = d.getLote().getProducto();
                break;
            }
        }

        // 2. Si requiere receta, abrir obligatoriamente la ventana modal de captura
        if (prodReceta != null) {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            RegistroRecetaDialog dialog = new RegistroRecetaDialog(parentFrame, prodReceta);
            dialog.setVisible(true);

            if (!dialog.isConfirmado() || dialog.getRecetaRegistrada() == null) {
                JOptionPane.showMessageDialog(this, "Venta cancelada: No se registro la receta medica obligatoria.", "Cobro Interrumpido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ventaActual.asignarReceta(dialog.getRecetaRegistrada());
        }

        ventaActual.setFormaPago((String) cmbFormaPago.getSelectedItem());

        // 3. Generar Ticket en PDF en la carpeta diaria (tickets/YYYY-MM-DD/)
        File ticketPDF = TicketGenerator.generarTicketPDF(ventaActual);
        String rutaMsg = (ticketPDF != null) ? "\nTicket PDF generado en:\n" + ticketPDF.getAbsolutePath() : "";

        JOptionPane.showMessageDialog(this,
                "✓ Venta #" + String.format("%05d", ventaActual.getId()) + " cobrada exitosamente.\nTotal: $" + String.format("%.2f", ventaActual.getTotal()) + rutaMsg,
                "Cobro y Ticket PDF Exitoso", JOptionPane.INFORMATION_MESSAGE);

        limpiarCarrito();
        txtEscaner.requestFocusInWindow();
    }
}
