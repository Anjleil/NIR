package project.NIR;

import project.NIR.Models.MapModel;

import javax.swing.*;

public class App {
    public static void run(){
        JLayeredPane layeredPane = MapModel.createPane();
        JFrame frame = new JFrame("JXMapviewer2 Example 1");
        frame.setContentPane(layeredPane);
        frame.setSize(layeredPane.getPreferredSize().width, layeredPane.getPreferredSize().height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
