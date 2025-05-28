package project.authorization;

import lombok.Data;

@Data
class Delivery {
    private String address;
    private DeliveryType type;
    private double cost;

    public Delivery(String address, DeliveryType type) {
        this.address = address;
        this.type = type;
        this.cost = calculateCost();
    }

    private double calculateCost() {
        return type.getBaseCost() * (type.isExpress() ? 1.5 : 1);
    }

    // Геттеры
}