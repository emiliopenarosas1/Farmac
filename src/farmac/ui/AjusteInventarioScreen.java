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
 * Pantalla para ajuste manual de inventario con lotes reales de la BD (RF09).
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
        cargarLotes();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);

        JLabel lblTitulo = new JLabel("Ajuste Manual de Inventario");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³dulo exclusivo para Encargados: Registro auditable de mermas, caducidades, robos y conteos (RF09)");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        JPanel pnlTit = new JPanel();
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        pnlTit.setOpaque(false);
        pnlTit.add(lblTitulo);
        pnlTit.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlTit.add(lblSub);

        pnlHeader.add(pnlTit, BorderLayout.WEST);
        add(pnlHeader, BorderLayout.NORTH);

        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new GridBagLayout());
        pnlCard.setPreferredSize(new Dimension(680, 440));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        // 1. Selector de Lote real
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblL = new JLabel("Lote a Ajustar *:");
        lblL.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblL, gbc);

        cmbLotes = new JComboBox<>();
        cmbLotes.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbLotes.setBackground(Color.WHITE);
        cmbLotes.setPreferredSize(new Dimension(340, 36));
        cmbLotes.addActionListener(e -> actualizarDetalleStock());
        gbc.gridx = 1;
        pnlCard.add(cmbLotes, gbc);

        // 2. Stock Disponible actual
        gbc.gridy = 1; gbc.gridx = 0;
        JLabel lblS = new JLabel("Existencia Disponible:");
        lblS.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblS, gbc);

        lblStockActual = new JLabel("0 piezas");
        lblStockActual.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblStockActual.setForeground(TemaFarmac.TURQUESA);
        gbc.gridx = 1;
        pnlCard.add(lblStockActual, gbc);

        // 3. Cantidad a descontar
        gbc.gridy = 2; gbc.gridx = 0;
        JLabel lblQ = new JLabel("Piezas a Descontar *:");
        lblQ.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblQ, gbc);

        txtCantidadAjuste = TemaFarmac.crearCampoTexto(15);
        txtCantidadAjuste.putClientProperty("JTextField.placeholderText", "Ej. 5");
        gbc.gridx = 1;
        pnlCard.add(txtCantidadAjuste, gbc);

        // 4. Motivo
        gbc.gridy = 3; gbc.gridx = 0;
        JLabel lblM = new JLabel("Motivo Justificado *:");
        lblM.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblM, gbc);

        cmbMotivo = new JComboBox<>(new String[]{
            "MERMA / PRODUCTO DAÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¹Ãƒâ€¦Ã¢â‚¬Å“ADO",
            "PRODUCTO CADUCADO",
            "DISCREPANCIA EN CONTEO FÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂSICO",
            "ROBO O EXTRAVÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂO",
            "DEVOLUCIÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œN A PROVEEDOR",
            "OTRO"
        });
        cmbMotivo.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbMotivo.setBackground(Color.WHITE);
        cmbMotivo.setPreferredSize(new Dimension(340, 36));
        gbc.gridx = 1;
        pnlCard.add(cmbMotivo, gbc);

        // BotÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n
        JButton btnAplicar = TemaFarmac.crearBoton("Autorizar y Aplicar Ajuste", TemaFarmac.ROJO, Color.WHITE);
        btnAplicar.setPreferredSize(new Dimension(340, 42));
        btnAplicar.addActionListener(e -> autorizar());

        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 12, 8, 12);
        pnlCard.add(btnAplicar, gbc);

        JPanel pnlContenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
        pnlContenedor.setOpaque(false);
        pnlContenedor.add(pnlCard);

        add(pnlContenedor, BorderLayout.CENTER);
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
            lblStockActual.setText(item.lote.getCantidadActual() + " piezas disponibles en almacÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©n");
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
            DialogoFarmac.mostrarJO(this, "Seleccione un lote vÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡lido.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cantStr = txtCantidadAjuste.getText().trim();
        if (cantStr.isEmpty()) {
            DialogoFarmac.mostrarJO(this, "Ingrese la cantidad de piezas a descontar.", "ValidaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantStr);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            DialogoFarmac.mostrarJO(this, "La cantidad a descontar debe ser un nÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Âºmero entero mayor a 0.", "ValidaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n", JOptionPane.WARNING_MESSAGE);
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
                "ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¿Confirma descontar " + cantidad + " piezas del lote '" + lote.getNumeroLote() + "'?\nMotivo: " + motivo,
                "Confirmar Ajuste Manual");

        if (confirm) {
            Connection conn = null;
            try {
                // Una sola transacciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n: actualizaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n de stock + movimiento de auditorÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­a.
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
