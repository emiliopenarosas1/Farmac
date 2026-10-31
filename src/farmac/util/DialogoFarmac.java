package farmac.util;

import farmac.util.TemaFarmac;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Dialogos modernos (redondeados, con acento de marca y animacion de aparicion)
 * que reemplazan a los JOptionPane clasicos.
 */
public class DialogoFarmac {

    public enum Tipo { INFO, EXITO, ADVERTENCIA, ERROR, CONFIRMACION }

    public static void mostrar(Component padre, String titulo, Object mensaje, Tipo tipo) {
        JDialog dialog = crearDialogo(padre, titulo, mensaje, tipo, false);
        dialog.setVisible(true);
    }

    public static void mostrar(Component padre, String titulo, String mensaje, Tipo tipo) {
        mostrar(padre, titulo, (Object) mensaje, tipo);
    }

    // Variante con el orden de argumentos de JOptionPane (mensaje, titulo, tipo)
    public static void mostrarJO(Component padre, Object mensaje, String titulo, Tipo tipo) {
        mostrar(padre, titulo, mensaje, tipo);
    }

    public static void mostrarJO(Component padre, Object mensaje, String titulo, int tipoJOption) {
        Tipo tipo = Tipo.INFO;
        if (tipoJOption == JOptionPane.WARNING_MESSAGE) tipo = Tipo.ADVERTENCIA;
        else if (tipoJOption == JOptionPane.ERROR_MESSAGE) tipo = Tipo.ERROR;
        else if (tipoJOption == JOptionPane.INFORMATION_MESSAGE) tipo = Tipo.INFO;
        mostrar(padre, titulo, mensaje, tipo);
    }

    public static boolean confirmar(Component padre, String titulo, Object mensaje) {
        JDialog dialog = crearDialogo(padre, titulo, mensaje, Tipo.CONFIRMACION, true);
        dialog.setVisible(true);
        Object r = dialog.getRootPane().getClientProperty("farmac.confirmado");
        return Boolean.TRUE.equals(r);
    }

    public static boolean confirmar(Component padre, String titulo, String mensaje) {
        return confirmar(padre, titulo, (Object) mensaje);
    }

    // Variante con el orden de argumentos de JOptionPane (mensaje, titulo)
    public static boolean confirmarJO(Component padre, Object mensaje, String titulo) {
        return confirmar(padre, titulo, mensaje);
    }

