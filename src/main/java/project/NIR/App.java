package project.NIR;

import project.NIR.Models.Data.SharedData;
import project.NIR.Models.MapModel;
import project.NIR.Models.Routes.GeoPath;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

public class App {
    public static void run() throws IOException {
        JLayeredPane layeredPane = MapModel.createPane();

        JFrame frame = new JFrame("JXMapviewer2 Example 1");
        frame.setContentPane(layeredPane);
        frame.setSize(layeredPane.getPreferredSize().width, layeredPane.getPreferredSize().height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        Timer timer = new Timer(5000, e -> {
            frame.repaint();

            List<GeoPath> paths = SharedData.getPaths();

            if (!paths.isEmpty())
                System.out.println(paths);
        });
        timer.start();
    }
}
