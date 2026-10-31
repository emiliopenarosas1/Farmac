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
 * Pantalla de Reporte de Existencias y Valorización Total del Inventario (RF18).
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
        cargarDatos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Encabezado
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(TemaFarmac.GRIS_FONDO);

        JLabel lblTitulo = new JLabel("Reporte de Existencias y Valorización");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Consolidación de stock por producto, alertas de desabasto y valuación total (RF18)");
        lblSub.setFont(TemaFarmac.FUENTE_REGULAR);
        lblSub.setForeground(TemaFarmac.GRIS_TEXTO);

        JPanel pnlTitulos = new JPanel(new BorderLayout(0, 4));
        pnlTitulos.setBackground(TemaFarmac.GRIS_FONDO);
        pnlTitulos.add(lblTitulo, BorderLayout.NORTH);
        pnlTitulos.add(lblSub, BorderLayout.SOUTH);
        pnlHeader.add(pnlTitulos, BorderLayout.WEST);

        // Barra derecha: Búsqueda y Exportar
        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlAcciones.setBackground(TemaFarmac.GRIS_FONDO);

        txtBuscar = TemaFarmac.crearCampoTexto(14);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar producto...");
        txtBuscar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filtrarTabla(txtBuscar.getText());
            }
        });

        JButton btnRefrescar = TemaFarmac.crearBotonSecundario("Actualizar");
        btnRefrescar.addActionListener(e -> cargarDatos());

        JButton btnExportar = TemaFarmac.crearBotonPrimario("Exportar CSV");
        btnExportar.addActionListener(e -> exportarCSV());

        pnlAcciones.add(new JLabel("Filtrar:"));
        pnlAcciones.add(txtBuscar);
        pnlAcciones.add(btnRefrescar);
        pnlAcciones.add(btnExportar);

        pnlHeader.add(pnlAcciones, BorderLayout.EAST);

        // Panel de KPIs superiores
        JPanel pnlKPIs = new JPanel(new GridLayout(1, 4, 15, 0));
        pnlKPIs.setBackground(TemaFarmac.GRIS_FONDO);
        pnlKPIs.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        lblTotalProductos = new JLabel("0", JLabel.CENTER);
        lblBajoStock = new JLabel("0", JLabel.CENTER);
        lblTotalUnidades = new JLabel("0", JLabel.CENTER);
        lblValorTotal = new JLabel("$0.00", JLabel.CENTER);

        pnlKPIs.add(TemaFarmac.crearTarjetaKPI("Productos Activos", lblTotalProductos, TemaFarmac.AZUL_PETROLEO));
        pnlKPIs.add(TemaFarmac.crearTarjetaKPI("Bajo Stock (Alerta)", lblBajoStock, TemaFarmac.AMBAR));
        pnlKPIs.add(TemaFarmac.crearTarjetaKPI("Unidades en Stock", lblTotalUnidades, TemaFarmac.TURQUESA));
        pnlKPIs.add(TemaFarmac.crearTarjetaKPI("Valor Total Inventario", lblValorTotal, TemaFarmac.VERDE));

        JPanel pnlNorte = new JPanel(new BorderLayout(0, 10));
        pnlNorte.setBackground(TemaFarmac.GRIS_FONDO);
        pnlNorte.add(pnlHeader, BorderLayout.NORTH);
        pnlNorte.add(pnlKPIs, BorderLayout.SOUTH);

        add(pnlNorte, BorderLayout.NORTH);

        // Tabla
        String[] columnas = {
            "Código Barras", "Nombre Producto", "Categoría", "Proveedor",
            "Stock Mín.", "Stock Actual", "Caducidad Próx.", "Valor Stock ($)", "Estado"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaExistencias = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tablaExistencias);

        JScrollPane scroll = new JScrollPane(tablaExistencias);
        scroll.setBorder(BorderFactory.createLineBorder(TemaFarmac.GRIS_BORDE));
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
        if (texto == null || texto.trim().isEmpty()) {
            actualizarTablaConLista(listaProductos);
            return;
        }
        String t = texto.trim().toLowerCase();
        List<Producto> filtrada = listaProductos.stream()
                .filter(p -> p.getNombre().toLowerCase().contains(t)
                          || p.getCodigoBarras().toLowerCase().contains(t)
                          || p.getCategoria().toLowerCase().contains(t))
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
