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
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
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
import javax.swing.JComponent;
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
 * Pantalla de Punto de Venta con validación de caja obligatoria,
 * descuento transaccional ACID, regla FEFO estricta, recetas y tickets PDF.
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
    private boolean catalogoCargando;
    private volatile boolean cajaAbierta;
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

        // Escuchar cambios de estado en el turno de caja
        verificarEstadoCaja();
        Sesion.agregarListenerTurno(this::verificarEstadoCaja);

        // Cargar el catálogo en segundo plano para no bloquear el EDT
        cargarDatosCatalogo();
    }

    // Método para cargar catálogo de forma perezosa (lazy) al primer acceso
    private void asegurarCatalogoCargado() {
        if (catalogoProductos == null) {
            cargarDatosCatalogo();
        }
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(8, 8));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(20, 26, 20, 26));

        // Banner de caja cerrada
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setOpaque(false);

        lblBannerCajaCerrada = new JLabel("CAJA CERRADA: Debe abrir un turno de caja en 'Control de caja' antes de registrar ventas.", SwingConstants.CENTER);
        lblBannerCajaCerrada.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblBannerCajaCerrada.setOpaque(true);
        lblBannerCajaCerrada.setBackground(TemaFarmac.ROJO);
        lblBannerCajaCerrada.setForeground(Color.WHITE);
        lblBannerCajaCerrada.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        lblBannerCajaCerrada.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        lblBannerCajaCerrada.setVisible(false);
        pnlNorte.add(lblBannerCajaCerrada);
        add(pnlNorte, BorderLayout.NORTH);

        // 2. División central (catálogo a la izquierda, carrito a la derecha)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.62);
        splitPane.setContinuousLayout(true);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        splitPane.setDividerSize(12);

        // LADO IZQUIERDO: búsqueda y catálogo
        JPanel pnlIzq = new JPanel(new BorderLayout(0, 10));
        pnlIzq.setOpaque(false);
        pnlIzq.setMinimumSize(new Dimension(340, 0));

        JPanel pnlTitCat = new JPanel(new BorderLayout());
        pnlTitCat.setOpaque(false);
        JLabel lblTitCat = new JLabel("Búsqueda y catálogo");
        lblTitCat.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 18));
        lblTitCat.setForeground(TemaFarmac.TEXTO_AZUL);
        JButton btnRefrescar = TemaFarmac.crearBotonSecundario("Actualizar catálogo");
        btnRefrescar.setIcon(FontIcon.of(FontAwesomeSolid.SYNC_ALT, 12, TemaFarmac.TEXTO));
        btnRefrescar.setIconTextGap(8);
        btnRefrescar.addActionListener(e -> cargarDatosCatalogo());
        pnlTitCat.add(lblTitCat, BorderLayout.WEST);
        pnlTitCat.add(btnRefrescar, BorderLayout.EAST);

        // Campo de escáner / búsqueda con lupa y código de barras
        txtEscaner = TemaFarmac.crearCampoTexto(14);
        txtEscaner.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        txtEscaner.setOpaque(false);
        txtEscaner.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 14));
        txtEscaner.putClientProperty("JTextField.placeholderText", "Escanear código de barras o buscar medicamento...");
        txtEscaner.addActionListener(e -> procesarDisparoEscaner());
        JPanel pnlBusqueda = new JPanel(new BorderLayout(6, 0));
        pnlBusqueda.setBackground(Color.WHITE);
        pnlBusqueda.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(TemaFarmac.TURQUESA, 12, 2),
                new EmptyBorder(4, 12, 4, 12)));
        pnlBusqueda.setPreferredSize(new Dimension(300, 48));
        pnlBusqueda.add(new JLabel(FontIcon.of(FontAwesomeSolid.SEARCH, 15, TemaFarmac.TEXTO_MUTED)), BorderLayout.WEST);
        pnlBusqueda.add(txtEscaner, BorderLayout.CENTER);
        pnlBusqueda.add(new JLabel(FontIcon.of(FontAwesomeSolid.BARCODE, 22, TemaFarmac.TEXTO_AZUL)), BorderLayout.EAST);

        JPanel pnlNorteIzq = new JPanel(new BorderLayout(0, 10));
        pnlNorteIzq.setOpaque(false);
        pnlNorteIzq.add(pnlTitCat, BorderLayout.NORTH);
        pnlNorteIzq.add(pnlBusqueda, BorderLayout.CENTER);
        pnlIzq.add(pnlNorteIzq, BorderLayout.NORTH);

        String[] colsCat = {"Código", "Medicamento", "Precio", "Stock", "Tipo", "Acción"};
        modeloCatalogo = new DefaultTableModel(colsCat, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return col == 5; }
        };
        tblCatalogo = new JTable(modeloCatalogo);
        TemaFarmac.estilizarTabla(tblCatalogo);
        tblCatalogo.setRowHeight(37);
        tblCatalogo.getColumnModel().getColumn(0).setPreferredWidth(78);
        tblCatalogo.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblCatalogo.getColumnModel().getColumn(2).setPreferredWidth(66);
        tblCatalogo.getColumnModel().getColumn(3).setPreferredWidth(70);
        tblCatalogo.getColumnModel().getColumn(4).setPreferredWidth(66);
        tblCatalogo.getColumnModel().getColumn(5).setPreferredWidth(110);
        tblCatalogo.getColumnModel().getColumn(4).setCellRenderer(new TemaFarmac.BadgeCellRenderer());
        tblCatalogo.getColumnModel().getColumn(5).setCellRenderer(
                new TemaFarmac.BotonTablaRenderer("+  Agregar", TemaFarmac.TURQUESA, Color.WHITE)
        );
        tblCatalogo.getColumnModel().getColumn(5).setCellEditor(
                new TemaFarmac.BotonTablaEditor("+  Agregar", TemaFarmac.TURQUESA, Color.WHITE, this::agregarProductoPorIndice)
        );

        JScrollPane scrollCat = new JScrollPane(tblCatalogo);
        scrollCat.setBorder(new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 8, 1));
        scrollCat.getViewport().setBackground(Color.WHITE);
        pnlIzq.add(scrollCat, BorderLayout.CENTER);

        // LADO DERECHO: carrito y cobro
        JPanel pnlCardCarrito = TemaFarmac.crearCard();
        pnlCardCarrito.setLayout(new BorderLayout(6, 8));
        pnlCardCarrito.setMinimumSize(new Dimension(320, 0));

        JPanel pnlHeaderCarrito = new JPanel(new BorderLayout());
        pnlHeaderCarrito.setOpaque(false);
        JLabel lblTitCar = new JLabel("Carrito de venta");
        lblTitCar.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 16));
        lblTitCar.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlHeaderCarrito.add(lblTitCar, BorderLayout.WEST);

        JPanel pnlControlesCarrito = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlControlesCarrito.setOpaque(false);

        btnSumar = TemaFarmac.crearBoton("+1", new Color(0x1F, 0xBF, 0x5B), TemaFarmac.TEXTO_AZUL);
        btnSumar.addActionListener(e -> modificarCantidadSeleccionada(1));
        btnRestar = TemaFarmac.crearBoton("-1", new Color(0xF2, 0xB3, 0x3D), TemaFarmac.TEXTO_AZUL);
        btnRestar.addActionListener(e -> modificarCantidadSeleccionada(-1));
        btnQuitar = TemaFarmac.crearBoton("Quitar", TemaFarmac.ROJO_SUAVE, Color.WHITE);
        btnQuitar.addActionListener(e -> quitarSeleccionado());

        pnlControlesCarrito.add(btnSumar);
        pnlControlesCarrito.add(btnRestar);
        pnlControlesCarrito.add(btnQuitar);
        pnlHeaderCarrito.add(pnlControlesCarrito, BorderLayout.EAST);
        pnlCardCarrito.add(pnlHeaderCarrito, BorderLayout.NORTH);

        String[] colsCar = {"ID", "Medicamento", "Cant.", "Precio", "Subtotal"};
        modeloCarrito = new DefaultTableModel(colsCar, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblCarrito = new JTable(modeloCarrito);
        TemaFarmac.estilizarTabla(tblCarrito);
        tblCarrito.getColumnModel().getColumn(1).setPreferredWidth(170);
        tblCarrito.getColumnModel().getColumn(2).setPreferredWidth(48);
        tblCarrito.getColumnModel().getColumn(3).setPreferredWidth(70);
        tblCarrito.getColumnModel().getColumn(4).setPreferredWidth(76);
        tblCarrito.getColumnModel().getColumn(2).setMinWidth(46);
        tblCarrito.getColumnModel().getColumn(3).setMinWidth(56);
        tblCarrito.getColumnModel().getColumn(4).setMinWidth(62);
        // La columna ID se conserva en el modelo pero no se muestra
        tblCarrito.removeColumn(tblCarrito.getColumnModel().getColumn(0));

        JScrollPane scrollCar = new JScrollPane(tblCarrito);
        scrollCar.setBorder(new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 8, 1));
        scrollCar.getViewport().setBackground(Color.WHITE);
        pnlCardCarrito.add(scrollCar, BorderLayout.CENTER);

        // Totales (caja azul) + forma de pago + acciones
        JPanel pnlTotales = new JPanel();
        pnlTotales.setLayout(new BoxLayout(pnlTotales, BoxLayout.Y_AXIS));
        pnlTotales.setOpaque(false);

        JPanel pnlCajaTotal = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TemaFarmac.AZUL_PETROLEO);
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        pnlCajaTotal.setOpaque(false);
        pnlCajaTotal.setBorder(new EmptyBorder(10, 14, 10, 14));
        pnlCajaTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlCajaTotal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        JLabel lblSubEt = new JLabel("Subtotal:");
        lblSubEt.setFont(TemaFarmac.FUENTE_REGULAR);
        lblSubEt.setForeground(new Color(0xD5, 0xDD, 0xEE));
        lblSubtotal = new JLabel("$0.00");
        lblSubtotal.setFont(TemaFarmac.FUENTE_REGULAR);
        lblSubtotal.setForeground(new Color(0xD5, 0xDD, 0xEE));
        JPanel pnlSubFila = new JPanel(new BorderLayout());
        pnlSubFila.setOpaque(false);
        pnlSubFila.add(lblSubEt, BorderLayout.WEST);
        pnlSubFila.add(lblSubtotal, BorderLayout.EAST);

        lblTotal = new JLabel("$0.00");
        lblTotal.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 30));
        lblTotal.setForeground(TemaFarmac.VERDE_CLARO);
        JPanel pnlTotalFila = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlTotalFila.setOpaque(false);
        JLabel lblTotEt = new JLabel("Total: ");
        lblTotEt.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 30));
        lblTotEt.setForeground(TemaFarmac.VERDE_CLARO);
        pnlTotalFila.add(lblTotEt);
        pnlTotalFila.add(lblTotal);

        pnlCajaTotal.add(pnlSubFila, BorderLayout.NORTH);
        pnlCajaTotal.add(pnlTotalFila, BorderLayout.CENTER);

        JLabel lblForma = new JLabel("Forma de pago");
        lblForma.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblForma.setForeground(TemaFarmac.TEXTO_AZUL);
        lblForma.setAlignmentX(Component.LEFT_ALIGNMENT);

        cmbFormaPago = new JComboBox<>(new String[]{"EFECTIVO", "TARJETA", "TRANSFERENCIA"});
        TemaFarmac.estilizarComboBox(cmbFormaPago);
        cmbFormaPago.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbFormaPago.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JPanel pnlBotones = new JPanel(new BorderLayout(8, 0));
        pnlBotones.setOpaque(false);
        pnlBotones.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlBotones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        btnVaciar = TemaFarmac.crearBotonSecundario("Vaciar");
        btnCobrar = TemaFarmac.crearBoton("Cobrar y generar ticket PDF", TemaFarmac.TURQUESA, Color.WHITE);
        btnCobrar.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 13));
        btnCobrar.setPreferredSize(new Dimension(200, 46));
        btnCobrar.addActionListener(e -> finalizarCobro());
        pnlBotones.add(btnCobrar, BorderLayout.CENTER);
        pnlBotones.add(btnVaciar, BorderLayout.EAST);

        pnlTotales.add(pnlCajaTotal);
        pnlTotales.add(Box.createRigidArea(new Dimension(0, 12)));
        pnlTotales.add(lblForma);
        pnlTotales.add(Box.createRigidArea(new Dimension(0, 4)));
        pnlTotales.add(cmbFormaPago);
        pnlTotales.add(Box.createRigidArea(new Dimension(0, 10)));
        pnlTotales.add(pnlBotones);
        pnlCardCarrito.add(pnlTotales, BorderLayout.SOUTH);

        splitPane.setLeftComponent(pnlIzq);
        splitPane.setRightComponent(pnlCardCarrito);
        add(splitPane, BorderLayout.CENTER);

        // Aplicar la división proporcional en cuanto el panel tenga ancho real
        splitPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            private boolean aplicado = false;

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (!aplicado && splitPane.getWidth() > 0) {
                    splitPane.setDividerLocation(0.62);
                    aplicado = true;
                }
            }
        });
    }

    public void verificarEstadoCaja() {
        new javax.swing.SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return Sesion.hayTurnoAbierto();
            }

            @Override
            protected void done() {
                boolean abierta;
                try {
                    abierta = get();
                } catch (Exception e) {
                    abierta = false;
                }
                cajaAbierta = abierta;
                lblBannerCajaCerrada.setVisible(!abierta);
                txtEscaner.setEnabled(abierta);
                tblCatalogo.setEnabled(abierta);
                btnCobrar.setEnabled(abierta);
                btnVaciar.setEnabled(abierta);
                btnSumar.setEnabled(abierta);
                btnRestar.setEnabled(abierta);
                btnQuitar.setEnabled(abierta);
            }
        }.execute();
    }

    public void cargarDatosCatalogo() {
        if (catalogoCargando) return;
        catalogoCargando = true;

        new javax.swing.SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                List<Producto> productos = productoDAO.listarTodos(true);
                List<Lote> lotes = loteDAO.listarTodos();
                return new Object[]{productos, lotes};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] datos = get();
                    catalogoProductos = (List<Producto>) datos[0];
                    inventarioLotes = (List<Lote>) datos[1];
                    operacionesInventario.setInventario(inventarioLotes);
                    llenarTablaCatalogo();
                } catch (Exception e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    DialogoFarmac.mostrarJO(PuntoVentaScreen.this,
                            "Error al cargar catálogo: " + causa.getMessage(),
                            "Error BD", JOptionPane.ERROR_MESSAGE);
                } finally {
                    catalogoCargando = false;
                }
            }
        }.execute();
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
                p.isRequiereReceta() ? "Receta" : "Libre",
                "Agregar"
            });
        }
    }

    private void procesarDisparoEscaner() {
        if (!cajaAbierta) {
            DialogoFarmac.mostrarJO(this, "Debe abrir un turno de caja para procesar ventas.", "Caja Cerrada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String texto = txtEscaner.getText().trim();
        if (texto.isEmpty()) return;

        // Asegurar que el catálogo esté cargado
        if (catalogoProductos == null) {
            asegurarCatalogoCargado();
            DialogoFarmac.mostrarJO(this, "El catálogo se está cargando, intente de nuevo en un momento.", "Espere", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

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
            DialogoFarmac.mostrarJO(this, "Medicamento no encontrado para el código/término: " + texto, "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void agregarProductoPorIndice(int fila) {
        if (!cajaAbierta) {
            DialogoFarmac.mostrarJO(this, "Debe abrir un turno de caja para vender.", "Caja Cerrada", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (catalogoProductos == null) {
            asegurarCatalogoCargado();
            return;
        }
        if (fila >= 0 && fila < catalogoProductos.size()) {
            Producto p = catalogoProductos.get(fila);
            agregarProductoAlCarrito(p, 1);
        }
    }

    private void agregarProductoAlCarrito(Producto prod, int cantidad) {
        // Validar stock disponible en lotes vigentes
        int stockTotalDisponible = operacionesInventario.calcularStockDisponible(prod);

        if (stockTotalDisponible <= 0) {
            DialogoFarmac.mostrarJO(this,
                    "Sin existencias disponibles en almacén para '" + prod.getNombre() + "'.\n"
                    + "(No se permite despachar lotes vencidos ni sin stock)",
                    "Producto Agotado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cantidadActualCarrito = cantidadesCarrito.getOrDefault(prod.getId(), 0);
        int nuevaCantidad = cantidadActualCarrito + cantidad;

        if (nuevaCantidad > stockTotalDisponible) {
            DialogoFarmac.mostrarJO(this,
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
                DialogoFarmac.mostrarJO(this, "No puede exceder el stock disponible (" + stockDisp + " pzas).", "Aviso", JOptionPane.WARNING_MESSAGE);
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
        lblSubtotal.setText(String.format("$%.2f", total));
        lblTotal.setText(String.format("$%.2f", total));
    }

    private void limpiarCarrito() {
        cantidadesCarrito.clear();
        productosCarrito.clear();
        actualizarTablaCarrito();
    }

    private void finalizarCobro() {
        if (!Sesion.hayTurnoAbierto()) {
            DialogoFarmac.mostrarJO(this, "Debe abrir un turno de caja para poder cobrar.", "Caja Cerrada", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (cantidadesCarrito.isEmpty()) {
            DialogoFarmac.mostrarJO(this, "El carrito de ventas está vacío. Agregue medicamentos.", "Carrito Vacío", JOptionPane.WARNING_MESSAGE);
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
        // Diálogo de cobro: forma de pago, efectivo recibido y cambio
        Object[] cobroDlg = solicitarCobro(totalVenta, formaPago);
        if (cobroDlg == null) {
            return; // Cancelado por el usuario
        }
        formaPago = (String) cobroDlg[0];
        cmbFormaPago.setSelectedItem(formaPago);
        efectivoRecibido = (Double) cobroDlg[1];
        cambio = (Double) cobroDlg[2];

        // 1. Detectar productos controlados que requieran receta médica
        List<RecetaMedica> recetasCapturadas = new ArrayList<>();
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);

        for (Map.Entry<Integer, Integer> e : cantidadesCarrito.entrySet()) {
            Producto p = productosCarrito.get(e.getKey());
            if (p != null && p.isRequiereReceta()) {
                RegistroRecetaDialog dialog = new RegistroRecetaDialog(parentFrame, p);
                dialog.setVisible(true);

                if (!dialog.isConfirmado() || dialog.getRecetaRegistrada() == null) {
                    DialogoFarmac.mostrarJO(this,
                            "Cobro cancelado: No se completó el registro de receta médica obligatoria para:\n" + p.getNombre(),
                            "Venta Interrumpida", JOptionPane.WARNING_MESSAGE);
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

            // 3. Ejecutar Transacción ACID en Base de Datos
            ventaDAO.registrarVentaTransaccional(venta);

            // 4. Generar Ticket PDF oficial
            File ticketPDF = TicketGenerator.generarTicketPDF(venta);
            String rutaMsg = (ticketPDF != null) ? "\nTicket PDF archivado en:\n" + ticketPDF.getAbsolutePath() : "";

            BitacoraDAO.registrar(usuario, "VENTA_REGISTRADA",
                    "Venta #" + venta.getId() + " - Total: $" + venta.getTotal() + " [" + formaPago + "]");

            String mensajeCobro = "Venta #" + String.format("%05d", venta.getId()) + " registrada con éxito.\n"
                    + "Total: $" + String.format("%.2f", venta.getTotal()) + "\n"
                    + ("EFECTIVO".equalsIgnoreCase(formaPago) ? "Efectivo Recibido: $" + String.format("%.2f", efectivoRecibido) + "\nCAMBIO A ENTREGAR: $" + String.format("%.2f", cambio) + "\n" : "")
                    + rutaMsg;

            DialogoFarmac.mostrarJO(this, mensajeCobro, "Cobro Completado con Éxito", JOptionPane.INFORMATION_MESSAGE);

            // Limpiar y refrescar catálogo
            limpiarCarrito();
            cargarDatosCatalogo();
            txtEscaner.requestFocusInWindow();

        } catch (SQLException ex) {
            DialogoFarmac.mostrarJO(this,
                    "Error transaccional en la venta (Se ejecutó Rollback): " + ex.getMessage(),
                    "Error de Transacción", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Diálogo "Cobrar Venta". Devuelve {formaPago, efectivoRecibido, cambio} o null si se cancela.
     * Enter avanza entre campos y, en el último, confirma el cobro.
     */
    private Object[] solicitarCobro(double total, String formaInicial) {
        java.awt.Window dueno = SwingUtilities.getWindowAncestor(this);
        javax.swing.JDialog dlg = new javax.swing.JDialog(dueno, "Cobrar Venta", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        final Object[][] resultado = {null};
        final String[] forma = {formaInicial};

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Color.WHITE);

        JLabel lblTit = new JLabel("Cobrar Venta");
        lblTit.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 17));
        lblTit.setForeground(TemaFarmac.TEXTO_AZUL);
        lblTit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TemaFarmac.BORDE),
                new EmptyBorder(16, 22, 14, 22)));
        raiz.add(lblTit, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(new EmptyBorder(18, 22, 12, 22));

        JPanel cajaTotal = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TemaFarmac.AZUL_PETROLEO);
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        cajaTotal.setOpaque(false);
        cajaTotal.setBorder(new EmptyBorder(18, 20, 18, 20));
        cajaTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
        cajaTotal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel lblTc = new JLabel("Total a cobrar");
        lblTc.setForeground(Color.WHITE);
        lblTc.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 14));
        JLabel lblTm = new JLabel(String.format("$%.2f", total));
        lblTm.setForeground(TemaFarmac.VERDE_CLARO);
        lblTm.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 30));
        cajaTotal.add(lblTc, BorderLayout.WEST);
        cajaTotal.add(lblTm, BorderLayout.EAST);

        JLabel lblFp = new JLabel("Forma de pago");
        lblFp.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblFp.setForeground(TemaFarmac.TEXTO_AZUL);
        lblFp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel segmento = new JPanel(new GridLayout(1, 3, 4, 0)) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xA9, 0xAC, 0xA9));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        segmento.setOpaque(false);
        segmento.setBorder(new EmptyBorder(4, 4, 4, 4));
        segmento.setAlignmentX(Component.LEFT_ALIGNMENT);
        segmento.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JLabel lblRec = new JLabel("Efectivo recibido ($)");
        lblRec.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblRec.setForeground(TemaFarmac.TEXTO_AZUL);
        lblRec.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField txtRecibido = TemaFarmac.crearCampoTexto(String.format("%.2f", total), 10);
        txtRecibido.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 18));
        txtRecibido.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtRecibido.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        txtRecibido.setPreferredSize(new Dimension(300, 46));

        JPanel cajaCambio = new JPanel(new BorderLayout());
        cajaCambio.setBackground(new Color(0xDF, 0xF0, 0xE6));
        cajaCambio.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(new Color(0xB5, 0xD9, 0xC4), 8, 1), new EmptyBorder(14, 16, 14, 16)));
        cajaCambio.setAlignmentX(Component.LEFT_ALIGNMENT);
        cajaCambio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        JLabel lblCe = new JLabel("CAMBIO A ENTREGAR");
        lblCe.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblCe.setForeground(TemaFarmac.VERDE);
        JLabel lblCm = new JLabel("$0.00");
        lblCm.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 24));
        lblCm.setForeground(TemaFarmac.VERDE);
        cajaCambio.add(lblCe, BorderLayout.WEST);
        cajaCambio.add(lblCm, BorderLayout.EAST);

        Runnable actualizarCambio = () -> {
            try {
                double rec = Double.parseDouble(txtRecibido.getText().trim());
                double cambio = Math.max(0, Math.round((rec - total) * 100.0) / 100.0);
                lblCm.setText(String.format("$%.2f", cambio));
            } catch (NumberFormatException ex) {
                lblCm.setText("$0.00");
            }
        };
        txtRecibido.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarCambio.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarCambio.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarCambio.run(); }
        });

        String[] formas = {"EFECTIVO", "TARJETA", "TRANSFERENCIA"};
        String[] etiquetas = {"Efectivo", "Tarjeta", "Transferencia"};
        List<JButton> botonesForma = new ArrayList<>();
        Runnable refrescarForma = () -> {
            for (int i = 0; i < botonesForma.size(); i++) {
                JButton b = botonesForma.get(i);
                boolean activo = formas[i].equals(forma[0]);
                ((TemaFarmac.BotonSegmento) b).setActivo(activo);
            }
            boolean efectivo = "EFECTIVO".equals(forma[0]);
            lblRec.setVisible(efectivo);
            txtRecibido.setVisible(efectivo);
            cajaCambio.setVisible(efectivo);
            dlg.revalidate();
            dlg.repaint();
        };
        for (int i = 0; i < formas.length; i++) {
            final String valor = formas[i];
            JButton b = new TemaFarmac.BotonSegmento(etiquetas[i]);
            b.setFont(TemaFarmac.FUENTE_NEGRITA);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            b.addActionListener(e -> {
                forma[0] = valor;
                refrescarForma.run();
                if ("EFECTIVO".equals(valor)) {
                    txtRecibido.requestFocusInWindow();
                    txtRecibido.selectAll();
                }
            });
            botonesForma.add(b);
            segmento.add(b);
        }

        cuerpo.add(cajaTotal);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 16)));
        cuerpo.add(lblFp);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 4)));
        cuerpo.add(segmento);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 12)));
        cuerpo.add(lblRec);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 4)));
        cuerpo.add(txtRecibido);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 12)));
        cuerpo.add(cajaCambio);
        raiz.add(cuerpo, BorderLayout.CENTER);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, TemaFarmac.BORDE),
                new EmptyBorder(14, 12, 14, 12)));
        JButton btnCancelar = TemaFarmac.crearBotonSecundario("Cancelar");
        JButton btnConfirmar = TemaFarmac.crearBoton("Confirmar y generar ticket PDF", TemaFarmac.TURQUESA, Color.WHITE);
        btnConfirmar.setIcon(FontIcon.of(FontAwesomeSolid.CHECK, 12, Color.WHITE));
        btnConfirmar.setIconTextGap(8);
        pie.add(btnCancelar);
        pie.add(btnConfirmar);
        raiz.add(pie, BorderLayout.SOUTH);

        Runnable confirmar = () -> {
            double recibido = total;
            double cambio = 0.0;
            if ("EFECTIVO".equals(forma[0])) {
                try {
                    recibido = Double.parseDouble(txtRecibido.getText().trim());
                } catch (NumberFormatException ex) {
                    DialogoFarmac.mostrarJO(dlg, "Monto recibido inválido.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (recibido < total) {
                    DialogoFarmac.mostrarJO(dlg, "El efectivo recibido es menor al total a pagar.", "Importe Insuficiente", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                cambio = Math.round((recibido - total) * 100.0) / 100.0;
            }
            resultado[0] = new Object[]{forma[0], recibido, cambio};
            dlg.dispose();
        };
        btnConfirmar.addActionListener(e -> confirmar.run());
        btnCancelar.addActionListener(e -> dlg.dispose());
        // Enter en el campo de efectivo confirma el cobro
        TemaFarmac.enterComoTab(confirmar, txtRecibido);
        dlg.getRootPane().registerKeyboardAction(e -> dlg.dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        refrescarForma.run();
        dlg.setContentPane(raiz);
        dlg.setSize(560, 560);
        dlg.pack();
        dlg.setSize(Math.max(dlg.getWidth(), 560), dlg.getHeight());
        dlg.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(() -> {
            if ("EFECTIVO".equals(forma[0])) {
                txtRecibido.requestFocusInWindow();
                txtRecibido.selectAll();
            } else {
                btnConfirmar.requestFocusInWindow();
            }
        });
        dlg.setVisible(true);
        return resultado[0];
    }
}
