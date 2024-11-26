package project.NIR.JXMapViewer;

import org.jxmapviewer.JXMapViewer;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * Used to pan using press and drag mouse gestures with inertia (smooth sliding)
 */
public class PanMouseInputListener extends MouseInputAdapter {
    private Point prev;
    private JXMapViewer viewer;
    private Cursor priorCursor;

    private double velocityX = 0;
    private double velocityY = 0;
    private Timer inertiaTimer;
    private static final double FRICTION = 0.92;  // Сила трения для замедления

    /**
     * @param viewer the jxmapviewer
     */
    public PanMouseInputListener(JXMapViewer viewer) {
        this.viewer = viewer;
        this.inertiaTimer = new Timer(5, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyInertia();
            }
        });
    }

    @Override
    public void mousePressed(MouseEvent evt) {
        if (!SwingUtilities.isLeftMouseButton(evt)) return;
        if (!viewer.isPanningEnabled()) return;

        prev = evt.getPoint();
        priorCursor = viewer.getCursor();
        viewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        // Останавливаем инерцию при новом движении
        inertiaTimer.stop();
    }

    @Override
    public void mouseDragged(MouseEvent evt) {
        if (!SwingUtilities.isLeftMouseButton(evt)) return;
        if (!viewer.isPanningEnabled()) return;

        Point current = evt.getPoint();
        double x = viewer.getCenter().getX();
        double y = viewer.getCenter().getY();

        if (prev != null) {
            velocityX = prev.x - current.x;
            velocityY = prev.y - current.y;

            x += velocityX;
            y += velocityY;
        }

        int maxHeight = (int) (viewer.getTileFactory().getMapSize(viewer.getZoom()).getHeight() * viewer
                .getTileFactory().getTileSize(viewer.getZoom()));
        if (y > maxHeight) {
            y = maxHeight;
        }

        prev = current;
        viewer.setCenter(new Point2D.Double(x, y));
        viewer.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent evt) {
        if (!SwingUtilities.isLeftMouseButton(evt)) return;

        prev = null;
        viewer.setCursor(priorCursor);

        // Запускаем инерцию при отпускании мыши
        if (Math.abs(velocityX) > 0 || Math.abs(velocityY) > 0) {
            inertiaTimer.start();
        }
    }

    /**
     * Применяет инерцию и плавно замедляет движение карты
     */
    private void applyInertia() {
        // Уменьшаем скорость под действием трения
        velocityX *= FRICTION;
        velocityY *= FRICTION;

        double x = viewer.getCenter().getX() + velocityX;
        double y = viewer.getCenter().getY() + velocityY;

        int maxHeight = (int) (viewer.getTileFactory().getMapSize(viewer.getZoom()).getHeight() * viewer
                .getTileFactory().getTileSize(viewer.getZoom()));
        if (y > maxHeight) {
            y = maxHeight;
        }

        viewer.setCenter(new Point2D.Double(x, y));
        viewer.repaint();

        // Останавливаем таймер, если скорость слишком мала
        if (Math.abs(velocityX) < 0.1 && Math.abs(velocityY) < 0.1) {
            inertiaTimer.stop();
        }
    }
}
