package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.TurnoCajaDAO;
import farmac.dominio.TurnoCaja;
import farmac.dominio.Usuario;
import farmac.util.Sesion;
import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.time.LocalDateTime;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla de Control de Caja, Apertura y Arqueo Diario con persistencia real.
 */
public class CorteCajaScreen extends JPanel {

    private final Usuario usuario;
    private final TurnoCajaDAO turnoCajaDAO;

    private TurnoCaja turnoActual;
    private JLabel lblEstado;
    private JLabel lblFondoInicial;
    private JLabel lblVentasEfectivo;
    private JLabel lblVentasTarjeta;
    private JLabel lblTotalEsperado;

    private JTextField txtMontoInicial;
    private JTextField txtEfectivoContado;
    private JButton btnAbrirTurno;
    private JButton btnCerrarTurno;

    private JPanel pnlModos;
    private CardLayout cardModos;
    private JButton btnModoAbrir;
    private JButton btnModoCerrar;
    private boolean modoAbrir = true;

    public CorteCajaScreen(Usuario usuario) {
        this.usuario = usuario != null ? usuario : Sesion.getUsuarioActivo();
        this.turnoCajaDAO = new TurnoCajaDAO();

        inicializarUI();
        refrescarDatosTurno();
    }

    private JLabel lblTurnoTag;
    private JPanel pnlAvisoTurno;
    private JLabel lblDetFondo;
    private JLabel lblDetVentas;
    private JLabel lblDetEsperado;
    private JLabel lblDetContado;
    private JPanel pnlDiferencia;
    private JLabel lblDiferenciaEt;
    private JLabel lblDiferenciaMonto;
    private double fondoActual;
    private double ventasEfectivoActual;

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 14));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(20, 26, 20, 26));

        // Cabecera
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        JLabel lblTitulo = new JLabel("Control de caja y turnos financieros");
        lblTitulo.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 20));
        lblTitulo.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlHeader.add(lblTitulo, BorderLayout.WEST);

        JButton btnActualizar = TemaFarmac.crearBotonSecundario("Actualizar valores");
        btnActualizar.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 12, TemaFarmac.TEXTO));
        btnActualizar.setIconTextGap(8);
        btnActualizar.addActionListener(e -> refrescarDatosTurno());
        pnlHeader.add(btnActualizar, BorderLayout.EAST);

        // Tarjetas de KPIs
        lblEstado = new JLabel("SIN TURNO");
        lblFondoInicial = new JLabel("$0.00");
        lblVentasEfectivo = new JLabel("$0.00");
        lblVentasTarjeta = new JLabel("$0.00");
        lblTotalEsperado = new JLabel("$0.00");

        JPanel pnlKpis = new JPanel(new GridLayout(1, 5, 14, 0));
        pnlKpis.setOpaque(false);
        pnlKpis.add(crearKpi("Estado del turno", lblEstado, TemaFarmac.AZUL_PETROLEO, false, 20));
        pnlKpis.add(crearKpi("Fondo inicial", lblFondoInicial, TemaFarmac.TEXTO_AZUL, false, 22));
        pnlKpis.add(crearKpi("Ventas en efectivo", lblVentasEfectivo, TemaFarmac.TEXTO_AZUL, false, 22));
        pnlKpis.add(crearKpi("Ventas tarjeta / transf.", lblVentasTarjeta, TemaFarmac.TEXTO_AZUL, false, 22));
        pnlKpis.add(crearKpi("Efectivo esperado", lblTotalEsperado, TemaFarmac.VERDE_CLARO, true, 22));

        JLabel lblOps = new JLabel("Operaciones de caja y arqueo");
        lblOps.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 14));
        lblOps.setForeground(TemaFarmac.TEXTO_AZUL);

        JPanel pnlNorte = new JPanel();
        pnlNorte.setOpaque(false);
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlHeader.setAlignmentX(LEFT_ALIGNMENT);
        pnlKpis.setAlignmentX(LEFT_ALIGNMENT);
        lblOps.setAlignmentX(LEFT_ALIGNMENT);
        pnlNorte.add(pnlHeader);
        pnlNorte.add(Box.createRigidArea(new Dimension(0, 14)));
        pnlNorte.add(pnlKpis);
        pnlNorte.add(Box.createRigidArea(new Dimension(0, 14)));
        pnlNorte.add(lblOps);
        add(pnlNorte, BorderLayout.NORTH);

        // Dos tarjetas lado a lado: abrir turno / realizar corte
        JPanel pnlOps = new JPanel(new GridLayout(1, 2, 18, 0));
        pnlOps.setOpaque(false);
        pnlOps.add(crearPanelApertura());
        pnlOps.add(crearPanelCierre());
        add(pnlOps, BorderLayout.CENTER);

        // Navegación con Enter
        TemaFarmac.enterComoTab(this::abrirTurno, txtMontoInicial);
        TemaFarmac.enterComoTab(this::cerrarTurno, txtEfectivoContado);
        txtEfectivoContado.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarArqueo(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarArqueo(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarArqueo(); }
        });
    }

    private JPanel crearKpi(String titulo, JLabel valor, Color colorValor, boolean oscuro, int tamValor) {
        JPanel card = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(oscuro ? TemaFarmac.AZUL_PETROLEO : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                if (!oscuro) {
                    g2.setColor(TemaFarmac.BORDE);
                    g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                }
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 16, 10, 16));
        card.setPreferredSize(new Dimension(150, 84));
        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(TemaFarmac.FUENTE_REGULAR);
        lblTit.setForeground(oscuro ? Color.WHITE : TemaFarmac.TEXTO_MUTED);
        valor.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, tamValor));
        valor.setForeground(colorValor);
        valor.setHorizontalAlignment(JLabel.LEFT);
        card.add(lblTit, BorderLayout.NORTH);
        card.add(valor, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearTarjetaOperacion() {
        JPanel card = TemaFarmac.crearCard();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        return card;
    }

    private JPanel crearPanelApertura() {
        JPanel card = crearTarjetaOperacion();

        JLabel lblTit = new JLabel("Abrir nuevo turno de caja");
        lblTit.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 16));
        lblTit.setForeground(TemaFarmac.TEXTO_AZUL);
        card.add(lblTit, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        // Aviso de turno en curso
        pnlAvisoTurno = new JPanel(new BorderLayout(10, 0));
        pnlAvisoTurno.setBackground(TemaFarmac.AMBAR_BG);
        pnlAvisoTurno.setBorder(new EmptyBorder(12, 14, 12, 14));
        pnlAvisoTurno.setAlignmentX(LEFT_ALIGNMENT);
        pnlAvisoTurno.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        JLabel lblAviso = new JLabel("<html>Ya existe un turno de caja abierto. Debe realizar el corte antes de abrir otro.</html>");
        lblAviso.setFont(TemaFarmac.FUENTE_REGULAR);
        lblAviso.setForeground(TemaFarmac.AMBAR);
        pnlAvisoTurno.add(new JLabel(org.kordamp.ikonli.swing.FontIcon.of(
                org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EXCLAMATION_TRIANGLE, 16, TemaFarmac.AMBAR)), BorderLayout.WEST);
        pnlAvisoTurno.add(lblAviso, BorderLayout.CENTER);
        pnlAvisoTurno.setVisible(false);

        JLabel lblFondo = new JLabel("Fondo inicial con el que se abre la caja ($)");
        lblFondo.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblFondo.setForeground(TemaFarmac.TEXTO_AZUL);
        lblFondo.setAlignmentX(LEFT_ALIGNMENT);

        txtMontoInicial = TemaFarmac.crearCampoTexto("500.00", 12);
        txtMontoInicial.setAlignmentX(LEFT_ALIGNMENT);
        txtMontoInicial.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        btnAbrirTurno = TemaFarmac.crearBoton("Abrir turno", TemaFarmac.TURQUESA, Color.WHITE);
        btnAbrirTurno.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 13));
        btnAbrirTurno.setAlignmentX(LEFT_ALIGNMENT);
        btnAbrirTurno.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnAbrirTurno.addActionListener(e -> abrirTurno());

        cuerpo.add(pnlAvisoTurno);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 14)));
        cuerpo.add(lblFondo);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 4)));
        cuerpo.add(txtMontoInicial);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 10)));
        cuerpo.add(btnAbrirTurno);
        card.add(cuerpo, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearPanelCierre() {
        JPanel card = crearTarjetaOperacion();

        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        JLabel lblTit = new JLabel("Realizar corte de caja (cerrar turno)");
        lblTit.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 16));
        lblTit.setForeground(TemaFarmac.TEXTO_AZUL);
        lblTurnoTag = new JLabel();
        lblTurnoTag.setFont(TemaFarmac.FUENTE_REGULAR);
        lblTurnoTag.setForeground(TemaFarmac.VERDE);
        cab.add(lblTit, BorderLayout.WEST);
        cab.add(lblTurnoTag, BorderLayout.EAST);
        card.add(cab, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        JLabel lblContado = new JLabel("Efectivo contado físicamente en caja ($)");
        lblContado.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblContado.setForeground(TemaFarmac.TEXTO_AZUL);
        lblContado.setAlignmentX(LEFT_ALIGNMENT);

        txtEfectivoContado = TemaFarmac.crearCampoTexto(12);
        txtEfectivoContado.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 20));
        txtEfectivoContado.putClientProperty("JTextField.placeholderText", "Capturar arqueo...");
        txtEfectivoContado.setAlignmentX(LEFT_ALIGNMENT);
        txtEfectivoContado.setPreferredSize(new Dimension(200, 52));
        txtEfectivoContado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        // Desglose del arqueo
        JPanel pnlDetalle = new JPanel(new GridLayout(4, 2, 0, 0));
        pnlDetalle.setBackground(Color.WHITE);
        pnlDetalle.setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(new Color(0xD5, 0xD8, 0xDC), 8, 1), new EmptyBorder(6, 14, 6, 14)));
        pnlDetalle.setAlignmentX(LEFT_ALIGNMENT);
        pnlDetalle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        lblDetFondo = new JLabel("$0.00", JLabel.RIGHT);
        lblDetVentas = new JLabel("$0.00", JLabel.RIGHT);
        lblDetEsperado = new JLabel("$0.00", JLabel.RIGHT);
        lblDetContado = new JLabel("$0.00", JLabel.RIGHT);
        agregarFilaDetalle(pnlDetalle, "Fondo inicial", lblDetFondo, false);
        agregarFilaDetalle(pnlDetalle, "+ Ventas en efectivo", lblDetVentas, false);
        agregarFilaDetalle(pnlDetalle, "Monto esperado", lblDetEsperado, true);
        agregarFilaDetalle(pnlDetalle, "Efectivo contado", lblDetContado, false);

        // Resultado del arqueo
        pnlDiferencia = new JPanel(new BorderLayout());
        pnlDiferencia.setBackground(TemaFarmac.VERDE_BG);
        pnlDiferencia.setBorder(new EmptyBorder(14, 16, 14, 16));
        pnlDiferencia.setAlignmentX(LEFT_ALIGNMENT);
        pnlDiferencia.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        lblDiferenciaEt = new JLabel("CAJA CUADRADA");
        lblDiferenciaEt.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblDiferenciaMonto = new JLabel("$0.00");
        lblDiferenciaMonto.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 24));
        pnlDiferencia.add(lblDiferenciaEt, BorderLayout.WEST);
        pnlDiferencia.add(lblDiferenciaMonto, BorderLayout.EAST);
        pnlDiferencia.setVisible(false);

        btnCerrarTurno = TemaFarmac.crearBoton("Realizar corte de caja", TemaFarmac.TURQUESA, Color.WHITE);
        btnCerrarTurno.setFont(TemaFarmac.fuente(java.awt.Font.BOLD, 13));
        btnCerrarTurno.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.WALLET, 13, Color.WHITE));
        btnCerrarTurno.setIconTextGap(8);
        btnCerrarTurno.setAlignmentX(LEFT_ALIGNMENT);
        btnCerrarTurno.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnCerrarTurno.addActionListener(e -> cerrarTurno());

        cuerpo.add(lblContado);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 4)));
        cuerpo.add(txtEfectivoContado);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 12)));
        cuerpo.add(pnlDetalle);
        cuerpo.add(Box.createRigidArea(new Dimension(0, 12)));
        cuerpo.add(pnlDiferencia);
        card.add(cuerpo, BorderLayout.CENTER);
        card.add(btnCerrarTurno, BorderLayout.SOUTH);
        return card;
    }

    private void agregarFilaDetalle(JPanel pnl, String etiqueta, JLabel valor, boolean negrita) {
        JLabel et = new JLabel(etiqueta);
        et.setFont(negrita ? TemaFarmac.FUENTE_NEGRITA : TemaFarmac.FUENTE_REGULAR);
        et.setForeground(TemaFarmac.TEXTO);
        valor.setFont(negrita ? TemaFarmac.FUENTE_NEGRITA : TemaFarmac.FUENTE_REGULAR);
        valor.setForeground(TemaFarmac.TEXTO);
        pnl.add(et);
        pnl.add(valor);
    }

    /** Actualiza el desglose y el resultado del arqueo mientras se captura el efectivo contado. */
    private void actualizarArqueo() {
        if (turnoActual == null || !turnoActual.estaAbierto()) {
            pnlDiferencia.setVisible(false);
            return;
        }
        double esperado = fondoActual + ventasEfectivoActual;
        lblDetFondo.setText(String.format("$%,.2f", fondoActual));
        lblDetVentas.setText(String.format("$%,.2f", ventasEfectivoActual));
        lblDetEsperado.setText(String.format("$%,.2f", esperado));
        String txt = txtEfectivoContado.getText().trim();
        if (txt.isEmpty()) {
            lblDetContado.setText("$0.00");
            pnlDiferencia.setVisible(false);
            return;
        }
        try {
            double contado = Double.parseDouble(txt);
            lblDetContado.setText(String.format("$%,.2f", contado));
            double dif = Math.round((contado - esperado) * 100.0) / 100.0;
            Color fg;
            Color bg;
            if (Math.abs(dif) < 0.01) {
                lblDiferenciaEt.setText("CAJA CUADRADA");
                fg = TemaFarmac.VERDE;
                bg = TemaFarmac.VERDE_BG;
            } else if (dif > 0) {
                lblDiferenciaEt.setText("SOBRANTE EN CAJA");
                fg = TemaFarmac.AMBAR;
                bg = TemaFarmac.AMBAR_BG;
            } else {
                lblDiferenciaEt.setText("FALTANTE EN CAJA");
                fg = TemaFarmac.ROJO;
                bg = TemaFarmac.ROJO_BG;
            }
            lblDiferenciaMonto.setText((dif < 0 ? "-" : "") + String.format("$%,.2f", Math.abs(dif)));
            lblDiferenciaEt.setForeground(fg);
            lblDiferenciaMonto.setForeground(fg);
            pnlDiferencia.setBackground(bg);
            pnlDiferencia.setVisible(true);
        } catch (NumberFormatException ex) {
            pnlDiferencia.setVisible(false);
        }
    }

    /** Habilita solo el panel que corresponde al estado actual del turno. */
    private void actualizarPaneles(boolean turnoAbierto) {
        pnlAvisoTurno.setVisible(turnoAbierto);
        txtMontoInicial.setEnabled(!turnoAbierto);
        btnAbrirTurno.setEnabled(!turnoAbierto);
        txtEfectivoContado.setEnabled(turnoAbierto);
        btnCerrarTurno.setEnabled(turnoAbierto);
        lblTurnoTag.setText(turnoAbierto && turnoActual != null ? "\u25CF Turno #" + turnoActual.getId() : "");
        if (turnoAbierto) {
            txtMontoInicial.setText(String.format("%.2f", fondoActual));
        }
        actualizarArqueo();
    }

    public void refrescarDatosTurno() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private TurnoCaja turno;
            private double ventasEf;
            private double ventasTarj;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    turno = turnoCajaDAO.obtenerTurnoAbierto();
                    Sesion.setTurnoActivo(turno);
                    if (turno != null && turno.estaAbierto()) {
                        ventasEf = turnoCajaDAO.obtenerTotalVentasEfectivoTurno(turno.getId());
                        ventasTarj = turnoCajaDAO.obtenerTotalVentasTurnoPorFormaPago(turno.getId(), "TARJETA")
                                   + turnoCajaDAO.obtenerTotalVentasTurnoPorFormaPago(turno.getId(), "TRANSFERENCIA");
                    }
                } catch (Exception e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    DialogoFarmac.mostrar(CorteCajaScreen.this, "Error", "Error al consultar turno: " + error.getMessage(), DialogoFarmac.Tipo.ERROR);
                    return;
                }
                turnoActual = turno;
                if (turnoActual != null && turnoActual.estaAbierto()) {
                    lblEstado.setText("ABIERTO (#" + turnoActual.getId() + ")");
                    lblEstado.setForeground(TemaFarmac.VERDE);
                    lblFondoInicial.setText(String.format("$%.2f", turnoActual.getMontoInicial()));

                    double esperado = turnoActual.getMontoInicial() + ventasEf;
                    fondoActual = turnoActual.getMontoInicial();
                    ventasEfectivoActual = ventasEf;

                    lblVentasEfectivo.setText(String.format("$%.2f", ventasEf));
                    lblVentasTarjeta.setText(String.format("$%.2f", ventasTarj));
                    lblTotalEsperado.setText(String.format("$%.2f", esperado));

                    actualizarPaneles(true);
                } else {
                    fondoActual = 0;
                    ventasEfectivoActual = 0;
                    lblEstado.setText("CERRADO");
                    lblEstado.setForeground(TemaFarmac.ROJO);
                    lblFondoInicial.setText("$0.00");
                    lblVentasEfectivo.setText("$0.00");
                    lblVentasTarjeta.setText("$0.00");
                    lblTotalEsperado.setText("$0.00");

                    txtEfectivoContado.setText("");
                    actualizarPaneles(false);
                }
            }
        };
        worker.execute();
    }

    private void abrirTurno() {
        String fondoStr = txtMontoInicial.getText().trim();
        double fondo;
        try {
            fondo = Double.parseDouble(fondoStr);
            if (fondo < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            DialogoFarmac.mostrarJO(this, "El fondo inicial debe ser un número válido mayor o igual a 0.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final double fondoInicial = fondo;
        new SwingWorker<TurnoCaja, Void>() {
            @Override
            protected TurnoCaja doInBackground() throws Exception {
                TurnoCaja nuevoTurno = new TurnoCaja(0, Sesion.getUsuarioActivo() != null ? Sesion.getUsuarioActivo() : usuario, fondoInicial);
                turnoCajaDAO.abrirTurno(nuevoTurno);
                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "CAJA_APERTURA",
                        "Apertura de Turno #" + nuevoTurno.getId() + " con fondo de $" + fondoInicial);
                return nuevoTurno;
            }

            @Override
            protected void done() {
                try {
                    TurnoCaja creado = get();
                    DialogoFarmac.mostrarJO(CorteCajaScreen.this,
                            "Turno de caja #" + creado.getId() + " abierto exitosamente con $" + String.format("%.2f", fondoInicial),
                            "Apertura Exitosa", JOptionPane.INFORMATION_MESSAGE);
                    refrescarDatosTurno();
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    DialogoFarmac.mostrarJO(CorteCajaScreen.this,
                            "No se pudo abrir el turno: " + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void cerrarTurno() {
        if (turnoActual == null || !turnoActual.estaAbierto()) {
            DialogoFarmac.mostrarJO(this, "No hay ningún turno abierto para cerrar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String contadoStr = txtEfectivoContado.getText().trim();
        if (contadoStr.isEmpty()) {
            DialogoFarmac.mostrarJO(this, "Debe ingresar el efectivo contado físico en caja.", "Arqueo Obligatorio", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double contado;
        try {
            contado = Double.parseDouble(contadoStr);
            if (contado < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            DialogoFarmac.mostrarJO(this, "El efectivo contado debe ser un número válido mayor o igual a 0.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final TurnoCaja turno = turnoActual;
        final double contadoFinal = contado;

        new SwingWorker<String, Void>() {
            private double ventasEf;

            @Override
            protected String doInBackground() throws Exception {
                ventasEf = turnoCajaDAO.obtenerTotalVentasEfectivoTurno(turno.getId());
                double dif = turno.cerrarTurno(contadoFinal, ventasEf);
                turnoCajaDAO.cerrarTurno(turno);

                String balanceStr;
                if (Math.abs(dif) < 0.01) {
                    balanceStr = "Caja cuadrada exactamente (Diferencia: $0.00).";
                } else if (dif > 0) {
                    balanceStr = "SOBRANTE en caja de: $" + String.format("%.2f", dif);
                } else {
                    balanceStr = "FALTANTE en caja de: -$" + String.format("%.2f", Math.abs(dif));
                }

                BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "CAJA_CORTE",
                        "Cierre de Turno #" + turno.getId() + " - Esperado: $" + turno.getMontoEsperado()
                        + ", Contado: $" + contadoFinal + " [" + balanceStr + "]");
                return balanceStr;
            }

            @Override
            protected void done() {
                try {
                    String balanceStr = get();
                    DialogoFarmac.mostrarJO(CorteCajaScreen.this,
                            "Corte de caja registrado exitosamente.\n\n"
                            + "Fondo Inicial: $" + String.format("%.2f", turno.getMontoInicial()) + "\n"
                            + "Ventas en Efectivo: $" + String.format("%.2f", ventasEf) + "\n"
                            + "Monto Esperado: $" + String.format("%.2f", turno.getMontoEsperado()) + "\n"
                            + "Efectivo Contado: $" + String.format("%.2f", contadoFinal) + "\n\n"
                            + "Resultado del Arqueo: " + balanceStr,
                            "Corte Diario Completado", JOptionPane.INFORMATION_MESSAGE);
                    txtEfectivoContado.setText("");
                    refrescarDatosTurno();
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    DialogoFarmac.mostrarJO(CorteCajaScreen.this,
                            "Error al cerrar turno: " + causa.getMessage(),
                            "Error BD", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
