package farmac.ui;

import farmac.dao.LoteDAO;
import farmac.dao.ProductoDAO;
import farmac.dominio.Lote;
import farmac.dominio.OperacionesInventario;
import farmac.dominio.Producto;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla de Reporte de Existencias y Valorización Total del Inventario.
 * Accesible exclusivamente para Encargados.
 */
public class ReporteExistenciasScreen extends JPanel {

    private final ProductoDAO productoDAO;
    private final LoteDAO loteDAO;
    private final OperacionesInventario operacionesInventario;

    private JLabel lblTotalProductos;
    private JLabel lblBajoStock;
    private JLabel lblTotalUnidades;
    private JLabel lblValorTotal;

    private JTable tablaExistencias;
    private DefaultTableModel modeloTabla;
    private JTextField txtBuscar;
    private List<Producto> listaProductos;
    private List<Lote> listaLotes;

    public ReporteExistenciasScreen() {
        this.productoDAO = new ProductoDAO();
        this.loteDAO = new LoteDAO();
        this.operacionesInventario = new OperacionesInventario();
        initComponents();
        // Cargar después de pintar la pantalla, sin bloquear la navegación
        javax.swing.SwingUtilities.invokeLater(this::cargarDatos);
    }

    private javax.swing.JComboBox<String> cmbFiltroEstado;

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));

        // Cabecera
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        JLabel lblTitulo = new JLabel("Reporte de existencias y valorización");
        lblTitulo.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 20));
        lblTitulo.setForeground(TemaFarmac.TEXTO_AZUL);

        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlAcciones.setOpaque(false);
        JButton btnRefrescar = TemaFarmac.crearBotonSecundario("Actualizar");
        btnRefrescar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 12, TemaFarmac.TEXTO));
        btnRefrescar.setIconTextGap(8);
        btnRefrescar.addActionListener(e -> cargarDatos());
        JButton btnExportar = TemaFarmac.crearBotonSecundario("Exportar CSV");
        btnExportar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.DOWNLOAD, 12, TemaFarmac.TEXTO));
        btnExportar.setIconTextGap(8);
        btnExportar.addActionListener(e -> exportarCSV());
        pnlAcciones.add(btnRefrescar);
        pnlAcciones.add(btnExportar);
        pnlHeader.add(lblTitulo, BorderLayout.WEST);
        pnlHeader.add(pnlAcciones, BorderLayout.EAST);

        // KPIs
        lblTotalProductos = new JLabel("0");
        lblBajoStock = new JLabel("0");
        lblTotalUnidades = new JLabel("0");
        lblValorTotal = new JLabel("$0.00");

        JPanel pnlKPIs = new JPanel(new GridLayout(1, 4, 14, 0));
        pnlKPIs.setOpaque(false);
        pnlKPIs.add(TemaFarmac.crearTarjetaKPI("Productos activos", lblTotalProductos, TemaFarmac.TEXTO_AZUL,
                java.awt.Color.WHITE, TemaFarmac.TEXTO_MUTED, null, 24));
        pnlKPIs.add(TemaFarmac.crearTarjetaKPI("Bajo stock (alerta)", lblBajoStock, TemaFarmac.AMBAR,
                TemaFarmac.AMBAR_BG, TemaFarmac.AMBAR, null, 24));
        pnlKPIs.add(TemaFarmac.crearTarjetaKPI("Unidades en stock", lblTotalUnidades, TemaFarmac.TEXTO_AZUL,
                java.awt.Color.WHITE, TemaFarmac.TEXTO_MUTED, null, 24));
        pnlKPIs.add(TemaFarmac.crearTarjetaKPI("Valor total del inventario", lblValorTotal, TemaFarmac.VERDE_CLARO,
                TemaFarmac.AZUL_PETROLEO, java.awt.Color.WHITE, null, 24));

        // Filtros
        cmbFiltroEstado = new javax.swing.JComboBox<>(new String[]{"TODOS", "BAJO STOCK", "ÓPTIMO"});
        TemaFarmac.estilizarComboBox(cmbFiltroEstado);
        cmbFiltroEstado.setPreferredSize(new java.awt.Dimension(240, 40));
        cmbFiltroEstado.addActionListener(e -> filtrarTabla(txtBuscar.getText()));

        txtBuscar = TemaFarmac.crearCampoTexto(14);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar producto...");
        txtBuscar.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        txtBuscar.setOpaque(false);
        txtBuscar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filtrarTabla(txtBuscar.getText());
            }
        });
        JPanel pnlCampoBuscar = new JPanel(new BorderLayout(6, 0));
        pnlCampoBuscar.setBackground(java.awt.Color.WHITE);
        pnlCampoBuscar.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE_CAMPO, 8, 1),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        pnlCampoBuscar.setPreferredSize(new java.awt.Dimension(300, 40));
        pnlCampoBuscar.add(new JLabel(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SEARCH, 13, TemaFarmac.TEXTO_MUTED)), BorderLayout.WEST);
        pnlCampoBuscar.add(txtBuscar, BorderLayout.CENTER);

        JPanel pnlFiltros = new JPanel(new java.awt.GridBagLayout());
        pnlFiltros.setBackground(java.awt.Color.WHITE);
        pnlFiltros.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 10, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        java.awt.GridBagConstraints g = new java.awt.GridBagConstraints();
        g.anchor = java.awt.GridBagConstraints.WEST;
        JLabel l1 = new JLabel("Filtrar");
        l1.setFont(TemaFarmac.FUENTE_NEGRITA);
        l1.setForeground(TemaFarmac.TEXTO_AZUL);
        JLabel l2 = new JLabel("Buscar");
        l2.setFont(TemaFarmac.FUENTE_NEGRITA);
        l2.setForeground(TemaFarmac.TEXTO_AZUL);
        g.gridx = 0; g.gridy = 0; g.insets = new java.awt.Insets(0, 0, 4, 14);
        pnlFiltros.add(l1, g);
        g.gridx = 1;
        pnlFiltros.add(l2, g);
        g.gridx = 0; g.gridy = 1;
        pnlFiltros.add(cmbFiltroEstado, g);
        g.gridx = 1; g.weightx = 1; g.fill = java.awt.GridBagConstraints.HORIZONTAL; g.insets = new java.awt.Insets(0, 0, 0, 0);
        pnlFiltros.add(pnlCampoBuscar, g);

        JPanel pnlNorte = new JPanel();
        pnlNorte.setOpaque(false);
        pnlNorte.setLayout(new javax.swing.BoxLayout(pnlNorte, javax.swing.BoxLayout.Y_AXIS));
        pnlHeader.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pnlKPIs.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pnlFiltros.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pnlNorte.add(pnlHeader);
        pnlNorte.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 12)));
        pnlNorte.add(pnlKPIs);
        pnlNorte.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 12)));
        pnlNorte.add(pnlFiltros);
        add(pnlNorte, BorderLayout.NORTH);

        // Tabla
        String[] columnas = {
            "Código", "Producto", "Categoría", "Proveedor",
            "Mín.", "Stock", "Caducidad", "Valor ($)", "Estado"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaExistencias = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tablaExistencias);
        tablaExistencias.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaExistencias.getColumnModel().getColumn(1).setPreferredWidth(190);
        tablaExistencias.getColumnModel().getColumn(2).setPreferredWidth(150);
        tablaExistencias.getColumnModel().getColumn(3).setPreferredWidth(140);
        tablaExistencias.getColumnModel().getColumn(4).setPreferredWidth(50);
        tablaExistencias.getColumnModel().getColumn(5).setPreferredWidth(80);
        tablaExistencias.getColumnModel().getColumn(6).setPreferredWidth(100);
        tablaExistencias.getColumnModel().getColumn(7).setPreferredWidth(100);
        tablaExistencias.getColumnModel().getColumn(8).setPreferredWidth(100);
        tablaExistencias.getColumnModel().getColumn(8).setCellRenderer(new TemaFarmac.BadgeCellRenderer());

        JScrollPane scroll = new JScrollPane(tablaExistencias);
        scroll.setBorder(new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 8, 1));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }

    public void cargarDatos() {
        try {
            listaProductos = productoDAO.listarTodos(true);
            listaLotes = loteDAO.listarTodos();
            operacionesInventario.setInventario(listaLotes);

            actualizarKPIs();
            actualizarTablaConLista(listaProductos);
        } catch (SQLException e) {
            DialogoFarmac.mostrarJO(this, "Error al cargar existencias: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarKPIs() {
        if (listaProductos == null || listaLotes == null) return;

        int totalProd = listaProductos.size();
        List<Producto> bajos = operacionesInventario.productosBajoStock(listaProductos);
        int totalBajos = bajos.size();

        int sumUnidades = 0;
        for (Lote l : listaLotes) {
            if (l != null && l.getCantidadActual() > 0) {
                sumUnidades += l.getCantidadActual();
            }
        }

        double valTotal = operacionesInventario.valorTotalInventario();

        lblTotalProductos.setText(String.valueOf(totalProd));
        lblBajoStock.setText(String.valueOf(totalBajos));
        lblTotalUnidades.setText(sumUnidades + " pzas");
        lblValorTotal.setText(String.format("$%,.2f", valTotal));
    }

    private void actualizarTablaConLista(List<Producto> productos) {
        modeloTabla.setRowCount(0);
        if (productos == null || listaLotes == null) return;

        for (Producto p : productos) {
            int stock = p.calcularStockTotal(listaLotes);
            LocalDate cadProxima = p.obtenerCaducidadProxima(listaLotes);

            // Calcular valorización de este producto sumando sus lotes
            double valorProducto = 0.0;
            for (Lote l : listaLotes) {
                if (l != null && l.getProducto() != null && l.getProducto().getId() == p.getId() && l.getCantidadActual() > 0) {
                    valorProducto += l.getCantidadActual() * l.getPrecioCompra();
                }
            }

            boolean bajo = stock <= p.getStockMinimo();
            String estado = bajo ? "BAJO STOCK" : "ÓPTIMO";

            modeloTabla.addRow(new Object[]{
                p.getCodigoBarras(),
                p.getNombre(),
                p.getCategoria(),
                p.getProveedor(),
                p.getStockMinimo(),
                stock + " pzas",
                cadProxima != null ? cadProxima.toString() : "Sin lotes vigentes",
                String.format("$%,.2f", valorProducto),
                estado
            });
        }
    }

    private void filtrarTabla(String texto) {
        if (listaProductos == null) return;
        String t = texto == null ? "" : texto.trim().toLowerCase();
        String estadoSel = cmbFiltroEstado != null ? (String) cmbFiltroEstado.getSelectedItem() : "TODOS";
        List<Producto> filtrada = listaProductos.stream()
                .filter(p -> t.isEmpty()
                          || p.getNombre().toLowerCase().contains(t)
                          || p.getCodigoBarras().toLowerCase().contains(t)
                          || p.getCategoria().toLowerCase().contains(t))
                .filter(p -> {
                    if (estadoSel == null || "TODOS".equals(estadoSel)) return true;
                    boolean bajo = listaLotes != null && p.calcularStockTotal(listaLotes) <= p.getStockMinimo();
                    return "BAJO STOCK".equals(estadoSel) ? bajo : !bajo;
                })
                .toList();
        actualizarTablaConLista(filtrada);
    }

    private void exportarCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("reporte_existencias_" + LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("CodigoBarras,Producto,Categoria,Proveedor,StockMinimo,StockActual,CaducidadProxima,ValorStock,Estado");
                for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                    pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            modeloTabla.getValueAt(i, 0),
                            modeloTabla.getValueAt(i, 1),
                            modeloTabla.getValueAt(i, 2),
                            modeloTabla.getValueAt(i, 3),
                            modeloTabla.getValueAt(i, 4),
                            modeloTabla.getValueAt(i, 5),
                            modeloTabla.getValueAt(i, 6),
                            modeloTabla.getValueAt(i, 7),
                            modeloTabla.getValueAt(i, 8));
                }
                DialogoFarmac.mostrarJO(this, "Reporte exportado exitosamente a:\n" + f.getAbsolutePath(), "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                DialogoFarmac.mostrarJO(this, "Error al exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
