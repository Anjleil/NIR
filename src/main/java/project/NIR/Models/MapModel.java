package project.NIR.Models;

import project.NIR.Models.Panes.InformationPane;
import project.NIR.Models.Panes.MapViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;

public class MapModel {
    private static MapViewer mapViewer; // Храним ссылку на MapViewer

    public static JLayeredPane createPane() throws IOException {



        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(1200, 1000));

        // Создаем информационную панель
        JPanel info = InformationPane.createInformationPane(100, 100);

        // Создаем карту
        mapViewer = new MapViewer(); // Инициализация MapViewer
        JPanel mapPanel = mapViewer.getMapViewer();

        // Устанавливаем начальные размеры для карты и добавляем ее в слой
        mapPanel.setBounds(0, 0, layeredPane.getPreferredSize().width, layeredPane.getPreferredSize().height);
        layeredPane.add(mapPanel, JLayeredPane.DEFAULT_LAYER);

        // Устанавливаем размеры информационной панели и добавляем ее
        info.setBounds(50, layeredPane.getPreferredSize().height - 200, layeredPane.getPreferredSize().width - 200, 160);
        layeredPane.add(info, JLayeredPane.PALETTE_LAYER);

        // Добавляем обработчик изменения размера для карты
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Обновляем размер карты в соответствии с новым размером окна
                mapPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                info.setBounds(50, layeredPane.getHeight() - 160, layeredPane.getWidth() - 100, 160);

                info.setBackground(new Color(230, 230, 230)); // Светлый фон
            }
        });

        return layeredPane;
    }
}
