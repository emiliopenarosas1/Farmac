package farmac.ui;

import farmac.dao.BitacoraDAO;
import farmac.dao.TurnoCajaDAO;
import farmac.dominio.TurnoCaja;
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
import java.awt.GridLayout;
import java.awt.Insets;
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
 * Pantalla de Control de Caja, Apertura y Arqueo Diario con persistencia real (RF12, RF16).
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

        JLabel lblSub = new JLabel("Apertura de turno, arqueo con ventas reales y corte financiero diario (RF12, RF16)");
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

        // Formulario de Operaciones
        JPanel pnlCardForm = TemaFarmac.crearCard();
        pnlCardForm.setLayout(new GridBagLayout());
        pnlCardForm.setPreferredSize(new Dimension(680, 360));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        JLabel lblSec = new JLabel("Operaciones de Caja y Arqueo");
        lblSec.setFont(TemaFarmac.FUENTE_SUBTITULO);
        lblSec.setForeground(TemaFarmac.AZUL_PETROLEO);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        pnlCardForm.add(lblSec, gbc);

        // Apertura
        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        JLabel lblFondo = new JLabel("Fondo Inicial para Abrir Turno ($):");
        lblFondo.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCardForm.add(lblFondo, gbc);

        txtMontoInicial = TemaFarmac.crearCampoTexto(12);
        txtMontoInicial.setText("500.00");
        gbc.gridx = 1;
        pnlCardForm.add(txtMontoInicial, gbc);

        btnAbrirTurno = TemaFarmac.crearBoton("Abrir Nuevo Turno de Caja", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
        btnAbrirTurno.addActionListener(e -> abrirTurno());
        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 2;
        pnlCardForm.add(btnAbrirTurno, gbc);

        // Cierre y Arqueo
        gbc.gridwidth = 1;
        gbc.gridy = 3; gbc.gridx = 0;
        JLabel lblContado = new JLabel("Efectivo Contado Físico en Caja ($) *:");
        lblContado.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCardForm.add(lblContado, gbc);

        txtEfectivoContado = TemaFarmac.crearCampoTexto(12);
        txtEfectivoContado.putClientProperty("JTextField.placeholderText", "Capturar arqueo...");
        gbc.gridx = 1;
        pnlCardForm.add(txtEfectivoContado, gbc);

        btnCerrarTurno = TemaFarmac.crearBoton("Realizar Corte de Caja (Cerrar Turno)", TemaFarmac.ROJO, Color.WHITE);
        btnCerrarTurno.addActionListener(e -> cerrarTurno());
        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 12, 8, 12);
        pnlCardForm.add(btnCerrarTurno, gbc);

        JPanel pnlContenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        pnlContenedor.setOpaque(false);
        pnlContenedor.add(pnlCardForm);

        pnlCentro.add(pnlContenedor, BorderLayout.CENTER);
        add(pnlCentro, BorderLayout.CENTER);
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

                    btnAbrirTurno.setEnabled(false);
                    txtMontoInicial.setEnabled(false);
                    btnCerrarTurno.setEnabled(true);
                    txtEfectivoContado.setEnabled(true);
                } else {
                    lblEstado.setText("CERRADO");
                    lblEstado.setForeground(TemaFarmac.ROJO);
                    lblFondoInicial.setText("$0.00");
                    lblVentasEfectivo.setText("$0.00");
                    lblVentasTarjeta.setText("$0.00");
                    lblTotalEsperado.setText("$0.00");

                    btnAbrirTurno.setEnabled(true);
                    txtMontoInicial.setEnabled(true);
                    btnCerrarTurno.setEnabled(false);
                    txtEfectivoContado.setEnabled(false);
                    txtEfectivoContado.setText("");
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

        try {
            TurnoCaja nuevoTurno = new TurnoCaja(0, Sesion.getUsuarioActivo() != null ? Sesion.getUsuarioActivo() : usuario, fondo);
            turnoCajaDAO.abrirTurno(nuevoTurno);
            BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "CAJA_APERTURA", "Apertura de Turno #" + nuevoTurno.getId() + " con fondo de $" + fondo);

            DialogoFarmac.mostrarJO(this,
                    "Turno de caja #" + nuevoTurno.getId() + " abierto exitosamente con $" + String.format("%.2f", fondo),
                    "Apertura Exitosa", JOptionPane.INFORMATION_MESSAGE);

            refrescarDatosTurno();
        } catch (SQLException ex) {
            DialogoFarmac.mostrarJO(this, "No se pudo abrir el turno: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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

        try {
            double ventasEf = turnoCajaDAO.obtenerTotalVentasEfectivoTurno(turnoActual.getId());
            double dif = turnoActual.cerrarTurno(contado, ventasEf);

            turnoCajaDAO.cerrarTurno(turnoActual);

            String balanceStr;
            if (Math.abs(dif) < 0.01) {
                balanceStr = "Caja cuadrada exactamente (Diferencia: $0.00).";
            } else if (dif > 0) {
                balanceStr = "SOBRANTE en caja de: $" + String.format("%.2f", dif);
            } else {
                balanceStr = "FALTANTE en caja de: -$" + String.format("%.2f", Math.abs(dif));
            }

            BitacoraDAO.registrar(Sesion.getUsuarioActivo(), "CAJA_CORTE",
                    "Cierre de Turno #" + turnoActual.getId() + " - Esperado: $" + turnoActual.getMontoEsperado()
                    + ", Contado: $" + contado + " [" + balanceStr + "]");

            DialogoFarmac.mostrarJO(this,
                    "Corte de caja registrado exitosamente.\n\n"
                    + "Fondo Inicial: $" + String.format("%.2f", turnoActual.getMontoInicial()) + "\n"
                    + "Ventas en Efectivo: $" + String.format("%.2f", ventasEf) + "\n"
                    + "Monto Esperado: $" + String.format("%.2f", turnoActual.getMontoEsperado()) + "\n"
                    + "Efectivo Contado: $" + String.format("%.2f", contado) + "\n\n"
                    + "Resultado del Arqueo: " + balanceStr,
                    "Corte Diario Completado (RF16)", JOptionPane.INFORMATION_MESSAGE);

            txtEfectivoContado.setText("");
            refrescarDatosTurno();

        } catch (SQLException ex) {
            DialogoFarmac.mostrarJO(this, "Error al cerrar turno: " + ex.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }
}
