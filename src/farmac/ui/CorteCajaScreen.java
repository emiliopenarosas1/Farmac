package farmac.ui;

import farmac.dominio.TurnoCaja;
import farmac.dominio.Usuario;
import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Pantalla de Control de Caja y Arqueo Diario con distribucion adaptable y holgada.
 */
public class CorteCajaScreen extends JPanel {
    private final Usuario usuario;
    private TurnoCaja turnoActual;
    private JTextField txtMontoInicial;
    private JTextField txtEfectivoContado;

    public CorteCajaScreen(Usuario usuario) {
        this.usuario = usuario;
        this.turnoActual = new TurnoCaja(101, usuario, 500.0);
        inicializarUI();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(TemaFarmac.FONDO);
        setBorder(new EmptyBorder(16, 18, 16, 18));

        // Cabecera
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);

        JLabel lblTitulo = new JLabel("Control de Caja y Turnos");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Arqueo financiero, apertura de turno y corte diario");
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

        // Grid de KPIs Superiores con espaciado flexible
        JPanel pnlKpis = new JPanel(new GridLayout(1, 4, 10, 0));
        pnlKpis.setOpaque(false);

        pnlKpis.add(TemaFarmac.crearKpiCard("ESTADO", "ABIERTO", TemaFarmac.VERDE));
        pnlKpis.add(TemaFarmac.crearKpiCard("FONDO INICIAL", "$500.00", TemaFarmac.AZUL_PETROLEO));
        pnlKpis.add(TemaFarmac.crearKpiCard("VENTAS EFECTIVO", "$1,450.00", TemaFarmac.TURQUESA));
        pnlKpis.add(TemaFarmac.crearKpiCard("TOTAL ESPERADO", "$1,950.00", TemaFarmac.TEXTO_AZUL));

        JPanel pnlCentro = new JPanel(new BorderLayout(0, 14));
        pnlCentro.setOpaque(false);
        pnlCentro.add(pnlKpis, BorderLayout.NORTH);

        JPanel pnlCardForm = TemaFarmac.crearCard();
        pnlCardForm.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        JLabel lblSec = new JLabel("Operaciones de Caja");
        lblSec.setFont(TemaFarmac.FUENTE_SUBTITULO);
        lblSec.setForeground(TemaFarmac.AZUL_PETROLEO);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        pnlCardForm.add(lblSec, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        JLabel lblFondo = new JLabel("Monto Inicial para Nuevo Turno:");
        lblFondo.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCardForm.add(lblFondo, gbc);

        txtMontoInicial = TemaFarmac.crearCampoTexto("500.00", 12);
        gbc.gridx = 1;
        pnlCardForm.add(txtMontoInicial, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        JLabel lblContado = new JLabel("Efectivo Contado al Cierre:");
        lblContado.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCardForm.add(lblContado, gbc);

        txtEfectivoContado = TemaFarmac.crearCampoTexto("1950.00", 12);
        gbc.gridx = 1;
        pnlCardForm.add(txtEfectivoContado, gbc);

        JButton btnAbrir = TemaFarmac.crearBoton("Abrir Turno", TemaFarmac.AZUL_PETROLEO, Color.WHITE);
        btnAbrir.addActionListener(e -> JOptionPane.showMessageDialog(this, "Turno de caja abierto con $" + txtMontoInicial.getText()));

        JButton btnCerrar = TemaFarmac.crearBoton("Realizar Corte Diario", TemaFarmac.AMBAR, Color.WHITE);
        btnCerrar.addActionListener(e -> {
            double contado = Double.parseDouble(txtEfectivoContado.getText());
            double dif = turnoActual.cerrarTurno(contado, 1450.0);
            JOptionPane.showMessageDialog(this, "Corte registrado con exito.\nDiferencia en caja: $" + String.format("%.2f", dif), "Corte Diario", JOptionPane.INFORMATION_MESSAGE);
        });

        gbc.gridy = 3; gbc.gridx = 0;
        pnlCardForm.add(btnAbrir, gbc);
        gbc.gridx = 1;
        pnlCardForm.add(btnCerrar, gbc);

        JPanel pnlContenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        pnlContenedor.setOpaque(false);
        pnlContenedor.add(pnlCardForm);

        pnlCentro.add(pnlContenedor, BorderLayout.CENTER);
        add(pnlCentro, BorderLayout.CENTER);
    }
}
