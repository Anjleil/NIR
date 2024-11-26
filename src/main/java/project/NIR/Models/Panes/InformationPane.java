package project.NIR.Models.Panes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InformationPane {

    private static boolean isMinimized = true;

    public static JPanel createInformationPane() {

        JPanel infoPanel = new RoundedPanel();
        JButton closeButton = new JButton("Развернуть");
        infoPanel.add(closeButton);

        int originalWidth = 250;
        int originalHeight = 250;
        int minimizedWidth = 120;
        int minimizedHeight = 40;

        infoPanel.setPreferredSize(new Dimension(minimizedWidth, minimizedHeight));
        infoPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isMinimized) {
                    infoPanel.setSize(new Dimension(originalWidth, originalHeight));
                    closeButton.setText("Скрыть");
                } else {
                    infoPanel.setSize(new Dimension(minimizedWidth, minimizedHeight));
                    closeButton.setText("Развернуть");
                }
                isMinimized = !isMinimized;
                infoPanel.revalidate();
            }
        });

        return infoPanel;
    }
}
