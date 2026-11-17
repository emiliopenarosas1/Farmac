package farmac.ui;

import farmac.util.DialogoFarmac;
import farmac.util.TemaFarmac;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Visor de tickets PDF integrado en la aplicación (Apache PDFBox).
 *
 * <p>Renderiza el PDF real dentro de una ventana con controles de zoom, ajuste
 * al ancho, navegación de páginas y un botón para abrirlo con el visor externo
 * del sistema. El render se ejecuta en segundo plano para no bloquear la
 * interfaz mientras se dibuja la hoja.</p>
 */
public class VisorTicketPDF {

    /** Resolución base de render (a 100% de zoom). */
    private static final float DPI_BASE = 110f;
    private static final float ZOOM_MIN = 0.5f;
    private static final float ZOOM_MAX = 4.0f;
    private static final float ZOOM_PASO = 0.25f;
    private static final Color FONDO_HOJA = new Color(0x9A, 0x9E, 0xA2);

    private final File archivo;
    private final JDialog dialogo;
    private final JLabel lblHoja = new JLabel();
    private final JLabel lblZoom = new JLabel();
    private final JLabel lblPagina = new JLabel();
    private final JButton btnMenos = TemaFarmac.crearBotonSecundario("-");
    private final JButton btnMas = TemaFarmac.crearBotonSecundario("+");
    private final JButton btnAjustar = TemaFarmac.crearBotonSecundario("Ajustar ancho");
    private final JButton btnAnterior = TemaFarmac.crearBotonSecundario("Anterior");
    private final JButton btnSiguiente = TemaFarmac.crearBotonSecundario("Siguiente");
    private final JButton btnAbrir = TemaFarmac.crearBotonSecundario("Abrir PDF");
    private final JButton btnCerrar = TemaFarmac.crearBotonPrimario("Cerrar");
    private final JScrollPane scroll = new JScrollPane();
    private final JPanel pagPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

    private PDDocument documento;
    private PDFRenderer renderer;
    private int paginaActual = 0;
    private int totalPaginas = 1;
    private float zoom = 1f;
    /** Incrementa en cada render para descartar resultados de renders antiguos. */
    private int generacion = 0;

    private VisorTicketPDF(Window dueno, File archivo) {
        this.archivo = archivo;
        this.dialogo = new JDialog(dueno, "Ticket de Venta", Dialog.ModalityType.MODELESS);
    }

    /**
     * Abre el visor para el ticket indicado. Si el archivo no existe o el
     * entorno no puede mostrar la ventana, no interrumpe el flujo de cobro.
     */
    public static void mostrar(Component padre, File archivo) {
        if (archivo == null || !archivo.isFile()) {
            return;
        }
        try {
            Window dueno = null;
            if (padre instanceof Window) {
                dueno = (Window) padre;
            } else if (padre != null) {
                dueno = SwingUtilities.getWindowAncestor(padre);
            }
            VisorTicketPDF visor = new VisorTicketPDF(dueno, archivo);
            visor.construir();
            visor.dialogo.setVisible(true);
            visor.cargarDocumento();
        } catch (Throwable t) {
            // Sin las librerías de PDFBox (o sin entorno gráfico) el cobro no se
            // rompe: el mensaje con la ruta del ticket ya fue mostrado.
            System.err.println("No se pudo abrir el visor de tickets: " + t);
        }
    }

    private void construir() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(TemaFarmac.FONDO);
        raiz.setBorder(new EmptyBorder(10, 12, 12, 12));

        raiz.add(crearCabecera(), BorderLayout.NORTH);
        raiz.add(crearCuerpo(), BorderLayout.CENTER);
        raiz.add(crearPie(), BorderLayout.SOUTH);

