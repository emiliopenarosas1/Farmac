package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.LoteDAO;
import farmac.dao.ProductoDAO;
import farmac.dao.VentaDAO;
import farmac.dominio.DetalleVenta;
import farmac.dominio.Lote;
import farmac.dominio.OperacionesInventario;
import farmac.dominio.Producto;
import farmac.dominio.RecetaMedica;
import farmac.dominio.TurnoCaja;
import farmac.dominio.Usuario;
import farmac.dominio.Venta;
import farmac.util.Sesion;
import farmac.util.TemaFarmac;
import farmac.util.TicketGenerator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla de Punto de Venta con validación de caja obligatoria (RF12),
 * descuento transaccional ACID (A4, RF13), regla FEFO estricta, recetas (RF14) y tickets PDF.
 */
public class PuntoVentaScreen extends JPanel {

    private final Usuario usuario;
    private final ProductoDAO productoDAO;
    private final LoteDAO loteDAO;
    private final VentaDAO ventaDAO;
    private final OperacionesInventario operacionesInventario;

    private JLabel lblBannerCajaCerrada;
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
    private JButton btnSumar;
    private JButton btnRestar;
    private JButton btnQuitar;

    private List<Producto> catalogoProductos;
    private List<Lote> inventarioLotes;
    private final Map<Integer, Integer> cantidadesCarrito; // productoId -> cantidad solicitada
    private final Map<Integer, Producto> productosCarrito; // productoId -> Producto

