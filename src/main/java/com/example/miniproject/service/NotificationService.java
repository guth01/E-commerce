package com.example.miniproject.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    // Fixed test address for now — swap for the real customer's email once
    // User has an email field.
    private static final String TEST_RECIPIENT = "customer@example.com";

    @Autowired
    private JavaMailSender mailSender;

    // @Async means this method runs on a separate thread pool, NOT the
    // request thread. The controller/service that calls this returns to the
    // client immediately; it does not wait for the email to actually send.
    @Async
    public void sendOrderConfirmation(Long orderId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(TEST_RECIPIENT);
            message.setSubject("Order Confirmation");
            message.setText("Order placed successfully order ID " + orderId);

            mailSender.send(message);
            logger.info("Order confirmation email sent for order ID {}", orderId);
        } catch (Exception e) {
            // Deliberately swallow the exception here rather than letting it
            // propagate. Since this runs async, there's no HTTP request left
            // to fail — an email failure should NEVER roll back or affect
            // the order that was already saved. We just log it.
            logger.error("Failed to send order confirmation email for order ID {}: {}", orderId, e.getMessage());
        }
    }
}