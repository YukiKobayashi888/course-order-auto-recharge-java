package dev.lessonshop.orders;

import dev.lessonshop.infrai.InfraiRestClient;

public final class CourseOrderExample {
    private CourseOrderExample() {}

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: CourseOrderExample learner-email");
            System.exit(2);
        }
        String apiKey = System.getenv("INFRAI_API_KEY");
        CommerceConfig config = CommerceConfig.fromEnvironment();
        OrderFlow orders = new OrderFlow(new InfraiRestClient(apiKey), config);

        OrderFlow.Result result = orders.checkoutAndFulfill(new OrderFlow.Checkout(
                "course-1042", args[0], "Practical Algebra", 4900));
        System.out.printf("order=%s stage=%s access=%s receipt_message_id=%s recharge_configured=%s%n",
                result.orderId(), result.stage(), result.accessNote(), result.messageId(), result.rechargeConfigured());

        String rechargeMessageId = orders.notifyRecharge(
                new OrderFlow.RechargeEvent("recharge-course-1042", config.rechargeAmount()), args[0]);
        System.out.printf("recharge_notification_message_id=%s fulfilled_orders=%d%n",
                rechargeMessageId, orders.fulfilledCount());
    }
}
