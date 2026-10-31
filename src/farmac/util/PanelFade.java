package farmac.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;

/**
 * Panel con animacion de aparicion (fade-in) para transiciones suaves entre pantallas.
 */
public class PanelFade extends JPanel {
    private float alpha = 1f;
    private Timer timer;

    public PanelFade(JPanel contenido) {
        setLayout(new BorderLayout());
        setOpaque(false);
        if (contenido != null) add(contenido, BorderLayout.CENTER);
    }

    public void fadeIn() {
        alpha = 0f;
        if (timer != null && timer.isRunning()) timer.stop();
        timer = new Timer(16, e -> {
            alpha = Math.min(1f, alpha + 0.10f);
            repaint();
            if (alpha >= 1f) ((Timer) e.getSource()).stop();
        });
        timer.start();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        super.paint(g2);
        g2.dispose();
    }
}
