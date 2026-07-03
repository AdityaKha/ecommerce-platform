package com.aditya.ecommerce.notification.strategy;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationStrategy implements NotificationStrategy {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotificationStrategy(JavaMailSender mailSender,
                                      @Value("${notification.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(OrderCreatedEvent event) {
        if (event.customerEmail() == null || event.customerEmail().isBlank()) {
            log.warn("Skipping email notification for order {}: no customer email on the event", event.orderId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(event.customerEmail());
        message.setSubject("Order #" + event.orderId() + " confirmed");
        message.setText("Hi %s,\n\nYour order #%d has been received. Total: %s.\n\nThanks for shopping with us!"
                .formatted(event.customerUsername(), event.orderId(), event.totalAmount()));

        mailSender.send(message);
        log.info("Sent order confirmation email to {} for order {}", event.customerEmail(), event.orderId());
    }
}
