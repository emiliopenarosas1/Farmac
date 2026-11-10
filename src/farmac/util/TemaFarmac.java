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

    // Paleta de colores (rediseño UI)
    public static final Color AZUL_PETROLEO = new Color(0x0E, 0x2A, 0x5C); // azul marino principal (sidebar, KPI oscuro)
    public static final Color AZUL_CABECERA = new Color(0x1B, 0x3D, 0x7A);
    public static final Color AZUL_OSCURO   = new Color(0x0A, 0x20, 0x4A);
    public static final Color AZUL_SELECCION= new Color(0x2B, 0x4F, 0x8E); // opción activa del menú
    public static final Color AZUL_ACCION   = new Color(0x21, 0x73, 0xC8); // botón de login / enlaces
    public static final Color TURQUESA       = new Color(0x2F, 0x6F, 0x4F); // botón de acción principal (verde oscuro)
    public static final Color VERDE          = new Color(0x1E, 0x7A, 0x4C);
    public static final Color VERDE_BG       = new Color(0xDC, 0xF2, 0xE7);
    public static final Color VERDE_CLARO    = new Color(0x6F, 0xC4, 0x8E); // cifras sobre fondo azul
    public static final Color AMBAR          = new Color(0xC2, 0x5E, 0x0A);
    public static final Color AMBAR_BG       = new Color(0xF8, 0xEB, 0xC8);
    public static final Color ROJO           = new Color(0xB3, 0x26, 0x26);
    public static final Color ROJO_SUAVE     = new Color(0xE5, 0x5B, 0x4F); // botón "Quitar"
    public static final Color ROJO_BG        = new Color(0xFC, 0xE1, 0xE1);
    public static final Color TEXTO          = new Color(0x1B, 0x23, 0x30);
    public static final Color TEXTO_AZUL     = new Color(0x0E, 0x2A, 0x5C);
    public static final Color TEXTO_MUTED    = new Color(0x5B, 0x65, 0x72);
    public static final Color FONDO          = new Color(0xC5, 0xC8, 0xC5); // gris de fondo del área de trabajo
    public static final Color BLANCO         = Color.WHITE;
    public static final Color BORDE          = new Color(0x8C, 0x91, 0x96); // borde de tarjetas / botones neutros
    public static final Color BORDE_CAMPO    = new Color(0x1B, 0x3D, 0x7A); // borde de campos de texto
    public static final Color TABLA_HEADER_BG= new Color(0xA9, 0xAC, 0xA9);
    public static final Color TABLA_ZEBRA    = new Color(0xDD, 0xDF, 0xDD);

    // Alias para compatibilidad
    public static final Color GRIS_FONDO     = FONDO;
    public static final Color GRIS_BORDE     = BORDE;
    public static final Color GRIS_TEXTO     = TEXTO_MUTED;

    // Tipografías escalables. Se usa Segoe UI si el sistema la tiene; en caso
    // contrario se cae a la fuente lógica SansSerif para que el layout no se
    // rompa en equipos con otros sistemas operativos.
    private static final String FAMILIA_BASE = resolverFamiliaBase();
    public static final Font FUENTE_DISPLAY   = fuente(Font.BOLD, 24);
    public static final Font FUENTE_TITULO    = fuente(Font.BOLD, 19);
    public static final Font FUENTE_SUBTITULO = fuente(Font.BOLD, 15);
    public static final Font FUENTE_REGULAR   = fuente(Font.PLAIN, 14);
    public static final Font FUENTE_NEGRITA   = fuente(Font.BOLD, 14);
    public static final Font FUENTE_PEQUENA   = fuente(Font.PLAIN, 13);
    public static final Font FUENTE_BADGE     = fuente(Font.BOLD, 12);

    /** Altura estándar de controles para que botones y campos queden alineados. */
    public static final int ALTO_CONTROL = 38;

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
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int fila = 0;
        for (int i = 0; i < etiquetas.length; i++) {
            String texto = etiquetas[i];
            if (texto.endsWith(":")) {
                texto = texto.substring(0, texto.length() - 1);
            }
            JLabel lbl = new JLabel(texto);
            lbl.setFont(FUENTE_NEGRITA);
            lbl.setForeground(TEXTO_AZUL);
            gbc.gridy = fila++;
            gbc.insets = new Insets(i == 0 ? 0 : 10, 4, 3, 4);
            form.add(lbl, gbc);

            gbc.gridy = fila++;
            gbc.insets = new Insets(0, 4, 0, 4);
            form.add(campos[i], gbc);
        }
        return form;
    }

    /**
     * Formulario de dos columnas (etiqueta arriba, campo debajo). {@code spans[i]} vale 1 (media fila)
     * o 2 (fila completa).
     */
    public static JPanel crearFormularioGrid(String[] etiquetas, JComponent[] campos, int[] spans) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int fila = 0;
        int col = 0;
        for (int i = 0; i < etiquetas.length; i++) {
            int span = spans[i];
            if (span == 2 && col != 0) {
                col = 0;
                fila += 2;
            }
            String texto = etiquetas[i];
            JLabel lbl = new JLabel(texto);
            lbl.setFont(FUENTE_NEGRITA);
            lbl.setForeground(TEXTO_AZUL);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1;
            gbc.gridx = col;
            gbc.gridwidth = span;
            gbc.gridy = fila;
            gbc.insets = new Insets(fila == 0 ? 0 : 12, col == 0 ? 0 : 8, 4, col + span == 2 ? 0 : 8);
            form.add(lbl, gbc);
            gbc.gridy = fila + 1;
            gbc.insets = new Insets(0, col == 0 ? 0 : 8, 0, col + span == 2 ? 0 : 8);
            form.add(campos[i], gbc);
            col += span;
            if (col >= 2) {
                col = 0;
                fila += 2;
            }
        }
        return form;
    }

    /** Aviso con fondo de color e icono (advertencia ámbar por defecto). */
    public static JPanel crearAviso(String texto, Color fondo, Color colorTexto) {
        JPanel aviso = new JPanel(new BorderLayout(10, 0));
        aviso.setBackground(fondo);
        aviso.setBorder(new EmptyBorder(11, 14, 11, 14));
        JLabel lbl = new JLabel("<html>" + texto + "</html>");
        lbl.setFont(FUENTE_REGULAR);
        lbl.setForeground(colorTexto);
        aviso.add(new JLabel(org.kordamp.ikonli.swing.FontIcon.of(
                org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EXCLAMATION_TRIANGLE, 15, colorTexto)), BorderLayout.WEST);
        aviso.add(lbl, BorderLayout.CENTER);
        return aviso;
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
                g2.fill(new RoundRectangle2D.Float(inset, inset, getWidth() - 2 * inset, getHeight() - 2 * inset, 8, 8));
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
        return crearBotonContorno(texto, TEXTO, TEXTO);
    }

    public static JButton crearBotonPeligro(String texto) {
        return crearBotonContorno(texto, ROJO, new Color(0xE8, 0xA0, 0xA0));
    }

    /** Botón blanco con borde y texto de color (estilo secundario del rediseño). */
    public static JButton crearBotonContorno(String texto, Color colorTexto, Color colorBorde) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fondo = BLANCO;
                if (getModel().isPressed()) {
                    fondo = new Color(0xE3, 0xE6, 0xEA);
                } else if (getModel().isRollover()) {
                    fondo = new Color(0xF2, 0xF4, 0xF7);
                }
                g2.setColor(fondo);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.setColor(colorBorde);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FUENTE_REGULAR);
        btn.setForeground(colorTexto);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        btn.setMinimumSize(new Dimension(0, ALTO_CONTROL));
        return btn;
    }

    public static JTextField crearCampoTexto(String valorInicial, int columnas) {
        JTextField txt = new JTextField(valorInicial, columnas);
        txt.setFont(FUENTE_REGULAR);
        txt.setForeground(TEXTO);
        txt.setBackground(BLANCO);
        txt.setCaretColor(AZUL_PETROLEO);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new BordeRedondeado(BORDE_CAMPO, 8, 1),
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
                new BordeRedondeado(BORDE_CAMPO, 8, 1),
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
                new BordeRedondeado(BORDE_CAMPO, 8, 1),
                new EmptyBorder(3, 6, 3, 6)
        ));
        cmb.setMinimumSize(new Dimension(120, ALTO_CONTROL));
    }


    /**
     * Navegación con la tecla Enter: al presionar Enter en un campo, el foco pasa
     * al siguiente de la lista. Con {@code alFinal} se ejecuta una acción al
     * presionar Enter en el último campo (por ejemplo, guardar).
     */
    public static void enterComoTab(Runnable alFinal, JComponent... campos) {
        for (int i = 0; i < campos.length; i++) {
            final JComponent actual = campos[i];
            final JComponent siguiente = (i + 1 < campos.length) ? campos[i + 1] : null;
            javax.swing.Action accion = new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (actual instanceof JComboBox && ((JComboBox<?>) actual).isPopupVisible()) {
                        ((JComboBox<?>) actual).setPopupVisible(false);
                        return;
                    }
                    if (siguiente != null) {
                        siguiente.requestFocusInWindow();
                        if (siguiente instanceof javax.swing.text.JTextComponent) {
                            ((javax.swing.text.JTextComponent) siguiente).selectAll();
                        }
                    } else if (alFinal != null) {
                        alFinal.run();
                    }
                }
            };
            javax.swing.KeyStroke enter = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0);
            actual.getInputMap(JComponent.WHEN_FOCUSED).put(enter, "farmac.enterSiguiente");
            actual.getActionMap().put("farmac.enterSiguiente", accion);
            // Combos editables: el foco está en el editor interno
            if (actual instanceof JComboBox && ((JComboBox<?>) actual).isEditable()) {
                Component ed = ((JComboBox<?>) actual).getEditor().getEditorComponent();
                if (ed instanceof JComponent) {
                    ((JComponent) ed).getInputMap(JComponent.WHEN_FOCUSED).put(enter, "farmac.enterSiguiente");
                    ((JComponent) ed).getActionMap().put("farmac.enterSiguiente", accion);
                }
            }
        }
    }

    public static void enterComoTab(JComponent... campos) {
        enterComoTab(null, campos);
    }



    /** Iconos de la aplicación (cruz verde de Farmac) en varios tamaños. */
    public static java.util.List<java.awt.Image> iconosApp() {
        java.util.List<java.awt.Image> lista = new java.util.ArrayList<>();
        for (int s : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(s, s, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int margen = Math.max(1, s / 16);
            int lado = s - 2 * margen;
            int grosor = lado / 3;
            int off = (lado - grosor) / 2;
            float arco = Math.max(2f, s / 6f);
            g2.setColor(new Color(0x2B, 0xB0, 0x8A));
            g2.fill(new RoundRectangle2D.Float(margen, margen + off, lado, grosor, arco, arco));
            g2.setColor(new Color(0x1F, 0x9A, 0x73));
            g2.fill(new RoundRectangle2D.Float(margen + off, margen, grosor, lado, arco, arco));
            g2.dispose();
            lista.add(img);
        }
        return lista;
    }

    /** Logotipo: cruz verde + texto FARMAC. */
    public static JComponent crearLogo(Color colorTexto, int alto) {
        JLabel lbl = new JLabel("FARMAC");
        lbl.setFont(fuente(Font.BOLD, alto));
        lbl.setForeground(colorTexto);
        lbl.setIconTextGap(Math.max(8, alto / 3));
        final int lado = (int) (alto * 1.15);
        lbl.setIcon(new javax.swing.Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int grosor = lado / 3;
                int off = (lado - grosor) / 2;
                g2.setColor(new Color(0x2B, 0xB0, 0x8A));
                g2.fill(new RoundRectangle2D.Float(x, y + off, lado, grosor, 6, 6));
                g2.setColor(new Color(0x1F, 0x9A, 0x73));
                g2.fill(new RoundRectangle2D.Float(x + off, y, grosor, lado, 6, 6));
                g2.dispose();
            }
            @Override public int getIconWidth() { return lado; }
            @Override public int getIconHeight() { return lado; }
        });
        return lbl;
    }

    public static JPanel crearCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
        return crearTarjetaKPI(titulo, lblValor, colorValor, BLANCO, TEXTO_MUTED, null, 24);
    }

    /**
     * Tarjeta de indicador: título pequeño arriba y valor grande. Admite fondo de color y una barra
     * vertical de acento a la izquierda.
     */
    public static JPanel crearTarjetaKPI(String titulo, JLabel lblValor, Color colorValor,
                                         Color fondo, Color colorTitulo, Color barra, int tamValor) {
        JPanel card = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fondo);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                if (!fondo.equals(AZUL_PETROLEO)) {
                    Color borde = fondo.equals(BLANCO) ? BORDE : new Color(0xE0, 0xC9, 0x8A);
                    g2.setColor(borde);
                    g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                }
                if (barra != null) {
                    g2.setColor(barra);
                    g2.fill(new RoundRectangle2D.Float(14, 14, 7, getHeight() - 29, 6, 6));
                }
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, barra != null ? 36 : 18, 12, 16));
        card.setPreferredSize(new Dimension(170, 80));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(FUENTE_REGULAR);
        lblTit.setForeground(colorTitulo);

        lblValor.setFont(fuente(Font.BOLD, tamValor));
        lblValor.setForeground(colorValor);
        lblValor.setHorizontalAlignment(SwingConstants.LEFT);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    public static void estilizarTabla(JTable tabla) {
        tabla.setFont(FUENTE_REGULAR);
        tabla.setForeground(TEXTO);
        tabla.setRowHeight(40);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setGridColor(new Color(0xE2, 0xE8, 0xF0));
        tabla.setSelectionBackground(new Color(0xB9, 0xD0, 0xEE));
        tabla.setSelectionForeground(TEXTO_AZUL);

        JTableHeader header = tabla.getTableHeader();
        // Encabezados ligeramente más compactos para que quepan en pantallas
        // angostas sin recortar el texto.
        header.setFont(fuente(Font.BOLD, 14));
        header.setBackground(TABLA_HEADER_BG);
        header.setForeground(TEXTO_AZUL);
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);
        header.setOpaque(true);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(TABLA_HEADER_BG);
                lbl.setForeground(TEXTO_AZUL);
                lbl.setFont(fuente(Font.BOLD, 14));
                lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setOpaque(true);
                return lbl;
            }
        };
        header.setDefaultRenderer(headerRenderer);

        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BLANCO : TABLA_ZEBRA);
                }
                return c;
            }
        };
        bodyRenderer.setBorder(new EmptyBorder(0, 8, 0, 8));
        bodyRenderer.setForeground(TEXTO);
        tabla.setDefaultRenderer(Object.class, bodyRenderer);
    }

    /** Botón embebido en una celda: se dibuja con margen y esquinas redondeadas. */
    private static JPanel envolverBotonTabla(JButton boton, boolean zebra, int fila) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(fila % 2 == 0 ? BLANCO : TABLA_ZEBRA);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(4, 4, 4, 4);
        panel.add(boton, gbc);
        return panel;
    }

    public static class BotonTablaRenderer implements TableCellRenderer {
        private final JButton boton;

        public BotonTablaRenderer(String texto, Color fondo, Color textoColor) {
            boton = crearBoton(texto, fondo, textoColor);
            boton.setFont(FUENTE_NEGRITA);
            boton.setBorder(new EmptyBorder(0, 6, 0, 6));
            boton.setMinimumSize(new Dimension(0, 0));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return envolverBotonTabla(boton, true, row);
        }
    }

    public static class BotonTablaEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private final JButton button;
        private final Consumer<Integer> accionFila;
        private int filaActual;

        public BotonTablaEditor(String texto, Color fondo, Color textoColor, Consumer<Integer> accionFila) {
            this.accionFila = accionFila;
            this.button = crearBoton(texto, fondo, textoColor);
            this.button.setFont(FUENTE_NEGRITA);
            this.button.setBorder(new EmptyBorder(0, 6, 0, 6));
            this.button.setMinimumSize(new Dimension(0, 0));
            this.button.addActionListener(this);
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.filaActual = row;
            return envolverBotonTabla(button, true, row);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
            if (accionFila != null) {
                accionFila.accept(filaActual);
            }
        }
    }

    /** Indicador de estado: punto de color + texto (rediseño). Los tipos neutros se muestran como texto plano. */
    public static class BadgeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String texto = (value != null) ? value.toString() : "";
            String u = texto.toUpperCase();
            Color punto = null;
            Color fg = TEXTO;
            boolean negrita = false;
            if (u.contains("INACTIVO") || u.contains("CERRADO")) {
                fg = TEXTO;
            } else if (u.contains("VIGENTE") || u.contains("ACTIV") || u.contains("COMPLETADA")
                    || u.contains("ABIERTO") || u.contains("ÓPTIMO")) {
                punto = VERDE; fg = VERDE;
            } else if (u.contains("POR VENCER")) {
                punto = AMBAR; fg = AMBAR;
            } else if (u.contains("VENCIDO") || u.contains("CANCELADA") || u.contains("BAJO STOCK")) {
                punto = ROJO; fg = ROJO;
            } else if (u.contains("RECETA") || u.contains("ENCARGADO")) {
                fg = AZUL_ACCION; negrita = true;
            }
            JLabel lbl = new JLabel(texto);
            lbl.setFont(negrita ? FUENTE_NEGRITA : FUENTE_REGULAR);
            lbl.setForeground(fg);
            lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
            lbl.setIconTextGap(6);
            if (punto != null) {
                final Color pc = punto;
                lbl.setIcon(new javax.swing.Icon() {
                    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(pc);
                        g2.fillOval(x, y, 8, 8);
                        g2.dispose();
                    }
                    @Override public int getIconWidth() { return 8; }
                    @Override public int getIconHeight() { return 8; }
                });
            }
            lbl.setOpaque(true);
            lbl.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? BLANCO : TABLA_ZEBRA));
            return lbl;
        }
    }

    /** Botón de selector segmentado: el activo se dibuja con esquinas redondeadas. */
    public static class BotonSegmento extends JButton {
        private boolean activo;

        public BotonSegmento(String texto) {
            super(texto);
            setFont(FUENTE_NEGRITA);
            setForeground(TEXTO_AZUL);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(6, 18, 6, 18));
        }

        public void setActivo(boolean activo) {
            this.activo = activo;
            setForeground(activo ? BLANCO : TEXTO_AZUL);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (activo) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AZUL_PETROLEO);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
            super.paintComponent(g);
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
            // Rellena las esquinas exteriores con el fondo del contenedor para que no se vean cuadros blancos
            if (c.isOpaque()) {
                Component padre = c.getParent();
                while (padre != null && !padre.isOpaque()) {
                    padre = padre.getParent();
                }
                if (padre != null) {
                    java.awt.geom.Area esquinas = new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(x, y, width, height));
                    esquinas.subtract(new java.awt.geom.Area(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radio, radio)));
                    g2.setColor(padre.getBackground());
                    g2.fill(esquinas);
                }
            }
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
