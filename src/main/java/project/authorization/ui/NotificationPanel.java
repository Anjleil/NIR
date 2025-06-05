package project.authorization.ui;

import javax.swing.*;
import java.awt.*;

public class NotificationPanel extends JPanel {
    private final JLabel messageLabel;

    // Store original colors to calculate new colors with alpha during fade
    private final Color baseBackgroundColor;
    private final Color baseForegroundColor = Color.WHITE;
    private final Color baseBorderColor = new Color(255, 255, 255, 80);

    public NotificationPanel(String message, NotificationManager.NotificationType type) {
        super(new BorderLayout());

        this.baseBackgroundColor = getColorForType(type);

        // The panel itself is not opaque; we draw the background manually in paintComponent
        setOpaque(false);
        // Set an empty border for padding. The visual border will be drawn in paintComponent.
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        messageLabel = new JLabel("<html><body style='width: 280px;'>" + message + "</body></html>");
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        // The label must also be transparent to not draw its own background
        messageLabel.setOpaque(false);
        
        add(messageLabel, BorderLayout.CENTER);

        // Set initial colors with full opacity before first paint
        setOpacity(1.0f);
        
        // Set a preferred size to help the layout manager in the JLayeredPane
        setPreferredSize(new Dimension(320, getPreferredSize().height));
    }

    public void setOpacity(float opacity) {
        opacity = Math.max(0.0f, Math.min(1.0f, opacity));

        // Create new colors with the given opacity (alpha)
        // Background color will be used by paintComponent
        setBackground(new Color(
            baseBackgroundColor.getRed(), 
            baseBackgroundColor.getGreen(), 
            baseBackgroundColor.getBlue(), 
            Math.round(baseBackgroundColor.getAlpha() * opacity)
        ));

        // Text color is applied directly to the label
        messageLabel.setForeground(new Color(
            baseForegroundColor.getRed(), 
            baseForegroundColor.getGreen(), 
            baseForegroundColor.getBlue(), 
            Math.round(baseForegroundColor.getAlpha() * opacity)
        ));
        
        // Request a repaint to draw with the new colors
        if (getParent() != null) {
            getParent().repaint();
        } else {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // First, let the superclass do its painting (which is nothing for a non-opaque panel).
        super.paintComponent(g);

        // Now, do our custom drawing.
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 1. Paint the background (using the background color with alpha set by setOpacity)
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

        // 2. Paint the border with the same opacity logic
        float currentOpacity = (float) getBackground().getAlpha() / (float) baseBackgroundColor.getAlpha();
        if (Float.isNaN(currentOpacity)) currentOpacity = 0f; // Avoid NaN if base alpha is 0
        
        g2d.setColor(new Color(
            baseBorderColor.getRed(), 
            baseBorderColor.getGreen(), 
            baseBorderColor.getBlue(), 
            Math.round(baseBorderColor.getAlpha() * currentOpacity)
        ));
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        
        g2d.dispose();
    }

    private Color getColorForType(NotificationManager.NotificationType type) {
        switch (type) {
            case SUCCESS:
                return new Color(40, 167, 69, 240); // Green
            case WARNING:
                return new Color(255, 193, 7, 240);  // Yellow
            case ERROR:
                return new Color(220, 53, 69, 240);  // Red
            case INFO:
            default:
                return new Color(23, 162, 184, 240); // Blue/Teal
        }
    }
} 