        dialogo.setContentPane(raiz);
        dialogo.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialogo.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrar();
            }
        });

        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int ancho = Math.min(760, pantalla.width - 80);
        int alto = Math.min(840, pantalla.height - 100);
        dialogo.setSize(ancho, alto);
        dialogo.setMinimumSize(new Dimension(440, 460));
        dialogo.setLocationRelativeTo(dialogo.getOwner());

        // ESC cierra la ventana.
        dialogo.getRootPane().registerKeyboardAction(
                e -> cerrar(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Ctrl + rueda del ratón = zoom.
        scroll.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                cambiarZoom(e.getWheelRotation() < 0 ? ZOOM_PASO : -ZOOM_PASO);
                e.consume();
            }
        });
    }

    private JComponent crearCabecera() {
        JPanel card = TemaFarmac.crearCard();
        card.setLayout(new BorderLayout(12, 0));

        JLabel titulo = new JLabel("Ticket de Venta");
        titulo.setFont(TemaFarmac.FUENTE_SUBTITULO);
        titulo.setForeground(TemaFarmac.TEXTO_AZUL);

        JLabel ruta = new JLabel(archivo.getName());
        ruta.setFont(TemaFarmac.FUENTE_PEQUENA);
        ruta.setForeground(TemaFarmac.TEXTO_MUTED);
        ruta.setToolTipText(archivo.getAbsolutePath());

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        ruta.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(titulo);
        textos.add(ruta);

        JLabel insignia = new JLabel("PDF");
        insignia.setFont(TemaFarmac.FUENTE_BADGE);
        insignia.setForeground(TemaFarmac.BLANCO);
        insignia.setOpaque(true);
        insignia.setBackground(TemaFarmac.ROJO_SUAVE);
        insignia.setBorder(new EmptyBorder(3, 8, 3, 8));

        card.add(textos, BorderLayout.CENTER);
        card.add(insignia, BorderLayout.EAST);
        return card;
    }

    private JComponent crearCuerpo() {
        JPanel cuerpo = new JPanel(new BorderLayout(0, 8));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(new EmptyBorder(10, 0, 0, 0));

        cuerpo.add(crearBarraHerramientas(), BorderLayout.NORTH);

        lblHoja.setText("Abriendo ticket...");
        lblHoja.setFont(TemaFarmac.FUENTE_REGULAR);
        lblHoja.setForeground(TemaFarmac.TEXTO_MUTED);
        lblHoja.setHorizontalAlignment(SwingConstants.CENTER);
        lblHoja.setVerticalAlignment(SwingConstants.CENTER);

        PanelHoja hoja = new PanelHoja();
        hoja.add(lblHoja);

        scroll.setViewportView(hoja);
        scroll.setBorder(BorderFactory.createLineBorder(TemaFarmac.BORDE));
        scroll.getViewport().setBackground(FONDO_HOJA);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getHorizontalScrollBar().setUnitIncrement(18);

        cuerpo.add(scroll, BorderLayout.CENTER);
        return cuerpo;
    }

    private JComponent crearPie() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel ayuda = new JLabel("Ctrl + rueda del ratón para acercar · Esc para cerrar");
        ayuda.setFont(TemaFarmac.FUENTE_PEQUENA);
        ayuda.setForeground(TemaFarmac.TEXTO_MUTED);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        btnAbrir.setToolTipText("Abrir el PDF con el visor predeterminado del sistema");
        btnAbrir.addActionListener(e -> abrirExterno());
        btnCerrar.addActionListener(e -> cerrar());
        botones.add(btnAbrir);
        botones.add(btnCerrar);

        pie.add(ayuda, BorderLayout.WEST);
        pie.add(botones, BorderLayout.EAST);
        return pie;
    }

    private JComponent crearBarraHerramientas() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);

        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        zoomPanel.setOpaque(false);

        lblZoom.setText("100%");
        lblZoom.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblZoom.setForeground(TemaFarmac.TEXTO_AZUL);
        lblZoom.setHorizontalAlignment(SwingConstants.CENTER);
        lblZoom.setPreferredSize(new Dimension(56, TemaFarmac.ALTO_CONTROL));

        btnMenos.setToolTipText("Reducir zoom");
        btnMas.setToolTipText("Aumentar zoom");
        btnAjustar.setToolTipText("Ajustar el ancho de la hoja a la ventana");
        btnMenos.addActionListener(e -> cambiarZoom(-ZOOM_PASO));
        btnMas.addActionListener(e -> cambiarZoom(ZOOM_PASO));
        btnAjustar.addActionListener(e -> ajustarAncho());

        zoomPanel.add(btnMenos);
        zoomPanel.add(lblZoom);
        zoomPanel.add(btnMas);
        zoomPanel.add(btnAjustar);

        pagPanel.setOpaque(false);

        lblPagina.setFont(TemaFarmac.FUENTE_NEGRITA);
        lblPagina.setForeground(TemaFarmac.TEXTO_AZUL);
        lblPagina.setHorizontalAlignment(SwingConstants.CENTER);
        lblPagina.setPreferredSize(new Dimension(110, TemaFarmac.ALTO_CONTROL));

        btnAnterior.addActionListener(e -> cambiarPagina(-1));
        btnSiguiente.addActionListener(e -> cambiarPagina(1));

        pagPanel.add(btnAnterior);
        pagPanel.add(lblPagina);
        pagPanel.add(btnSiguiente);

        barra.add(zoomPanel, BorderLayout.WEST);
        barra.add(pagPanel, BorderLayout.EAST);
        actualizarPaginacion();
        return barra;
    }

    private void cargarDocumento() {
        new SwingWorker<PDDocument, Void>() {
            @Override
            protected PDDocument doInBackground() throws Exception {
                return PDDocument.load(archivo);
            }

            @Override
            protected void done() {
                try {
                    documento = get();
                    renderer = new PDFRenderer(documento);
                    totalPaginas = documento.getNumberOfPages();
                    actualizarPaginacion();
                    ajustarAncho();
                } catch (Exception ex) {
                    lblHoja.setText("No se pudo abrir el ticket.");
                    DialogoFarmac.mostrarJO(dialogo,
                            "No se pudo mostrar el ticket PDF:\n" + archivo.getAbsolutePath()
                                    + "\n\n" + ex.getMessage(),
                            "Error al mostrar el ticket", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Ajusta el zoom para que el ancho de la página ocupe el ancho disponible. */
    private void ajustarAncho() {
        if (renderer == null) {
            return;
        }
        try {
            PDPage pagina = documento.getPage(paginaActual);
            float anchoPt = pagina.getMediaBox().getWidth();
            int anchoDisponible = scroll.getViewport().getWidth() - 8;
            if (anchoDisponible < 60) {
                anchoDisponible = 560; // Aún sin layout definitivo: valor razonable.
            }
            float anchoPx = anchoPt / 72f * DPI_BASE;
            zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, anchoDisponible / anchoPx));
            renderizar();
        } catch (Exception ex) {
            renderizar();
        }
    }

    private void cambiarZoom(float delta) {
        if (renderer == null) {
            return;
        }
        float nuevo = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom + delta));
        if (Math.abs(nuevo - zoom) < 0.001f) {
            return;
        }
        zoom = nuevo;
        renderizar();
    }

    private void cambiarPagina(int delta) {
        if (renderer == null) {
            return;
        }
        int nueva = paginaActual + delta;
        if (nueva < 0 || nueva >= totalPaginas) {
            return;
        }
        paginaActual = nueva;
        actualizarPaginacion();
        renderizar();
    }

    private void actualizarPaginacion() {
        lblPagina.setText("Página " + (paginaActual + 1) + " / " + totalPaginas);
        btnAnterior.setEnabled(paginaActual > 0);
        btnSiguiente.setEnabled(paginaActual < totalPaginas - 1);
        // Los controles de página solo aportan cuando el documento tiene varias.
        pagPanel.setVisible(totalPaginas > 1);
    }

    /** Renderiza la página actual en segundo plano con el zoom vigente. */
    private void renderizar() {
        if (renderer == null) {
            return;
        }
        final int gen = ++generacion;
        final float z = zoom;
        final int pag = paginaActual;

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return renderer.renderImageWithDPI(pag, DPI_BASE * z);
            }

            @Override
            protected void done() {
                if (gen != generacion) {
                    return; // Un render más reciente ya tomó el relevo.
                }
                try {
                    BufferedImage img = get();
                    lblHoja.setText(null);
                    lblHoja.setIcon(new ImageIcon(img));
                    lblHoja.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
                    lblZoom.setText(Math.round(z * 100) + "%");
                    lblHoja.revalidate();
                    lblHoja.repaint();
                } catch (Exception ignored) {
                    // Render cancelado o interrumpido por el cierre de la ventana.
                }
            }
        }.execute();
    }

    private void abrirExterno() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(archivo);
            } else {
                DialogoFarmac.mostrarJO(dialogo,
                        "Este sistema no permite abrir el archivo automáticamente.\nUbicación:\n"
                                + archivo.getAbsolutePath(),
                        "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            DialogoFarmac.mostrarJO(dialogo,
                    "No se pudo abrir el PDF con el visor externo:\n" + ex.getMessage(),
                    "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cerrar() {
        generacion++; // Invalida cualquier render pendiente.
        if (documento != null) {
            try {
                documento.close();
            } catch (Exception ignored) {
                // Nada que hacer: la ventana se cierra igualmente.
            }
            documento = null;
        }
        dialogo.dispose();
    }

    /**
     * Panel que aloja la hoja renderizada. Se estira al viewport para centrar
     * el ticket cuando es más pequeño que la ventana, pero conserva su tamaño
     * cuando es más grande para permitir el desplazamiento.
     */
    private static class PanelHoja extends JPanel implements Scrollable {

        PanelHoja() {
            super(new GridBagLayout());
            setOpaque(true);
            setBackground(FONDO_HOJA);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 180;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() != null && getParent().getWidth() > getPreferredSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }
}
