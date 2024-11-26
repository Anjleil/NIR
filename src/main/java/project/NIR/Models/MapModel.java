package project.NIR.Models;

import project.NIR.Models.Panes.InformationPane;
import project.NIR.Models.Panes.MapViewer;

import javax.swing.*;
import java.awt.*;

public class MapModel {

    public static JLayeredPane createPane() {

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(1000, 800));

        JPanel info = InformationPane.createInformationPane();
        JPanel map = new MapViewer().getMapViewer();

        map.setBounds(0, 0, layeredPane.getPreferredSize().width, layeredPane.getPreferredSize().height);
        layeredPane.add(map, JLayeredPane.DEFAULT_LAYER);


        info.setBounds(10, 10, info.getPreferredSize().width, info.getPreferredSize().height);
        layeredPane.add(info, JLayeredPane.PALETTE_LAYER);

        return layeredPane;
    }
}
