package farmac.ui;

import farmac.dominio.Encargado;
import farmac.dominio.Producto;
import farmac.dominio.Usuario;
import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de Catalogo con barra de acciones superior prominente para CRUD completo y exportacion.
 */
public class CatalogoProductosScreen extends JPanel {
    private final Usuario usuarioActivo;
    private JTextField txtBuscar;
    private JTable tblProductos;
    private DefaultTableModel modeloTabla;
    private List<Producto> listaProductos;

    public CatalogoProductosScreen() {
        this(new Encargado(1, "admin", "Administrador General", "••••••", true));
    }

    public CatalogoProductosScreen(Usuario usuario) {
        this.usuarioActivo = usuario;
        this.listaProductos = new ArrayList<>();
        cargarProductosIniciales();
        inicializarUI();
    }

    private void cargarProductosIniciales() {
        listaProductos.add(new Producto(1, "75010001", "Amoxicilina 500mg (Caja c/12)", "Antibioticos", "Farmaceutica Nal", 85.00, true, 5));
        listaProductos.add(new Producto(2, "75010002", "Paracetamol 500mg (Caja c/20)", "Analgesicos", "Laboratorios Alfa", 25.00, false, 10));
        listaProductos.add(new Producto(3, "75010003", "Ibuprofeno 400mg (Caja c/10)", "Antiinflamatorios", "Genericos MX", 45.00, false, 8));
        listaProductos.add(new Producto(4, "75010004", "Omeprazol 20mg (Caja c/14)", "Gastroenterologia", "Laboratorios Beta", 60.00, false, 6));
        listaProductos.add(new Producto(5, "75010005", "Azitromicina 500mg (Caja c/3)", "Antibioticos", "Farmaceutica Nal", 145.00, true, 4));
        listaProductos.add(new Producto(6, "75010006", "Loratadina 10mg (Caja c/10)", "Antihistaminicos", "Laboratorios Alfa", 35.00, false, 5));
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(TemaFarmac.FONDO);
        setBorder(new EmptyBorder(16, 18, 16, 18));

        // 1. Panel Superior con Titulo y Barra de Herramientas CRUD
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setOpaque(false);

        // Fila 1: Titulo
        JPanel pnlTit = new JPanel(new BorderLayout());
        pnlTit.setOpaque(false);
        JLabel lblTitulo = new JLabel("Catalogo y Mantenimiento de Medicamentos");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Administracion de articulos, precios oficiales, receta medica obligatoria y stock minimo");
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

        // Fila 2: Barra de Botones de Accion y Buscador (Siempre Visible arriba)
        JPanel pnlBarraHerramientas = new JPanel(new BorderLayout(10, 0));
        pnlBarraHerramientas.setOpaque(false);

        // Botones CRUD (Lado izquierdo)
        JPanel pnlBotonesCrud = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlBotonesCrud.setOpaque(false);

        boolean esAdmin = (usuarioActivo instanceof Encargado) || "ENCARGADO".equalsIgnoreCase(usuarioActivo.obtenerRol());
        if (esAdmin) {
            JButton btnNuevo = TemaFarmac.crearBoton("+ Nuevo Medicamento", TemaFarmac.TURQUESA, Color.WHITE);
            btnNuevo.addActionListener(e -> abrirFormularioNuevo());

            JButton btnEditar = TemaFarmac.crearBoton("Editar Medicamento", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
            btnEditar.addActionListener(e -> abrirFormularioEditar());

            JButton btnEliminar = TemaFarmac.crearBoton("Desactivar Medicamento", TemaFarmac.ROJO, Color.WHITE);
            btnEliminar.addActionListener(e -> eliminarProductoSeleccionado());

            pnlBotonesCrud.add(btnNuevo);
            pnlBotonesCrud.add(btnEditar);
            pnlBotonesCrud.add(btnEliminar);
        }

        // Buscador y Exportacion (Lado derecho)
        JPanel pnlBuscarExportar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBuscarExportar.setOpaque(false);

        txtBuscar = TemaFarmac.crearCampoTexto("", 16);
        txtBuscar.setPreferredSize(new Dimension(200, 34));
        txtBuscar.addActionListener(e -> filtrar());

        JButton btnBuscar = TemaFarmac.crearBoton("Buscar", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
        btnBuscar.addActionListener(e -> filtrar());

        JButton btnExportar = TemaFarmac.crearBoton("Exportar Catalogo (CSV)", TemaFarmac.VERDE, Color.WHITE);
        btnExportar.addActionListener(e -> exportarCSV());

        pnlBuscarExportar.add(new JLabel("Filtrar:"));
        pnlBuscarExportar.add(txtBuscar);
        pnlBuscarExportar.add(btnBuscar);
        pnlBuscarExportar.add(btnExportar);

        pnlBarraHerramientas.add(pnlBotonesCrud, BorderLayout.WEST);
        pnlBarraHerramientas.add(pnlBuscarExportar, BorderLayout.EAST);
        pnlNorte.add(pnlBarraHerramientas);

        add(pnlNorte, BorderLayout.NORTH);

        // 2. Tabla Principal en Card
        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new BorderLayout(8, 8));

        String[] columnas = {"Codigo de Barras", "Nombre del Medicamento", "Categoria", "Laboratorio", "Precio Venta", "Tipo Venta", "Stock Minimo"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblProductos = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tblProductos);
        tblProductos.getColumnModel().getColumn(5).setCellRenderer(new TemaFarmac.BadgeCellRenderer());

        JScrollPane scroll = new JScrollPane(tblProductos);
        scroll.setBorder(BorderFactory.createLineBorder(TemaFarmac.BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        pnlCard.add(scroll, BorderLayout.CENTER);

        JLabel lblInfoSeleccion = new JLabel("Haga clic sobre un medicamento de la tabla para seleccionarlo y luego use los botones superiores.");
        lblInfoSeleccion.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblInfoSeleccion.setForeground(TemaFarmac.TEXTO_MUTED);
        pnlCard.add(lblInfoSeleccion, BorderLayout.SOUTH);

        llenarTabla(listaProductos);
        add(pnlCard, BorderLayout.CENTER);
    }

    private void llenarTabla(List<Producto> productos) {
        modeloTabla.setRowCount(0);
        for (Producto p : productos) {
            modeloTabla.addRow(new Object[]{
                    p.getCodigoBarras(),
                    p.getNombre(),
                    p.getCategoria(),
                    p.getProveedor(),
                    String.format("$%.2f", p.getPrecioVenta()),
                    p.isRequiereReceta() ? "REQUIERE RECETA" : "VENTA LIBRE",
                    p.getStockMinimo() + " pzas"
            });
        }
    }

    private void filtrar() {
        String query = txtBuscar.getText().toLowerCase().trim();
        List<Producto> filtrados = new ArrayList<>();
        for (Producto p : listaProductos) {
            if (p.getNombre().toLowerCase().contains(query) || p.getCodigoBarras().contains(query)) {
                filtrados.add(p);
            }
        }
        llenarTabla(filtrados);
    }

    private void abrirFormularioNuevo() {
        JTextField txtCod = TemaFarmac.crearCampoTexto("7501" + (10000 + listaProductos.size() + 1), 15);
        JTextField txtNom = TemaFarmac.crearCampoTexto("", 20);
        JComboBox<String> cmbCat = new JComboBox<>(new String[]{"Antibioticos", "Analgesicos", "Antiinflamatorios", "Gastroenterologia", "Antihistaminicos", "Vitaminas"});
        cmbCat.setBackground(Color.WHITE);
        JTextField txtProv = TemaFarmac.crearCampoTexto("Laboratorios Nacionales", 15);
        JTextField txtPrecio = TemaFarmac.crearCampoTexto("50.00", 10);
        JCheckBox chkReceta = new JCheckBox("Requiere Receta Medica Obligatoria");
        JTextField txtMin = TemaFarmac.crearCampoTexto("5", 5);

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.add(new JLabel("Codigo de Barras:")); form.add(txtCod);
        form.add(new JLabel("Nombre Medicamento:")); form.add(txtNom);
        form.add(new JLabel("Categoria:")); form.add(cmbCat);
        form.add(new JLabel("Laboratorio / Proveedor:")); form.add(txtProv);
        form.add(new JLabel("Precio Venta ($):")); form.add(txtPrecio);
        form.add(new JLabel("Control Medico:")); form.add(chkReceta);
        form.add(new JLabel("Stock Minimo (Alertas):")); form.add(txtMin);

        int res = JOptionPane.showConfirmDialog(this, form, "Alta de Nuevo Medicamento", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String nom = txtNom.getText().trim();
            if (nom.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre del medicamento es obligatorio.", "Validacion", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Producto nuevo = new Producto(
                    listaProductos.size() + 1,
                    txtCod.getText().trim(),
                    nom,
                    (String) cmbCat.getSelectedItem(),
                    txtProv.getText().trim(),
                    Double.parseDouble(txtPrecio.getText().trim()),
                    chkReceta.isSelected(),
                    Integer.parseInt(txtMin.getText().trim())
            );
            listaProductos.add(nuevo);
            llenarTabla(listaProductos);
            JOptionPane.showMessageDialog(this, "Medicamento '" + nom + "' agregado exitosamente al catalogo.", "Alta Exitosa", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void abrirFormularioEditar() {
        int fila = tblProductos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un medicamento de la tabla para modificar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Producto p = listaProductos.get(fila);

        JTextField txtCod = TemaFarmac.crearCampoTexto(p.getCodigoBarras(), 15);
        JTextField txtNom = TemaFarmac.crearCampoTexto(p.getNombre(), 20);
        JTextField txtCat = TemaFarmac.crearCampoTexto(p.getCategoria(), 15);
        JTextField txtProv = TemaFarmac.crearCampoTexto(p.getProveedor(), 15);
        JTextField txtPrecio = TemaFarmac.crearCampoTexto(String.valueOf(p.getPrecioVenta()), 10);
        JCheckBox chkReceta = new JCheckBox("Requiere Receta Medica Obligatoria", p.isRequiereReceta());
        JTextField txtMin = TemaFarmac.crearCampoTexto(String.valueOf(p.getStockMinimo()), 5);

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.add(new JLabel("Codigo de Barras:")); form.add(txtCod);
        form.add(new JLabel("Nombre Medicamento:")); form.add(txtNom);
        form.add(new JLabel("Categoria:")); form.add(txtCat);
        form.add(new JLabel("Laboratorio:")); form.add(txtProv);
        form.add(new JLabel("Precio Venta ($):")); form.add(txtPrecio);
        form.add(new JLabel("Control Medico:")); form.add(chkReceta);
        form.add(new JLabel("Stock Minimo:")); form.add(txtMin);

        int res = JOptionPane.showConfirmDialog(this, form, "Modificar Medicamento #" + p.getId(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            p.setCodigoBarras(txtCod.getText().trim());
            p.setNombre(txtNom.getText().trim());
            p.setCategoria(txtCat.getText().trim());
            p.setProveedor(txtProv.getText().trim());
            p.setPrecioVenta(Double.parseDouble(txtPrecio.getText().trim()));
            p.setRequiereReceta(chkReceta.isSelected());
            p.setStockMinimo(Integer.parseInt(txtMin.getText().trim()));
            llenarTabla(listaProductos);
            JOptionPane.showMessageDialog(this, "Datos de medicamento actualizados correctamente.", "Actualizado", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void eliminarProductoSeleccionado() {
        int fila = tblProductos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un medicamento de la tabla para desactivar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Producto p = listaProductos.get(fila);
        int confirm = JOptionPane.showConfirmDialog(this, "¿Esta seguro de desactivar el medicamento '" + p.getNombre() + "' del catalogo?", "Confirmar Desactivacion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            listaProductos.remove(fila);
            llenarTabla(listaProductos);
            JOptionPane.showMessageDialog(this, "Medicamento retirado del catalogo.", "Desactivado", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exportarCSV() {
        try {
            File dirReportes = new File("reportes");
            if (!dirReportes.exists()) dirReportes.mkdirs();

            File archivoCSV = new File(dirReportes, "catalogo_medicamentos.csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivoCSV))) {
                pw.println("CODIGO_BARRAS,NOMBRE,CATEGORIA,PROVEEDOR,PRECIO_VENTA,REQUIERE_RECETA,STOCK_MINIMO");
                for (Producto p : listaProductos) {
                    pw.printf("%s,\"%s\",\"%s\",\"%s\",%.2f,%s,%d%n",
                            p.getCodigoBarras(),
                            p.getNombre(),
                            p.getCategoria(),
                            p.getProveedor(),
                            p.getPrecioVenta(),
                            p.isRequiereReceta() ? "SI" : "NO",
                            p.getStockMinimo());
                }
            }
            JOptionPane.showMessageDialog(this, "Catalogo exportado exitosamente a:\n" + archivoCSV.getAbsolutePath(), "Exportacion Exitosa", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al exportar catalogo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