    public PuntoVentaScreen(Usuario usuario) {
        this.usuario = usuario != null ? usuario : Sesion.getUsuarioActivo();
        this.productoDAO = new ProductoDAO();
        this.loteDAO = new LoteDAO();
        this.ventaDAO = new VentaDAO();
        this.operacionesInventario = new OperacionesInventario();
        this.cantidadesCarrito = new HashMap<>();
        this.productosCarrito = new HashMap<>();

        inicializarUI();
        cargarDatosCatalogo();

        // Escuchar cambios de estado en el turno de caja
        Sesion.agregarListenerTurno(this::verificarEstadoCaja);
        verificarEstadoCaja();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(8, 8));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(10, 12, 10, 12));

        // 1. Cabecera con Buscador / Escáner y Banner de Caja
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setOpaque(false);

        lblBannerCajaCerrada = new JLabel("CAJA CERRADA: Debe abrir un turno de caja en 'Control de Caja' antes de registrar ventas (RF12).", SwingConstants.CENTER);
        lblBannerCajaCerrada.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblBannerCajaCerrada.setOpaque(true);
        lblBannerCajaCerrada.setBackground(TemaFarmac.ROJO);
        lblBannerCajaCerrada.setForeground(Color.WHITE);
        lblBannerCajaCerrada.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        lblBannerCajaCerrada.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        lblBannerCajaCerrada.setVisible(false);

        pnlNorte.add(lblBannerCajaCerrada);
        pnlNorte.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel pnlHeader = new JPanel(new BorderLayout(6, 6));
        pnlHeader.setOpaque(false);

        JPanel pnlTit = new JPanel();
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        pnlTit.setOpaque(false);

        JLabel lblTitulo = new JLabel("Punto de Venta y Facturación");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Escanee el código de barras o agregue productos desde el catálogo con regla FEFO");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        pnlTit.add(lblTitulo);
        pnlTit.add(lblSub);

        JPanel pnlEscaner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlEscaner.setOpaque(false);

        JLabel lblLector = new JLabel("Lector / Código:");
        lblLector.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblLector.setForeground(TemaFarmac.TEXTO_AZUL);

        txtEscaner = TemaFarmac.crearCampoTexto(14);
        txtEscaner.setPreferredSize(new Dimension(170, 32));
        txtEscaner.addActionListener(e -> procesarDisparoEscaner());

        JButton btnBuscar = TemaFarmac.crearBoton("Buscar", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
        btnBuscar.addActionListener(e -> procesarDisparoEscaner());

        JButton btnRefrescar = TemaFarmac.crearBotonSecundario("Actualizar Catálogo");
        btnRefrescar.addActionListener(e -> cargarDatosCatalogo());

        pnlEscaner.add(lblLector);
        pnlEscaner.add(txtEscaner);
        pnlEscaner.add(btnBuscar);
        pnlEscaner.add(btnRefrescar);

        pnlHeader.add(pnlTit, BorderLayout.WEST);
        pnlHeader.add(pnlEscaner, BorderLayout.EAST);
        pnlNorte.add(pnlHeader);

        add(pnlNorte, BorderLayout.NORTH);

        // 2. División Central (Catálogo a la izquierda, Carrito a la derecha)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(580);
        splitPane.setResizeWeight(0.55);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        // LADO IZQUIERDO: Catálogo
        JPanel pnlCardCatalogo = TemaFarmac.crearCard();
        pnlCardCatalogo.setLayout(new BorderLayout(4, 4));

        JLabel lblTitCat = new JLabel("Catálogo de Medicamentos Disponibles");
        lblTitCat.setFont(TemaFarmac.FUENTE_SUBTITULO);
        lblTitCat.setForeground(TemaFarmac.AZUL_PETROLEO);
        pnlCardCatalogo.add(lblTitCat, BorderLayout.NORTH);

        String[] colsCat = {"Código", "Medicamento", "Precio", "Stock Total", "Control", "Acción"};
        modeloCatalogo = new DefaultTableModel(colsCat, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return col == 5; }
        };
        tblCatalogo = new JTable(modeloCatalogo);
        TemaFarmac.estilizarTabla(tblCatalogo);
        tblCatalogo.getColumnModel().getColumn(0).setPreferredWidth(75);
        tblCatalogo.getColumnModel().getColumn(1).setPreferredWidth(190);
        tblCatalogo.getColumnModel().getColumn(2).setPreferredWidth(65);
        tblCatalogo.getColumnModel().getColumn(3).setPreferredWidth(75);
        tblCatalogo.getColumnModel().getColumn(4).setPreferredWidth(95);
        tblCatalogo.getColumnModel().getColumn(5).setPreferredWidth(75);

        tblCatalogo.getColumnModel().getColumn(5).setCellRenderer(
                new TemaFarmac.BotonTablaRenderer("Agregar", TemaFarmac.TURQUESA, Color.WHITE)
        );
        tblCatalogo.getColumnModel().getColumn(5).setCellEditor(
                new TemaFarmac.BotonTablaEditor("Agregar", TemaFarmac.TURQUESA, Color.WHITE, this::agregarProductoPorIndice)
        );

        JScrollPane scrollCat = new JScrollPane(tblCatalogo);
        scrollCat.setBorder(BorderFactory.createLineBorder(TemaFarmac.GRIS_BORDE));
        scrollCat.getViewport().setBackground(Color.WHITE);
        pnlCardCatalogo.add(scrollCat, BorderLayout.CENTER);

        // LADO DERECHO: Carrito y Cobro
        JPanel pnlCardCarrito = TemaFarmac.crearCard();
        pnlCardCarrito.setLayout(new BorderLayout(6, 6));

        JPanel pnlHeaderCarrito = new JPanel(new BorderLayout());
        pnlHeaderCarrito.setOpaque(false);
        JLabel lblTitCar = new JLabel("Carrito de la Venta");
        lblTitCar.setFont(TemaFarmac.FUENTE_SUBTITULO);
        lblTitCar.setForeground(TemaFarmac.AZUL_PETROLEO);
        pnlHeaderCarrito.add(lblTitCar, BorderLayout.WEST);

        // Botones de ajuste de cantidad en carrito
        JPanel pnlControlesCarrito = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlControlesCarrito.setOpaque(false);

        btnSumar = TemaFarmac.crearBoton("+ 1", TemaFarmac.TURQUESA, Color.WHITE);
        btnSumar.setPreferredSize(new Dimension(50, 26));
        btnSumar.addActionListener(e -> modificarCantidadSeleccionada(1));

        btnRestar = TemaFarmac.crearBoton("- 1", TemaFarmac.AMBAR, Color.WHITE);
        btnRestar.setPreferredSize(new Dimension(50, 26));
        btnRestar.addActionListener(e -> modificarCantidadSeleccionada(-1));

        btnQuitar = TemaFarmac.crearBoton("Quitar", TemaFarmac.ROJO, Color.WHITE);
        btnQuitar.setPreferredSize(new Dimension(70, 26));
        btnQuitar.addActionListener(e -> quitarSeleccionado());

        pnlControlesCarrito.add(btnSumar);
        pnlControlesCarrito.add(btnRestar);
        pnlControlesCarrito.add(btnQuitar);
        pnlHeaderCarrito.add(pnlControlesCarrito, BorderLayout.EAST);

        pnlCardCarrito.add(pnlHeaderCarrito, BorderLayout.NORTH);

        String[] colsCar = {"ID", "Medicamento", "Cant.", "P. Unit.", "Subtotal"};
        modeloCarrito = new DefaultTableModel(colsCar, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblCarrito = new JTable(modeloCarrito);
        TemaFarmac.estilizarTabla(tblCarrito);
        tblCarrito.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblCarrito.getColumnModel().getColumn(1).setPreferredWidth(180);
        tblCarrito.getColumnModel().getColumn(2).setPreferredWidth(50);
        tblCarrito.getColumnModel().getColumn(3).setPreferredWidth(70);
        tblCarrito.getColumnModel().getColumn(4).setPreferredWidth(75);

        JScrollPane scrollCar = new JScrollPane(tblCarrito);
        scrollCar.setBorder(BorderFactory.createLineBorder(TemaFarmac.GRIS_BORDE));
        scrollCar.getViewport().setBackground(Color.WHITE);
        pnlCardCarrito.add(scrollCar, BorderLayout.CENTER);

        // Panel de Totales y Cobro
        JPanel pnlTotales = new JPanel();
        pnlTotales.setLayout(new BoxLayout(pnlTotales, BoxLayout.Y_AXIS));
        pnlTotales.setOpaque(false);
        pnlTotales.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel pnlMontos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
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
        JLabel lblForma = new JLabel("Forma de Pago:");
        lblForma.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblForma.setForeground(TemaFarmac.TEXTO_AZUL);
        cmbFormaPago = new JComboBox<>(new String[]{"EFECTIVO", "TARJETA", "TRANSFERENCIA"});
        cmbFormaPago.setBackground(Color.WHITE);
        pnlPago.add(lblForma);
        pnlPago.add(cmbFormaPago);

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlBotones.setOpaque(false);

        btnVaciar = TemaFarmac.crearBoton("Vaciar", TemaFarmac.BORDE, TemaFarmac.TEXTO);
        btnVaciar.addActionListener(e -> limpiarCarrito());

        btnCobrar = TemaFarmac.crearBoton("Cobrar y Generar Ticket PDF", TemaFarmac.VERDE, Color.WHITE);
        btnCobrar.setFont(TemaFarmac.FUENTE_SUBTITULO);
        btnCobrar.setPreferredSize(new Dimension(240, 38));
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
    }

    public void verificarEstadoCaja() {
        boolean abierta = Sesion.hayTurnoAbierto();
        lblBannerCajaCerrada.setVisible(!abierta);
        txtEscaner.setEnabled(abierta);
        tblCatalogo.setEnabled(abierta);
        btnCobrar.setEnabled(abierta);
        btnVaciar.setEnabled(abierta);
        btnSumar.setEnabled(abierta);
        btnRestar.setEnabled(abierta);
        btnQuitar.setEnabled(abierta);
    }

    public void cargarDatosCatalogo() {
        try {
            catalogoProductos = productoDAO.listarTodos(true);
            inventarioLotes = loteDAO.listarTodos();
            operacionesInventario.setInventario(inventarioLotes);

            llenarTablaCatalogo();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar catálogo: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void llenarTablaCatalogo() {
        modeloCatalogo.setRowCount(0);
        if (catalogoProductos == null) return;
        for (Producto p : catalogoProductos) {
            // Calcular existencias únicamente en lotes vigentes (no vencidos)
            int stockDisponible = operacionesInventario.calcularStockDisponible(p);

            modeloCatalogo.addRow(new Object[]{
                p.getCodigoBarras(),
                p.getNombre(),
                String.format("$%.2f", p.getPrecioVenta()),
                stockDisponible + " pzas",
                p.isRequiereReceta() ? "RECETA" : "LIBRE",
                "Agregar"
            });
        }
    }

    private void procesarDisparoEscaner() {
        if (!Sesion.hayTurnoAbierto()) {
            JOptionPane.showMessageDialog(this, "Debe abrir un turno de caja para procesar ventas.", "Caja Cerrada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String texto = txtEscaner.getText().trim();
        if (texto.isEmpty()) return;

        Producto encontrado = null;
        if (catalogoProductos != null) {
            for (Producto p : catalogoProductos) {
                if (p.getCodigoBarras().equalsIgnoreCase(texto) || p.getNombre().toLowerCase().contains(texto.toLowerCase())) {
                    encontrado = p;
                    break;
                }
            }
        }

        if (encontrado != null) {
            agregarProductoAlCarrito(encontrado, 1);
            txtEscaner.setText("");
            txtEscaner.requestFocusInWindow();
        } else {
            JOptionPane.showMessageDialog(this, "Medicamento no encontrado para el código/término: " + texto, "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void agregarProductoPorIndice(int fila) {
        if (!Sesion.hayTurnoAbierto()) {
            JOptionPane.showMessageDialog(this, "Debe abrir un turno de caja para vender.", "Caja Cerrada", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (catalogoProductos != null && fila >= 0 && fila < catalogoProductos.size()) {
            Producto p = catalogoProductos.get(fila);
            agregarProductoAlCarrito(p, 1);
        }
    }

    private void agregarProductoAlCarrito(Producto prod, int cantidad) {
        // Validar stock disponible en lotes vigentes
        int stockTotalDisponible = operacionesInventario.calcularStockDisponible(prod);

        if (stockTotalDisponible <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Sin existencias disponibles en almacén para '" + prod.getNombre() + "'.\n"
                    + "(No se permite despachar lotes vencidos ni sin stock)",
                    "Producto Agotado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cantidadActualCarrito = cantidadesCarrito.getOrDefault(prod.getId(), 0);
        int nuevaCantidad = cantidadActualCarrito + cantidad;

        if (nuevaCantidad > stockTotalDisponible) {
            JOptionPane.showMessageDialog(this,
                    "Existencias insuficientes para '" + prod.getNombre() + "'.\n"
                    + "Stock total vigente: " + stockTotalDisponible + " pzas.\n"
                    + "Cantidad solicitada en carrito: " + nuevaCantidad + " pzas.",
                    "Límite de Stock Superado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        cantidadesCarrito.put(prod.getId(), nuevaCantidad);
        productosCarrito.put(prod.getId(), prod);

        actualizarTablaCarrito();
    }

    private void modificarCantidadSeleccionada(int delta) {
        int fila = tblCarrito.getSelectedRow();
        if (fila < 0) return;
        int prodId = (int) modeloCarrito.getValueAt(fila, 0);
        Producto p = productosCarrito.get(prodId);
        if (p == null) return;

        int cant = cantidadesCarrito.getOrDefault(prodId, 0) + delta;
        if (cant <= 0) {
            cantidadesCarrito.remove(prodId);
            productosCarrito.remove(prodId);
        } else {
            // Validar stock disponible
            int stockDisp = operacionesInventario.calcularStockDisponible(p);
            if (cant > stockDisp) {
                JOptionPane.showMessageDialog(this, "No puede exceder el stock disponible (" + stockDisp + " pzas).", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            cantidadesCarrito.put(prodId, cant);
        }
        actualizarTablaCarrito();
    }

    private void quitarSeleccionado() {
        int fila = tblCarrito.getSelectedRow();
        if (fila < 0) return;
        int prodId = (int) modeloCarrito.getValueAt(fila, 0);
        cantidadesCarrito.remove(prodId);
        productosCarrito.remove(prodId);
        actualizarTablaCarrito();
    }

    private void actualizarTablaCarrito() {
        modeloCarrito.setRowCount(0);
        double total = 0.0;

        for (Map.Entry<Integer, Integer> entry : cantidadesCarrito.entrySet()) {
            int prodId = entry.getKey();
            int cant = entry.getValue();
            Producto p = productosCarrito.get(prodId);
            if (p != null) {
                double subtotal = Math.round((cant * p.getPrecioVenta()) * 100.0) / 100.0;
                total += subtotal;

                modeloCarrito.addRow(new Object[]{
                    p.getId(),
                    p.getNombre(),
                    cant,
                    String.format("$%.2f", p.getPrecioVenta()),
                    String.format("$%.2f", subtotal)
                });
            }
        }

        total = Math.round(total * 100.0) / 100.0;
        lblSubtotal.setText(String.format("Subtotal: $%.2f", total));
        lblTotal.setText(String.format("$%.2f", total));
    }

    private void limpiarCarrito() {
        cantidadesCarrito.clear();
        productosCarrito.clear();
        actualizarTablaCarrito();
    }

    private void finalizarCobro() {
        if (!Sesion.hayTurnoAbierto()) {
            JOptionPane.showMessageDialog(this, "Debe abrir un turno de caja para poder cobrar (RF12).", "Caja Cerrada", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (cantidadesCarrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito de ventas está vacío. Agregue medicamentos.", "Carrito Vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }

        TurnoCaja turno = Sesion.getTurnoActivo();
        String formaPago = (String) cmbFormaPago.getSelectedItem();

        // Calcular total
        double totalVenta = 0.0;
        for (Map.Entry<Integer, Integer> e : cantidadesCarrito.entrySet()) {
            Producto p = productosCarrito.get(e.getKey());
            totalVenta += e.getValue() * p.getPrecioVenta();
        }
        totalVenta = Math.round(totalVenta * 100.0) / 100.0;

        // Si es pago en efectivo, solicitar importe recibido y calcular cambio
        double efectivoRecibido = totalVenta;
        double cambio = 0.0;
        if ("EFECTIVO".equalsIgnoreCase(formaPago)) {
            String recibidoStr = JOptionPane.showInputDialog(this,
                    "Total a Cobrar: $" + String.format("%.2f", totalVenta) + "\nIngrese el Efectivo Recibido ($):",
                    String.format("%.2f", totalVenta));
            if (recibidoStr == null) {
                return; // Cancelado por el usuario
            }
            try {
                efectivoRecibido = Double.parseDouble(recibidoStr.trim());
                if (efectivoRecibido < totalVenta) {
                    JOptionPane.showMessageDialog(this, "El efectivo recibido es menor al total a pagar.", "Importe Insuficiente", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                cambio = Math.round((efectivoRecibido - totalVenta) * 100.0) / 100.0;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Monto recibido inválido.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // 1. Detectar productos controlados que requieran receta médica (RF14)
        List<RecetaMedica> recetasCapturadas = new ArrayList<>();
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);

        for (Map.Entry<Integer, Integer> e : cantidadesCarrito.entrySet()) {
            Producto p = productosCarrito.get(e.getKey());
            if (p != null && p.isRequiereReceta()) {
                RegistroRecetaDialog dialog = new RegistroRecetaDialog(parentFrame, p);
                dialog.setVisible(true);

                if (!dialog.isConfirmado() || dialog.getRecetaRegistrada() == null) {
                    JOptionPane.showMessageDialog(this,
                            "Cobro cancelado: No se completó el registro de receta médica obligatoria para:\n" + p.getNombre(),
                            "Venta Interrumpida (RF14)", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                recetasCapturadas.add(dialog.getRecetaRegistrada());
            }
        }

        // 2. Construir la Venta y aplicar algoritmo FEFO para asociar lotes exactos
        Venta venta = new Venta(0, turno, usuario, formaPago);
        venta.setFechaHora(LocalDateTime.now());
        for (RecetaMedica r : recetasCapturadas) {
            venta.agregarReceta(r);
        }

        try {
            // Recargar lotes frescos para garantizar precisión en la asignación FEFO
            List<Lote> lotesFrescos = loteDAO.listarTodos();
            operacionesInventario.setInventario(lotesFrescos);

            for (Map.Entry<Integer, Integer> e : cantidadesCarrito.entrySet()) {
                Producto p = productosCarrito.get(e.getKey());
                int cantidadRequerida = e.getValue();

                List<OperacionesInventario.DespachoLote> despachos = operacionesInventario.calcularDespachoFEFO(p, cantidadRequerida);
                int cantidadSurtida = 0;
                for (OperacionesInventario.DespachoLote d : despachos) {
                    venta.agregarDetalle(d.getLote(), d.getCantidad(), p.getPrecioVenta());
                    cantidadSurtida += d.getCantidad();
                }

                if (cantidadSurtida < cantidadRequerida) {
                    throw new SQLException("Existencias vigentes insuficientes para surtir " + cantidadRequerida + " pzas de " + p.getNombre());
                }
            }

            // 3. Ejecutar Transacción ACID en Base de Datos (A4)
            ventaDAO.registrarVentaTransaccional(venta);

            // 4. Generar Ticket PDF oficial (RNF08)
            File ticketPDF = TicketGenerator.generarTicketPDF(venta);
            String rutaMsg = (ticketPDF != null) ? "\nTicket PDF archivado en:\n" + ticketPDF.getAbsolutePath() : "";

            BitacoraDAO.registrar(usuario, "VENTA_REGISTRADA",
                    "Venta #" + venta.getId() + " - Total: $" + venta.getTotal() + " [" + formaPago + "]");

            String mensajeCobro = "✓ Venta #" + String.format("%05d", venta.getId()) + " registrada con éxito.\n"
                    + "Total: $" + String.format("%.2f", venta.getTotal()) + "\n"
                    + ("EFECTIVO".equalsIgnoreCase(formaPago) ? "Efectivo Recibido: $" + String.format("%.2f", efectivoRecibido) + "\nCAMBIO A ENTREGAR: $" + String.format("%.2f", cambio) + "\n" : "")
                    + rutaMsg;

            JOptionPane.showMessageDialog(this, mensajeCobro, "Cobro Completado con Éxito", JOptionPane.INFORMATION_MESSAGE);

            // Limpiar y refrescar catálogo
            limpiarCarrito();
            cargarDatosCatalogo();
            txtEscaner.requestFocusInWindow();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error transaccional en la venta (Se ejecutó Rollback): " + ex.getMessage(),
                    "Error de Transacción (A4)", JOptionPane.ERROR_MESSAGE);
        }
    }
}
