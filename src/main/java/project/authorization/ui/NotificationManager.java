package project.authorization.ui;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

public class NotificationManager {

    public enum NotificationType {
        SUCCESS, INFO, WARNING, ERROR
    }

    private static final int NOTIFICATION_Y_OFFSET = 20;
    private static final int NOTIFICATION_X_OFFSET = 20;
    private static final int NOTIFICATION_SPACING = 10;

    private final JLayeredPane layeredPane;
    private final Queue<NotificationPanel> notificationQueue = new LinkedList<>();
    private final java.util.List<NotificationPanel> activeNotifications = new LinkedList<>();
    private boolean isShowing = false;

    public NotificationManager(JLayeredPane layeredPane) {
        this.layeredPane = layeredPane;
    }

    public void show(String message, NotificationType type) {
        NotificationPanel panel = new NotificationPanel(message, type);
        notificationQueue.add(panel);
        if (!isShowing) {
            showNext();
        }
    }

    private void showNext() {
        if (notificationQueue.isEmpty()) {
            isShowing = false;
            return;
        }

        isShowing = true;
        NotificationPanel panel = notificationQueue.poll();
        activeNotifications.add(panel);
        layeredPane.add(panel, JLayeredPane.POPUP_LAYER);
        
        // Recalculate positions for all active notifications
        repositionNotifications();

        // Animate fade-in
        panel.setOpacity(0f);
        Timer fadeInTimer = new Timer(15, null);
        final float[] opacity = {0.0f};
        fadeInTimer.addActionListener(e -> {
            opacity[0] += 0.05f;
            if (opacity[0] >= 1.0f) {
                opacity[0] = 1.0f;
                fadeInTimer.stop();
                // After fade in, wait for a few seconds then fade out
                Timer delayTimer = new Timer(4000, e2 -> {
                    Timer fadeOutTimer = new Timer(15, null);
                    fadeOutTimer.addActionListener(e3 -> {
                        opacity[0] -= 0.05f;
                        if (opacity[0] <= 0.0f) {
                            opacity[0] = 0.0f;
                            fadeOutTimer.stop();
                            layeredPane.remove(panel);
                            activeNotifications.remove(panel);
                            repositionNotifications(); // Reposition remaining notifications
                            layeredPane.revalidate();
                            layeredPane.repaint();
                            // Check if more notifications are waiting
                            if(activeNotifications.isEmpty()){
                                showNext();
                            }
                        }
                        panel.setOpacity(opacity[0]);
                    });
                    fadeOutTimer.start();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            }
            panel.setOpacity(opacity[0]);
        });
        fadeInTimer.start();
        
        // If there's more in the queue, show them with a slight delay
        if (!notificationQueue.isEmpty()) {
            Timer nextTimer = new Timer(500, e -> showNext());
            nextTimer.setRepeats(false);
            nextTimer.start();
        } else {
             isShowing = false; // Allow new notifications to be triggered
        }
    }
    
    private void repositionNotifications() {
        if (layeredPane == null) return;
        int bottomY = layeredPane.getHeight() - NOTIFICATION_Y_OFFSET;

        // Iterate through the active notifications to place them from the bottom up
        for (int i = 0; i < activeNotifications.size(); i++) {
            NotificationPanel activePanel = activeNotifications.get(i);
            Dimension prefSize = activePanel.getPreferredSize();
            int x = layeredPane.getWidth() - prefSize.width - NOTIFICATION_X_OFFSET;
            int y = bottomY - prefSize.height;
            
            activePanel.setBounds(x, y, prefSize.width, prefSize.height);
            // The next notification will be placed above this one
            bottomY = y - NOTIFICATION_SPACING;
        }
        layeredPane.repaint();
    }
} 