package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.LoteDAO;
import farmac.dao.MovimientoDAO;
import farmac.dominio.Lote;
import farmac.dominio.MovimientoInventario;
import farmac.dominio.OperacionesInventario;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla de Control y Semáforo de Caducidades conectada a PostgreSQL.
 * Accesible exclusivamente para Encargados.
 */
public class ReporteCaducidadScreen extends JPanel {

    private final LoteDAO loteDAO;
    private final MovimientoDAO movimientoDAO;
    private final OperacionesInventario operacionesInventario;

    private JTable tblAlertas;
    private DefaultTableModel modeloTabla;
    private List<Lote> lotesActuales;

    private JLabel lblKpiVencidos;
    private JLabel lblKpiPorVencer;
    private JLabel lblKpiVigentes;
    private JComboBox<String> cmbUmbralDias;
    private JButton btnActualizar;

    public ReporteCaducidadScreen() {
        this.loteDAO = new LoteDAO();
        this.movimientoDAO = new MovimientoDAO();
        this.operacionesInventario = new OperacionesInventario(60);
        this.lotesActuales = new ArrayList<>();

        inicializarUI();
        // Cargar después de pintar la pantalla, sin bloquear la navegación
        javax.swing.SwingUtilities.invokeLater(this::cargarDatos);
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        // Cabecera reacomodable: título y acciones se acomodan en varias filas
        JPanel pnlHeader = new JPanel(new TemaFarmac.WrapLayout(FlowLayout.LEFT, 10, 4));
        pnlHeader.setOpaque(false);

        JPanel pnlTit = new JPanel();
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        pnlTit.setOpaque(false);

        JLabel lblTitulo = new JLabel("Control de Caducidades y Semáforo Preventivo");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Monitoreo de lotes vencidos, partidas en riesgo de caducidad y bajas por merma");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        pnlTit.add(lblTitulo);
        pnlTit.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlTit.add(lblSub);

        // Barra de acciones derecha
        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlAcciones.setOpaque(false);

        cmbUmbralDias = new JComboBox<>(new String[]{"30 días", "60 días (Estándar)", "90 días", "180 días"});
        cmbUmbralDias.setSelectedIndex(1);
        TemaFarmac.estilizarComboBox(cmbUmbralDias);
        cmbUmbralDias.addActionListener(e -> aplicarUmbral());

        btnActualizar = TemaFarmac.crearBotonSecundario("Actualizar");
        btnActualizar.addActionListener(e -> cargarDatos());

        JButton btnAjustarCaducado = TemaFarmac.crearBotonPeligro("Dar Baja Lote Vencido");
        btnAjustarCaducado.addActionListener(e -> darDeBajaLoteVencido());

        JButton btnExportar = TemaFarmac.crearBoton("Exportar CSV", TemaFarmac.VERDE, Color.WHITE);
        btnExportar.addActionListener(e -> exportarReporteCSV());

        pnlAcciones.add(new JLabel("Umbral de Alerta:"));
        pnlAcciones.add(cmbUmbralDias);
        pnlAcciones.add(btnActualizar);
        pnlAcciones.add(btnAjustarCaducado);
        pnlAcciones.add(btnExportar);

        pnlHeader.add(pnlTit);
        pnlHeader.add(pnlAcciones);
        add(pnlHeader, BorderLayout.NORTH);

        JPanel pnlCentro = new JPanel(new BorderLayout(0, 16));
        pnlCentro.setOpaque(false);

        // KPIs
        JPanel pnlKpis = new JPanel(new GridLayout(1, 3, 12, 0));
        pnlKpis.setOpaque(false);

        lblKpiVencidos = new JLabel("0 lotes", JLabel.CENTER);
        lblKpiPorVencer = new JLabel("0 lotes", JLabel.CENTER);
        lblKpiVigentes = new JLabel("0 lotes", JLabel.CENTER);

        pnlKpis.add(TemaFarmac.crearTarjetaKPI("LOTES VENCIDOS", lblKpiVencidos, TemaFarmac.ROJO));
        pnlKpis.add(TemaFarmac.crearTarjetaKPI("POR VENCER (ALERTA)", lblKpiPorVencer, TemaFarmac.AMBAR));
        pnlKpis.add(TemaFarmac.crearTarjetaKPI("LOTES VIGENTES", lblKpiVigentes, TemaFarmac.VERDE));
        pnlCentro.add(pnlKpis, BorderLayout.NORTH);

        // Tabla
        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new BorderLayout());

