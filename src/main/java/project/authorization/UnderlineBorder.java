package project.authorization;

import javax.swing.border.AbstractBorder;
import java.awt.*;

public class UnderlineBorder extends AbstractBorder {
    private Color color;
    private int thickness;

    public UnderlineBorder(Color color) {
        this(color, 1); // Default thickness 1
    }

    public UnderlineBorder(Color color, int thickness) {
        this.color = color;
        this.thickness = thickness;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(this.color);
        g2d.fillRect(x, y + height - this.thickness, width, this.thickness);
        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(0, 0, this.thickness, 0);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 0;
        insets.top = 0;
        insets.right = 0;
        insets.bottom = this.thickness;
        return insets;
    }

    public void setColor(Color color) {
        this.color = color;
    }
} 