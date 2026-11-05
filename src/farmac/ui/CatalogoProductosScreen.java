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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
        setLayout(new BorderLayout(14, 14));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(16, 18, 16, 18));

        // Cabecera superior
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setOpaque(false);

        JPanel pnlTit = new JPanel(new BorderLayout());
        pnlTit.setOpaque(false);
        JLabel lblTitulo = new JLabel("Catálogo de Medicamentos y Productos");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Administración de catálogo, baja lógica, control médico y trazabilidad de lotes");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        JPanel pnlTitText = new JPanel();
        pnlTitText.setLayout(new BoxLayout(pnlTitText, BoxLayout.Y_AXIS));
        pnlTitText.setOpaque(false);
        pnlTitText.add(lblTitulo);
        pnlTitText.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlTitText.add(lblSub);
        pnlTit.add(pnlTitText, BorderLayout.WEST);

        pnlNorte.add(pnlTit);
        pnlNorte.add(Box.createRigidArea(new Dimension(0, 10)));

        // Barra de Herramientas (FlowLayout: se reacomoda en varias filas
        // cuando el ancho de la ventana no alcanza para una sola)
        JPanel pnlBarraHerramientas = new JPanel(new TemaFarmac.WrapLayout(FlowLayout.LEFT, 8, 4));
        pnlBarraHerramientas.setOpaque(false);

        // Botones CRUD (Exclusivo Encargado)
        JPanel pnlBotonesCrud = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlBotonesCrud.setOpaque(false);

        boolean esAdmin = (usuarioActivo != null) && "ENCARGADO".equalsIgnoreCase(usuarioActivo.obtenerRol());
        if (esAdmin) {
            JButton btnNuevo = TemaFarmac.crearBoton("+ Nuevo Medicamento", TemaFarmac.TURQUESA, Color.WHITE);
            btnNuevo.addActionListener(e -> abrirFormularioNuevo());

            JButton btnEditar = TemaFarmac.crearBoton("Editar Medicamento", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
            btnEditar.addActionListener(e -> abrirFormularioEditar());

            JButton btnEliminar = TemaFarmac.crearBoton("Desactivar (Baja Lógica)", TemaFarmac.ROJO, Color.WHITE);
            btnEliminar.addActionListener(e -> eliminarProductoSeleccionado());

            pnlBotonesCrud.add(btnNuevo);
            pnlBotonesCrud.add(btnEditar);
            pnlBotonesCrud.add(btnEliminar);
        }

        // Buscador y Exportación
        JPanel pnlBuscarExportar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBuscarExportar.setOpaque(false);

        cmbFiltroCategoria = new JComboBox<>(new String[]{"TODAS"});
        TemaFarmac.estilizarComboBox(cmbFiltroCategoria);
        cmbFiltroCategoria.addActionListener(e -> filtrar());

        txtBuscar = TemaFarmac.crearCampoTexto(14);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Nombre o código...");
        txtBuscar.addActionListener(e -> filtrar());

        JButton btnBuscar = TemaFarmac.crearBoton("Buscar", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
        btnBuscar.addActionListener(e -> filtrar());

        JButton btnExportar = TemaFarmac.crearBoton("Exportar CSV", TemaFarmac.VERDE, Color.WHITE);
        btnExportar.addActionListener(e -> exportarCSV());

        pnlBuscarExportar.add(new JLabel("Categoría:"));
        pnlBuscarExportar.add(cmbFiltroCategoria);
        pnlBuscarExportar.add(new JLabel("Buscar:"));
        pnlBuscarExportar.add(txtBuscar);
        pnlBuscarExportar.add(btnBuscar);
        pnlBuscarExportar.add(btnExportar);

        pnlBarraHerramientas.add(pnlBotonesCrud);
        pnlBarraHerramientas.add(pnlBuscarExportar);
        pnlNorte.add(pnlBarraHerramientas);

        add(pnlNorte, BorderLayout.NORTH);

        // Tabla Principal
        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new BorderLayout(8, 8));

        String[] columnas = {
            "ID", "Código", "Medicamento", "Categoría",
            "Proveedor", "P. Venta", "Tipo", "Mín.", "Existencia", "Caduc."
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblProductos = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tblProductos);
        tblProductos.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblProductos.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblProductos.getColumnModel().getColumn(2).setPreferredWidth(170);
        tblProductos.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblProductos.getColumnModel().getColumn(4).setPreferredWidth(95);
        tblProductos.getColumnModel().getColumn(5).setPreferredWidth(75);
        tblProductos.getColumnModel().getColumn(6).setPreferredWidth(75);
        tblProductos.getColumnModel().getColumn(7).setPreferredWidth(75);
        tblProductos.getColumnModel().getColumn(8).setPreferredWidth(85);
        tblProductos.getColumnModel().getColumn(9).setPreferredWidth(85);

        tblProductos.getColumnModel().getColumn(6).setCellRenderer(new TemaFarmac.BadgeCellRenderer());

        JScrollPane scroll = new JScrollPane(tblProductos);
        scroll.setBorder(BorderFactory.createLineBorder(TemaFarmac.GRIS_BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        pnlCard.add(scroll, BorderLayout.CENTER);

        add(pnlCard, BorderLayout.CENTER);
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
                p.isRequiereReceta() ? "REQUIERE RECETA" : "VENTA LIBRE",
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
            JComboBox<Proveedor> cmbProv = new JComboBox<>(proveedores.toArray(new Proveedor[0]));
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
            cmbCat.setSelectedItem(p.getCategoria());

            JComboBox<Proveedor> cmbProv = new JComboBox<>(proveedores.toArray(new Proveedor[0]));
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
