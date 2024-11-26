package project.NIR;

import org.jxmapviewer.viewer.GeoPosition;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

public class MissionApp extends JFrame {
    private JTextField departureAddressField;
    private JTextField deliveryAddressField;

    public MissionApp() {
        initUI();
    }

    private void initUI() {
        setTitle("Address Input");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        try {
            setContentPane(new BackgroundPanel());
        } catch (IOException e) {
            e.printStackTrace();
        }

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false); // Убираем фон у панели, чтобы был виден фон окна

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel departureLabel = new JLabel("Адрес отправления:");
        departureLabel.setForeground(Color.BLACK);
        panel.add(departureLabel, gbc);

        gbc.gridx = 1;
        departureAddressField = new JTextField(20);
        panel.add(departureAddressField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel deliveryLabel = new JLabel("Адрес доставки:");
        deliveryLabel.setForeground(Color.BLACK);
        panel.add(deliveryLabel, gbc);

        gbc.gridx = 1;
        deliveryAddressField = new JTextField(20);
        panel.add(deliveryAddressField, gbc);

        JPanel buttonPanel = getjPanel();
        buttonPanel.setOpaque(false);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints contentGbc = new GridBagConstraints();
        contentGbc.gridx = 0;
        contentGbc.gridy = 0;
        contentGbc.anchor = GridBagConstraints.CENTER;
        contentPane.add(panel, contentGbc);
    }

    private JPanel getjPanel() {
        JButton okButton = getOkButton();
        JButton cancelButton = getCancelButton();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        return buttonPanel;
    }

    private JButton getCancelButton() {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        return cancelButton;
    }

    private JButton getOkButton() {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            String departureAddress = departureAddressField.getText();
            String deliveryAddress = deliveryAddressField.getText();

            System.out.println("Departure Address: " + departureAddress);
            System.out.println("Destination Address: " + deliveryAddress);

            try {
                GeoPosition delivery = Geocode.getCoordinates(deliveryAddress);
                GeoPosition departure = Geocode.getCoordinates(departureAddress);

                Client client = new Client();
                client.setDeparture(departure);
                client.setDelivery(delivery);
                client.connectToServer("localhost", 12345);
                client.run();

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        return okButton;
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            MissionApp ex = new MissionApp();
            ex.setVisible(true);
        });
    }
}

class BackgroundPanel extends JPanel {
    private Image background;
    private Image drone;

    public BackgroundPanel() throws IOException {
        background = loadImage("/back.jpg");
        drone = loadImage("/drone.png");
    }

    private Image loadImage(String path) throws IOException {
        URL imageUrl = getClass().getResource(path);
        if (imageUrl == null) {
            throw new IOException("Image not found: " + path);
        }
        return ImageIO.read(imageUrl);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        int droneX = (getWidth() - 50) / 2;
        int droneY = 20; // Расположение квадрокоптера сверху по центру
        //g.drawImage(drone, droneX, droneY, 50, 50, this);

        // Рисуем надпись "Доставка" под изображением квадрокоптера
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        String text = "Доставка";
        int textX = (getWidth() - fm.stringWidth(text)) / 2;
        int textY = 100;
        g.drawString(text, textX, textY);
    }
}
