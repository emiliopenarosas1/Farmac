package farmac.ui;

import farmac.dominio.Empleado;
import farmac.dominio.Encargado;
import farmac.dominio.Usuario;
import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla completa de Administracion y Gestion CRUD de Usuarios (Encargado / Empleados).
 */
public class GestionUsuariosScreen extends JPanel {
    private JTable tblUsuarios;
    private DefaultTableModel modeloTabla;
    private List<Usuario> listaUsuarios;
    private JButton btnNuevo;
    private JButton btnEditar;
    private JButton btnCambiarEstado;

    public GestionUsuariosScreen() {
        this.listaUsuarios = new ArrayList<>();
        cargarUsuariosIniciales();
        inicializarUI();
    }

    private void cargarUsuariosIniciales() {
        listaUsuarios.add(new Encargado(1, "admin", "Administrador General", "123456", true));
        listaUsuarios.add(new Empleado(2, "empleado1", "Carlos Lopez Perez", "123456", true));
        listaUsuarios.add(new Empleado(3, "empleado2", "Maria Fernandez Gomez", "123456", true));
        listaUsuarios.add(new Empleado(4, "empleado3", "Juan Morales Santos", "123456", false));
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(TemaFarmac.FONDO);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        // 1. Cabecera con Titulo y Barra de Acciones CRUD
        JPanel pnlHeader = new JPanel(new BorderLayout(12, 12));
        pnlHeader.setOpaque(false);

        JPanel pnlTit = new JPanel();
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        pnlTit.setOpaque(false);

        JLabel lblTitulo = new JLabel("Administracion y CRUD de Usuarios");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Alta, modificacion de nombres, roles (Encargado/Empleado), contrasenas y activacion de cuentas");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        pnlTit.add(lblTitulo);
        pnlTit.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlTit.add(lblSub);

        // Barra de botones de accion CRUD destacada
        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlAcciones.setOpaque(false);

        btnNuevo = TemaFarmac.crearBoton("+ Nuevo Usuario", TemaFarmac.TURQUESA, Color.WHITE);
        btnNuevo.addActionListener(e -> abrirFormularioNuevoUsuario());

        btnEditar = TemaFarmac.crearBoton("Editar Usuario Seleccionado", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
        btnEditar.addActionListener(e -> abrirFormularioEditarUsuario());

        btnCambiarEstado = TemaFarmac.crearBoton("Activar / Desactivar", TemaFarmac.AMBAR, Color.WHITE);
        btnCambiarEstado.addActionListener(e -> alternarEstadoUsuario());

        pnlAcciones.add(btnNuevo);
        pnlAcciones.add(btnEditar);
        pnlAcciones.add(btnCambiarEstado);

        pnlHeader.add(pnlTit, BorderLayout.WEST);
        pnlHeader.add(pnlAcciones, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Tabla Principal de Usuarios en Card
        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new BorderLayout(10, 10));

        String[] columnas = {"ID", "Usuario (Login)", "Nombre Completo del Personal", "Rol Asignado", "Estado de la Cuenta"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblUsuarios = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tblUsuarios);
        tblUsuarios.getColumnModel().getColumn(4).setCellRenderer(new TemaFarmac.BadgeCellRenderer());

        JScrollPane scroll = new JScrollPane(tblUsuarios);
        scroll.setBorder(BorderFactory.createLineBorder(TemaFarmac.BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        pnlCard.add(scroll, BorderLayout.CENTER);

        // Leyenda inferior de ayuda
        JLabel lblAyuda = new JLabel("Seleccione una fila de la tabla y use los botones superiores para Modificar datos o cambiar el estado.");
        lblAyuda.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblAyuda.setForeground(TemaFarmac.TEXTO_MUTED);
        pnlCard.add(lblAyuda, BorderLayout.SOUTH);

        llenarTabla();
        add(pnlCard, BorderLayout.CENTER);
    }

    private void llenarTabla() {
        modeloTabla.setRowCount(0);
        for (Usuario u : listaUsuarios) {
            modeloTabla.addRow(new Object[]{
                    "#" + u.getId(),
                    u.getNombreUsuario(),
                    u.getNombreCompleto(),
                    u.obtenerRol(),
                    u.isActivo() ? "ACTIVO" : "INACTIVO"
            });
        }
    }

    private void abrirFormularioNuevoUsuario() {
        JTextField txtUser = TemaFarmac.crearCampoTexto("", 15);
        JTextField txtNombre = TemaFarmac.crearCampoTexto("", 20);
        JPasswordField txtPass = TemaFarmac.crearCampoPassword("", 15);
        JComboBox<String> cmbRol = new JComboBox<>(new String[]{"EMPLEADO", "ENCARGADO"});
        cmbRol.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbRol.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Nombre de Usuario (Login):")); form.add(txtUser);
        form.add(new JLabel("Nombre Completo:")); form.add(txtNombre);
        form.add(new JLabel("Contrasena Inicial:")); form.add(txtPass);
        form.add(new JLabel("Rol en el Sistema:")); form.add(cmbRol);

        int res = JOptionPane.showConfirmDialog(this, form, "Registrar Nuevo Usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String u = txtUser.getText().trim();
            String n = txtNombre.getText().trim();
            String p = new String(txtPass.getPassword()).trim();
            String r = (String) cmbRol.getSelectedItem();

            if (u.isEmpty() || n.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Validacion", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Usuario nuevo;
            if ("ENCARGADO".equals(r)) {
                nuevo = new Encargado(listaUsuarios.size() + 1, u, n, p, true);
            } else {
                nuevo = new Empleado(listaUsuarios.size() + 1, u, n, p, true);
            }

            listaUsuarios.add(nuevo);
            llenarTabla();
            JOptionPane.showMessageDialog(this, "Usuario '" + u + "' creado exitosamente.", "Usuario Registrado", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void abrirFormularioEditarUsuario() {
        int fila = tblUsuarios.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla para modificar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Usuario u = listaUsuarios.get(fila);
        JTextField txtUser = TemaFarmac.crearCampoTexto(u.getNombreUsuario(), 15);
        JTextField txtNombre = TemaFarmac.crearCampoTexto(u.getNombreCompleto(), 20);
        JPasswordField txtPass = TemaFarmac.crearCampoPassword("", 15);
        JComboBox<String> cmbRol = new JComboBox<>(new String[]{"EMPLEADO", "ENCARGADO"});
        cmbRol.setSelectedItem(u.obtenerRol());
        cmbRol.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbRol.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Usuario (Login):")); form.add(txtUser);
        form.add(new JLabel("Nombre Completo:")); form.add(txtNombre);
        form.add(new JLabel("Nueva Contrasena (dejar vacio para no cambiar):")); form.add(txtPass);
        form.add(new JLabel("Rol Asignado:")); form.add(cmbRol);

        int res = JOptionPane.showConfirmDialog(this, form, "Modificar Usuario #" + u.getId(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            u.setNombreUsuario(txtUser.getText().trim());
            u.setNombreCompleto(txtNombre.getText().trim());

            String nuevaPass = new String(txtPass.getPassword()).trim();
            if (!nuevaPass.isEmpty()) {
                u.cambiarPassword(nuevaPass);
            }

            // Si cambió el rol, recrear la instancia polimórfica adecuada
            String nuevoRol = (String) cmbRol.getSelectedItem();
            if (!nuevoRol.equalsIgnoreCase(u.obtenerRol())) {
                Usuario actualizado;
                if ("ENCARGADO".equals(nuevoRol)) {
                    actualizado = new Encargado(u.getId(), u.getNombreUsuario(), u.getNombreCompleto(), u.getPassword(), u.isActivo());
                } else {
                    actualizado = new Empleado(u.getId(), u.getNombreUsuario(), u.getNombreCompleto(), u.getPassword(), u.isActivo());
                }
                listaUsuarios.set(fila, actualizado);
            }

            llenarTabla();
            JOptionPane.showMessageDialog(this, "Datos de usuario actualizados correctamente.", "Actualizacion Exitosa", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void alternarEstadoUsuario() {
        int fila = tblUsuarios.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Usuario u = listaUsuarios.get(fila);
        u.setActivo(!u.isActivo());
        llenarTabla();
        String estadoStr = u.isActivo() ? "ACTIVADO" : "DESACTIVADO";
        JOptionPane.showMessageDialog(this, "El usuario '" + u.getNombreUsuario() + "' ha sido " + estadoStr + ".", "Estado Modificado", JOptionPane.INFORMATION_MESSAGE);
    }
}
