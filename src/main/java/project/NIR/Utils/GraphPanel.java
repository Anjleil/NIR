package project.NIR.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Collections;
import java.util.List;

public class GraphPanel extends JPanel {
    private final List<ResponseTimeDataPoint> data;
    private static final int PADDING = 25;
    private static final int LABEL_PADDING = 25;
    private static final Color LINE_COLOR = new Color(44, 102, 230, 180);
    private static final Color POINT_COLOR = new Color(100, 100, 100, 180);
    private static final Color GRID_COLOR = new Color(200, 200, 200, 200);

    public GraphPanel(List<ResponseTimeDataPoint> data) {
        this.data = data;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null || data.isEmpty()) {
            g.drawString("No data to display.", 10, 20);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double maxResponseTime = data.stream().mapToLong(ResponseTimeDataPoint::getResponseTime).max().orElse(0);
        double minTimestamp = data.stream().mapToLong(ResponseTimeDataPoint::getTimestamp).min().orElse(0);
        double maxTimestamp = data.stream().mapToLong(ResponseTimeDataPoint::getTimestamp).max().orElse(0);
        
        double avgResponseTime = data.stream().mapToLong(ResponseTimeDataPoint::getResponseTime).average().orElse(0);

        int width = getWidth();
        int height = getHeight();

        g2.setColor(Color.WHITE);
        g2.fillRect(PADDING + LABEL_PADDING, PADDING, width - (2 * PADDING) - LABEL_PADDING, height - (2 * PADDING) - LABEL_PADDING);
        g2.setColor(Color.BLACK);

        // Draw axes
        g2.drawLine(PADDING + LABEL_PADDING, height - PADDING - LABEL_PADDING, PADDING + LABEL_PADDING, PADDING);
        g2.drawLine(PADDING + LABEL_PADDING, height - PADDING - LABEL_PADDING, width - PADDING, height - PADDING - LABEL_PADDING);
        
        // Draw Y-axis labels and grid lines
        for (int i = 0; i < 10; i++) {
            int y = height - ((i * (height - 2 * PADDING - LABEL_PADDING)) / 10 + PADDING + LABEL_PADDING);
            g2.setColor(GRID_COLOR);
            g2.drawLine(PADDING + LABEL_PADDING + 1, y, width - PADDING, y);
            g2.setColor(Color.BLACK);
            String yLabel = String.format("%.0f ms", (maxResponseTime * i) / 10.0);
            g2.drawString(yLabel, PADDING, y);
        }

        // Draw X-axis labels (time progression)
        double timeRange = maxTimestamp - minTimestamp;
        if(timeRange == 0) timeRange = 1;
        for (int i = 0; i <= 5; i++) {
            int x = PADDING + LABEL_PADDING + (i * (width - 2 * PADDING - LABEL_PADDING)) / 5;
            g2.setColor(Color.BLACK);
            String xLabel = String.format("+%.1f s", (timeRange * i) / 5000.0);
            g2.drawString(xLabel, x - 10, height - PADDING);
        }

        // Draw the actual data
        Stroke oldStroke = g2.getStroke();
        g2.setColor(LINE_COLOR);
        g2.setStroke(new BasicStroke(2f));
        
        Path2D path = new Path2D.Double();
        
        for (int i = 0; i < data.size(); i++) {
            double x = PADDING + LABEL_PADDING + ((data.get(i).getTimestamp() - minTimestamp) / timeRange) * (width - 2 * PADDING - LABEL_PADDING);
            double y = height - PADDING - LABEL_PADDING - ((data.get(i).getResponseTime() / maxResponseTime) * (height - 2 * PADDING - LABEL_PADDING));

            if(i == 0) {
                 path.moveTo(x, y);
            } else {
                 path.lineTo(x, y);
            }
        }
        g2.draw(path);
        g2.setStroke(oldStroke);
        
        g2.setColor(Color.RED);
        g2.drawString(String.format("Average Response Time: %.2f ms", avgResponseTime), PADDING + LABEL_PADDING, PADDING - 5);
    }
} 