        String[] columnas = {"ID", "Lote", "Medicamento", "Proveedor", "Caducidad", "Stock", "Días Rest.", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblAlertas = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tblAlertas);
        tblAlertas.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblAlertas.getColumnModel().getColumn(1).setPreferredWidth(125);
        tblAlertas.getColumnModel().getColumn(2).setPreferredWidth(200);
        tblAlertas.getColumnModel().getColumn(3).setPreferredWidth(145);
        tblAlertas.getColumnModel().getColumn(4).setPreferredWidth(115);
        tblAlertas.getColumnModel().getColumn(5).setPreferredWidth(92);
        tblAlertas.getColumnModel().getColumn(6).setPreferredWidth(120);
        tblAlertas.getColumnModel().getColumn(7).setPreferredWidth(115);

        tblAlertas.getColumnModel().getColumn(7).setCellRenderer(new TemaFarmac.BadgeCellRenderer());

        JScrollPane scroll = new JScrollPane(tblAlertas);
        scroll.setBorder(BorderFactory.createLineBorder(TemaFarmac.GRIS_BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        pnlCard.add(scroll, BorderLayout.CENTER);

        pnlCentro.add(pnlCard, BorderLayout.CENTER);
        add(pnlCentro, BorderLayout.CENTER);
    }

    private int obtenerDiasUmbral() {
        int idx = cmbUmbralDias.getSelectedIndex();
        return switch (idx) {
            case 0 -> 30;
            case 2 -> 90;
            case 3 -> 180;
            default -> 60;
        };
    }

    private void aplicarUmbral() {
        int dias = obtenerDiasUmbral();
        operacionesInventario.setDiasAlertaCaducidad(dias);
        llenarTabla();
    }

    public void cargarDatos() {
        btnActualizar.setEnabled(false);
        SwingWorker<List<Lote>, Void> worker = new SwingWorker<List<Lote>, Void>() {
            @Override
            protected List<Lote> doInBackground() throws Exception {
                return loteDAO.listarTodos();
            }
            @Override
            protected void done() {
                try {
                    lotesActuales = get();
                    operacionesInventario.setInventario(lotesActuales);
                    llenarTabla();
                } catch (Exception e) {
                    DialogoFarmac.mostrar(ReporteCaducidadScreen.this, "Error", "Error al cargar lotes: " + e.getMessage(), DialogoFarmac.Tipo.ERROR);
                } finally {
                    btnActualizar.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void llenarTabla() {
        modeloTabla.setRowCount(0);
        LocalDate hoy = LocalDate.now();
        int diasUmbral = obtenerDiasUmbral();

        int contVencidos = 0;
        int pzasVencidas = 0;
        int contPorVencer = 0;
        int pzasPorVencer = 0;
        int contVigentes = 0;
        int pzasVigentes = 0;

        for (Lote l : lotesActuales) {
            long dias = l.diasParaVencer(hoy);
            String estado;
            if (l.estaVencido(hoy)) {
                estado = "VENCIDO";
                contVencidos++;
                pzasVencidas += l.getCantidadActual();
            } else if (dias <= diasUmbral) {
                estado = "POR VENCER";
                contPorVencer++;
                pzasPorVencer += l.getCantidadActual();
            } else {
                estado = "VIGENTE";
                contVigentes++;
                pzasVigentes += l.getCantidadActual();
            }

            modeloTabla.addRow(new Object[]{
                l.getId(),
                l.getNumeroLote(),
                l.getProducto() != null ? l.getProducto().getNombre() : "-",
                l.getProveedor() != null ? l.getProveedor().getRazonSocial() : "-",
                l.getFechaCaducidad(),
                l.getCantidadActual() + " pzas",
                (dias < 0 ? Math.abs(dias) + " días vencido" : dias + " días restantes"),
                estado
            });
        }

        lblKpiVencidos.setText(contVencidos + " lotes (" + pzasVencidas + " pzas)");
        lblKpiPorVencer.setText(contPorVencer + " lotes (" + pzasPorVencer + " pzas)");
        lblKpiVigentes.setText(contVigentes + " lotes (" + pzasVigentes + " pzas)");
    }

    private void darDeBajaLoteVencido() {
        int fila = tblAlertas.getSelectedRow();
        if (fila < 0) {
            DialogoFarmac.mostrarJO(this, "Seleccione un lote vencido de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int loteId = (int) modeloTabla.getValueAt(fila, 0);
        Lote l = null;
        for (Lote item : lotesActuales) {
            if (item.getId() == loteId) {
                l = item;
                break;
            }
        }

        if (l == null) return;

        if (!l.estaVencido(LocalDate.now())) {
            DialogoFarmac.mostrarJO(this, "El lote seleccionado todavía no está vencido. Solo se pueden dar de baja por este medio los lotes vencidos.", "Acción No Permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (l.getCantidadActual() <= 0) {
            DialogoFarmac.mostrarJO(this, "El lote ya no cuenta con existencias para dar de baja.", "Sin Existencias", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean confirm = DialogoFarmac.confirmarJO(this,
                "¿Desea enviar a merma/baja las " + l.getCantidadActual() + " piezas del lote vencido '"
                + l.getNumeroLote() + "'?\nMotivo: PRODUCTO CADUCADO",
                "Confirmar Ajuste por Caducidad");

        if (confirm) {
            try {
                int cantMerma = l.getCantidadActual();
                loteDAO.actualizarStock(l.getId(), 0, null);
                l.setCantidadActual(0);

                MovimientoInventario mov = new MovimientoInventario(
                        0, l, "AJUSTE", -cantMerma, "PRODUCTO CADUCADO",
                        "MERMA-CAD-" + l.getId(), Sesion.getUsuarioActivo(), LocalDateTime.now()
                );
                movimientoDAO.insertar(mov, null);

                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "INVENTARIO_CADUCADO",
                        "Baja por caducidad de lote " + l.getNumeroLote() + " (-" + cantMerma + " pzas)");

                DialogoFarmac.mostrarJO(this, "Lote vencido dado de baja exitosamente del inventario.", "Ajuste Completado", JOptionPane.INFORMATION_MESSAGE);
                cargarDatos();
            } catch (SQLException ex) {
                DialogoFarmac.mostrarJO(this, "Error al registrar baja: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarReporteCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("reporte_caducidades_" + LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                pw.println("ID,NumeroLote,Medicamento,Proveedor,FechaCaducidad,StockActual,DiasRestantes,Estado");
                for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                    pw.printf("%s,\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\",\"%s\"%n",
                            modeloTabla.getValueAt(i, 0),
                            modeloTabla.getValueAt(i, 1),
                            modeloTabla.getValueAt(i, 2),
                            modeloTabla.getValueAt(i, 3),
                            modeloTabla.getValueAt(i, 4),
                            modeloTabla.getValueAt(i, 5),
                            modeloTabla.getValueAt(i, 6),
                            modeloTabla.getValueAt(i, 7));
                }
                DialogoFarmac.mostrarJO(this, "Reporte exportado exitosamente a:\n" + archivo.getAbsolutePath(), "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                DialogoFarmac.mostrarJO(this, "Error al exportar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
