package sample4_fancy;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MapWindow extends JFrame {
    private JLabel mapLabel;
    private JTextField textField;
    private JTextArea descriptionArea;
    private JButton confirmButton;

    public MapWindow() {
        // Устанавливаем размеры окна
        setSize(800, 600);
        // Задаем заголовок окна
        setTitle("Окно с картой");
        // Закрываем окно при нажатии на крестик
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Создаем панель для карты
        JPanel mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Рисуем карту на панели
                ImageIcon mapIcon = new ImageIcon("C:\\Users\\timof\\IdeaProjects\\JavaMap\\jxmapviewer2\\examples\\src\\sample4_fancy\\map.jpg");
                g.drawImage(mapIcon.getImage(), 0, 0, this);
            }
        };
        // Устанавливаем размеры панели для карты
        mapPanel.setPreferredSize(new Dimension(600, 600));

        // Создаем метку для карты
        mapLabel = new JLabel();
        // Устанавливаем размеры метки для карты
        mapLabel.setPreferredSize(new Dimension(600, 600));
        // Добавляем метку для карты на панель
        mapPanel.add(mapLabel);

        // Создаем панель для ввода текста
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.setPreferredSize(new Dimension(200, 600));
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        inputPanel.setBackground(Color.WHITE);

        // Создаем поле для ввода текста
        textField = new JTextField();
        textField.setPreferredSize(new Dimension(200, 30));
        inputPanel.add(textField, BorderLayout.NORTH);

        // Создаем блок для описания
        descriptionArea = new JTextArea();
        descriptionArea.setPreferredSize(new Dimension(200, 400));
        inputPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);

        // Создаем кнопку "Подтвердить"
        confirmButton = new JButton("Подтвердить");
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Закрываем окно при нажатии на кнопку "Подтвердить"
                dispose();
            }
        });
        inputPanel.add(confirmButton, BorderLayout.SOUTH);

        // Создаем панель с закругленными краями
        JPanel roundedPanel = new JPanel();
        roundedPanel.setLayout(new BorderLayout());
        roundedPanel.setPreferredSize(new Dimension(200, 600));
        roundedPanel.setBackground(Color.WHITE);
        roundedPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true));

        // Добавляем inputPanel на roundedPanel
        roundedPanel.add(inputPanel, BorderLayout.CENTER);

        // Добавляем панели на окно
        add(mapPanel, BorderLayout.CENTER);
        add(roundedPanel, BorderLayout.EAST);

        // Делаем окно видимым
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MapWindow::new);
    }
}


