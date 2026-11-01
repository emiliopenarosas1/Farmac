package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.RecetaDAO;
import farmac.dao.TurnoCajaDAO;
import farmac.dao.VentaDAO;
import farmac.dominio.DetalleVenta;
import farmac.dominio.RecetaMedica;
import farmac.dominio.TurnoCaja;
import farmac.dominio.Venta;
import farmac.util.Sesion;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla de Historial de Ventas, Cancelaciones y Rotación de Productos (RF15, RF17).
 */
public class HistorialVentasScreen extends JPanel {

    private final VentaDAO ventaDAO;
    private final RecetaDAO recetaDAO;
    private final TurnoCajaDAO turnoCajaDAO;
    private JTable tablaVentas;
    private DefaultTableModel modeloVentas;
    private JTable tablaRotacion;
    private DefaultTableModel modeloRotacion;
    private JTable tablaVentasConReceta;
    private DefaultTableModel modeloVentasConReceta;
    private JTable tablaTurnos;
    private DefaultTableModel modeloTurnos;
    private JTextField txtFechaDesde;
    private JTextField txtFechaHasta;
    private JComboBox<String> cmbEstado;
    private JCheckBox chkSoloRecetas;
    private List<Venta> listaVentasActual;

    public HistorialVentasScreen() {
        this.ventaDAO = new VentaDAO();
        this.recetaDAO = new RecetaDAO();
        this.turnoCajaDAO = new TurnoCajaDAO();
        initComponents();
        // Cargar después de pintar la pantalla, sin bloquear la navegación
        javax.swing.SwingUtilities.invokeLater(this::cargarDatos);
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Encabezado
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(TemaFarmac.GRIS_FONDO);

        JLabel lblTitulo = new JLabel("Historial de Ventas y Rotación");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Auditoría de tickets, cancelaciones controladas y análisis de rotación (RF15, RF17)");
        lblSub.setFont(TemaFarmac.FUENTE_REGULAR);
        lblSub.setForeground(TemaFarmac.GRIS_TEXTO);

        JPanel pnlTitulos = new JPanel(new BorderLayout(0, 4));
        pnlTitulos.setBackground(TemaFarmac.GRIS_FONDO);
        pnlTitulos.add(lblTitulo, BorderLayout.NORTH);
        pnlTitulos.add(lblSub, BorderLayout.SOUTH);
        pnlHeader.add(pnlTitulos, BorderLayout.WEST);

        // Barra de filtros
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlFiltros.setBackground(TemaFarmac.GRIS_FONDO);

        LocalDate hoy = LocalDate.now();
        LocalDate haceUnMes = hoy.minusDays(30);

        txtFechaDesde = TemaFarmac.crearCampoTexto(8);
        txtFechaDesde.setText(haceUnMes.toString());
        txtFechaHasta = TemaFarmac.crearCampoTexto(8);
        txtFechaHasta.setText(hoy.toString());

        cmbEstado = new JComboBox<>(new String[]{"TODAS", "COMPLETADA", "CANCELADA"});
        TemaFarmac.estilizarComboBox(cmbEstado);

        JButton btnFiltrar = TemaFarmac.crearBotonPrimario("Filtrar");
        btnFiltrar.addActionListener(e -> cargarDatos());

        JButton btnDetalles = TemaFarmac.crearBotonSecundario("Ver Detalles");
        btnDetalles.addActionListener(e -> verDetallesSeleccionada());

        JButton btnCancelar = TemaFarmac.crearBotonPeligro("Cancelar Venta");
        btnCancelar.addActionListener(e -> cancelarVentaSeleccionada());

        chkSoloRecetas = new JCheckBox("Solo ventas con receta");
        pnlFiltros.add(chkSoloRecetas);

        pnlFiltros.add(new JLabel("Desde:"));
        pnlFiltros.add(txtFechaDesde);
        pnlFiltros.add(new JLabel("Hasta:"));
        pnlFiltros.add(txtFechaHasta);
        pnlFiltros.add(new JLabel("Estado:"));
        pnlFiltros.add(cmbEstado);
        pnlFiltros.add(btnFiltrar);
        pnlFiltros.add(btnDetalles);
        pnlFiltros.add(btnCancelar);

        pnlHeader.add(pnlFiltros, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        // Pestañas (Ventas vs Rotación)
        JTabbedPane tabbed = new JTabbedPane();

        // Pestaña 1: Ventas
        String[] colsVentas = {"ID / Ticket", "Fecha y Hora", "Cajero", "Turno", "Forma Pago", "Total", "Estado", "Motivo Canc."};
        modeloVentas = new DefaultTableModel(colsVentas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaVentas = new JTable(modeloVentas);
        TemaFarmac.estilizarTabla(tablaVentas);
        tablaVentas.getColumnModel().getColumn(0).setPreferredWidth(80);
        tablaVentas.getColumnModel().getColumn(1).setPreferredWidth(140);
        tablaVentas.getColumnModel().getColumn(2).setPreferredWidth(150);
        tablaVentas.getColumnModel().getColumn(3).setPreferredWidth(70);
        tablaVentas.getColumnModel().getColumn(4).setPreferredWidth(100);
        tablaVentas.getColumnModel().getColumn(5).setPreferredWidth(90);
        tablaVentas.getColumnModel().getColumn(6).setPreferredWidth(110);
        tablaVentas.getColumnModel().getColumn(7).setPreferredWidth(180);

        JScrollPane scrollVentas = new JScrollPane(tablaVentas);
        tabbed.addTab("Ventas Registradas", scrollVentas);

        // Pestaña 2: Rotación de Productos (RF17)
        if (Sesion.esEncargado()) {
            String[] colsRot = {"Código Barras", "Producto", "Unidades Vendidas", "Importe Generado ($)"};
            modeloRotacion = new DefaultTableModel(colsRot, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
            tablaRotacion = new JTable(modeloRotacion);
            TemaFarmac.estilizarTabla(tablaRotacion);
            JScrollPane scrollRot = new JScrollPane(tablaRotacion);
            tabbed.addTab("Ranking de Rotación (Más / Menos Vendidos)", scrollRot);

            // Pestaña 3: Ventas con Receta Médica asociada (RF14, auditoría)
            String[] colsRec = {"Venta", "Fecha", "Cajero", "Producto", "Folio Receta", "Médico", "Estado"};
            modeloVentasConReceta = new DefaultTableModel(colsRec, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
            tablaVentasConReceta = new JTable(modeloVentasConReceta);
            TemaFarmac.estilizarTabla(tablaVentasConReceta);
            tabbed.addTab("Ventas con Receta", new JScrollPane(tablaVentasConReceta));

            // Pestaña 4: Historial de Turnos de Caja (apertura / cierre) (RF12, RF16)
            String[] colsTurnos = {"ID", "Usuario", "Apertura", "Cierre", "Estado", "Fondo Inicial", "Monto Esperado", "Contado", "Diferencia"};
            modeloTurnos = new DefaultTableModel(colsTurnos, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
            tablaTurnos = new JTable(modeloTurnos);
            TemaFarmac.estilizarTabla(tablaTurnos);
            tabbed.addTab("Historial de Turnos", new JScrollPane(tablaTurnos));
        }

        add(tabbed, BorderLayout.CENTER);
    }

    public void cargarDatos() {
        LocalDate desde = null;
        LocalDate hasta = null;
        try {
            if (!txtFechaDesde.getText().trim().isEmpty()) {
                desde = LocalDate.parse(txtFechaDesde.getText().trim());
            }
            if (!txtFechaHasta.getText().trim().isEmpty()) {
                hasta = LocalDate.parse(txtFechaHasta.getText().trim());
            }
        } catch (Exception ex) {
            DialogoFarmac.mostrarJO(this, "Formato de fecha inválido. Utilice YYYY-MM-DD.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String estado = (String) cmbEstado.getSelectedItem();

        try {
            listaVentasActual = ventaDAO.listarVentas(desde, hasta, estado);
            modeloVentas.setRowCount(0);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Venta v : listaVentasActual) {
                modeloVentas.addRow(new Object[]{
                    "#" + v.getId(),
                    v.getFechaHora() != null ? v.getFechaHora().format(dtf) : "-",
                    v.getUsuario() != null ? v.getUsuario().getNombreCompleto() : "N/A",
                    v.getTurnoCaja() != null ? "Turno #" + v.getTurnoCaja().getId() : "-",
                    v.getFormaPago(),
                    String.format("$%.2f", v.getTotal()),
                    v.getEstado(),
                    v.getMotivoCancelacion() != null ? v.getMotivoCancelacion() : "-"
                });
            }

            // Cargar rotación si existe modelo
            if (modeloRotacion != null) {
                modeloRotacion.setRowCount(0);
                List<VentaDAO.RotacionProducto> rotacion = ventaDAO.obtenerRankingRotacion(desde, hasta);
                for (VentaDAO.RotacionProducto r : rotacion) {
                    modeloRotacion.addRow(new Object[]{
                        r.getCodigoBarras(),
                        r.getNombreProducto(),
                        r.getUnidadesVendidas() + " pzas",
                        String.format("$%.2f", r.getTotalImporte())
                    });
                }
            }

            // Filtro opcional: solo ventas con receta médica capturada (RF14)
            if (chkSoloRecetas.isSelected() && listaVentasActual != null) {
                List<Venta> ventasConReceta = new ArrayList<>();
                for (Venta v : listaVentasActual) {
                    if (v == null) continue;
                    List<RecetaMedica> recetas = recetaDAO.listarPorVenta(v.getId());
                    if (recetas != null && !recetas.isEmpty()) {
                        ventasConReceta.add(v);
                    }
                }
                listaVentasActual = ventasConReceta;
            }

            // Historial de ventas con receta asociada
            if (modeloVentasConReceta != null) {
                cargarVentasConReceta();
            }

            // Historial de turnos de caja (apertura / cierre)
            if (modeloTurnos != null) {
                cargarTurnos();
            }
        } catch (SQLException e) {
            DialogoFarmac.mostrarJO(this, "Error al cargar historial: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga en la pestaña las ventas del periodo/estado que tengan receta médica asociada.
     */
    private void cargarVentasConReceta() {
        modeloVentasConReceta.setRowCount(0);
        LocalDate desde = null;
        LocalDate hasta = null;
        try {
            if (!txtFechaDesde.getText().trim().isEmpty()) {
                desde = LocalDate.parse(txtFechaDesde.getText().trim());
            }
            if (!txtFechaHasta.getText().trim().isEmpty()) {
                hasta = LocalDate.parse(txtFechaHasta.getText().trim());
            }
        } catch (Exception ex) {
            return;
        }
        String estado = (String) cmbEstado.getSelectedItem();

        try {
            List<Venta> ventas = ventaDAO.listarVentas(desde, hasta, estado);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (Venta v : ventas) {
                if (v == null) continue;
                List<RecetaMedica> recetas = recetaDAO.listarPorVenta(v.getId());
                if (recetas == null || recetas.isEmpty()) continue;
                for (RecetaMedica r : recetas) {
                    modeloVentasConReceta.addRow(new Object[]{
                        "#" + v.getId(),
                        v.getFechaHora() != null ? v.getFechaHora().format(dtf) : "-",
                        v.getUsuario() != null ? v.getUsuario().getNombreCompleto() : "N/A",
                        v.getTurnoCaja() != null ? "Turno #" + v.getTurnoCaja().getId() : "-",
                        String.format("$%.2f", v.getTotal()),
                        r.getFolioReceta(),
                        r.getNombreMedico()
                    });
                }
            }
        } catch (SQLException e) {
            DialogoFarmac.mostrarJO(this, "Error al cargar ventas con receta: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga el historial de turnos de caja con sus datos de apertura y cierre.
     */
    private void cargarTurnos() {
        modeloTurnos.setRowCount(0);
        try {
            List<TurnoCaja> turnos = turnoCajaDAO.listarHistorial(200);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (TurnoCaja t : turnos) {
                modeloTurnos.addRow(new Object[]{
                    t.getId(),
                    t.getUsuario() != null ? t.getUsuario().getNombreCompleto() : "N/A",
                    t.getFechaApertura() != null ? t.getFechaApertura().format(dtf) : "-",
                    t.getFechaCierre() != null ? t.getFechaCierre().format(dtf) : "Abierto",
                    t.getEstado(),
                    String.format("$%.2f", t.getMontoInicial()),
                    String.format("$%.2f", t.getMontoEsperado()),
                    String.format("$%.2f", t.getEfectivoContado()),
                    String.format("$%.2f", t.getDiferencia())
                });
            }
        } catch (SQLException e) {
            DialogoFarmac.mostrarJO(this, "Error al cargar historial de turnos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Venta obtenerSeleccionada() {
        int fila = tablaVentas.getSelectedRow();
        if (fila < 0) {
            DialogoFarmac.mostrarJO(this, "Seleccione una venta de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (listaVentasActual != null && fila < listaVentasActual.size()) {
            return listaVentasActual.get(fila);
        }
        return null;
    }

    private void verDetallesSeleccionada() {
        Venta v = obtenerSeleccionada();
        if (v == null) return;

        JDialog dlg = new JDialog((java.awt.Frame) null, "Detalles de Venta #" + v.getId(), true);
        dlg.setSize(600, 450);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel pnlInfo = new JPanel(new GridLayout(2, 2, 8, 4));
        pnlInfo.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        pnlInfo.add(new JLabel("Fecha: " + v.getFechaHora()));
        pnlInfo.add(new JLabel("Total: $" + v.getTotal()));
        pnlInfo.add(new JLabel("Forma Pago: " + v.getFormaPago()));
        pnlInfo.add(new JLabel("Estado: " + v.getEstado()));
        dlg.add(pnlInfo, BorderLayout.NORTH);

        try {
            List<DetalleVenta> detalles = ventaDAO.listarDetallesPorVenta(v.getId());
            String[] colsD = {"Producto", "Lote", "Caducidad", "Cant", "P. Unit", "Subtotal"};
            DefaultTableModel modD = new DefaultTableModel(colsD, 0);
            for (DetalleVenta d : detalles) {
                modD.addRow(new Object[]{
                    d.getLote().getProducto().getNombre(),
                    d.getLote().getNumeroLote(),
                    d.getLote().getFechaCaducidad(),
                    d.getCantidad(),
                    String.format("$%.2f", d.getPrecioUnitario()),
                    String.format("$%.2f", d.getSubtotal())
                });
            }
            JTable tblD = new JTable(modD);
            TemaFarmac.estilizarTabla(tblD);

            // Recetas asociadas
            List<RecetaMedica> recetas = recetaDAO.listarPorVenta(v.getId());
            JPanel pnlCentro = new JPanel(new BorderLayout(5, 5));
            pnlCentro.add(new JScrollPane(tblD), BorderLayout.CENTER);

            if (!recetas.isEmpty()) {
                StringBuilder sb = new StringBuilder("<html><b>Receta(s) Médica(s) Asociada(s):</b><br>");
                for (RecetaMedica r : recetas) {
                    sb.append("• Dr. ").append(r.getNombreMedico())
                      .append(" (Céd: ").append(r.getCedulaProfesional()).append(") - Folio: ")
                      .append(r.getFolioReceta()).append("<br>");
                }
                sb.append("</html>");
                JLabel lblR = new JLabel(sb.toString());
                lblR.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                pnlCentro.add(lblR, BorderLayout.SOUTH);
            }

            dlg.add(pnlCentro, BorderLayout.CENTER);
        } catch (SQLException ex) {
DialogoFarmac.mostrar(dlg, "Error", "Error al cargar detalles: " + ex.getMessage(), DialogoFarmac.Tipo.ERROR);
        }

        JButton btnCerrar = TemaFarmac.crearBotonSecundario("Cerrar");
        btnCerrar.addActionListener(e -> dlg.dispose());
        JPanel pnlSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlSur.add(btnCerrar);
        dlg.add(pnlSur, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void cancelarVentaSeleccionada() {
        Venta v = obtenerSeleccionada();
        if (v == null) return;

        if ("CANCELADA".equalsIgnoreCase(v.getEstado())) {
            DialogoFarmac.mostrarJO(this, "La venta #" + v.getId() + " ya se encuentra cancelada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String motivo = JOptionPane.showInputDialog(this,
                "Ingrese el motivo de cancelación para la Venta #" + v.getId() + " (Obligatorio - RF15):",
                "Cancelar Venta", JOptionPane.QUESTION_MESSAGE);

        if (motivo == null || motivo.trim().isEmpty()) {
            return;
        }

        try {
            boolean exito = ventaDAO.cancelarVentaTransaccional(v.getId(), motivo.trim(), Sesion.getUsuarioActivo());
            if (exito) {
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "VENTA_CANCELADA",
                        "Cancelación Venta #" + v.getId() + " - Motivo: " + motivo.trim());
                DialogoFarmac.mostrarJO(this,
                        "Venta #" + v.getId() + " cancelada exitosamente.\nExistencias reintegradas al inventario.",
                        "Cancelación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                cargarDatos();
            }
        } catch (SQLException e) {
            DialogoFarmac.mostrarJO(this,
                    "No se pudo cancelar la venta: " + e.getMessage(),
                    "Restricción de Cancelación", JOptionPane.ERROR_MESSAGE);
        }
    }
}
