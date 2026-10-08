package farmac.ui;

import farmac.dominio.Encargado;
import farmac.dominio.Lote;
import farmac.dominio.Producto;
import farmac.dominio.Usuario;
import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

/**
 * Pantalla para ajuste de inventario exclusiva para Encargado.
 */
public class AjusteInventarioScreen extends JPanel {
    private final Usuario usuario;
    private JTextField txtLote;
    private JTextField txtCantidadAjuste;
    private JComboBox<String> cmbMotivo;

    public AjusteInventarioScreen(Usuario usuario) {
        this.usuario = usuario;
        inicializarUI();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(TemaFarmac.FONDO);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);

        JLabel lblTitulo = new JLabel("Ajuste Manual de Inventario");
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.AZUL_PETROLEO);

        JLabel lblSub = new JLabel("Modulo exclusivo para Encargados: Registro de mermas, caducidades y conteos");
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

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblL = new JLabel("Numero de Lote a Ajustar:");
        lblL.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblL, gbc);

        txtLote = TemaFarmac.crearCampoTexto("LT-2026-A", 15);
        gbc.gridx = 1;
        pnlCard.add(txtLote, gbc);

        gbc.gridy = 1; gbc.gridx = 0;
        JLabel lblQ = new JLabel("Piezas a Descontar:");
        lblQ.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblQ, gbc);

        txtCantidadAjuste = TemaFarmac.crearCampoTexto("3", 15);
        gbc.gridx = 1;
        pnlCard.add(txtCantidadAjuste, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        JLabel lblM = new JLabel("Motivo Justificado:");
        lblM.setForeground(TemaFarmac.TEXTO_AZUL);
        pnlCard.add(lblM, gbc);

        cmbMotivo = new JComboBox<>(new String[]{"MERMA / PRODUCTO DANADO", "PRODUCTO CADUCADO", "DISCREPANCIA EN CONTEO", "DEVOLUCION A PROVEEDOR"});
        cmbMotivo.setFont(TemaFarmac.FUENTE_REGULAR);
        cmbMotivo.setBackground(Color.WHITE);
        cmbMotivo.setPreferredSize(new Dimension(280, 36));
        gbc.gridx = 1;
        pnlCard.add(cmbMotivo, gbc);

        JButton btnAplicar = TemaFarmac.crearBoton("Autorizar y Aplicar Ajuste", TemaFarmac.ROJO, Color.WHITE);
        btnAplicar.setPreferredSize(new Dimension(280, 40));
        btnAplicar.addActionListener(e -> autorizar());

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 12, 8, 12);
        pnlCard.add(btnAplicar, gbc);

        JPanel pnlContenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
        pnlContenedor.setOpaque(false);
        pnlContenedor.add(pnlCard);

        add(pnlContenedor, BorderLayout.CENTER);
    }

    private void autorizar() {
        if (usuario instanceof Encargado enc) {
            Producto p = new Producto(1, "75010001", "Amoxicilina 500mg", "Antibioticos", "Nacional", 85.0, true, 5);
            Lote lote = new Lote(1, txtLote.getText(), LocalDate.now().plusMonths(6), 25, 40.0, p);
            boolean exito = enc.autorizarAjuste(lote, Integer.parseInt(txtCantidadAjuste.getText()), (String) cmbMotivo.getSelectedItem());
            if (exito) {
                JOptionPane.showMessageDialog(this, "Ajuste autorizado y descontado del inventario por " + enc.getNombreCompleto(), "Ajuste Exitoso", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Permiso denegado: solo el Encargado puede autorizar ajustes.", "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
        }
    }
}
