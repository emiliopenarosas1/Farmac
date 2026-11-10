package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.ProveedorDAO;
import farmac.dominio.Proveedor;
import farmac.util.Sesion;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla de gestión integral de Proveedores.
 * Accesible exclusivamente para Encargados.
 */
public class ProveedoresScreen extends JPanel {

    private final ProveedorDAO proveedorDAO;
    private JTable tablaProveedores;
    private DefaultTableModel modeloTabla;
    private JTextField txtBuscar;
    private List<Proveedor> listaActual;

    public ProveedoresScreen() {
        this.proveedorDAO = new ProveedorDAO();
        initComponents();
        // Cargar después de pintar la pantalla, sin bloquear la navegación
        javax.swing.SwingUtilities.invokeLater(this::cargarDatos);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));

        // Cabecera
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        JLabel lblTitulo = new JLabel("Administración de proveedores");
        lblTitulo.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 20));
        lblTitulo.setForeground(TemaFarmac.TEXTO_AZUL);
        JButton btnNuevo = TemaFarmac.crearBotonPrimario("Nuevo proveedor");
        btnNuevo.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.PLUS, 12, java.awt.Color.WHITE));
        btnNuevo.setIconTextGap(8);
        btnNuevo.addActionListener(e -> abrirDialogoEdicion(null));
        pnlHeader.add(lblTitulo, BorderLayout.WEST);
        pnlHeader.add(btnNuevo, BorderLayout.EAST);

        // Buscador
        txtBuscar = TemaFarmac.crearCampoTexto(15);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar razón social...");
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
        pnlCampoBuscar.setPreferredSize(new Dimension(300, 40));
        pnlCampoBuscar.add(new JLabel(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SEARCH, 13, TemaFarmac.TEXTO_MUTED)), BorderLayout.WEST);
        pnlCampoBuscar.add(txtBuscar, BorderLayout.CENTER);

        JLabel lblBus = new JLabel("Buscar");
        lblBus.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblBus.setForeground(TemaFarmac.TEXTO_AZUL);
        JPanel pnlFiltros = new JPanel(new BorderLayout(0, 4));
        pnlFiltros.setBackground(java.awt.Color.WHITE);
        pnlFiltros.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 10, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        pnlFiltros.add(lblBus, BorderLayout.NORTH);
        pnlFiltros.add(pnlCampoBuscar, BorderLayout.CENTER);

        JPanel pnlNorte = new JPanel();
        pnlNorte.setOpaque(false);
        pnlNorte.setLayout(new javax.swing.BoxLayout(pnlNorte, javax.swing.BoxLayout.Y_AXIS));
        pnlHeader.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pnlFiltros.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pnlNorte.add(pnlHeader);
        pnlNorte.add(Box.createRigidArea(new Dimension(0, 12)));
        pnlNorte.add(pnlFiltros);
        add(pnlNorte, BorderLayout.NORTH);

        // Tabla
        String[] columnas = {"ID", "Razón social", "Contacto", "Teléfono", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tablaProveedores = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tablaProveedores);
        tablaProveedores.getColumnModel().getColumn(0).setPreferredWidth(60);
        tablaProveedores.getColumnModel().getColumn(1).setPreferredWidth(320);
        tablaProveedores.getColumnModel().getColumn(2).setPreferredWidth(200);
        tablaProveedores.getColumnModel().getColumn(3).setPreferredWidth(130);
        tablaProveedores.getColumnModel().getColumn(4).setPreferredWidth(100);
        tablaProveedores.getColumnModel().getColumn(4).setCellRenderer(new TemaFarmac.BadgeCellRenderer());
        // La columna ID se conserva en el modelo pero no se muestra
        tablaProveedores.removeColumn(tablaProveedores.getColumnModel().getColumn(0));

        JScrollPane scroll = new JScrollPane(tablaProveedores);
        scroll.setBorder(new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 8, 1));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        // Acciones sobre la fila seleccionada
        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlAcciones.setOpaque(false);
        JButton btnEditar = TemaFarmac.crearBotonSecundario("Editar");
        btnEditar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.PENCIL_ALT, 12, TemaFarmac.TEXTO));
        btnEditar.setIconTextGap(8);
        btnEditar.addActionListener(e -> editarSeleccionado());
        JButton btnToggle = TemaFarmac.crearBotonPeligro("Activar / desactivar");
        btnToggle.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.POWER_OFF, 12, TemaFarmac.ROJO));
        btnToggle.setIconTextGap(8);
        btnToggle.addActionListener(e -> toggleEstadoSeleccionado());
        pnlAcciones.add(btnEditar);
        pnlAcciones.add(btnToggle);
        add(pnlAcciones, BorderLayout.SOUTH);
    }

    public void cargarDatos() {
        try {
            listaActual = proveedorDAO.listarTodos(false);
            actualizarTablaConLista(listaActual);
        } catch (SQLException e) {
            DialogoFarmac.mostrarJO(this,
                    "Error al cargar proveedores de la base de datos: " + e.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarTablaConLista(List<Proveedor> lista) {
        modeloTabla.setRowCount(0);
        if (lista == null) return;
        for (Proveedor p : lista) {
            modeloTabla.addRow(new Object[]{
                p.getId(),
                p.getRazonSocial(),
                p.getContacto() != null ? p.getContacto() : "-",
                p.getTelefono() != null ? p.getTelefono() : "-",
                p.isActivo() ? "ACTIVO" : "INACTIVO"
            });
        }
    }

    private void filtrarTabla(String texto) {
        if (listaActual == null) return;
        if (texto == null || texto.trim().isEmpty()) {
            actualizarTablaConLista(listaActual);
            return;
        }
        String t = texto.trim().toLowerCase();
        List<Proveedor> filtrada = listaActual.stream()
                .filter(p -> p.getRazonSocial().toLowerCase().contains(t)
                          || (p.getContacto() != null && p.getContacto().toLowerCase().contains(t)))
                .toList();
        actualizarTablaConLista(filtrada);
    }

    private Proveedor obtenerSeleccionado() {
        int fila = tablaProveedores.getSelectedRow();
        if (fila < 0) {
            DialogoFarmac.mostrarJO(this, "Seleccione un proveedor de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int id = (int) modeloTabla.getValueAt(fila, 0);
        if (listaActual != null) {
            for (Proveedor p : listaActual) {
                if (p.getId() == id) return p;
            }
        }
        return null;
    }

    private void editarSeleccionado() {
        Proveedor p = obtenerSeleccionado();
        if (p != null) {
            abrirDialogoEdicion(p);
        }
    }

    private void toggleEstadoSeleccionado() {
        Proveedor p = obtenerSeleccionado();
        if (p == null) return;

        boolean nuevoEstado = !p.isActivo();
        String accion = nuevoEstado ? "activar" : "desactivar";
        boolean confirm = DialogoFarmac.confirmarJO(this,
                "¿Desea " + accion + " al proveedor '" + p.getRazonSocial() + "'?",
                "Confirmar Cambio de Estado");
        if (confirm) {
            try {
                proveedorDAO.cambiarEstado(p.getId(), nuevoEstado);
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "PROVEEDOR_ESTADO",
                        "Proveedor #" + p.getId() + " marcado como " + (nuevoEstado ? "ACTIVO" : "INACTIVO"));
                cargarDatos();
            } catch (SQLException e) {
                DialogoFarmac.mostrarJO(this, "Error al actualizar estado: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirDialogoEdicion(Proveedor p) {
        boolean esNuevo = (p == null);
        JDialog dlg = new JDialog((java.awt.Frame) null, esNuevo ? "Nuevo Proveedor" : "Editar Proveedor", true);
        dlg.setLayout(new BorderLayout());

        JTextField txtRazon = TemaFarmac.crearCampoTexto(18);
        JTextField txtContacto = TemaFarmac.crearCampoTexto(18);
        JTextField txtTelefono = TemaFarmac.crearCampoTexto(18);
        JCheckBox chkActivo = new JCheckBox("Proveedor Activo", true);
        chkActivo.setOpaque(false);

        if (!esNuevo) {
            txtRazon.setText(p.getRazonSocial());
            txtContacto.setText(p.getContacto() != null ? p.getContacto() : "");
            txtTelefono.setText(p.getTelefono() != null ? p.getTelefono() : "");
            chkActivo.setSelected(p.isActivo());
        }

        JPanel pnlForm = TemaFarmac.crearFormulario(
                new String[]{"Razón Social *:", "Contacto:", "Teléfono:", "Estado:"},
                new javax.swing.JComponent[]{txtRazon, txtContacto, txtTelefono, chkActivo});
        pnlForm.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        pnlForm.setBackground(java.awt.Color.WHITE);
        pnlForm.setOpaque(true);

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = TemaFarmac.crearBotonPrimario("Guardar proveedor");
        JButton btnCancelar = TemaFarmac.crearBotonSecundario("Cancelar");

        btnCancelar.addActionListener(e -> dlg.dispose());
        btnGuardar.addActionListener(e -> {
            String razon = txtRazon.getText().trim();
            if (razon.isEmpty()) {
                DialogoFarmac.mostrarJO(dlg, "La razón social es obligatoria.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (esNuevo) {
                    Proveedor nuevo = new Proveedor(0, razon, txtContacto.getText().trim(), txtTelefono.getText().trim(), chkActivo.isSelected());
                    proveedorDAO.insertar(nuevo);
                    BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "PROVEEDOR_ALTA", "Alta de proveedor: " + razon);
                } else {
                    p.setRazonSocial(razon);
                    p.setContacto(txtContacto.getText().trim());
                    p.setTelefono(txtTelefono.getText().trim());
                    p.setActivo(chkActivo.isSelected());
                    proveedorDAO.actualizar(p);
                    BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "PROVEEDOR_EDICION", "Edición de proveedor #" + p.getId());
                }
                dlg.dispose();
                cargarDatos();
            } catch (SQLException ex) {
                DialogoFarmac.mostrarJO(dlg, "Error al guardar en base de datos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        pnlBotones.add(btnCancelar);
        pnlBotones.add(btnGuardar);

        // Navegación con Enter: razón social -> contacto -> teléfono -> estado -> guardar
        TemaFarmac.enterComoTab(btnGuardar::doClick, txtRazon, txtContacto, txtTelefono, chkActivo);
        dlg.getRootPane().registerKeyboardAction(ev -> dlg.dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        pnlBotones.setBackground(java.awt.Color.WHITE);
        pnlBotones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, TemaFarmac.BORDE));

        dlg.add(pnlForm, BorderLayout.CENTER);
        dlg.add(pnlBotones, BorderLayout.SOUTH);
        dlg.pack();
        Dimension pantalla = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(
                Math.min(Math.max(dlg.getWidth(), 420), pantalla.width - 40),
                Math.min(Math.max(dlg.getHeight(), 280), pantalla.height - 80));
        dlg.setMinimumSize(new Dimension(380, 260));
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }
}
