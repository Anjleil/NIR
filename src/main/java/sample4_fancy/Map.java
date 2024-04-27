package sample4_fancy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Map extends JFrame {
    private JTextField textField;
    private JLabel descriptionLabel;

    public Map() {
        setTitle("Map Window");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600); // устанавливаем размер окна

        // Добавляем компонент для отображения изображения карты местности
        ImageIcon mapImage = new ImageIcon("C:\\Users\\timof\\IdeaProjects\\JavaMap\\jxmapviewer2\\examples\\src\\sample4_fancy\\map.jpg"); // замените "map.jpg" на путь к вашему изображению
        JLabel mapLabel = new JLabel(mapImage);
        getContentPane().add(mapLabel, BorderLayout.CENTER);

        // Создаем панель для элементов управления
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(20, 20, 20, 20)); // устанавливаем отступы от краев окна

        // Добавляем закругленные края панели
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setOpaque(true);
        controlPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.BLUE, 4, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        // Добавляем поле для ввода текста
        textField = new JTextField();
        controlPanel.add(textField, BorderLayout.NORTH);

        // Добавляем блок текста для описания
        descriptionLabel = new JLabel("Описание");
        controlPanel.add(descriptionLabel, BorderLayout.CENTER);

        // Добавляем кнопку "Подтвердить"
        JButton confirmButton = new JButton("Подтвердить");
        confirmButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose(); // закрываем окно при нажатии кнопки "Подтвердить"
            }
        });
        controlPanel.add(confirmButton, BorderLayout.SOUTH);

        // Добавляем панель с элементами управления в правую часть окна с учетом отступов
        getContentPane().add(controlPanel, BorderLayout.EAST);

        setVisible(true); // делаем окно видимым
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MapWindow(); // создаем и отображаем окно
            }
        });
    }
}


