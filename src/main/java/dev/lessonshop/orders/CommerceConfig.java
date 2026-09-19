package dev.lessonshop.orders;

public record CommerceConfig(double rechargeTrigger, double rechargeAmount, String storeName) {
    public CommerceConfig {
        if (rechargeTrigger < 0 || rechargeAmount <= 0) {
            throw new IllegalArgumentException("Recharge values must be positive");
        }
        if (storeName == null || storeName.isBlank()) throw new IllegalArgumentException("Store name is required");
    }

    public static CommerceConfig fromEnvironment() {
        return new CommerceConfig(
                number("RECHARGE_TRIGGER", 20.0),
                number("RECHARGE_AMOUNT", 100.0),
                System.getenv().getOrDefault("STORE_NAME", "Learning Counter"));
    }

    private static double number(String name, double fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Double.parseDouble(value);
    }
}
