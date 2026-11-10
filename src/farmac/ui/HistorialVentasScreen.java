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
import farmac.util.SelectorFecha;
import farmac.util.TemaFarmac;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla de Historial de Ventas, Cancelaciones y Rotación de Productos.
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
    private JTabbedPane tabbed;
    private List<TurnoCaja> listaTurnosActual;
    private JComboBox<String> cmbDiaDesde;
    private JComboBox<String> cmbMesDesde;
    private JComboBox<String> cmbAnioDesde;
    private JComboBox<String> cmbDiaHasta;
    private JComboBox<String> cmbMesHasta;
    private JComboBox<String> cmbAnioHasta;
    private JComboBox<String> cmbEstado;
    private JCheckBox chkSoloRecetas;
    private List<Venta> listaVentasActual;
    private boolean cargando;

    public HistorialVentasScreen() {
        this.ventaDAO = new VentaDAO();
        this.recetaDAO = new RecetaDAO();
        this.turnoCajaDAO = new TurnoCajaDAO();
        initComponents();
        // Cargar después de pintar la pantalla, sin bloquear la navegación
        javax.swing.SwingUtilities.invokeLater(this::cargarDatos);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));

        JLabel lblTitulo = new JLabel("Historial de ventas y rotación");
        lblTitulo.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 20));
        lblTitulo.setForeground(TemaFarmac.TEXTO_AZUL);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        LocalDate hoy = LocalDate.now();
        LocalDate haceUnMes = hoy.minusDays(30);

        cmbDiaDesde = crearComboDias();
        cmbMesDesde = crearComboMeses();
        cmbAnioDesde = crearComboAnios();
        cmbDiaHasta = crearComboDias();
        cmbMesHasta = crearComboMeses();
        cmbAnioHasta = crearComboAnios();

        seleccionarFecha(cmbDiaDesde, cmbMesDesde, cmbAnioDesde, haceUnMes);
        seleccionarFecha(cmbDiaHasta, cmbMesHasta, cmbAnioHasta, hoy);

        cmbEstado = new JComboBox<>(new String[]{"TODAS", "COMPLETADA", "CANCELADA"});
        TemaFarmac.estilizarComboBox(cmbEstado);

        JButton btnFiltrar = TemaFarmac.crearBotonPrimario("Filtrar");
        btnFiltrar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.FILTER, 12, java.awt.Color.WHITE));
        btnFiltrar.setIconTextGap(8);
        btnFiltrar.addActionListener(e -> cargarDatos());

        JButton btnDetalles = TemaFarmac.crearBotonSecundario("Ver detalles");
        btnDetalles.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EYE, 12, TemaFarmac.TEXTO));
        btnDetalles.setIconTextGap(8);
        btnDetalles.addActionListener(e -> verDetallesActivo());

        JButton btnCancelar = TemaFarmac.crearBotonPeligro("Cancelar venta");
        btnCancelar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.TIMES, 12, TemaFarmac.ROJO));
        btnCancelar.setIconTextGap(8);
        btnCancelar.addActionListener(e -> cancelarVentaSeleccionada());

        chkSoloRecetas = new JCheckBox("Solo ventas con receta");
        chkSoloRecetas.setOpaque(false);
        chkSoloRecetas.setFont(TemaFarmac.FUENTE_REGULAR);
        chkSoloRecetas.addActionListener(e -> cargarDatos());

        // Pestañas (selector segmentado propio; el contenido vive en un JTabbedPane sin barra)
        tabbed = new JTabbedPane();
        tabbed.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) { return 0; }
            @Override protected void paintTabArea(java.awt.Graphics g, int tabPlacement, int selectedIndex) { }
            @Override protected void paintContentBorder(java.awt.Graphics g, int tabPlacement, int selectedIndex) { }
            @Override protected java.awt.Insets getContentBorderInsets(int tabPlacement) { return new java.awt.Insets(0, 0, 0, 0); }
        });
        tabbed.setOpaque(false);
        tabbed.setBorder(null);

        pnlSelectorPestanas = new JPanel(new GridLayout(1, 0, 4, 0)) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new java.awt.Color(0xA9, 0xAC, 0xA9));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        pnlSelectorPestanas.setOpaque(false);
        pnlSelectorPestanas.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel pnlSelectorWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlSelectorWrap.setOpaque(false);
        pnlSelectorWrap.add(pnlSelectorPestanas);

        JPanel pnlFiltros = new JPanel(new BorderLayout(14, 0));
        pnlFiltros.setBackground(java.awt.Color.WHITE);
        pnlFiltros.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 10, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JPanel filaCampos = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        filaCampos.setOpaque(false);
        selDesde = new SelectorFecha(haceUnMes);
        selHasta = new SelectorFecha(hoy);
        filaCampos.add(crearGrupoFecha("Desde", selDesde));
        filaCampos.add(crearGrupoFecha("Hasta", selHasta));
        JPanel grpEstado = new JPanel();
        grpEstado.setOpaque(false);
        grpEstado.setLayout(new BoxLayout(grpEstado, BoxLayout.Y_AXIS));
        JLabel lblEst = new JLabel("Estado");
        lblEst.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblEst.setForeground(TemaFarmac.TEXTO_AZUL);
        lblEst.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbEstado.setAlignmentX(Component.LEFT_ALIGNMENT);
        grpEstado.add(lblEst);
        grpEstado.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 4)));
        grpEstado.add(cmbEstado);
        filaCampos.add(grpEstado);
        filaCampos.add(chkSoloRecetas);
        pnlFiltros.add(filaCampos, BorderLayout.CENTER);
        pnlFiltros.add(btnFiltrar, BorderLayout.EAST);

        JPanel pnlNorte = new JPanel();
        pnlNorte.setOpaque(false);
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlSelectorWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlNorte.add(lblTitulo);
        pnlNorte.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 12)));
        pnlNorte.add(pnlSelectorWrap);
        pnlNorte.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 12)));
        pnlNorte.add(pnlFiltros);
        add(pnlNorte, BorderLayout.NORTH);

        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlAcciones.setOpaque(false);
        pnlAcciones.add(btnDetalles);
        pnlAcciones.add(btnCancelar);
        add(pnlAcciones, BorderLayout.SOUTH);

        // Pestaña 1: Ventas
        String[] colsVentas = {"Ticket", "Fecha y Hora", "Cajero", "Turno", "Forma Pago", "Total", "Estado", "Motivo Canc."};
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

        tablaVentas.getColumnModel().getColumn(6).setCellRenderer(new TemaFarmac.BadgeCellRenderer());
        agregarPestana("Ventas registradas", crearScrollTabla(tablaVentas));

        // Pestaña 2: Rotación de Productos
        if (Sesion.esEncargado()) {
            String[] colsRot = {"Código Barras", "Producto", "Unidades Vendidas", "Importe Generado ($)"};
            modeloRotacion = new DefaultTableModel(colsRot, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
            tablaRotacion = new JTable(modeloRotacion);
            TemaFarmac.estilizarTabla(tablaRotacion);
            JScrollPane scrollRot = crearScrollTabla(tablaRotacion);
            agregarPestana("Ranking de rotación", scrollRot);

            // Pestaña 3: Ventas con Receta Médica asociada
            String[] colsRec = {"Venta", "Fecha", "Cajero", "Producto", "Folio Receta", "Médico", "Estado Venta", "Receta"};
            modeloVentasConReceta = new DefaultTableModel(colsRec, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
            tablaVentasConReceta = new JTable(modeloVentasConReceta);
            TemaFarmac.estilizarTabla(tablaVentasConReceta);
            tablaVentasConReceta.getColumnModel().getColumn(6).setCellRenderer(new TemaFarmac.BadgeCellRenderer());
            agregarPestana("Ventas con receta", crearScrollTabla(tablaVentasConReceta));

            // Pestaña 4: Historial de Turnos de Caja (apertura / cierre)
            String[] colsTurnos = {"ID", "Usuario", "Apertura", "Cierre", "Estado", "Fondo", "Esperado", "Contado", "Diferencia"};
            modeloTurnos = new DefaultTableModel(colsTurnos, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
            tablaTurnos = new JTable(modeloTurnos);
            TemaFarmac.estilizarTabla(tablaTurnos);
            tablaTurnos.getColumnModel().getColumn(4).setCellRenderer(new TemaFarmac.BadgeCellRenderer());
            agregarPestana("Historial de turnos", crearScrollTabla(tablaTurnos));
        }

        add(tabbed, BorderLayout.CENTER);
        seleccionarPestana(0);
    }

    private SelectorFecha selDesde;
    private SelectorFecha selHasta;
    private JPanel pnlSelectorPestanas;
    private final List<JButton> botonesPestana = new ArrayList<>();

    private JScrollPane crearScrollTabla(JTable tabla) {
        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 8, 1));
        sp.getViewport().setBackground(java.awt.Color.WHITE);
        return sp;
    }

    private void agregarPestana(String titulo, Component contenido) {
        final int indice = tabbed.getTabCount();
        tabbed.addTab(titulo, contenido);
        TemaFarmac.BotonSegmento b = new TemaFarmac.BotonSegmento(titulo);
        b.addActionListener(e -> seleccionarPestana(indice));
        botonesPestana.add(b);
        pnlSelectorPestanas.add(b);
    }

    private void seleccionarPestana(int indice) {
        tabbed.setSelectedIndex(indice);
        for (int i = 0; i < botonesPestana.size(); i++) {
            ((TemaFarmac.BotonSegmento) botonesPestana.get(i)).setActivo(i == indice);
        }
    }

    private JPanel crearGrupoFecha(String titulo, SelectorFecha selector) {
        JPanel g = new JPanel();
        g.setOpaque(false);
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(titulo);
        l.setFont(TemaFarmac.FUENTE_NEGRITA);
        l.setForeground(TemaFarmac.TEXTO_AZUL);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        selector.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.add(l);
        g.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 4)));
        g.add(selector);
        return g;
    }

    public void cargarDatos() {
        if (cargando) return;

        final LocalDate desde = selDesde.getFecha();
        final LocalDate hasta = selHasta.getFecha();
        if (desde.isAfter(hasta)) {
            DialogoFarmac.mostrarJO(this,
                    "La fecha 'Desde' (" + desde + ") no puede ser posterior a 'Hasta' (" + hasta + ").",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final String estado = (String) cmbEstado.getSelectedItem();
        final boolean soloRecetas = chkSoloRecetas.isSelected();
        final boolean esEncargado = Sesion.esEncargado();

        cargando = true;
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                List<Venta> ventas = ventaDAO.listarVentas(desde, hasta, estado);
                Map<Integer, List<RecetaMedica>> recetas = recetaDAO.listarTodasPorVenta();

                if (soloRecetas) {
                    List<Venta> filtradas = new ArrayList<>();
                    for (Venta v : ventas) {
                        List<RecetaMedica> rs = recetas.get(v.getId());
                        if (rs != null && !rs.isEmpty()) {
                            filtradas.add(v);
                        }
                    }
                    ventas = filtradas;
                }

                List<VentaDAO.RotacionProducto> rotacion = esEncargado
                        ? ventaDAO.obtenerRankingRotacion(desde, hasta) : null;
                List<TurnoCaja> turnos = esEncargado
                        ? turnoCajaDAO.listarHistorial(200) : null;
                listaTurnosActual = turnos;

                return new Object[]{ventas, recetas, rotacion, turnos};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                cargando = false;
                try {
                    Object[] datos = get();
                    listaVentasActual = (List<Venta>) datos[0];
                    Map<Integer, List<RecetaMedica>> recetas = (Map<Integer, List<RecetaMedica>>) datos[1];
                    List<VentaDAO.RotacionProducto> rotacion = (List<VentaDAO.RotacionProducto>) datos[2];
                    List<TurnoCaja> turnos = (List<TurnoCaja>) datos[3];

                    llenarTablaVentas(listaVentasActual);
                    if (modeloRotacion != null && rotacion != null) {
                        llenarTablaRotacion(rotacion);
                    }
                    if (modeloVentasConReceta != null) {
                        llenarTablaVentasConReceta(listaVentasActual, recetas);
                    }
                    if (modeloTurnos != null && turnos != null) {
                        llenarTablaTurnos(turnos);
                    }
                } catch (Exception e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    DialogoFarmac.mostrarJO(HistorialVentasScreen.this,
                            "Error al cargar historial: " + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void llenarTablaVentas(List<Venta> ventas) {
        modeloVentas.setRowCount(0);
        if (ventas == null) return;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Venta v : ventas) {
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
    }

    private void llenarTablaRotacion(List<VentaDAO.RotacionProducto> rotacion) {
        modeloRotacion.setRowCount(0);
        for (VentaDAO.RotacionProducto r : rotacion) {
            modeloRotacion.addRow(new Object[]{
                r.getCodigoBarras(),
                r.getNombreProducto(),
                r.getUnidadesVendidas() + " pzas",
                String.format("$%.2f", r.getTotalImporte())
            });
        }
    }

    /**
     * Llena la pestaña de ventas con receta médica asociada.
     * Cada fila corresponde a una receta, con el producto realmente recetado.
     */
    private void llenarTablaVentasConReceta(List<Venta> ventas, Map<Integer, List<RecetaMedica>> recetasPorVenta) {
        modeloVentasConReceta.setRowCount(0);
        if (ventas == null || recetasPorVenta == null) return;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Venta v : ventas) {
            List<RecetaMedica> recetas = recetasPorVenta.get(v.getId());
            if (recetas == null || recetas.isEmpty()) continue;

            for (RecetaMedica r : recetas) {
                modeloVentasConReceta.addRow(new Object[]{
                    "#" + v.getId(),
                    v.getFechaHora() != null ? v.getFechaHora().format(dtf) : "-",
                    v.getUsuario() != null ? v.getUsuario().getNombreCompleto() : "N/A",
                    r.getProducto() != null ? r.getProducto().getNombre() : "-",
                    r.getFolioReceta() != null ? r.getFolioReceta() : "-",
                    r.getNombreMedico() != null ? r.getNombreMedico() : "-",
                    v.getEstado(),
                    r.estaVigente() ? "VIGENTE" : "VENCIDA"
                });
            }
        }
    }

    /**
     * Llena el historial de turnos de caja con sus datos de apertura y cierre.
     */
    private void llenarTablaTurnos(List<TurnoCaja> turnos) {
        modeloTurnos.setRowCount(0);
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
    }

    // ------------------------------------------------------------------
    // Selectores de fecha (día / mes / año)
    // ------------------------------------------------------------------

    private JComboBox<String> crearComboDias() {
        String[] dias = new String[31];
        for (int i = 1; i <= 31; i++) {
            dias[i - 1] = String.format("%02d", i);
        }
        JComboBox<String> cmb = new JComboBox<>(dias);
        TemaFarmac.estilizarComboBox(cmb);
        cmb.setPreferredSize(new java.awt.Dimension(62, TemaFarmac.ALTO_CONTROL));
        return cmb;
    }

    private JComboBox<String> crearComboMeses() {
        String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun",
                          "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        JComboBox<String> cmb = new JComboBox<>(meses);
        TemaFarmac.estilizarComboBox(cmb);
        cmb.setPreferredSize(new java.awt.Dimension(72, TemaFarmac.ALTO_CONTROL));
        return cmb;
    }

    private JComboBox<String> crearComboAnios() {
        int actual = LocalDate.now().getYear();
        String[] anios = new String[6];
        for (int i = 0; i < 6; i++) {
            anios[i] = String.valueOf(actual - 5 + i);
        }
        JComboBox<String> cmb = new JComboBox<>(anios);
        TemaFarmac.estilizarComboBox(cmb);
        cmb.setPreferredSize(new java.awt.Dimension(82, TemaFarmac.ALTO_CONTROL));
        cmb.setSelectedItem(String.valueOf(actual));
        return cmb;
    }

    private void seleccionarFecha(JComboBox<String> dia, JComboBox<String> mes,
                                  JComboBox<String> anio, LocalDate fecha) {
        dia.setSelectedIndex(fecha.getDayOfMonth() - 1);
        mes.setSelectedIndex(fecha.getMonthValue() - 1);
        anio.setSelectedItem(String.valueOf(fecha.getYear()));
    }

    private LocalDate leerFecha(JComboBox<String> dia, JComboBox<String> mes, JComboBox<String> anio) {
        int d = dia.getSelectedIndex() + 1;
        int m = mes.getSelectedIndex() + 1;
        int y = Integer.parseInt((String) anio.getSelectedItem());
        // Ajusta el día si el mes elegido tiene menos días (ej. 31 de febrero)
        int ultimoDia = YearMonth.of(y, m).lengthOfMonth();
        return LocalDate.of(y, m, Math.min(d, ultimoDia));
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
        mostrarDetalleVenta(v);
    }

    private void verDetallesActivo() {
        if (tabbed == null) { verDetallesSeleccionada(); return; }
        int idx = tabbed.getSelectedIndex();
        if (idx == 2) {
            verDetalleVentaReceta();
        } else if (idx == 3) {
            verDetalleTurno();
        } else {
            verDetallesSeleccionada();
        }
    }

    private void verDetalleVentaReceta() {
        int fila = tablaVentasConReceta.getSelectedRow();
        if (fila < 0) {
            DialogoFarmac.mostrarJO(this, "Seleccione una venta con receta de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object val = tablaVentasConReceta.getValueAt(fila, 0);
        int ventaId;
        try {
            ventaId = Integer.parseInt(val.toString().replace("#", "").trim());
        } catch (NumberFormatException ex) {
            DialogoFarmac.mostrarJO(this, "No se pudo identificar la venta seleccionada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Venta v = null;
        if (listaVentasActual != null) {
            for (Venta x : listaVentasActual) {
                if (x.getId() == ventaId) { v = x; break; }
            }
        }
        if (v == null) {
            v = new Venta();
            v.setId(ventaId);
        }
        mostrarDetalleVenta(v);
    }

    private void verDetalleTurno() {
        int fila = tablaTurnos.getSelectedRow();
        if (fila < 0) {
            DialogoFarmac.mostrarJO(this, "Seleccione un turno de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object val = tablaTurnos.getValueAt(fila, 0);
        int turnoId;
        try {
            turnoId = Integer.parseInt(val.toString().trim());
        } catch (NumberFormatException ex) {
            DialogoFarmac.mostrarJO(this, "No se pudo identificar el turno seleccionado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        TurnoCaja t = null;
        if (listaTurnosActual != null) {
            for (TurnoCaja x : listaTurnosActual) {
                if (x.getId() == turnoId) { t = x; break; }
            }
        }
        if (t == null) {
            DialogoFarmac.mostrarJO(this, "No se encontró el turno seleccionado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog((java.awt.Frame) null, "Detalles de Turno #" + t.getId(), true);
        dlg.setLayout(new BorderLayout(10, 10));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        JPanel pnlInfo = new JPanel(new GridLayout(0, 2, 8, 4));
        pnlInfo.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        pnlInfo.add(new JLabel("Usuario:"));
        pnlInfo.add(new JLabel(t.getUsuario() != null ? t.getUsuario().getNombreCompleto() : "N/A"));
        pnlInfo.add(new JLabel("Estado:"));
        pnlInfo.add(new JLabel(t.getEstado()));
        pnlInfo.add(new JLabel("Apertura:"));
        pnlInfo.add(new JLabel(t.getFechaApertura() != null ? t.getFechaApertura().format(dtf) : "-"));
        pnlInfo.add(new JLabel("Cierre:"));
        pnlInfo.add(new JLabel(t.getFechaCierre() != null ? t.getFechaCierre().format(dtf) : "Abierto"));
        pnlInfo.add(new JLabel("Fondo Inicial:"));
        pnlInfo.add(new JLabel(String.format("$%.2f", t.getMontoInicial())));
        pnlInfo.add(new JLabel("Monto Esperado:"));
        pnlInfo.add(new JLabel(String.format("$%.2f", t.getMontoEsperado())));
        pnlInfo.add(new JLabel("Efectivo Contado:"));
        pnlInfo.add(new JLabel(String.format("$%.2f", t.getEfectivoContado())));
        pnlInfo.add(new JLabel("Diferencia:"));
        pnlInfo.add(new JLabel(String.format("$%.2f", t.getDiferencia())));
        dlg.add(pnlInfo, BorderLayout.NORTH);

        // Desglose por forma de pago
        JPanel pnlDesglose = new JPanel(new GridLayout(0, 2, 8, 4));
        pnlDesglose.setBorder(BorderFactory.createTitledBorder("Ventas del turno por forma de pago"));
        try {
            double efectivo = turnoCajaDAO.obtenerTotalVentasTurnoPorFormaPago(t.getId(), "EFECTIVO");
            double tarjeta = turnoCajaDAO.obtenerTotalVentasTurnoPorFormaPago(t.getId(), "TARJETA");
            double transferencia = turnoCajaDAO.obtenerTotalVentasTurnoPorFormaPago(t.getId(), "TRANSFERENCIA");
            pnlDesglose.add(new JLabel("Efectivo:"));
            pnlDesglose.add(new JLabel(String.format("$%.2f", efectivo)));
            pnlDesglose.add(new JLabel("Tarjeta:"));
            pnlDesglose.add(new JLabel(String.format("$%.2f", tarjeta)));
            pnlDesglose.add(new JLabel("Transferencia:"));
            pnlDesglose.add(new JLabel(String.format("$%.2f", transferencia)));
            pnlDesglose.add(new JLabel("Total ventas:"));
            pnlDesglose.add(new JLabel(String.format("$%.2f", efectivo + tarjeta + transferencia)));
        } catch (SQLException ex) {
            pnlDesglose.add(new JLabel("No se pudo cargar el desglose: " + ex.getMessage()));
        }
        dlg.add(pnlDesglose, BorderLayout.CENTER);

        JButton btnCerrar = TemaFarmac.crearBotonSecundario("Cerrar");
        btnCerrar.addActionListener(e -> dlg.dispose());
        JPanel pnlSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlSur.add(btnCerrar);
        dlg.add(pnlSur, BorderLayout.SOUTH);

        dlg.pack();
        java.awt.Dimension pantalla = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(
                Math.min(Math.max(dlg.getWidth(), 420), pantalla.width - 40),
                Math.min(Math.max(dlg.getHeight(), 360), pantalla.height - 80));
        dlg.setMinimumSize(new java.awt.Dimension(400, 320));
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void mostrarDetalleVenta(Venta v) {
        JDialog dlg = new JDialog((java.awt.Frame) null, "Detalles de Venta #" + v.getId(), true);
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

        dlg.pack();
        java.awt.Dimension pantalla = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(
                Math.min(Math.max(dlg.getWidth(), 560), pantalla.width - 40),
                Math.min(Math.max(dlg.getHeight(), 400), pantalla.height - 80));
        dlg.setMinimumSize(new java.awt.Dimension(480, 340));
        dlg.setLocationRelativeTo(this);
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
                "Ingrese el motivo de cancelación para la Venta #" + v.getId() + " (Obligatorio):",
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
