package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.UsuarioDAO;
import farmac.dominio.Empleado;
import farmac.dominio.Encargado;
import farmac.dominio.Usuario;
import farmac.util.Sesion;
import farmac.util.TemaFarmac;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Pantalla de Administración y CRUD de Usuarios con persistencia en PostgreSQL (RF01).
 * Aplica restricciones de seguridad para no eliminar al último encargado ni a sí mismo.
 */
public class GestionUsuariosScreen extends JPanel {

    private final UsuarioDAO usuarioDAO;
    private JTable tblUsuarios;
    private DefaultTableModel modeloTabla;
    private List<Usuario> listaUsuarios;
    private JButton btnNuevo;
    private JButton btnEditar;
    private JButton btnCambiarEstado;

    public GestionUsuariosScreen() {
        this.usuarioDAO = new UsuarioDAO();
        inicializarUI();
        cargarDatos();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        // Cabecera
        JPanel pnlHeader = new JPanel(new BorderLayout(12, 12));
        pnlHeader.setOpaque(false);

        JPanel pnlTit = new JPanel();
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        pnlTit.setOpaque(false);

        JLabel lblTitulo = new JLabel("Administración y CRUD de Usuarios");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Alta, edición de credenciales (BCrypt), asignación de roles y control de acceso (RF01)");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        pnlTit.add(lblTitulo);
        pnlTit.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlTit.add(lblSub);

        // Barra de botones CRUD
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

        // Tabla Principal
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
        scroll.setBorder(BorderFactory.createLineBorder(TemaFarmac.GRIS_BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        pnlCard.add(scroll, BorderLayout.CENTER);

        JLabel lblAyuda = new JLabel("Nota de seguridad: No está permitido desactivar al último Encargado ni modificar el estado de su propia cuenta activa.");
        lblAyuda.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblAyuda.setForeground(TemaFarmac.TEXTO_MUTED);
        pnlCard.add(lblAyuda, BorderLayout.SOUTH);

        add(pnlCard, BorderLayout.CENTER);
    }

    public void cargarDatos() {
        try {
            listaUsuarios = usuarioDAO.listarTodos();
            llenarTabla();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar usuarios de la base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void llenarTabla() {
        modeloTabla.setRowCount(0);
        if (listaUsuarios == null) return;
        for (Usuario u : listaUsuarios) {
            modeloTabla.addRow(new Object[]{
                u.getId(),
                u.getNombreUsuario(),
                u.getNombreCompleto(),
                u.obtenerRol(),
                u.isActivo() ? "ACTIVO" : "INACTIVO"
            });
        }
    }

    private Usuario obtenerSeleccionado() {
        int fila = tblUsuarios.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int id = (int) modeloTabla.getValueAt(fila, 0);
        if (listaUsuarios != null) {
            for (Usuario u : listaUsuarios) {
                if (u.getId() == id) return u;
            }
        }
        return null;
    }

    private void abrirFormularioNuevoUsuario() {
        JTextField txtUser = TemaFarmac.crearCampoTexto(15);
        JTextField txtNombre = TemaFarmac.crearCampoTexto(20);
        JPasswordField txtPass = new JPasswordField(15);
        JComboBox<String> cmbRol = new JComboBox<>(new String[]{"EMPLEADO", "ENCARGADO"});
        cmbRol.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbRol.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Nombre de Usuario (Login) *:")); form.add(txtUser);
        form.add(new JLabel("Nombre Completo *:")); form.add(txtNombre);
        form.add(new JLabel("Contraseña Inicial *:")); form.add(txtPass);
        form.add(new JLabel("Rol en el Sistema:")); form.add(cmbRol);

        int res = JOptionPane.showConfirmDialog(this, form, "Registrar Nuevo Usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String u = txtUser.getText().trim();
            String n = txtNombre.getText().trim();
            String p = new String(txtPass.getPassword()).trim();
            String r = (String) cmbRol.getSelectedItem();

            if (u.isEmpty() || n.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos los campos marcados con (*) son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (usuarioDAO.existeLogin(u, 0)) {
                    JOptionPane.showMessageDialog(this, "El nombre de usuario '" + u + "' ya está en uso. Elija otro.", "Login Duplicado", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String hash = BCrypt.hashpw(p, BCrypt.gensalt());
                Usuario nuevo;
                if ("ENCARGADO".equalsIgnoreCase(r)) {
                    nuevo = new Encargado(0, u, n, hash, true);
                } else {
                    nuevo = new Empleado(0, u, n, hash, true);
                }

                usuarioDAO.insertar(nuevo);
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "USUARIO_ALTA", "Alta de cuenta de usuario: " + u + " (" + r + ")");
                cargarDatos();
                JOptionPane.showMessageDialog(this, "Usuario '" + u + "' creado exitosamente en la base de datos.", "Usuario Registrado", JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar usuario: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirFormularioEditarUsuario() {
        Usuario u = obtenerSeleccionado();
        if (u == null) return;

        JTextField txtUser = TemaFarmac.crearCampoTexto(15);
        txtUser.setText(u.getNombreUsuario());
        JTextField txtNombre = TemaFarmac.crearCampoTexto(20);
        txtNombre.setText(u.getNombreCompleto());
        JPasswordField txtPass = new JPasswordField(15);
        JComboBox<String> cmbRol = new JComboBox<>(new String[]{"EMPLEADO", "ENCARGADO"});
        cmbRol.setSelectedItem(u.obtenerRol());
        cmbRol.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbRol.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Usuario (Login) *:")); form.add(txtUser);
        form.add(new JLabel("Nombre Completo *:")); form.add(txtNombre);
        form.add(new JLabel("Nueva Contraseña (vacío = no cambiar):")); form.add(txtPass);
        form.add(new JLabel("Rol Asignado:")); form.add(cmbRol);

        int res = JOptionPane.showConfirmDialog(this, form, "Modificar Usuario #" + u.getId(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String nuevoLogin = txtUser.getText().trim();
            String nuevoNombre = txtNombre.getText().trim();
            String nuevaPass = new String(txtPass.getPassword()).trim();
            String nuevoRol = (String) cmbRol.getSelectedItem();

            if (nuevoLogin.isEmpty() || nuevoNombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El login y el nombre no pueden quedar vacíos.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (usuarioDAO.existeLogin(nuevoLogin, u.getId())) {
                    JOptionPane.showMessageDialog(this, "El nombre de usuario '" + nuevoLogin + "' ya pertenece a otra cuenta.", "Login Duplicado", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Restricción: No degradar al último Encargado
                if ("ENCARGADO".equalsIgnoreCase(u.obtenerRol()) && "EMPLEADO".equalsIgnoreCase(nuevoRol)) {
                    if (usuarioDAO.contarEncargadosActivos() <= 1) {
                        JOptionPane.showMessageDialog(this, "No es posible cambiar el rol: es el único Encargado activo del sistema.", "Restricción de Seguridad", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                u.setNombreUsuario(nuevoLogin);
                u.setNombreCompleto(nuevoNombre);

                boolean actualizaPass = false;
                if (!nuevaPass.isEmpty()) {
                    u.cambiarPassword(nuevaPass);
                    actualizaPass = true;
                }

                Usuario actualizado;
                if ("ENCARGADO".equalsIgnoreCase(nuevoRol)) {
                    actualizado = new Encargado(u.getId(), nuevoLogin, nuevoNombre, u.getPassword(), u.isActivo());
                } else {
                    actualizado = new Empleado(u.getId(), nuevoLogin, nuevoNombre, u.getPassword(), u.isActivo());
                }

                usuarioDAO.actualizar(actualizado, actualizaPass);
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "USUARIO_EDICION", "Edición de cuenta #" + u.getId() + " (" + nuevoLogin + ")");
                cargarDatos();
                JOptionPane.showMessageDialog(this, "Datos de usuario actualizados correctamente.", "Actualización Exitosa", JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar usuario: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void alternarEstadoUsuario() {
        Usuario u = obtenerSeleccionado();
        if (u == null) return;

        // Validaciones de seguridad
        Usuario sesionActual = Sesion.getUsuarioActivo();
        if (sesionActual != null && sesionActual.getId() == u.getId()) {
            JOptionPane.showMessageDialog(this, "No puede desactivar su propia cuenta de sesión activa.", "Acción Denegada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (u.isActivo() && "ENCARGADO".equalsIgnoreCase(u.obtenerRol())) {
                if (usuarioDAO.contarEncargadosActivos() <= 1) {
                    JOptionPane.showMessageDialog(this, "No puede desactivar al único Encargado activo del sistema.", "Restricción de Seguridad", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            boolean nuevoEstado = !u.isActivo();
            String accionStr = nuevoEstado ? "activar" : "desactivar";
            int conf = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de " + accionStr + " la cuenta de '" + u.getNombreUsuario() + "'?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                usuarioDAO.cambiarEstado(u.getId(), nuevoEstado);
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "USUARIO_ESTADO",
                        "Usuario #" + u.getId() + " marcado como " + (nuevoEstado ? "ACTIVO" : "INACTIVO"));
                cargarDatos();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cambiar estado: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }
}
