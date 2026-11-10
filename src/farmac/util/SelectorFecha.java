package farmac.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Campo de fecha con calendario desplegable (dd/MM/yyyy). Permite escribir la fecha o elegirla
 * con un clic en el calendario.
 */
public class SelectorFecha extends JPanel {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ES = new Locale("es", "MX");

    private final JTextField txt;
    private LocalDate fecha;
    private Consumer<LocalDate> alCambiar;

    public SelectorFecha(LocalDate inicial) {
        super(new BorderLayout(4, 0));
        this.fecha = inicial != null ? inicial : LocalDate.now();
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE_CAMPO, 8, 1),
                new EmptyBorder(0, 8, 0, 4)));
        setPreferredSize(new Dimension(170, 40));

        txt = new JTextField(fecha.format(FORMATO), 9);
        txt.setBorder(null);
        txt.setOpaque(false);
        txt.setFont(TemaFarmac.FUENTE_REGULAR);
        txt.addActionListener(e -> aplicarTexto());
        txt.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                aplicarTexto();
            }
        });

        JButton btn = new JButton(FontIcon.of(FontAwesomeSolid.CALENDAR_ALT, 15, TemaFarmac.AZUL_PETROLEO));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(0, 6, 0, 6));
        btn.addActionListener(e -> mostrarCalendario(btn));

        add(txt, BorderLayout.CENTER);
        add(btn, BorderLayout.EAST);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate nueva) {
        if (nueva == null) return;
        fecha = nueva;
        txt.setText(fecha.format(FORMATO));
    }

    public void setAlCambiar(Consumer<LocalDate> alCambiar) {
        this.alCambiar = alCambiar;
    }

    public JTextField getCampoTexto() {
        return txt;
    }

    private void aplicarTexto() {
        try {
            LocalDate nueva = LocalDate.parse(txt.getText().trim(), FORMATO);
            cambiar(nueva);
        } catch (DateTimeParseException ex) {
            txt.setText(fecha.format(FORMATO));
        }
    }

    private void cambiar(LocalDate nueva) {
        boolean distinta = !nueva.equals(fecha);
        fecha = nueva;
        txt.setText(fecha.format(FORMATO));
        if (distinta && alCambiar != null) {
            alCambiar.accept(fecha);
        }
    }

    private void mostrarCalendario(java.awt.Component origen) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(new TemaFarmac.BordeRedondeado(TemaFarmac.BORDE, 10, 1));
        popup.setBackground(Color.WHITE);
        final YearMonth[] mes = {YearMonth.from(fecha)};
        JPanel contenido = new JPanel(new BorderLayout(0, 6));
        contenido.setBackground(Color.WHITE);
        contenido.setBorder(new EmptyBorder(8, 10, 10, 10));
        construir(popup, contenido, mes);
        popup.add(contenido);
        popup.show(origen, origen.getWidth() - 300, origen.getHeight() + 2);
    }

    private void construir(JPopupMenu popup, JPanel contenido, YearMonth[] mes) {
        contenido.removeAll();

        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        JButton ant = botonNavegacion(FontAwesomeSolid.CHEVRON_LEFT);
        JButton sig = botonNavegacion(FontAwesomeSolid.CHEVRON_RIGHT);
        String nombreMes = mes[0].getMonth().getDisplayName(TextStyle.FULL, ES);
        JLabel lbl = new JLabel(Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1)
                + " " + mes[0].getYear(), SwingConstants.CENTER);
        lbl.setFont(TemaFarmac.fuente(Font.BOLD, 14));
        lbl.setForeground(TemaFarmac.TEXTO_AZUL);
        ant.addActionListener(e -> {
            mes[0] = mes[0].minusMonths(1);
            construir(popup, contenido, mes);
        });
        sig.addActionListener(e -> {
            mes[0] = mes[0].plusMonths(1);
            construir(popup, contenido, mes);
        });
        cab.add(ant, BorderLayout.WEST);
        cab.add(lbl, BorderLayout.CENTER);
        cab.add(sig, BorderLayout.EAST);
        contenido.add(cab, BorderLayout.NORTH);

        JPanel rejilla = new JPanel(new GridLayout(0, 7, 2, 2));
        rejilla.setOpaque(false);
        String[] dias = {"L", "M", "M", "J", "V", "S", "D"};
        for (String d : dias) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setFont(TemaFarmac.fuente(Font.BOLD, 12));
            l.setForeground(TemaFarmac.TEXTO_MUTED);
            rejilla.add(l);
        }
        int desfase = mes[0].atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < desfase; i++) {
            rejilla.add(new JLabel());
        }
        LocalDate hoy = LocalDate.now();
        for (int d = 1; d <= mes[0].lengthOfMonth(); d++) {
            final LocalDate dia = mes[0].atDay(d);
            boolean elegido = dia.equals(fecha);
            boolean esHoy = dia.equals(hoy);
            JButton b = new JButton(String.valueOf(d)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (elegido) {
                        g2.setColor(TemaFarmac.AZUL_PETROLEO);
                        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    } else if (getModel().isRollover()) {
                        g2.setColor(new Color(0xDDE3EE));
                        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    } else if (esHoy) {
                        g2.setColor(TemaFarmac.AZUL_ACCION);
                        g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            b.setPreferredSize(new Dimension(36, 32));
            b.setFont(TemaFarmac.fuente(elegido ? Font.BOLD : Font.PLAIN, 13));
            b.setForeground(elegido ? Color.WHITE : TemaFarmac.TEXTO);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setMargin(new java.awt.Insets(0, 0, 0, 0));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> {
                popup.setVisible(false);
                cambiar(dia);
            });
            rejilla.add(b);
        }
        contenido.add(rejilla, BorderLayout.CENTER);

        JButton btnHoy = TemaFarmac.crearBotonSecundario("Hoy");
        btnHoy.addActionListener(e -> {
            popup.setVisible(false);
            cambiar(LocalDate.now());
        });
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(btnHoy, BorderLayout.CENTER);
        contenido.add(pie, BorderLayout.SOUTH);

        contenido.revalidate();
        contenido.repaint();
        popup.pack();
    }

    private JButton botonNavegacion(FontAwesomeSolid icono) {
        JButton b = new JButton(FontIcon.of(icono, 13, TemaFarmac.AZUL_PETROLEO));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4, 10, 4, 10));
        return b;
    }
}
