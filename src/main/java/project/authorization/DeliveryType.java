package project.authorization;

enum DeliveryType {
    STANDARD(300, false),
    EXPRESS(500, true);

    private final double baseCost;
    private final boolean express;

    DeliveryType(double baseCost, boolean express) {
        this.baseCost = baseCost;
        this.express = express;
    }

    public double getBaseCost() {
        return baseCost;
    }

    public boolean isExpress() {
        return express;
    }
}