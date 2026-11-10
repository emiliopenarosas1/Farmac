package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.LoteDAO;
import farmac.dao.ProductoDAO;
import farmac.dao.ProveedorDAO;
import farmac.dominio.Lote;
import farmac.dominio.Producto;
import farmac.dominio.Proveedor;
import farmac.dominio.Usuario;
import farmac.util.Sesion;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla de Catálogo y Mantenimiento de Medicamentos.
 * Persistencia en PostgreSQL, baja lógica, proveedores dinámicos y stock/caducidad calculados.
 */
public class CatalogoProductosScreen extends JPanel {

    private final Usuario usuarioActivo;
    private final ProductoDAO productoDAO;
    private final ProveedorDAO proveedorDAO;
    private final LoteDAO loteDAO;

    private JTextField txtBuscar;
    private JComboBox<String> cmbFiltroCategoria;
    private JTable tblProductos;
    private DefaultTableModel modeloTabla;
    private List<Producto> listaProductosActual;
    private List<Lote> listaLotesActual;

    public CatalogoProductosScreen(Usuario usuario) {
        this.usuarioActivo = usuario != null ? usuario : Sesion.getUsuarioActivo();
        this.productoDAO = new ProductoDAO();
        this.proveedorDAO = new ProveedorDAO();
        this.loteDAO = new LoteDAO();
        this.listaProductosActual = new ArrayList<>();
        this.listaLotesActual = new ArrayList<>();

        inicializarUI();
        // Cargar después de pintar la pantalla, sin bloquear la navegación
        javax.swing.SwingUtilities.invokeLater(this::cargarDatos);
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 12));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(20, 26, 20, 26));

        boolean esAdmin = (usuarioActivo != null) && "ENCARGADO".equalsIgnoreCase(usuarioActivo.obtenerRol());

        // Cabecera: título + exportar / nuevo
        JPanel pnlTit = new JPanel(new BorderLayout());
        pnlTit.setOpaque(false);
        JLabel lblTitulo = new JLabel("Catálogo de medicamentos y productos");
        lblTitulo.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 20));
        lblTitulo.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlTit.add(lblTitulo, BorderLayout.WEST);

        JPanel pnlAccionesTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlAccionesTop.setOpaque(false);
        JButton btnExportar = TemaFarmac.crearBotonSecundario("Exportar CSV");
        btnExportar.setIcon(FontIcon.of(FontAwesomeSolid.DOWNLOAD, 12, TemaFarmac.TEXTO));
        btnExportar.setIconTextGap(8);
        btnExportar.addActionListener(e -> exportarCSV());
        pnlAccionesTop.add(btnExportar);
        if (esAdmin) {
            JButton btnNuevo = TemaFarmac.crearBoton("Nuevo medicamento", TemaFarmac.TURQUESA, Color.WHITE);
            btnNuevo.setIcon(FontIcon.of(FontAwesomeSolid.PLUS, 12, Color.WHITE));
            btnNuevo.setIconTextGap(8);
            btnNuevo.addActionListener(e -> abrirFormularioNuevo());
            pnlAccionesTop.add(btnNuevo);
        }
        pnlTit.add(pnlAccionesTop, BorderLayout.EAST);

        // Filtros
        cmbFiltroCategoria = new JComboBox<>(new String[]{"TODAS"});
        TemaFarmac.estilizarComboBox(cmbFiltroCategoria);
        cmbFiltroCategoria.setPreferredSize(new Dimension(240, 40));
        cmbFiltroCategoria.addActionListener(e -> filtrar());

        txtBuscar = TemaFarmac.crearCampoTexto(14);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Nombre o código...");
        txtBuscar.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        txtBuscar.setOpaque(false);
        txtBuscar.addActionListener(e -> filtrar());
        JPanel pnlCampoBuscar = new JPanel(new BorderLayout(6, 0));
        pnlCampoBuscar.setBackground(Color.WHITE);
        pnlCampoBuscar.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE_CAMPO, 8, 1),
                new EmptyBorder(0, 10, 0, 10)));
        pnlCampoBuscar.add(new JLabel(FontIcon.of(FontAwesomeSolid.SEARCH, 13, TemaFarmac.TEXTO_MUTED)), BorderLayout.WEST);
        pnlCampoBuscar.add(txtBuscar, BorderLayout.CENTER);

        JButton btnBuscar = TemaFarmac.crearBoton("Buscar", TemaFarmac.TURQUESA, Color.WHITE);
        btnBuscar.addActionListener(e -> filtrar());

        JPanel pnlFiltros = new JPanel(new GridBagLayout());
        pnlFiltros.setBackground(Color.WHITE);
        pnlFiltros.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 10, 1),
                new EmptyBorder(12, 16, 12, 16)));
        java.awt.GridBagConstraints g = new java.awt.GridBagConstraints();
        g.anchor = java.awt.GridBagConstraints.WEST;
        JLabel lblCat = new JLabel("Categoría");
        lblCat.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblCat.setForeground(TemaFarmac.TEXTO_AZUL);
        JLabel lblBus = new JLabel("Buscar");
        lblBus.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblBus.setForeground(TemaFarmac.TEXTO_AZUL);
        g.gridx = 0; g.gridy = 0; g.insets = new java.awt.Insets(0, 0, 4, 14);
        pnlFiltros.add(lblCat, g);
        g.gridx = 1; g.insets = new java.awt.Insets(0, 0, 4, 14);
        pnlFiltros.add(lblBus, g);
        g.gridx = 0; g.gridy = 1; g.insets = new java.awt.Insets(0, 0, 0, 14);
        pnlFiltros.add(cmbFiltroCategoria, g);
        g.gridx = 1; g.weightx = 1; g.fill = java.awt.GridBagConstraints.HORIZONTAL; g.insets = new java.awt.Insets(0, 0, 0, 14);
        pnlCampoBuscar.setPreferredSize(new Dimension(300, 40));
        pnlFiltros.add(pnlCampoBuscar, g);
        g.gridx = 2; g.weightx = 0; g.fill = java.awt.GridBagConstraints.NONE; g.insets = new java.awt.Insets(0, 0, 0, 0);
        btnBuscar.setPreferredSize(new Dimension(90, 40));
        pnlFiltros.add(btnBuscar, g);

        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setOpaque(false);
        pnlTit.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pnlFiltros.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pnlNorte.add(pnlTit);
        pnlNorte.add(Box.createRigidArea(new Dimension(0, 12)));
        pnlNorte.add(pnlFiltros);
        add(pnlNorte, BorderLayout.NORTH);

        // Tabla principal
        String[] columnas = {
            "ID", "Código", "Medicamento", "Categoría",
            "Proveedor", "P. venta", "Tipo", "Mín.", "Existencia", "Caduc."
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblProductos = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tblProductos);
        tblProductos.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblProductos.getColumnModel().getColumn(1).setPreferredWidth(90);
        tblProductos.getColumnModel().getColumn(2).setPreferredWidth(190);
        tblProductos.getColumnModel().getColumn(3).setPreferredWidth(130);
        tblProductos.getColumnModel().getColumn(4).setPreferredWidth(140);
        tblProductos.getColumnModel().getColumn(5).setPreferredWidth(75);
        tblProductos.getColumnModel().getColumn(6).setPreferredWidth(70);
        tblProductos.getColumnModel().getColumn(7).setPreferredWidth(60);
        tblProductos.getColumnModel().getColumn(8).setPreferredWidth(85);
        tblProductos.getColumnModel().getColumn(9).setPreferredWidth(90);
        tblProductos.getColumnModel().getColumn(6).setCellRenderer(new TemaFarmac.BadgeCellRenderer());
        // La columna ID se conserva en el modelo pero no se muestra
        tblProductos.removeColumn(tblProductos.getColumnModel().getColumn(0));

        JScrollPane scroll = new JScrollPane(tblProductos);
        scroll.setBorder(new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 8, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        // Acciones sobre la fila seleccionada
        if (esAdmin) {
            JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            pnlAcciones.setOpaque(false);
            JButton btnEditar = TemaFarmac.crearBotonSecundario("Editar medicamento");
            btnEditar.setIcon(FontIcon.of(FontAwesomeSolid.PENCIL_ALT, 12, TemaFarmac.TEXTO));
            btnEditar.setIconTextGap(8);
            btnEditar.addActionListener(e -> abrirFormularioEditar());
            JButton btnEliminar = TemaFarmac.crearBotonPeligro("Desactivar (baja lógica)");
            btnEliminar.setIcon(FontIcon.of(FontAwesomeSolid.POWER_OFF, 12, TemaFarmac.ROJO));
            btnEliminar.setIconTextGap(8);
            btnEliminar.addActionListener(e -> eliminarProductoSeleccionado());
            pnlAcciones.add(btnEditar);
            pnlAcciones.add(btnEliminar);
            add(pnlAcciones, BorderLayout.SOUTH);
        }
    }

    public void cargarDatos() {
        try {
            listaProductosActual = productoDAO.listarTodos(true); // Solo productos activos
            listaLotesActual = loteDAO.listarTodos();

            // Cargar categorías en el combo
            List<String> categorias = productoDAO.listarCategorias();
            String seleccionPrevia = (String) cmbFiltroCategoria.getSelectedItem();
            cmbFiltroCategoria.removeAllItems();
            cmbFiltroCategoria.addItem("TODAS");
            for (String c : categorias) {
                cmbFiltroCategoria.addItem(c);
            }
            if (seleccionPrevia != null) {
                cmbFiltroCategoria.setSelectedItem(seleccionPrevia);
            }

            llenarTabla(listaProductosActual);
        } catch (SQLException e) {
            DialogoFarmac.mostrarJO(this, "Error al cargar medicamentos: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void llenarTabla(List<Producto> productos) {
        modeloTabla.setRowCount(0);
        if (productos == null) return;
        for (Producto p : productos) {
            int stock = p.calcularStockTotal(listaLotesActual);
            LocalDate cadProxima = p.obtenerCaducidadProxima(listaLotesActual);

            modeloTabla.addRow(new Object[]{
                p.getId(),
                p.getCodigoBarras(),
                p.getNombre(),
                p.getCategoria(),
                p.getProveedor(),
                String.format("$%.2f", p.getPrecioVenta()),
                p.isRequiereReceta() ? "Receta" : "Libre",
                p.getStockMinimo() + " pzas",
                stock + " pzas",
                cadProxima != null ? cadProxima.toString() : "Sin lotes vigentes"
            });
        }
    }

    private void filtrar() {
        String texto = txtBuscar.getText().trim();
        String cat = (String) cmbFiltroCategoria.getSelectedItem();
        try {
            List<Producto> filtrados = productoDAO.buscarPorFiltro(texto, cat, true);
            llenarTabla(filtrados);
        } catch (SQLException e) {
DialogoFarmac.mostrar(this, "Error", "Error al filtrar: " + e.getMessage(), DialogoFarmac.Tipo.ERROR);
        }
    }

    private Producto obtenerProductoSeleccionado() {
        int fila = tblProductos.getSelectedRow();
        if (fila < 0) {
            DialogoFarmac.mostrarJO(this, "Seleccione un medicamento de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int id = (int) modeloTabla.getValueAt(fila, 0);
        try {
            return productoDAO.buscarPorId(id);
        } catch (SQLException e) {
DialogoFarmac.mostrar(this, "Error", "Error al consultar producto: " + e.getMessage(), DialogoFarmac.Tipo.ERROR);
            return null;
        }
    }

    private void abrirFormularioNuevo() {
        try {
            List<Proveedor> proveedores = proveedorDAO.listarTodos(true);
            if (proveedores.isEmpty()) {
                DialogoFarmac.mostrarJO(this, "Debe registrar al menos un Proveedor antes de agregar productos.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<String> categorias = productoDAO.listarCategorias();

            JTextField txtCod = TemaFarmac.crearCampoTexto(15);
            JTextField txtNom = TemaFarmac.crearCampoTexto(20);
            JComboBox<String> cmbCat = new JComboBox<>(categorias.toArray(new String[0]));
            cmbCat.setEditable(true);
            TemaFarmac.estilizarComboBox(cmbCat);
            JComboBox<Proveedor> cmbProv = new JComboBox<>(proveedores.toArray(new Proveedor[0]));
            TemaFarmac.estilizarComboBox(cmbProv);
            JTextField txtPrecio = TemaFarmac.crearCampoTexto("50.00", 10);
            JCheckBox chkReceta = new JCheckBox("Requiere Receta Médica Obligatoria (Antibiótico / Controlado)");
            JTextField txtMin = TemaFarmac.crearCampoTexto("5", 5);

            JPanel form = TemaFarmac.crearFormulario(
                    new String[]{
                        "Código de Barras *:",
                        "Nombre Medicamento *:",
                        "Categoría:",
                        "Proveedor *:",
                        "Precio Venta ($) *:",
                        "Control Médico:",
                        "Stock Mínimo (Seguridad):"
                    },
                    new javax.swing.JComponent[]{
                        txtCod, txtNom, cmbCat, cmbProv, txtPrecio, chkReceta, txtMin
                    });

boolean res = DialogoFarmac.confirmar(this, "Alta de Nuevo Medicamento", form);
            if (res) {
                String cod = txtCod.getText().trim();
                String nom = txtNom.getText().trim();
                String cat = cmbCat.getSelectedItem() != null ? cmbCat.getSelectedItem().toString().trim() : "General";
                Proveedor prov = (Proveedor) cmbProv.getSelectedItem();

                if (cod.isEmpty() || nom.isEmpty()) {
                    DialogoFarmac.mostrarJO(this, "Código de barras y nombre son campos obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (productoDAO.existeCodigoBarras(cod, 0)) {
                    DialogoFarmac.mostrarJO(this, "El código de barras '" + cod + "' ya está registrado.", "Código Duplicado", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                double precio;
                int stockMin;
                try {
                    precio = Double.parseDouble(txtPrecio.getText().trim());
                    if (precio <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    DialogoFarmac.mostrarJO(this, "El precio de venta debe ser un número mayor a 0.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    stockMin = Integer.parseInt(txtMin.getText().trim());
                    if (stockMin < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    DialogoFarmac.mostrarJO(this, "El stock mínimo debe ser un número mayor o igual a 0.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Producto nuevo = new Producto(0, cod, nom, cat, prov, precio, chkReceta.isSelected(), stockMin, true);
                productoDAO.insertar(nuevo);
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "PRODUCTO_ALTA", "Alta de medicamento: " + nom + " [" + cod + "]");
                cargarDatos();
                DialogoFarmac.mostrarJO(this, "Medicamento '" + nom + "' agregado exitosamente al catálogo.", "Alta Exitosa", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            DialogoFarmac.mostrarJO(this, "Error: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirFormularioEditar() {
        Producto p = obtenerProductoSeleccionado();
        if (p == null) return;

        try {
            List<Proveedor> proveedores = proveedorDAO.listarTodos(true);
            List<String> categorias = productoDAO.listarCategorias();

            JTextField txtCod = TemaFarmac.crearCampoTexto(p.getCodigoBarras(), 15);
            JTextField txtNom = TemaFarmac.crearCampoTexto(p.getNombre(), 20);
            JComboBox<String> cmbCat = new JComboBox<>(categorias.toArray(new String[0]));
            cmbCat.setEditable(true);
            TemaFarmac.estilizarComboBox(cmbCat);
            cmbCat.setSelectedItem(p.getCategoria());

            JComboBox<Proveedor> cmbProv = new JComboBox<>(proveedores.toArray(new Proveedor[0]));
            TemaFarmac.estilizarComboBox(cmbProv);
            if (p.getProveedorObj() != null) {
                for (int i = 0; i < cmbProv.getItemCount(); i++) {
                    if (cmbProv.getItemAt(i).getId() == p.getProveedorObj().getId()) {
                        cmbProv.setSelectedIndex(i);
                        break;
                    }
                }
            }

            JTextField txtPrecio = TemaFarmac.crearCampoTexto(String.valueOf(p.getPrecioVenta()), 10);
            JCheckBox chkReceta = new JCheckBox("Requiere Receta Médica Obligatoria", p.isRequiereReceta());
            JTextField txtMin = TemaFarmac.crearCampoTexto(String.valueOf(p.getStockMinimo()), 5);

            JPanel form = TemaFarmac.crearFormulario(
                    new String[]{
                        "Código de Barras *:",
                        "Nombre Medicamento *:",
                        "Categoría:",
                        "Proveedor:",
                        "Precio Venta ($) *:",
                        "Control Médico:",
                        "Stock Mínimo:"
                    },
                    new javax.swing.JComponent[]{
                        txtCod, txtNom, cmbCat, cmbProv, txtPrecio, chkReceta, txtMin
                    });

boolean res = DialogoFarmac.confirmar(this, "Modificar Medicamento #" + p.getId(), form);
            if (res) {
                String cod = txtCod.getText().trim();
                String nom = txtNom.getText().trim();
                String cat = cmbCat.getSelectedItem() != null ? cmbCat.getSelectedItem().toString().trim() : "General";
                Proveedor prov = (Proveedor) cmbProv.getSelectedItem();

                if (cod.isEmpty() || nom.isEmpty()) {
                    DialogoFarmac.mostrarJO(this, "Código y nombre son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (productoDAO.existeCodigoBarras(cod, p.getId())) {
                    DialogoFarmac.mostrarJO(this, "El código '" + cod + "' ya pertenece a otro medicamento.", "Código Duplicado", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                double precio;
                int stockMin;
                try {
                    precio = Double.parseDouble(txtPrecio.getText().trim());
                    if (precio <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    DialogoFarmac.mostrarJO(this, "El precio debe ser mayor a 0.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    stockMin = Integer.parseInt(txtMin.getText().trim());
                    if (stockMin < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    DialogoFarmac.mostrarJO(this, "El stock mínimo debe ser >= 0.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                p.setCodigoBarras(cod);
                p.setNombre(nom);
                p.setCategoria(cat);
                p.setProveedorObj(prov);
                p.setPrecioVenta(precio);
                p.setRequiereReceta(chkReceta.isSelected());
                p.setStockMinimo(stockMin);

                productoDAO.actualizar(p);
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "PRODUCTO_EDICION", "Edición de medicamento #" + p.getId() + " [" + cod + "]");
                cargarDatos();
                DialogoFarmac.mostrarJO(this, "Medicamento actualizado correctamente en base de datos.", "Actualización Exitosa", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            DialogoFarmac.mostrarJO(this, "Error BD: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarProductoSeleccionado() {
        Producto p = obtenerProductoSeleccionado();
        if (p == null) return;

        boolean confirm = DialogoFarmac.confirmarJO(this,
                "¿Está seguro de desactivar el medicamento '" + p.getNombre() + "' del catálogo activo?\n"
                + "Se aplicará BAJA LÓGICA conservando la trazabilidad de ventas e inventario.",
                "Confirmar Desactivación");

        if (confirm) {
            try {
                productoDAO.darDeBajaLogica(p.getId());
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "PRODUCTO_BAJA", "Baja lógica de producto #" + p.getId() + " (" + p.getNombre() + ")");
                cargarDatos();
                DialogoFarmac.mostrarJO(this, "Medicamento desactivado exitosamente (baja lógica aplicada).", "Baja Completada", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                DialogoFarmac.mostrarJO(this, "Error al desactivar: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("catalogo_medicamentos_" + LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("CodigoBarras,Nombre,Categoria,Proveedor,PrecioVenta,RequiereReceta,StockMinimo,ExistenciaActual");
                for (Producto p : listaProductosActual) {
                    int stock = p.calcularStockTotal(listaLotesActual);
                    pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%s,%d,%d%n",
                            p.getCodigoBarras(),
                            p.getNombre(),
                            p.getCategoria(),
                            p.getProveedor(),
                            p.getPrecioVenta(),
                            p.isRequiereReceta() ? "SI" : "NO",
                            p.getStockMinimo(),
                            stock);
                }
                DialogoFarmac.mostrarJO(this, "Catálogo exportado exitosamente a:\n" + f.getAbsolutePath(), "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                DialogoFarmac.mostrarJO(this, "Error al exportar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
