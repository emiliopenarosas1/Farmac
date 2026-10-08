package farmac.app;

import farmac.ui.LoginScreen;

import javax.swing.*;

/**
 * Punto de entrada principal de la aplicación Farmac.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new LoginScreen().setVisible(true);
        });
    }
}
