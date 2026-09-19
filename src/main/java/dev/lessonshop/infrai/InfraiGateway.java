package dev.lessonshop.infrai;

public interface InfraiGateway {
    double balance();
    void configureAutoRecharge(double triggerBalance, double rechargeAmount);
    String sendEmail(String to, String subject, String html);
}
