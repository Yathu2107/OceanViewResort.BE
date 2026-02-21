package com.oceanviewresort.service;

import com.oceanviewresort.model.Bill;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    public void sendBillEmail(String toEmail, Bill bill) {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        final String fromEmail = "your_email@gmail.com";
        final String password = "your_app_password";

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication
                    getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                fromEmail, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toEmail)
            );
            message.setSubject("Ocean View Resort - Billing Details");

            message.setText(
                    "Thank you for staying with Ocean View Resort.\n\n" +
                            "Reservation ID: " +
                            bill.getReservation().getReservationId() + "\n" +
                            "Nights: " + bill.getNumberOfNights() + "\n" +
                            "Rate per night: " +
                            bill.getRoomRatePerNight() + "\n" +
                            "Total Amount: " +
                            bill.getTotalAmount()
            );

            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Email sending failed", e);
        }
    }
}