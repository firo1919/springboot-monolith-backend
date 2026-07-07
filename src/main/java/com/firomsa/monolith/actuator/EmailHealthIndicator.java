
package com.firomsa.monolith.actuator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EmailHealthIndicator implements HealthIndicator {

    private final JavaMailSender javaMailSender;
    private final String mailHost;
    private final int mailPort;

    public EmailHealthIndicator(JavaMailSender javaMailSender,
            @Value("${spring.mail.host:localhost}") String mailHost,
            @Value("${spring.mail.port:1025}") int mailPort) {
        this.javaMailSender = javaMailSender;
        this.mailHost = mailHost;
        this.mailPort = mailPort;
    }

    @Override
    public Health health() {
        try {
            Session session = javaMailSender.createMimeMessage().getSession();

            Transport transport = session.getTransport("smtp");
            transport.connect(mailHost, mailPort, null, null);
            transport.close();

            return Health.up()
                    .withDetail("host", mailHost)
                    .withDetail("port", mailPort)
                    .build();
        } catch (MessagingException e) {
            log.error("Email health check failed", e);
            return Health.down()
                    .withDetail("host", mailHost)
                    .withDetail("port", mailPort)
                    .withDetail("error", e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Email health check failed with unexpected error", e);
            return Health.down()
                    .withDetail("host", mailHost)
                    .withDetail("port", mailPort)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
