package dev.lessonshop.orders;

import dev.lessonshop.infrai.InfraiGateway;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OrderFlow {
    public enum Stage { CHECKED_OUT, FULFILLED, RECEIPT_SENT }
    public record Checkout(String orderId, String learnerEmail, String courseTitle, long totalCents) {}
    public record RechargeEvent(String reference, double amount) {}
    public record Result(String orderId, Stage stage, String accessNote, String messageId, boolean rechargeConfigured) {}

    private final InfraiGateway infrai;
    private final CommerceConfig config;
    private final Map<String, Checkout> fulfilledOrders = new LinkedHashMap<>();

    public OrderFlow(InfraiGateway infrai, CommerceConfig config) {
        this.infrai = infrai;
        this.config = config;
    }

    public Result checkoutAndFulfill(Checkout checkout) {
        validate(checkout);
        boolean rechargeConfigured = infrai.balance() <= config.rechargeTrigger();
        if (rechargeConfigured) {
            infrai.configureAutoRecharge(config.rechargeTrigger(), config.rechargeAmount());
        }

        fulfilledOrders.put(checkout.orderId(), checkout);
        String accessNote = "Enrollment opened for " + checkout.courseTitle();
        String html = "<h1>Your course is ready</h1><p>Order " + escape(checkout.orderId())
                + " is fulfilled. " + escape(accessNote) + ".</p><p>Total: "
                + money(checkout.totalCents()) + "</p>";
        String messageId = infrai.sendEmail(checkout.learnerEmail(),
                config.storeName() + " receipt for " + checkout.orderId(), html);
        return new Result(checkout.orderId(), Stage.RECEIPT_SENT, accessNote, messageId, rechargeConfigured);
    }

    public String notifyRecharge(RechargeEvent event, String operationsEmail) {
        if (event.reference() == null || event.reference().isBlank() || event.amount() <= 0) {
            throw new IllegalArgumentException("Recharge event must have a reference and positive amount");
        }
        String html = "<h1>Teaching storefront balance recharged</h1><p>Reference "
                + escape(event.reference()) + " added " + String.format("%.2f", event.amount())
                + ". Checkout and learner updates remain active.</p>";
        return infrai.sendEmail(operationsEmail, config.storeName() + " recharge recorded", html);
    }

    public int fulfilledCount() { return fulfilledOrders.size(); }

    private static void validate(Checkout checkout) {
        if (checkout.orderId() == null || checkout.orderId().isBlank()
                || checkout.learnerEmail() == null || checkout.learnerEmail().isBlank()
                || checkout.courseTitle() == null || checkout.courseTitle().isBlank()
                || checkout.totalCents() < 0) {
            throw new IllegalArgumentException("Checkout fields are incomplete");
        }
    }

    private static String money(long cents) {
        return String.format("%.2f", cents / 100.0);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
