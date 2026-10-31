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
 * Pantalla de gestión integral de Proveedores (RF20).
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
        cargarDatos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Encabezado
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(TemaFarmac.GRIS_FONDO);

        JLabel lblTitulo = new JLabel("Administración de Proveedores");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Alta, edición y control de estado de casas farmacéuticas y distribuidores (RF20)");
        lblSub.setFont(TemaFarmac.FUENTE_REGULAR);
        lblSub.setForeground(TemaFarmac.GRIS_TEXTO);

        JPanel pnlTitulos = new JPanel(new BorderLayout(0, 4));
        pnlTitulos.setBackground(TemaFarmac.GRIS_FONDO);
        pnlTitulos.add(lblTitulo, BorderLayout.NORTH);
        pnlTitulos.add(lblSub, BorderLayout.SOUTH);
        pnlHeader.add(pnlTitulos, BorderLayout.WEST);

        // Barra de acciones
        JPanel pnlBarra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlBarra.setBackground(TemaFarmac.GRIS_FONDO);

        txtBuscar = TemaFarmac.crearCampoTexto(15);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar razón social...");
        txtBuscar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filtrarTabla(txtBuscar.getText());
            }
        });

        JButton btnNuevo = TemaFarmac.crearBotonPrimario("+ Nuevo Proveedor");
        btnNuevo.addActionListener(e -> abrirDialogoEdicion(null));

        JButton btnEditar = TemaFarmac.crearBotonSecundario("Editar");
        btnEditar.addActionListener(e -> editarSeleccionado());

        JButton btnToggle = TemaFarmac.crearBotonSecundario("Activar / Desactivar");
        btnToggle.addActionListener(e -> toggleEstadoSeleccionado());

        pnlBarra.add(new JLabel("Buscar:"));
        pnlBarra.add(txtBuscar);
        pnlBarra.add(btnNuevo);
        pnlBarra.add(btnEditar);
        pnlBarra.add(btnToggle);

        pnlHeader.add(pnlBarra, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        // Tabla
        String[] columnas = {"ID", "Razón Social", "Contacto", "Teléfono", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tablaProveedores = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tablaProveedores);
        tablaProveedores.getColumnModel().getColumn(0).setPreferredWidth(60);
        tablaProveedores.getColumnModel().getColumn(1).setPreferredWidth(250);
        tablaProveedores.getColumnModel().getColumn(2).setPreferredWidth(180);
        tablaProveedores.getColumnModel().getColumn(3).setPreferredWidth(140);
        tablaProveedores.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scroll = new JScrollPane(tablaProveedores);
        scroll.setBorder(BorderFactory.createLineBorder(TemaFarmac.GRIS_BORDE));
        add(scroll, BorderLayout.CENTER);
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
        dlg.setSize(420, 320);
        dlg.setLocationRelativeTo(this);

        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        pnlForm.setBackground(java.awt.Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField txtRazon = TemaFarmac.crearCampoTexto(18);
        JTextField txtContacto = TemaFarmac.crearCampoTexto(18);
        JTextField txtTelefono = TemaFarmac.crearCampoTexto(18);
        JCheckBox chkActivo = new JCheckBox("Proveedor Activo", true);
        chkActivo.setBackground(java.awt.Color.WHITE);

        if (!esNuevo) {
            txtRazon.setText(p.getRazonSocial());
            txtContacto.setText(p.getContacto() != null ? p.getContacto() : "");
            txtTelefono.setText(p.getTelefono() != null ? p.getTelefono() : "");
            chkActivo.setSelected(p.isActivo());
        }

        gbc.gridx = 0; gbc.gridy = 0; pnlForm.add(new JLabel("Razón Social *:"), gbc);
        gbc.gridx = 1; pnlForm.add(txtRazon, gbc);

        gbc.gridx = 0; gbc.gridy = 1; pnlForm.add(new JLabel("Contacto:"), gbc);
        gbc.gridx = 1; pnlForm.add(txtContacto, gbc);

        gbc.gridx = 0; gbc.gridy = 2; pnlForm.add(new JLabel("Teléfono:"), gbc);
        gbc.gridx = 1; pnlForm.add(txtTelefono, gbc);

        gbc.gridx = 1; gbc.gridy = 3; pnlForm.add(chkActivo, gbc);

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = TemaFarmac.crearBotonPrimario("Guardar");
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

        dlg.add(pnlForm, BorderLayout.CENTER);
        dlg.add(pnlBotones, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
}