    private static JDialog crearDialogo(Component padre, String titulo, Object mensaje, Tipo tipo, boolean conCancelar) {
        Window owner = null;
        if (padre instanceof Window) owner = (Window) padre;
        else if (padre != null) owner = SwingUtilities.getWindowAncestor(padre);

        JDialog dialog = new JDialog(owner, titulo, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        try { dialog.setBackground(new Color(0, 0, 0, 0)); } catch (Exception ignored) {}

        Color colorTipo = colorPorTipo(tipo);

        JPanel root = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 6; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 0, 6 * i));
                    g2.fill(new RoundRectangle2D.Float(i, i + 2, getWidth() - 2 * i, getHeight() - 2 * i, 16, 16));
                }
                g2.setColor(TemaFarmac.BLANCO);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.setColor(TemaFarmac.BORDE);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Cabecera
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(4, 6, 8, 6));

        JLabel lblIcono = new JLabel();
        lblIcono.setIcon(FontIcon.of(iconoSolido(tipo), 20, colorTipo));
        lblIcono.setPreferredSize(new Dimension(34, 34));
        lblIcono.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(TemaFarmac.FUENTE_TITULO);
        lblTitulo.setForeground(TemaFarmac.TEXTO);

        header.add(lblIcono, BorderLayout.WEST);
        header.add(lblTitulo, BorderLayout.CENTER);

        // Linea acento
        JPanel linea = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(colorTipo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        linea.setOpaque(false);
        linea.setPreferredSize(new Dimension(0, 3));

        JPanel lineaWrap = new JPanel(new BorderLayout());
        lineaWrap.setOpaque(false);
        lineaWrap.setBorder(new EmptyBorder(0, 6, 8, 6));
        lineaWrap.add(linea, BorderLayout.CENTER);

        // Cuerpo
        JPanel cuerpo = new JPanel(new BorderLayout(8, 8));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(new EmptyBorder(6, 6, 6, 6));

        if (mensaje instanceof Component) {
            cuerpo.add((Component) mensaje, BorderLayout.CENTER);
        } else {
            JTextArea txt = new JTextArea(String.valueOf(mensaje));
            txt.setFont(TemaFarmac.FUENTE_REGULAR);
            txt.setForeground(TemaFarmac.TEXTO);
            txt.setOpaque(false);
            txt.setEditable(false);
            txt.setWrapStyleWord(true);
            txt.setLineWrap(true);
            txt.setFocusable(false);
            txt.setBorder(null);
            cuerpo.add(txt, BorderLayout.CENTER);
        }

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBotones.setOpaque(false);

        if (conCancelar) {
            JButton btnCancelar = TemaFarmac.crearBoton("Cancelar", TemaFarmac.BORDE, TemaFarmac.TEXTO);
            btnCancelar.addActionListener(e -> {
                dialog.getRootPane().putClientProperty("farmac.confirmado", Boolean.FALSE);
                cerrarConFade(dialog);
            });
            pnlBotones.add(btnCancelar);
        }

        String textoOk = conCancelar ? "Aceptar" : "Entendido";
        if (tipo == Tipo.CONFIRMACION) textoOk = "Confirmar";
        JButton btnOk = TemaFarmac.crearBoton(textoOk, colorTipo, Color.WHITE);
        btnOk.addActionListener(e -> {
            dialog.getRootPane().putClientProperty("farmac.confirmado", Boolean.TRUE);
            cerrarConFade(dialog);
        });
        pnlBotones.add(btnOk);

        cuerpo.add(pnlBotones, BorderLayout.SOUTH);

        // Ensamblar vertical
        JPanel vertical = new JPanel();
        vertical.setOpaque(false);
        vertical.setLayout(new BoxLayout(vertical, BoxLayout.Y_AXIS));
        vertical.add(header);
        vertical.add(lineaWrap);
        vertical.add(cuerpo);

        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setOpaque(false);
        tarjeta.add(vertical, BorderLayout.CENTER);
        root.add(tarjeta, BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setSize(Math.max(dialog.getWidth(), 360), dialog.getHeight());
        dialog.setLocationRelativeTo(padre);

        // animacion de aparicion (fade-in): arrancar timer antes de mostrar
        try { dialog.setOpacity(0f); } catch (Exception ignored) {}
        final float[] alpha = {0f};
        Timer timer = new Timer(16, e -> {
            alpha[0] = Math.min(1f, alpha[0] + 0.12f);
            try { dialog.setOpacity(alpha[0]); } catch (Exception ignored) {}
            if (alpha[0] >= 1f) ((Timer) e.getSource()).stop();
        });
        timer.start();

        return dialog;
    }

    private static void cerrarConFade(JDialog dialog) {
        Timer timer = new Timer(16, null);
        final float[] alpha = {1f};
        ActionListener al = e -> {
            alpha[0] = Math.max(0f, alpha[0] - 0.18f);
            try { dialog.setOpacity(alpha[0]); } catch (Exception ignored) {}
            if (alpha[0] <= 0f) {
                timer.stop();
                dialog.dispose();
            }
        };
        timer.addActionListener(al);
        timer.start();
    }

    private static Color colorPorTipo(Tipo tipo) {
        switch (tipo) {
            case EXITO: return TemaFarmac.VERDE;
            case ADVERTENCIA: return TemaFarmac.AMBAR;
            case ERROR: return TemaFarmac.ROJO;
            case CONFIRMACION: return TemaFarmac.TURQUESA;
            default: return TemaFarmac.AZUL_PETROLEO;
        }
    }

    private static FontAwesomeSolid iconoSolido(Tipo tipo) {
        switch (tipo) {
            case EXITO: return FontAwesomeSolid.CHECK_CIRCLE;
            case ADVERTENCIA: return FontAwesomeSolid.EXCLAMATION_TRIANGLE;
            case ERROR: return FontAwesomeSolid.TIMES_CIRCLE;
            case CONFIRMACION: return FontAwesomeSolid.QUESTION_CIRCLE;
            default: return FontAwesomeSolid.INFO_CIRCLE;
        }
    }
}