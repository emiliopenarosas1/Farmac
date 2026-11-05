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

    private void inicializarUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(TemaFarmac.GRIS_FONDO);
        setBorder(new EmptyBorder(16, 18, 16, 18));

        // Cabecera
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);

        JLabel lblTitulo = new JLabel("Control de Caja y Turnos Financieros");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Apertura de turno, arqueo con ventas reales y corte financiero diario");
        lblSub.setFont(TemaFarmac.FUENTE_PEQUENA);
        lblSub.setForeground(TemaFarmac.TEXTO_MUTED);

        JPanel pnlTit = new JPanel();
        pnlTit.setLayout(new BoxLayout(pnlTit, BoxLayout.Y_AXIS));
        pnlTit.setOpaque(false);
        pnlTit.add(lblTitulo);
        pnlTit.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlTit.add(lblSub);

        pnlHeader.add(pnlTit, BorderLayout.WEST);

        JButton btnActualizar = TemaFarmac.crearBotonSecundario("Actualizar Valores");
        btnActualizar.addActionListener(e -> refrescarDatosTurno());
        pnlHeader.add(btnActualizar, BorderLayout.EAST);

        add(pnlHeader, BorderLayout.NORTH);

        // Tarjetas de KPIs Superiores
        JPanel pnlKpis = new JPanel(new GridLayout(1, 5, 10, 0));
        pnlKpis.setOpaque(false);

        lblEstado = new JLabel("SIN TURNO", JLabel.CENTER);
        lblFondoInicial = new JLabel("$0.00", JLabel.CENTER);
        lblVentasEfectivo = new JLabel("$0.00", JLabel.CENTER);
        lblVentasTarjeta = new JLabel("$0.00", JLabel.CENTER);
        lblTotalEsperado = new JLabel("$0.00", JLabel.CENTER);

        pnlKpis.add(TemaFarmac.crearTarjetaKPI("Estado Turno", lblEstado, TemaFarmac.AZUL_PETROLEO));
        pnlKpis.add(TemaFarmac.crearTarjetaKPI("Fondo Inicial", lblFondoInicial, TemaFarmac.TURQUESA));
        pnlKpis.add(TemaFarmac.crearTarjetaKPI("Ventas Efectivo", lblVentasEfectivo, TemaFarmac.VERDE));
        pnlKpis.add(TemaFarmac.crearTarjetaKPI("Ventas Tarjeta / Transf.", lblVentasTarjeta, TemaFarmac.TEXTO_AZUL));
        pnlKpis.add(TemaFarmac.crearTarjetaKPI("Efectivo Esperado", lblTotalEsperado, TemaFarmac.AZUL_OSCURO));

        JPanel pnlCentro = new JPanel(new BorderLayout(0, 14));
        pnlCentro.setOpaque(false);
        pnlCentro.add(pnlKpis, BorderLayout.NORTH);

        // Formulario de Operaciones: switch Abrir / Cerrar turno
        JPanel pnlCardForm = TemaFarmac.crearCard();
        pnlCardForm.setLayout(new BorderLayout(0, 10));

        JLabel lblSec = new JLabel("Operaciones de Caja y Arqueo");
        lblSec.setFont(TemaFarmac.FUENTE_SUBTITULO);
        lblSec.setForeground(TemaFarmac.AZUL_PETROLEO);

        JPanel pnlEncabezadoForm = new JPanel(new BorderLayout(0, 8));
        pnlEncabezadoForm.setOpaque(false);
        pnlEncabezadoForm.add(lblSec, BorderLayout.NORTH);

        JPanel pnlSwitchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlSwitchWrap.setOpaque(false);
        pnlSwitchWrap.add(crearSwitchModo());
        pnlEncabezadoForm.add(pnlSwitchWrap, BorderLayout.CENTER);
        pnlCardForm.add(pnlEncabezadoForm, BorderLayout.NORTH);

        cardModos = new CardLayout();
        pnlModos = new JPanel(cardModos);
        pnlModos.setOpaque(false);
        pnlModos.add(crearPanelApertura(), "ABRIR");
        pnlModos.add(crearPanelCierre(), "CERRAR");
        pnlCardForm.add(pnlModos, BorderLayout.CENTER);

        JPanel pnlContenedor = new JPanel(new BorderLayout());
        pnlContenedor.setOpaque(false);
        pnlContenedor.add(pnlCardForm, BorderLayout.NORTH);

        pnlCentro.add(TemaFarmac.crearScrollResponsivo(pnlContenedor), BorderLayout.CENTER);
        add(pnlCentro, BorderLayout.CENTER);

        mostrarModo(true);
    }

    private JPanel crearPanelApertura() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);

        JLabel lblFondo = new JLabel("Fondo inicial con el que se abre la caja ($):");
        lblFondo.setForeground(TemaFarmac.TEXTO_AZUL);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1;
        pnl.add(lblFondo, gbc);

        txtMontoInicial = TemaFarmac.crearCampoTexto("500.00", 12);
        gbc.gridy = 1;
        pnl.add(txtMontoInicial, gbc);

        btnAbrirTurno = TemaFarmac.crearBoton("Abrir Nuevo Turno de Caja", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
        btnAbrirTurno.addActionListener(e -> abrirTurno());
        gbc.gridy = 2;
        gbc.insets = new Insets(14, 4, 4, 4);
        pnl.add(btnAbrirTurno, gbc);

        gbc.gridy = 3;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel relleno = new JPanel();
        relleno.setOpaque(false);
        pnl.add(relleno, gbc);

        return pnl;
    }

    private JPanel crearPanelCierre() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);

        JLabel lblContado = new JLabel("Efectivo contado físicamente en caja ($):");
        lblContado.setForeground(TemaFarmac.TEXTO_AZUL);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1;
        pnl.add(lblContado, gbc);

        txtEfectivoContado = TemaFarmac.crearCampoTexto(12);
        txtEfectivoContado.putClientProperty("JTextField.placeholderText", "Capturar arqueo...");
        gbc.gridy = 1;
        pnl.add(txtEfectivoContado, gbc);

        btnCerrarTurno = TemaFarmac.crearBoton("Realizar Corte de Caja (Cerrar Turno)", TemaFarmac.ROJO, Color.WHITE);
        btnCerrarTurno.addActionListener(e -> cerrarTurno());
        gbc.gridy = 2;
        gbc.insets = new Insets(14, 4, 4, 4);
        pnl.add(btnCerrarTurno, gbc);

        gbc.gridy = 3;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel relleno = new JPanel();
        relleno.setOpaque(false);
        pnl.add(relleno, gbc);

        return pnl;
    }

    private JPanel crearSwitchModo() {
        JPanel contenedor = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        contenedor.setOpaque(false);
        contenedor.setLayout(new GridLayout(1, 2, 4, 0));
        contenedor.setBorder(new EmptyBorder(3, 3, 3, 3));
        contenedor.setPreferredSize(new Dimension(320, 34));

        btnModoAbrir = crearBotonSegmento("Abrir Turno");
        btnModoCerrar = crearBotonSegmento("Cerrar Turno");
        btnModoAbrir.addActionListener(e -> mostrarModo(true));
        btnModoCerrar.addActionListener(e -> mostrarModo(false));

        contenedor.add(btnModoAbrir);
        contenedor.add(btnModoCerrar);
        return contenedor;
    }

    private JButton crearBotonSegmento(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(TemaFarmac.FUENTE_NEGRITA);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setRolloverEnabled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void mostrarModo(boolean abrir) {
        if (abrir && turnoActual != null && turnoActual.estaAbierto()) {
            DialogoFarmac.mostrarJO(this,
                    "Ya existe un turno de caja abierto (#" + turnoActual.getId() + ").\n"
                    + "Debe realizar el corte antes de abrir otro.",
                    "Turno en Curso", JOptionPane.WARNING_MESSAGE);
            abrir = false;
        }
        modoAbrir = abrir;
        cardModos.show(pnlModos, abrir ? "ABRIR" : "CERRAR");
        actualizarEstiloSwitch();
    }

    private void actualizarEstiloSwitch() {
        if (modoAbrir) {
            btnModoAbrir.setOpaque(true);
            btnModoAbrir.setContentAreaFilled(true);
            btnModoAbrir.setBackground(TemaFarmac.AZUL_PETROLEO);
            btnModoAbrir.setForeground(Color.WHITE);

            btnModoCerrar.setOpaque(false);
            btnModoCerrar.setContentAreaFilled(false);
            btnModoCerrar.setForeground(TemaFarmac.TEXTO_MUTED);
        } else {
            btnModoCerrar.setOpaque(true);
            btnModoCerrar.setContentAreaFilled(true);
            btnModoCerrar.setBackground(TemaFarmac.TURQUESA);
            btnModoCerrar.setForeground(Color.WHITE);

            btnModoAbrir.setOpaque(false);
            btnModoAbrir.setContentAreaFilled(false);
            btnModoAbrir.setForeground(TemaFarmac.TEXTO_MUTED);
        }
        btnModoAbrir.repaint();
        btnModoCerrar.repaint();
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

                    lblVentasEfectivo.setText(String.format("$%.2f", ventasEf));
                    lblVentasTarjeta.setText(String.format("$%.2f", ventasTarj));
                    lblTotalEsperado.setText(String.format("$%.2f", esperado));

                    mostrarModo(false);
                } else {
                    lblEstado.setText("CERRADO");
                    lblEstado.setForeground(TemaFarmac.ROJO);
                    lblFondoInicial.setText("$0.00");
                    lblVentasEfectivo.setText("$0.00");
                    lblVentasTarjeta.setText("$0.00");
                    lblTotalEsperado.setText("$0.00");

                    txtEfectivoContado.setText("");
                    mostrarModo(true);
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
