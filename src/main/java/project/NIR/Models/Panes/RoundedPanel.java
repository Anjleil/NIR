package project.NIR.Models.Panes;

import javax.swing.*;
import java.awt.*;

public class RoundedPanel extends JPanel {
    public RoundedPanel() {
        setOpaque(false);
        //setBorder(new LineBorder(new Color(0, 0, 0), 0, true));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        g2.setColor(getForeground());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        g2.dispose();
        super.paintComponent(g);
    }
}
