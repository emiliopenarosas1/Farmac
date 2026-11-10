package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.UsuarioDAO;
import farmac.dominio.Empleado;
import farmac.dominio.Encargado;
import farmac.dominio.Usuario;
import farmac.util.Sesion;
import farmac.util.DialogoFarmac;
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
 * Pantalla de Administración y CRUD de Usuarios con persistencia en PostgreSQL.
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
        // Cargar después de pintar la pantalla, sin bloquear la navegación
        javax.swing.SwingUtilities.invokeLater(this::cargarDatos);
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 12));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(20, 26, 20, 26));

        // Cabecera
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        JLabel lblTitulo = new JLabel("Administración de usuarios");
        lblTitulo.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 20));
        lblTitulo.setForeground(TemaFarmac.TEXTO_AZUL);
        btnNuevo = TemaFarmac.crearBoton("Nuevo usuario", TemaFarmac.TURQUESA, Color.WHITE);
        btnNuevo.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.PLUS, 12, Color.WHITE));
        btnNuevo.setIconTextGap(8);
        btnNuevo.addActionListener(e -> abrirFormularioNuevoUsuario());
        pnlHeader.add(lblTitulo, BorderLayout.WEST);
        pnlHeader.add(btnNuevo, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        // Tabla principal
        String[] columnas = {"ID", "Usuario (login)", "Nombre completo del personal", "Rol asignado", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblUsuarios = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tblUsuarios);
        tblUsuarios.getColumnModel().getColumn(0).setPreferredWidth(55);
        tblUsuarios.getColumnModel().getColumn(1).setPreferredWidth(150);
        tblUsuarios.getColumnModel().getColumn(2).setPreferredWidth(360);
        tblUsuarios.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblUsuarios.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblUsuarios.getColumnModel().getColumn(3).setCellRenderer(new TemaFarmac.BadgeCellRenderer());
        tblUsuarios.getColumnModel().getColumn(4).setCellRenderer(new TemaFarmac.BadgeCellRenderer());
        // La columna ID se conserva en el modelo pero no se muestra
        tblUsuarios.removeColumn(tblUsuarios.getColumnModel().getColumn(0));

        JScrollPane scroll = new JScrollPane(tblUsuarios);
        scroll.setBorder(new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 8, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        // Acciones sobre la fila seleccionada
        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlAcciones.setOpaque(false);
        btnEditar = TemaFarmac.crearBotonSecundario("Editar usuario seleccionado");
        btnEditar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.PENCIL_ALT, 12, TemaFarmac.TEXTO));
        btnEditar.setIconTextGap(8);
        btnEditar.addActionListener(e -> abrirFormularioEditarUsuario());
        btnCambiarEstado = TemaFarmac.crearBotonPeligro("Activar / desactivar");
        btnCambiarEstado.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.POWER_OFF, 12, TemaFarmac.ROJO));
        btnCambiarEstado.setIconTextGap(8);
        btnCambiarEstado.addActionListener(e -> alternarEstadoUsuario());
        pnlAcciones.add(btnEditar);
        pnlAcciones.add(btnCambiarEstado);
        add(pnlAcciones, BorderLayout.SOUTH);
    }

    public void cargarDatos() {
        try {
            listaUsuarios = usuarioDAO.listarTodos();
            llenarTabla();
        } catch (SQLException e) {
            DialogoFarmac.mostrarJO(this, "Error al cargar usuarios de la base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            DialogoFarmac.mostrarJO(this, "Seleccione un usuario de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
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
        JPasswordField txtPass = TemaFarmac.crearCampoPassword(15);
        JComboBox<String> cmbRol = new JComboBox<>(new String[]{"EMPLEADO", "ENCARGADO"});
        TemaFarmac.estilizarComboBox(cmbRol);

        JPanel form = TemaFarmac.crearFormulario(
                new String[]{
                    "Nombre de Usuario (Login) *:",
                    "Nombre Completo *:",
                    "Contraseña Inicial *:",
                    "Rol en el Sistema:"
                },
                new javax.swing.JComponent[]{txtUser, txtNombre, txtPass, cmbRol});

boolean res = DialogoFarmac.confirmar(this, "Registrar Nuevo Usuario", form);
        if (res) {
            String u = txtUser.getText().trim();
            String n = txtNombre.getText().trim();
            String p = new String(txtPass.getPassword()).trim();
            String r = (String) cmbRol.getSelectedItem();

            if (u.isEmpty() || n.isEmpty() || p.isEmpty()) {
                DialogoFarmac.mostrarJO(this, "Todos los campos marcados con (*) son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (usuarioDAO.existeLogin(u, 0)) {
                    DialogoFarmac.mostrarJO(this, "El nombre de usuario '" + u + "' ya está en uso. Elija otro.", "Login Duplicado", JOptionPane.WARNING_MESSAGE);
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
                DialogoFarmac.mostrarJO(this, "Usuario '" + u + "' creado exitosamente en la base de datos.", "Usuario Registrado", JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException ex) {
                DialogoFarmac.mostrarJO(this, "Error al guardar usuario: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
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
        JPasswordField txtPass = TemaFarmac.crearCampoPassword(15);
        JComboBox<String> cmbRol = new JComboBox<>(new String[]{"EMPLEADO", "ENCARGADO"});
        cmbRol.setSelectedItem(u.obtenerRol());
        TemaFarmac.estilizarComboBox(cmbRol);

        JPanel form = TemaFarmac.crearFormulario(
                new String[]{
                    "Usuario (Login) *:",
                    "Nombre Completo *:",
                    "Nueva Contraseña (vacío = no cambiar):",
                    "Rol Asignado:"
                },
                new javax.swing.JComponent[]{txtUser, txtNombre, txtPass, cmbRol});

boolean res = DialogoFarmac.confirmar(this, "Modificar Usuario #" + u.getId(), form);
        if (res) {
            String nuevoLogin = txtUser.getText().trim();
            String nuevoNombre = txtNombre.getText().trim();
            String nuevaPass = new String(txtPass.getPassword()).trim();
            String nuevoRol = (String) cmbRol.getSelectedItem();

            if (nuevoLogin.isEmpty() || nuevoNombre.isEmpty()) {
                DialogoFarmac.mostrarJO(this, "El login y el nombre no pueden quedar vacíos.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (usuarioDAO.existeLogin(nuevoLogin, u.getId())) {
                    DialogoFarmac.mostrarJO(this, "El nombre de usuario '" + nuevoLogin + "' ya pertenece a otra cuenta.", "Login Duplicado", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Restricción: No degradar al último Encargado
                if ("ENCARGADO".equalsIgnoreCase(u.obtenerRol()) && "EMPLEADO".equalsIgnoreCase(nuevoRol)) {
                    if (usuarioDAO.contarEncargadosActivos() <= 1) {
                        DialogoFarmac.mostrarJO(this, "No es posible cambiar el rol: es el único Encargado activo del sistema.", "Restricción de Seguridad", JOptionPane.WARNING_MESSAGE);
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
                DialogoFarmac.mostrarJO(this, "Datos de usuario actualizados correctamente.", "Actualización Exitosa", JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException ex) {
                DialogoFarmac.mostrarJO(this, "Error al actualizar usuario: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void alternarEstadoUsuario() {
        Usuario u = obtenerSeleccionado();
        if (u == null) return;

        // Validaciones de seguridad
        Usuario sesionActual = Sesion.getUsuarioActivo();
        if (sesionActual != null && sesionActual.getId() == u.getId()) {
            DialogoFarmac.mostrarJO(this, "No puede desactivar su propia cuenta de sesión activa.", "Acción Denegada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (u.isActivo() && "ENCARGADO".equalsIgnoreCase(u.obtenerRol())) {
                if (usuarioDAO.contarEncargadosActivos() <= 1) {
                    DialogoFarmac.mostrarJO(this, "No puede desactivar al único Encargado activo del sistema.", "Restricción de Seguridad", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            boolean nuevoEstado = !u.isActivo();
            String accionStr = nuevoEstado ? "activar" : "desactivar";
            boolean conf = DialogoFarmac.confirmarJO(this,
                    "¿Está seguro de " + accionStr + " la cuenta de '" + u.getNombreUsuario() + "'?",
                    "Confirmar");
            if (conf) {
                usuarioDAO.cambiarEstado(u.getId(), nuevoEstado);
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "USUARIO_ESTADO",
                        "Usuario #" + u.getId() + " marcado como " + (nuevoEstado ? "ACTIVO" : "INACTIVO"));
                cargarDatos();
            }
        } catch (SQLException ex) {
            DialogoFarmac.mostrarJO(this, "Error al cambiar estado: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }
}
