package dev.lessonshop.orders;

import dev.lessonshop.infrai.InfraiGateway;

import java.util.ArrayList;
import java.util.List;

public final class OrderFlowTest {
    public static void main(String[] args) {
        configuresRechargeAtThresholdAndCompletesTheLearnerUpdate();
        leavesRechargeAloneAboveThreshold();
        System.out.println("OrderFlowTest: 2 decisions passed");
    }

    private static void configuresRechargeAtThresholdAndCompletesTheLearnerUpdate() {
        RecordingGateway gateway = new RecordingGateway(12.0);
        OrderFlow flow = new OrderFlow(gateway, new CommerceConfig(20.0, 100.0, "Study Shelf"));

        OrderFlow.Result result = flow.checkoutAndFulfill(
                new OrderFlow.Checkout("order-7", "learner@example.com", "Geometry Lab", 3200));
        String rechargeMessage = flow.notifyRecharge(
                new OrderFlow.RechargeEvent("recharge-7", 100.0), "teacher@example.com");

        check(result.rechargeConfigured(), "low balance should configure recharge");
        check(gateway.configurations == 1, "configuration should happen once");
        check(result.stage() == OrderFlow.Stage.RECEIPT_SENT, "order should end with a sent receipt");
        check(flow.fulfilledCount() == 1, "fulfilled order should be recorded");
        check(gateway.recipients.equals(List.of("learner@example.com", "teacher@example.com")),
                "receipt and recharge update should use the same gateway");
        check("message-2".equals(rechargeMessage), "recharge message id should be returned");
    }

    private static void leavesRechargeAloneAboveThreshold() {
        RecordingGateway gateway = new RecordingGateway(75.0);
        OrderFlow flow = new OrderFlow(gateway, new CommerceConfig(20.0, 100.0, "Study Shelf"));
        OrderFlow.Result result = flow.checkoutAndFulfill(
                new OrderFlow.Checkout("order-8", "learner@example.com", "Writing Studio", 2800));
        check(!result.rechargeConfigured(), "healthy balance should not rewrite recharge configuration");
        check(gateway.configurations == 0, "configuration should remain untouched");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class RecordingGateway implements InfraiGateway {
        private final double currentBalance;
        private int configurations;
        private final List<String> recipients = new ArrayList<>();

        private RecordingGateway(double currentBalance) { this.currentBalance = currentBalance; }
        public double balance() { return currentBalance; }
        public void configureAutoRecharge(double triggerBalance, double rechargeAmount) { configurations++; }
        public String sendEmail(String to, String subject, String html) {
            recipients.add(to);
            return "message-" + recipients.size();
        }
    }
}
