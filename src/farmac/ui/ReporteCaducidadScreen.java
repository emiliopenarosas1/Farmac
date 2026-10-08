package farmac.ui;

import farmac.dominio.Lote;
import farmac.dominio.Producto;
import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de Control y Semaforo de Caducidades con exportacion funcional a CSV.
 */
public class ReporteCaducidadScreen extends JPanel {
    private JTable tblAlertas;
    private DefaultTableModel modeloTabla;
    private List<Lote> lotes;
    private JButton btnExportar;

    public ReporteCaducidadScreen() {
        this.lotes = new ArrayList<>();
        cargarLotesDemo();
        inicializarUI();
    }

    private void cargarLotesDemo() {
        Producto p1 = new Producto(1, "75010001", "Amoxicilina 500mg", "Antibioticos", "Farmaceutica Nal", 85.0, true, 5);
        Producto p2 = new Producto(2, "75010002", "Paracetamol 500mg", "Analgesicos", "Laboratorios Alfa", 25.0, false, 10);
        Producto p3 = new Producto(3, "75010003", "Ibuprofeno 400mg", "Antiinflamatorios", "Genericos MX", 45.0, false, 8);
        Producto p4 = new Producto(4, "75010004", "Omeprazol 20mg", "Gastroenterologia", "Laboratorios Beta", 60.0, false, 6);

        lotes.add(new Lote(1, "LT-2026-V1", LocalDate.now().minusDays(12), 15, 30.0, p1));
        lotes.add(new Lote(2, "LT-2026-PV", LocalDate.now().plusDays(20), 38, 18.0, p2));
        lotes.add(new Lote(3, "LT-2026-PV2", LocalDate.now().plusDays(45), 25, 35.0, p4));
        lotes.add(new Lote(4, "LT-2027-OK", LocalDate.now().plusMonths(9), 120, 22.0, p3));
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(TemaFarmac.FONDO);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel pnlHeader = new JPanel(new BorderLayout(12, 12));
        pnlHeader.setOpaque(false);

        JPanel pnlTit = new JPanel();
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        pnlTit.setOpaque(false);

        JLabel lblTitulo = new JLabel("Control de Caducidades y Semaforo Preventivo");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Monitoreo de lotes por vencer (umbral <=60 dias) y lotes caducados");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        pnlTit.add(lblTitulo);
        pnlTit.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlTit.add(lblSub);

        btnExportar = TemaFarmac.crearBoton("Exportar Reporte (CSV)", TemaFarmac.VERDE, Color.WHITE);
        btnExportar.addActionListener(e -> exportarReporteCSV());

        pnlHeader.add(pnlTit, BorderLayout.WEST);
        pnlHeader.add(btnExportar, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        JPanel pnlCentro = new JPanel(new BorderLayout(0, 16));
        pnlCentro.setOpaque(false);

        JPanel pnlKpis = new JPanel(new GridLayout(1, 3, 12, 0));
        pnlKpis.setOpaque(false);
        pnlKpis.add(TemaFarmac.crearKpiCard("LOTES VENCIDOS", "1 lote (15 pzas)", TemaFarmac.ROJO));
        pnlKpis.add(TemaFarmac.crearKpiCard("POR VENCER (<=60 DIAS)", "2 lotes (63 pzas)", TemaFarmac.AMBAR));
        pnlKpis.add(TemaFarmac.crearKpiCard("LOTES VIGENTES", "1 lote (120 pzas)", TemaFarmac.VERDE));
        pnlCentro.add(pnlKpis, BorderLayout.NORTH);

        JPanel pnlCard = TemaFarmac.crearCard();
        pnlCard.setLayout(new BorderLayout());

        String[] columnas = {"Numero de Lote", "Medicamento", "Fecha Caducidad", "Stock Actual", "Dias Restantes", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblAlertas = new JTable(modeloTabla);
        TemaFarmac.estilizarTabla(tblAlertas);
        tblAlertas.getColumnModel().getColumn(5).setCellRenderer(new TemaFarmac.BadgeCellRenderer());

        JScrollPane scroll = new JScrollPane(tblAlertas);
        scroll.setBorder(BorderFactory.createLineBorder(TemaFarmac.BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        pnlCard.add(scroll, BorderLayout.CENTER);

        LocalDate hoy = LocalDate.now();
        for (Lote l : lotes) {
            long dias = l.diasParaVencer(hoy);
            String estado;
            if (l.estaVencido(hoy)) {
                estado = "VENCIDO";
            } else if (dias <= 60) {
                estado = "POR VENCER";
            } else {
                estado = "VIGENTE";
            }

            modeloTabla.addRow(new Object[]{
                    l.getNumeroLote(),
                    l.getProducto() != null ? l.getProducto().getNombre() : "",
                    l.getFechaCaducidad(),
                    l.getCantidadActual() + " pzas",
                    (dias < 0 ? Math.abs(dias) + " dias vencido" : dias + " dias restantes"),
                    estado
            });
        }

        pnlCentro.add(pnlCard, BorderLayout.CENTER);
        add(pnlCentro, BorderLayout.CENTER);
    }

    private void exportarReporteCSV() {
        try {
            File dir = new File("reportes");
            if (!dir.exists()) dir.mkdirs();

            File archivo = new File(dir, "reporte_caducidades_" + LocalDate.now() + ".csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                pw.println("NUMERO_LOTE,MEDICAMENTO,CADUCIDAD,STOCK_ACTUAL,DIAS_RESTANTES,ESTADO");
                LocalDate hoy = LocalDate.now();
                for (Lote l : lotes) {
                    long dias = l.diasParaVencer(hoy);
                    String estado = l.estaVencido(hoy) ? "VENCIDO" : (dias <= 60 ? "POR_VENCER" : "VIGENTE");
                    pw.printf("%s,\"%s\",%s,%d,%d,%s%n",
                            l.getNumeroLote(),
                            l.getProducto() != null ? l.getProducto().getNombre() : "",
                            l.getFechaCaducidad(),
                            l.getCantidadActual(),
                            dias,
                            estado);
                }
            }
            JOptionPane.showMessageDialog(this, "Reporte de caducidades exportado a:\n" + archivo.getAbsolutePath(), "Exportacion Exitosa", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al exportar reporte: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
