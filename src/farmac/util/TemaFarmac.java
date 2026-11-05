package farmac.util;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Sistema de diseño y componentes responsivos para la interfaz de escritorio de Farmac.
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

    // Alias para compatibilidad
    public static final Color GRIS_FONDO     = FONDO;
    public static final Color GRIS_BORDE     = BORDE;
    public static final Color GRIS_TEXTO     = TEXTO_MUTED;

    // Tipografías escalables. Se usa Segoe UI si el sistema la tiene; en caso
    // contrario se cae a la fuente lógica SansSerif para que el layout no se
    // rompa en equipos con otros sistemas operativos.
    private static final String FAMILIA_BASE = resolverFamiliaBase();
    public static final Font FUENTE_DISPLAY   = fuente(Font.BOLD, 22);
    public static final Font FUENTE_TITULO    = fuente(Font.BOLD, 17);
    public static final Font FUENTE_SUBTITULO = fuente(Font.BOLD, 13);
    public static final Font FUENTE_REGULAR   = fuente(Font.PLAIN, 12);
    public static final Font FUENTE_NEGRITA   = fuente(Font.BOLD, 12);
    public static final Font FUENTE_PEQUENA   = fuente(Font.PLAIN, 11);
    public static final Font FUENTE_BADGE     = fuente(Font.BOLD, 10);

    /** Altura estándar de controles para que botones y campos queden alineados. */
    public static final int ALTO_CONTROL = 34;

    public static Font fuente(int estilo, int tamano) {
        return new Font(FAMILIA_BASE, estilo, tamano);
    }

    private static String resolverFamiliaBase() {
        try {
            String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            for (String nombre : disponibles) {
                if ("Segoe UI".equalsIgnoreCase(nombre)) {
                    return nombre;
                }
            }
        } catch (Exception ignored) {
            // Sin acceso a las fuentes del sistema: se usa la lógica.
        }
        return Font.SANS_SERIF;
    }

    /**
     * Crea un JScrollPane vertical reutilizable. El contenido se adapta al
     * ancho del viewport (nunca aparece scroll horizontal) y solo se desplaza
     * en vertical cuando no cabe en alto.
     */
    public static JScrollPane crearScrollResponsivo(Component contenido) {
        PanelScrollable wrapper = new PanelScrollable(new BorderLayout());
        wrapper.add(contenido, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBlockIncrement(96);
        return scroll;
    }

    /**
     * Envuelve una tarjeta de formulario para que se estire a lo ancho, se
     * mantenga arriba y tenga scroll vertical si la pantalla es baja.
     */
    public static JScrollPane crearScrollFormulario(JComponent tarjeta) {
        JPanel centrador = new JPanel(new GridBagLayout());
        centrador.setOpaque(false);
        centrador.setBorder(new EmptyBorder(4, 0, 18, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centrador.add(tarjeta, gbc);

        return crearScrollResponsivo(centrador);
    }

    /**
     * Construye un formulario de dos columnas con pesos: las etiquetas
     * conservan su ancho y los campos absorben el espacio disponible.
     */
    public static JPanel crearFormulario(String[] etiquetas, JComponent[] campos) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < etiquetas.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            JLabel lbl = new JLabel(etiquetas[i]);
            lbl.setForeground(TEXTO_AZUL);
            form.add(lbl, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            form.add(campos[i], gbc);
        }
        return form;
    }

    /**
     * FlowLayout que calcula su altura preferida considerando el ancho real
     * disponible. Así, cuando los botones no caben en una fila, el contenedor
     * crece en alto en lugar de recortar la segunda fila.
     */
    public static class WrapLayout extends FlowLayout {

        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                Container container = target;
                while (container.getSize().width == 0 && container.getParent() != null) {
                    container = container.getParent();
                }
                int targetWidth = container.getSize().width;
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;
                int miembros = target.getComponentCount();

                for (int i = 0; i < miembros; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowWidth + d.width > maxWidth) {
                            addRow(dim, rowWidth, rowHeight);
                            rowWidth = 0;
                            rowHeight = 0;
                        }
                        if (rowWidth != 0) {
                            rowWidth += hgap;
                        }
                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                addRow(dim, rowWidth, rowHeight);

                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;

                Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
                if (scrollPane != null && target.isValid()) {
                    dim.width -= (hgap + 1);
                }
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) {
                dim.height += getVgap();
            }
            dim.height += rowHeight;
        }
    }

    /** Panel interno que hace que el contenido siga el ancho del viewport. */
    private static class PanelScrollable extends JPanel implements Scrollable {        PanelScrollable(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 96;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Crea un botón estilizado con color personalizado.
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
                float inset = getModel().isPressed() ? 1f : 0f;
                g2.fill(new RoundRectangle2D.Float(inset, inset, getWidth() - 2 * inset, getHeight() - 2 * inset, 6, 6));
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
        // Altura mínima común: todos los botones conservan el mismo alto aunque
        // cambie el texto, el icono o el tamaño de la ventana.
        btn.setMinimumSize(new Dimension(0, ALTO_CONTROL));
        return btn;
    }

    public static JButton crearBotonPrimario(String texto) {
        return crearBoton(texto, TURQUESA, BLANCO);
    }

    public static JButton crearBotonSecundario(String texto) {
        return crearBoton(texto, AZUL_PETROLEO, BLANCO);
    }

    public static JButton crearBotonPeligro(String texto) {
        return crearBoton(texto, ROJO, BLANCO);
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
        Dimension pref = txt.getPreferredSize();
        txt.setPreferredSize(new Dimension(pref.width, ALTO_CONTROL));
        txt.setMinimumSize(new Dimension(0, ALTO_CONTROL));
        return txt;
    }

    public static JTextField crearCampoTexto(int columnas) {
        return crearCampoTexto("", columnas);
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
        Dimension pref = txt.getPreferredSize();
        txt.setPreferredSize(new Dimension(pref.width, ALTO_CONTROL));
        txt.setMinimumSize(new Dimension(0, ALTO_CONTROL));
        return txt;
    }

    public static JPasswordField crearCampoPassword(int columnas) {
        return crearCampoPassword("", columnas);
    }

    public static void estilizarComboBox(JComboBox<?> cmb) {
        cmb.setFont(FUENTE_REGULAR);
        cmb.setForeground(TEXTO);
        cmb.setBackground(BLANCO);
        cmb.setBorder(BorderFactory.createCompoundBorder(
                new BordeRedondeado(BORDE, 6, 1),
                new EmptyBorder(3, 6, 3, 6)
        ));
        cmb.setMinimumSize(new Dimension(120, ALTO_CONTROL));
    }

    public static JPanel crearCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 4; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 0, 6 * i));
                    g2.fill(new RoundRectangle2D.Float(i, i + 1, getWidth() - 2 * i, getHeight() - 2 * i, 10, 10));
                }
                g2.setColor(BLANCO);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.setColor(BORDE);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
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

    public static JPanel crearTarjetaKPI(String titulo, JLabel lblValor, Color colorValor) {
        JPanel card = crearCard();
        card.setLayout(new BorderLayout(4, 4));
        card.setPreferredSize(new Dimension(170, 75));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(FUENTE_PEQUENA);
        lblTit.setForeground(TEXTO_AZUL);

        lblValor.setFont(FUENTE_TITULO);
        lblValor.setForeground(colorValor);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
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
        // Encabezados ligeramente más compactos para que quepan en pantallas
        // angostas sin recortar el texto.
        header.setFont(fuente(Font.BOLD, 11));
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
                lbl.setFont(fuente(Font.BOLD, 11));
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

    public static class BotonTablaRenderer extends JButton implements TableCellRenderer {
        public BotonTablaRenderer(String texto, Color fondo, Color textoColor) {
            super(texto);
            setFont(FUENTE_BADGE);
            setBackground(fondo);
            setForeground(textoColor);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
            setMargin(new Insets(0, 0, 0, 0));
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
            this.button.setMargin(new Insets(0, 0, 0, 0));
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
                    if (texto.contains("VIGENTE") || texto.contains("ACTIVO") || texto.contains("COMPLETADA") || texto.contains("ABIERTO") || texto.contains("VENTA LIBRE") || texto.contains("ÓPTIMO") || texto.contains("LIBRE")) {
                        bg = VERDE_BG; fg = VERDE;
                    } else if (texto.contains("POR VENCER") || texto.contains("TARJETA") || texto.contains("TRANSFERENCIA") || texto.contains("BAJO STOCK")) {
                        bg = AMBAR_BG; fg = AMBAR;
                    } else if (texto.contains("VENCIDO") || texto.contains("INACTIVO") || texto.contains("CANCELADA") || texto.contains("CERRADO") || texto.contains("REQUIERE RECETA") || texto.contains("RECETA")) {
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
