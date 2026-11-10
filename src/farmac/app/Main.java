package farmac.app;

import com.formdev.flatlaf.FlatLightLaf;
import farmac.ui.LoginScreen;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("ProgressBar.arc", 8);
                UIManager.put("ScrollBar.thumbArc", 999);
                UIManager.put("AccentColor", new Color(0x21, 0x73, 0xC8));
                UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
            } catch (Exception ignored) {}
            new LoginScreen().setVisible(true);
        });
    }
}
