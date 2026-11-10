package farmac.ui;

import farmac.config.ConexionBD;
import farmac.dao.BitacoraDAO;
import farmac.dao.LoteDAO;
import farmac.dao.MovimientoDAO;
import farmac.dominio.Encargado;
import farmac.dominio.Lote;
import farmac.dominio.MovimientoInventario;
import farmac.dominio.Usuario;
import farmac.util.Sesion;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla para ajuste manual de inventario con lotes reales de la BD.
 * Exclusiva para Encargados.
 */
public class AjusteInventarioScreen extends JPanel {

    private final Usuario usuario;
    private final LoteDAO loteDAO;
    private final MovimientoDAO movimientoDAO;

    private JComboBox<LoteItem> cmbLotes;
    private JLabel lblStockActual;
    private JTextField txtCantidadAjuste;
    private JComboBox<String> cmbMotivo;

    private static class LoteItem {
        final Lote lote;
        LoteItem(Lote lote) { this.lote = lote; }
        @Override
        public String toString() {
            return lote.getNumeroLote() + " - " + lote.getProducto().getNombre()
                 + " (Disp: " + lote.getCantidadActual() + " pzas, Cad: " + lote.getFechaCaducidad() + ")";
        }
    }

    public AjusteInventarioScreen(Usuario usuario) {
        this.usuario = usuario != null ? usuario : Sesion.getUsuarioActivo();
        this.loteDAO = new LoteDAO();
        this.movimientoDAO = new MovimientoDAO();

        inicializarUI();
        // Cargar después de pintar la pantalla, sin bloquear la navegación
        javax.swing.SwingUtilities.invokeLater(this::cargarLotes);
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 12));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(20, 26, 20, 26));

        JLabel lblTitulo = new JLabel("Ajuste manual de inventario");
        lblTitulo.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 20));
        lblTitulo.setForeground(TemaFarmac.TEXTO_AZUL);
        add(lblTitulo, BorderLayout.NORTH);

        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new BorderLayout(0, 16));
        pnlCard.setBorder(new EmptyBorder(18, 18, 18, 18));

        cmbLotes = new JComboBox<>();
        TemaFarmac.estilizarComboBox(cmbLotes);
        cmbLotes.setPreferredSize(new Dimension(200, 38));
        cmbLotes.addActionListener(e -> actualizarDetalleStock());

        lblStockActual = new JLabel("0 piezas");
        lblStockActual.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 13));
        lblStockActual.setForeground(TemaFarmac.TEXTO_MUTED);

        txtCantidadAjuste = TemaFarmac.crearCampoTexto(15);
        txtCantidadAjuste.putClientProperty("JTextField.placeholderText", "Ej. 5");

        cmbMotivo = new JComboBox<>(new String[]{
            "MERMA / PRODUCTO DAÑADO",
            "PRODUCTO CADUCADO",
            "DISCREPANCIA EN CONTEO FÍSICO",
            "ROBO O EXTRAVÍO",
            "DEVOLUCIÓN A PROVEEDOR",
            "OTRO"
        });
        TemaFarmac.estilizarComboBox(cmbMotivo);
        cmbMotivo.setPreferredSize(new Dimension(200, 38));

        // Lote (fila completa)
        JPanel pnlLote = TemaFarmac.crearFormularioGrid(
                new String[]{"Lote a ajustar *"}, new javax.swing.JComponent[]{cmbLotes}, new int[]{2});

        // Caja de existencia disponible
        JPanel pnlExistencia = new JPanel(new BorderLayout());
        pnlExistencia.setBackground(new Color(0xEC, 0xEF, 0xF3));
        pnlExistencia.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel lblExistEt = new JLabel("Existencia disponible");
        lblExistEt.setFont(TemaFarmac.fuente(java.awt.Font.PLAIN, 13));
        lblExistEt.setForeground(TemaFarmac.TEXTO_MUTED);
        pnlExistencia.add(lblExistEt, BorderLayout.WEST);
        pnlExistencia.add(lblStockActual, BorderLayout.EAST);

        // Cantidad y motivo (dos columnas)
        JPanel pnlCampos = TemaFarmac.crearFormularioGrid(
                new String[]{"Piezas a descontar *", "Motivo justificado *"},
                new javax.swing.JComponent[]{txtCantidadAjuste, cmbMotivo},
                new int[]{1, 1});

        JPanel pnlAviso = TemaFarmac.crearAviso(
                "Solo el Encargado puede autorizar ajustes de inventario. La operación es transaccional y se revierte si falla.",
                TemaFarmac.AMBAR_BG, TemaFarmac.AMBAR);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        for (JPanel c : new JPanel[]{pnlLote, pnlExistencia, pnlCampos, pnlAviso}) {
            c.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        }
        centro.add(pnlLote);
        centro.add(Box.createRigidArea(new Dimension(0, 12)));
        centro.add(pnlExistencia);
        centro.add(Box.createRigidArea(new Dimension(0, 12)));
        centro.add(pnlCampos);
        centro.add(Box.createRigidArea(new Dimension(0, 12)));
        centro.add(pnlAviso);
        pnlCard.add(centro, BorderLayout.CENTER);

        JButton btnAplicar = TemaFarmac.crearBoton("Autorizar y aplicar ajuste", TemaFarmac.TURQUESA, Color.WHITE);
        btnAplicar.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 13));
        btnAplicar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHECK, 13, Color.WHITE));
        btnAplicar.setIconTextGap(8);
        btnAplicar.setPreferredSize(new Dimension(200, 46));
        btnAplicar.addActionListener(e -> autorizar());
        pnlCard.add(btnAplicar, BorderLayout.SOUTH);

        // Navegación con Enter: lote -> piezas -> motivo -> autorizar
        TemaFarmac.enterComoTab(this::autorizar, cmbLotes, txtCantidadAjuste, cmbMotivo);

        pnlCard.setPreferredSize(new Dimension(800, pnlCard.getPreferredSize().height));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(pnlCard, BorderLayout.WEST);
        add(TemaFarmac.crearScrollResponsivo(wrap), BorderLayout.CENTER);
    }

    public void cargarLotes() {
        try {
            List<Lote> lotes = loteDAO.listarTodos();
            cmbLotes.removeAllItems();
            for (Lote l : lotes) {
                if (l.getCantidadActual() > 0) {
                    cmbLotes.addItem(new LoteItem(l));
                }
            }
            actualizarDetalleStock();
        } catch (SQLException e) {
DialogoFarmac.mostrar(this, "Error", "Error al cargar lotes: " + e.getMessage(), DialogoFarmac.Tipo.ERROR);
        }
    }

    private void actualizarDetalleStock() {
        LoteItem item = (LoteItem) cmbLotes.getSelectedItem();
        if (item != null) {
            lblStockActual.setText(item.lote.getCantidadActual() + " piezas disponibles en almacén");
        } else {
            lblStockActual.setText("No hay lotes con existencias disponibles.");
        }
    }

    private void autorizar() {
        if (!Sesion.esEncargado() && !(usuario instanceof Encargado)) {
            DialogoFarmac.mostrarJO(this, "Permiso denegado: solo el Encargado puede autorizar ajustes de inventario.", "Seguridad", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LoteItem item = (LoteItem) cmbLotes.getSelectedItem();
        if (item == null) {
            DialogoFarmac.mostrarJO(this, "Seleccione un lote válido.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cantStr = txtCantidadAjuste.getText().trim();
        if (cantStr.isEmpty()) {
            DialogoFarmac.mostrarJO(this, "Ingrese la cantidad de piezas a descontar.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantStr);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            DialogoFarmac.mostrarJO(this, "La cantidad a descontar debe ser un número entero mayor a 0.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Lote lote = item.lote;
        if (cantidad > lote.getCantidadActual()) {
            DialogoFarmac.mostrarJO(this,
                    "No se puede descontar " + cantidad + " piezas: el lote solo tiene "
                    + lote.getCantidadActual() + " disponibles.", "Existencias Insuficientes", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String motivo = (String) cmbMotivo.getSelectedItem();

        boolean confirm = DialogoFarmac.confirmarJO(this,
                "¿Confirma descontar " + cantidad + " piezas del lote '" + lote.getNumeroLote() + "'?\nMotivo: " + motivo,
                "Confirmar Ajuste Manual");

        if (confirm) {
            Connection conn = null;
            try {
                // Una sola transacción: actualización de stock + movimiento de auditoría.
                conn = ConexionBD.obtenerConexion();
                conn.setAutoCommit(false);

                int nuevoStock = lote.getCantidadActual() - cantidad;
                if (!loteDAO.actualizarStock(lote.getId(), nuevoStock, conn)) {
                    throw new SQLException("El lote ya no existe o no pudo actualizarse.");
                }

                MovimientoInventario mov = new MovimientoInventario(
                        0, lote, "AJUSTE", -cantidad, motivo,
                        "AJUSTE-" + System.currentTimeMillis(), usuario, LocalDateTime.now()
                );
                movimientoDAO.insertar(mov, conn);

                conn.commit();
                lote.setCantidadActual(nuevoStock);

                BitacoraDAO.registrar(usuario, "INVENTARIO_AJUSTE",
                        "Ajuste en lote " + lote.getNumeroLote() + " (-" + cantidad + " pzas) por: " + motivo);

                DialogoFarmac.mostrarJO(this,
                        "Ajuste autorizado y aplicado correctamente.\nNuevo saldo del lote: " + nuevoStock + " piezas.",
                        "Ajuste Exitoso", JOptionPane.INFORMATION_MESSAGE);

                txtCantidadAjuste.setText("");
                cargarLotes();

            } catch (SQLException ex) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        System.err.println("Error al revertir ajuste: " + rollbackEx.getMessage());
                    }
                }
                DialogoFarmac.mostrarJO(this, "Error al aplicar ajuste en base de datos (rollback): " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        // Cierre seguro
                    }
                }
            }
        }
    }
}
