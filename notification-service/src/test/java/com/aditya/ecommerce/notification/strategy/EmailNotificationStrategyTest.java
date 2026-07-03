package com.aditya.ecommerce.notification.strategy;

import com.aditya.ecommerce.notification.event.OrderCreatedEvent;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies EmailNotificationStrategy actually speaks SMTP end-to-end: a real
 * JavaMailSender delivers to a real (embedded, fake) SMTP server rather than
 * just being mocked out.
 */
class EmailNotificationStrategyTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void sendsOrderConfirmationEmailOverRealSmtpConnection() throws Exception {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(ServerSetupTest.SMTP.getPort());

        EmailNotificationStrategy strategy =
                new EmailNotificationStrategy(mailSender, "no-reply@ecommerce-platform.local");

        OrderCreatedEvent event = new OrderCreatedEvent(
                42L,
                "jdoe",
                "jdoe@example.com",
                List.of(new OrderCreatedEvent.Item(7L, 2)),
                new BigDecimal("59.98"));

        strategy.send(event);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("Order #42 confirmed");
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("jdoe@example.com");
        assertThat(received[0].getFrom()[0].toString()).isEqualTo("no-reply@ecommerce-platform.local");
        assertThat(received[0].getContent().toString()).contains("59.98").contains("jdoe");
    }

    @Test
    void skipsSendWhenCustomerEmailMissing() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(ServerSetupTest.SMTP.getPort());

        EmailNotificationStrategy strategy =
                new EmailNotificationStrategy(mailSender, "no-reply@ecommerce-platform.local");

        OrderCreatedEvent event = new OrderCreatedEvent(
                43L, "jdoe", null, List.of(new OrderCreatedEvent.Item(7L, 1)), new BigDecimal("10.00"));

        strategy.send(event);

        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }
}
