package farmac.util;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * Sistema de diseno y componentes responsivos para la interfaz de escritorio de Farmac.
 */
public class TemaFarmac {

    // Paleta de colores oficial
    public static final Color AZUL_PETROLEO = new Color(0x0F, 0x47, 0x61);
    public static final Color AZUL_CABECERA = new Color(0x13, 0x58, 0x78);
    public static final Color AZUL_OSCURO   = new Color(0x0A, 0x30, 0x42);
    public static final Color TURQUESA       = new Color(0x18, 0x7C, 0x93);
    public static final Color VERDE          = new Color(0x23, 0x80, 0x5A);
    public static final Color VERDE_BG       = new Color(0xDC, 0xF2, 0xE7);
    public static final Color AMBAR          = new Color(0xD9, 0x82, 0x18);
    public static final Color AMBAR_BG       = new Color(0xFE, 0xF2, 0xD6);
    public static final Color ROJO           = new Color(0xC9, 0x3B, 0x3B);
    public static final Color ROJO_BG        = new Color(0xFD, 0xEA, 0xE8);
    public static final Color TEXTO          = new Color(0x1F, 0x29, 0x33);
    public static final Color TEXTO_AZUL     = new Color(0x0F, 0x47, 0x61);
    public static final Color TEXTO_MUTED    = new Color(0x52, 0x60, 0x6D);
    public static final Color FONDO          = new Color(0xF0, 0xF4, 0xF8);
    public static final Color BLANCO         = Color.WHITE;
    public static final Color BORDE          = new Color(0xCB, 0xD5, 0xE1);
    public static final Color TABLA_HEADER_BG= new Color(0x0F, 0x47, 0x61);

    // Tipografias escalables
    public static final Font FUENTE_DISPLAY   = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FUENTE_TITULO    = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FUENTE_SUBTITULO = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FUENTE_REGULAR   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FUENTE_NEGRITA   = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FUENTE_PEQUENA   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FUENTE_BADGE     = new Font("Segoe UI", Font.BOLD, 10);

    /**
     * Crea un boton estilizado con padding flexible.
     */
    public static JButton crearBoton(String texto, Color colorFondo, Color colorTexto) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(colorFondo.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(colorFondo.brighter());
                } else {
                    g2.setColor(colorFondo);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FUENTE_NEGRITA);
        btn.setForeground(colorTexto);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        return btn;
    }

    public static JTextField crearCampoTexto(String valorInicial, int columnas) {
        JTextField txt = new JTextField(valorInicial, columnas);
        txt.setFont(FUENTE_REGULAR);
        txt.setForeground(TEXTO);
        txt.setBackground(BLANCO);
        txt.setCaretColor(AZUL_PETROLEO);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new BordeRedondeado(BORDE, 6, 1),
                new EmptyBorder(5, 8, 5, 8)
        ));
        return txt;
    }

    public static JPasswordField crearCampoPassword(String valorInicial, int columnas) {
        JPasswordField txt = new JPasswordField(valorInicial, columnas);
        txt.setFont(FUENTE_REGULAR);
        txt.setForeground(TEXTO);
        txt.setBackground(BLANCO);
        txt.setCaretColor(AZUL_PETROLEO);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new BordeRedondeado(BORDE, 6, 1),
                new EmptyBorder(5, 8, 5, 8)
        ));
        return txt;
    }

    public static JPanel crearCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLANCO);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(BORDE);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        return card;
    }

    public static JPanel crearKpiCard(String titulo, String valor, Color colorValor) {
        JPanel card = crearCard();
        card.setLayout(new BorderLayout(2, 2));
        card.setPreferredSize(new Dimension(160, 68));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(FUENTE_PEQUENA);
        lblTit.setForeground(TEXTO_AZUL);

        JLabel lblVal = new JLabel(valor);
        lblVal.setFont(FUENTE_TITULO);
        lblVal.setForeground(colorValor);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);
        return card;
    }

    public static void estilizarTabla(JTable tabla) {
        tabla.setFont(FUENTE_REGULAR);
        tabla.setForeground(TEXTO);
        tabla.setRowHeight(34);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xE2, 0xE8, 0xF0));
        tabla.setSelectionBackground(new Color(0xD8, 0xEE, 0xF8));
        tabla.setSelectionForeground(TEXTO_AZUL);

        JTableHeader header = tabla.getTableHeader();
        header.setFont(FUENTE_NEGRITA);
        header.setBackground(TABLA_HEADER_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 32));
        header.setReorderingAllowed(false);
        header.setOpaque(true);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(TABLA_HEADER_BG);
                lbl.setForeground(Color.WHITE);
                lbl.setFont(FUENTE_NEGRITA);
                lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setOpaque(true);
                return lbl;
            }
        };
        header.setDefaultRenderer(headerRenderer);

        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer();
        bodyRenderer.setBorder(new EmptyBorder(0, 8, 0, 8));
        bodyRenderer.setForeground(TEXTO);
        tabla.setDefaultRenderer(Object.class, bodyRenderer);
    }

    /**
     * Renderer y Editor para Boton en Celda de JTable (Boton "Agregar").
     */
    public static class BotonTablaRenderer extends JButton implements TableCellRenderer {
        public BotonTablaRenderer(String texto, Color fondo, Color textoColor) {
            super(texto);
            setFont(FUENTE_BADGE);
            setBackground(fondo);
            setForeground(textoColor);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    public static class BotonTablaEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private final JButton button;
        private final Consumer<Integer> accionFila;
        private int filaActual;

        public BotonTablaEditor(String texto, Color fondo, Color textoColor, Consumer<Integer> accionFila) {
            this.accionFila = accionFila;
            this.button = new JButton(texto);
            this.button.setFont(FUENTE_BADGE);
            this.button.setBackground(fondo);
            this.button.setForeground(textoColor);
            this.button.setFocusPainted(false);
            this.button.setBorderPainted(false);
            this.button.setOpaque(true);
            this.button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            this.button.addActionListener(this);
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.filaActual = row;
            return button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
            if (accionFila != null) {
                accionFila.accept(filaActual);
            }
        }
    }

    public static class BadgeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String texto = (value != null) ? value.toString() : "";
            JLabel badge = new JLabel(texto, SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = new Color(0xF1, 0xF5, 0xF9);
                    Color fg = TEXTO;
                    if (texto.contains("VIGENTE") || texto.contains("ACTIVO") || texto.contains("COMPLETADA") || texto.contains("ABIERTO") || texto.contains("VENTA LIBRE")) {
                        bg = VERDE_BG; fg = VERDE;
                    } else if (texto.contains("POR VENCER") || texto.contains("TARJETA")) {
                        bg = AMBAR_BG; fg = AMBAR;
                    } else if (texto.contains("VENCIDO") || texto.contains("INACTIVO") || texto.contains("CANCELADA") || texto.contains("CERRADO") || texto.contains("REQUIERE RECETA")) {
                        bg = ROJO_BG; fg = ROJO;
                    }
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(2, 3, getWidth() - 4, getHeight() - 6, 6, 6));
                    g2.dispose();
                    setForeground(fg);
                    super.paintComponent(g);
                }
            };
            badge.setFont(FUENTE_BADGE);
            badge.setOpaque(false);
            return badge;
        }
    }

    public static class BordeRedondeado extends AbstractBorder {
        private final Color color;
        private final int radio;
        private final int grosor;

        public BordeRedondeado(Color color, int radio, int grosor) {
            this.color = color;
            this.radio = radio;
            this.grosor = grosor;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(grosor));
            g2.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radio, radio));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radio / 2, radio / 2, radio / 2, radio / 2);
        }
    }
}
