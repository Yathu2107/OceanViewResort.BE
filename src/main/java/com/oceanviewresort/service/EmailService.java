package com.oceanviewresort.service;

import com.oceanviewresort.model.Bill;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.util.ConfigLoader;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = ConfigLoader.getProperty("email.smtp.host", "smtp.gmail.com");
    private static final String SMTP_PORT = ConfigLoader.getProperty("email.smtp.port", "587");
    private static final String SMTP_USERNAME = ConfigLoader.getProperty("email.username");
    private static final String SMTP_PASSWORD = ConfigLoader.getProperty("email.password");
    private static final String FROM_EMAIL = ConfigLoader.getProperty("email.from");
    private static final String FROM_NAME = ConfigLoader.getProperty("email.from.name", "Ocean View Resort");

    public EmailService() {
        logEmailConfiguration();
    }

    /**
     * Log email configuration (for debugging)
     */
    private static void logEmailConfiguration() {
        System.out.println("[EMAIL CONFIG] SMTP Host: " + SMTP_HOST);
        System.out.println("[EMAIL CONFIG] SMTP Port: " + SMTP_PORT);
        System.out.println("[EMAIL CONFIG] From Email: " + FROM_EMAIL);
        System.out.println("[EMAIL CONFIG] From Name: " + FROM_NAME);
        System.out.println("[EMAIL CONFIG] Username: " + (SMTP_USERNAME != null ? SMTP_USERNAME : "NOT SET"));
        System.out.println("[EMAIL CONFIG] Password: " + (SMTP_PASSWORD != null ? "SET" : "NOT SET"));
        System.out.println();
    }

    /**
     * Create InternetAddress with display name safely
     */
    private static InternetAddress createFromAddress() throws UnsupportedEncodingException {
        return new InternetAddress(FROM_EMAIL, FROM_NAME);
    }

    public void sendBillEmail(String toEmail, Bill bill) {

        System.out.println("[EMAIL] Preparing to send billing email to: " + toEmail);

        // Validate email configuration
        if (SMTP_USERNAME == null || SMTP_PASSWORD == null) {
            System.err.println("[EMAIL ERROR] Email credentials not configured in application.properties");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        final String password = SMTP_PASSWORD;

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                SMTP_USERNAME, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(createFromAddress());
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
            message.setSubject("Ocean View Resort - Billing Details");

            String htmlBody = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <style>\n" +
                    "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }\n"
                    +
                    "        .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; border-radius: 8px; }\n"
                    +
                    "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px 8px 0 0; text-align: center; }\n"
                    +
                    "        .header h1 { margin: 0; font-size: 28px; }\n" +
                    "        .content { background: white; padding: 30px; border-radius: 0 0 8px 8px; }\n" +
                    "        .details-table { width: 100%; margin: 20px 0; border-collapse: collapse; }\n" +
                    "        .details-table td { padding: 12px; border-bottom: 1px solid #eee; }\n" +
                    "        .details-table td:first-child { font-weight: 600; color: #667eea; width: 40%; }\n" +
                    "        .room-table { width: 100%; margin: 10px 0; border-collapse: collapse; }\n" +
                    "        .room-table th { background-color: #667eea; color: white; padding: 10px; text-align: left; font-weight: 600; font-size: 13px; }\n"
                    +
                    "        .room-table td { padding: 10px; border-bottom: 1px solid #eee; font-size: 13px; }\n" +
                    "        .room-table tr:last-child td { border-bottom: none; }\n" +
                    "        .amount-box { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; }\n"
                    +
                    "        .amount-box .label { font-size: 14px; opacity: 0.9; }\n" +
                    "        .amount-box .value { font-size: 32px; font-weight: bold; }\n" +
                    "        .footer { text-align: center; padding: 20px; color: #999; font-size: 12px; border-top: 1px solid #eee; margin-top: 20px; }\n"
                    +
                    "        .reservation-badge { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px 20px; border-radius: 8px; text-align: center; margin-bottom: 20px; }\n"
                    +
                    "        .reservation-badge .label { font-size: 12px; opacity: 0.9; text-transform: uppercase; letter-spacing: 1px; }\n"
                    +
                    "        .reservation-badge .value { font-size: 24px; font-weight: bold; margin-top: 5px; }\n"
                    +
                    "        .footer a { color: #667eea; text-decoration: none; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"header\">\n" +
                    "            <h1>🏨 Ocean View Resort</h1>\n" +
                    "            <p>Billing Details</p>\n" +
                    "        </div>\n" +
                    "        <div class=\"content\">\n" +
                    "            <div class=\"reservation-badge\">\n" +
                    "                <div class=\"label\">Reservation ID</div>\n" +
                    "                <div class=\"value\">#" + bill.getReservation().getReservationId() + "</div>\n" +
                    "            </div>\n" +
                    "            <p>Thank you for staying with Ocean View Resort!</p>\n" +
                    "            <p style=\"font-weight: 600; color: #667eea; margin-top: 20px; margin-bottom: 10px;\">Room Details:</p>\n"
                    +
                    "            <table class=\"room-table\">\n" +
                    "                <tr><th>Room No</th><th>Price/Night</th><th>Nights</th><th>Subtotal</th></tr>\n" +
                    buildRoomDetailsHtml(bill) +
                    "            </table>\n" +
                    "            <div class=\"amount-box\">\n" +
                    "                <div class=\"label\">Total Amount Due</div>\n" +
                    "                <div class=\"value\">Rs." + bill.getTotalAmount() + "</div>\n" +
                    "            </div>\n" +
                    "            <p style=\"text-align: center; color: #999; margin-top: 20px;\">If you have any questions, please contact our support team.</p>\n"
                    +
                    "        </div>\n" +
                    "        <div class=\"footer\">\n" +
                    "            <p>&copy; 2026 Ocean View Resort. All rights reserved.</p>\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";

            // Set content as HTML
            message.setContent(htmlBody, "text/html; charset=utf-8");

            System.out.println("[EMAIL] Sending billing email via SMTP (" + SMTP_HOST + ":" + SMTP_PORT + ")...");
            Transport.send(message);
            System.out.println("[EMAIL] ✓ Billing email sent successfully to: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("[EMAIL ERROR] Failed to send billing email to: " + toEmail);
            System.err.println("[EMAIL ERROR] Error message: " + e.getMessage());
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            System.err.println("[EMAIL ERROR] Failed to set sender name for email");
            System.err.println("[EMAIL ERROR] Error message: " + e.getMessage());
        }
    }

    /**
     * Send reservation confirmation email to guest
     * Includes greeting message and reservation details
     */
    public void sendReservationConfirmation(Reservation reservation) {

        if (reservation == null) {
            System.err.println("[EMAIL ERROR] Reservation is null. Cannot send email.");
            return;
        }

        if (reservation.getGuest() == null) {
            System.err.println("[EMAIL ERROR] Guest details are missing in reservation. Cannot send email.");
            System.err.println("[EMAIL ERROR] Reservation ID: " + reservation.getReservationId());
            return;
        }

        if (reservation.getGuest().getEmail() == null || reservation.getGuest().getEmail().trim().isEmpty()) {
            System.err.println("[EMAIL ERROR] Guest email is missing or empty. Cannot send email.");
            System.err.println("[EMAIL ERROR] Guest Name: " + reservation.getGuest().getName());
            System.err.println("[EMAIL ERROR] Guest ID: " + reservation.getGuest().getId());
            return;
        }

        String guestEmail = reservation.getGuest().getEmail();
        System.out.println("[EMAIL] Preparing to send reservation confirmation email to: " + guestEmail);

        // Validate email configuration
        if (SMTP_USERNAME == null || SMTP_PASSWORD == null) {
            System.err.println("[EMAIL ERROR] Email credentials not configured in application.properties");
            System.err.println("[EMAIL ERROR] Set email.username and email.password in application.properties");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        final String password = SMTP_PASSWORD;

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SMTP_USERNAME, password);
                    }
                });

        try {
            System.out.println("[EMAIL] Building email message for guest: " + reservation.getGuest().getName());

            Message message = new MimeMessage(session);
            message.setFrom(createFromAddress());
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(guestEmail));
            message.setSubject("Reservation Confirmation - Ocean View Resort");

            String htmlBody = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <style>\n" +
                    "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }\n"
                    +
                    "        .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; border-radius: 8px; }\n"
                    +
                    "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px 30px; border-radius: 8px 8px 0 0; text-align: center; }\n"
                    +
                    "        .header h1 { margin: 0 0 10px 0; font-size: 32px; }\n" +
                    "        .header p { margin: 0; font-size: 16px; opacity: 0.9; }\n" +
                    "        .content { background: white; padding: 30px; border-radius: 0 0 8px 8px; }\n" +
                    "        .greeting { font-size: 18px; color: #333; margin-bottom: 20px; }\n" +
                    "        .card { background: #f5f7fa; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #667eea; }\n"
                    +
                    "        .card-title { font-weight: 600; color: #667eea; font-size: 14px; text-transform: uppercase; margin-bottom: 15px; }\n"
                    +
                    "        .card-table { width: 100%; }\n" +
                    "        .card-table tr { border-bottom: 1px solid #e0e0e0; }\n" +
                    "        .card-table tr:last-child { border-bottom: none; }\n" +
                    "        .card-table td { padding: 8px 0; }\n" +
                    "        .card-table td:first-child { font-weight: 600; color: #555; width: 45%; }\n" +
                    "        .card-table td:last-child { text-align: right; color: #333; }\n" +
                    "        .status-badge { display: inline-block; background: #4caf50; color: white; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }\n"
                    +
                    "        .highlight-row td:first-child { color: #667eea; font-weight: 700; }\n" +
                    "        .highlight-row td:last-child { color: #667eea; font-weight: 700; font-size: 18px; }\n" +
                    "        .footer { text-align: center; padding: 20px; color: #999; font-size: 12px; border-top: 1px solid #eee; margin-top: 20px; }\n"
                    +
                    "        .footer a { color: #667eea; text-decoration: none; }\n" +
                    "        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; border-radius: 6px; text-decoration: none; margin-top: 15px; }\n"
                    +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"header\">\n" +
                    "            <h1>🏨 Ocean View Resort</h1>\n" +
                    "            <p>Reservation Confirmation</p>\n" +
                    "        </div>\n" +
                    "        <div class=\"content\">\n" +
                    "            <p class=\"greeting\">Dear <strong>" + reservation.getGuest().getName()
                    + "</strong>,</p>\n" +
                    "            <p>Welcome to Ocean View Resort! Thank you for choosing us for your stay. Your reservation has been confirmed and we're excited to host you.</p>\n"
                    +
                    "            \n" +
                    "            <div class=\"card\">\n" +
                    "                <div class=\"card-title\">📋 Reservation Details</div>\n" +
                    "                <table class=\"card-table\">\n" +
                    "                    <tr><td>Reservation ID:</td><td>" + reservation.getReservationId()
                    + "</td></tr>\n" +
                    "                    <tr><td>Guest Name:</td><td>" + reservation.getGuest().getName()
                    + "</td></tr>\n" +
                    "                    <tr><td>Contact Number:</td><td>" + reservation.getGuest().getContactNumber()
                    + "</td></tr>\n" +
                    "                    <tr><td>Room Numbers:</td><td>" + formatRoomNumbers(reservation.getRoomIds())
                    + "</td></tr>\n" +
                    "                    <tr><td>Number of Rooms:</td><td>" + reservation.getRoomIds().size()
                    + "</td></tr>\n" +
                    "                </table>\n" +
                    "            </div>\n" +
                    "            \n" +
                    "            <div class=\"card\">\n" +
                    "                <div class=\"card-title\">📅 Stay Information</div>\n" +
                    "                <table class=\"card-table\">\n" +
                    "                    <tr><td>Check-In Date:</td><td>" + reservation.getCheckInDate()
                    + "</td></tr>\n" +
                    "                    <tr><td>Check-Out Date:</td><td>" + reservation.getCheckOutDate()
                    + "</td></tr>\n" +
                    "                    <tr><td>Status:</td><td><span class=\"status-badge\">"
                    + reservation.getStatus() + "</span></td></tr>\n" +
                    "                </table>\n" +
                    "            </div>\n" +
                    "            \n" +
                    "            <p><strong>Important Information:</strong></p>\n" +
                    "            <ul>\n" +
                    "                <li>Check-in begins at 3:00 PM</li>\n" +
                    "                <li>Check-out is by 11:00 AM</li>\n" +
                    "                <li>For early check-in or late check-out, please contact us in advance</li>\n" +
                    "            </ul>\n" +
                    "            \n" +
                    "            <p>If you have any questions or need assistance, please don't hesitate to contact our customer service team. We're here to make your stay unforgettable!</p>\n"
                    +
                    "            \n" +
                    "        </div>\n" +
                    "        <div class=\"footer\">\n" +
                    "            <p><strong>Ocean View Resort</strong> | www.oceanviewresort.com</p>\n" +
                    "            <p>&copy; 2026 Ocean View Resort. All rights reserved. | <a href=\"#\">Privacy Policy</a> | <a href=\"#\">Contact Us</a></p>\n"
                    +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";

            // Set content as HTML
            message.setContent(htmlBody, "text/html; charset=utf-8");

            System.out.println("[EMAIL] Sending email via SMTP (" + SMTP_HOST + ":" + SMTP_PORT + ")...");
            Transport.send(message);
            System.out.println("[EMAIL] ✓ Reservation confirmation email sent successfully to: " + guestEmail);

        } catch (MessagingException e) {
            System.err.println("[EMAIL ERROR] Failed to send reservation confirmation email to: " + guestEmail);
            System.err.println("[EMAIL ERROR] Error message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("[EMAIL ERROR] Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            System.err.println("[EMAIL ERROR] Failed to set sender name for email");
            System.err.println("[EMAIL ERROR] Error message: " + e.getMessage());
        }
    }

    /**
     * Format room numbers for email display
     * Converts List<Integer> to comma-separated string (e.g., "101, 102, 103")
     */
    private static String formatRoomNumbers(java.util.List<Integer> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roomIds.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(roomIds.get(i));
        }
        return sb.toString();
    }

    /**
     * Build HTML table rows for room details in billing email
     */
    private static String buildRoomDetailsHtml(Bill bill) {
        if (bill.getRoomDetails() == null || bill.getRoomDetails().isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        for (Bill.RoomDetail room : bill.getRoomDetails()) {
            html.append("                <tr><td>").append(room.getRoomNumber())
                    .append("</td><td>Rs.").append(room.getPricePerNight())
                    .append("</td><td>").append(bill.getNumberOfNights())
                    .append("</td><td>Rs.").append(room.getTotalForRoom())
                    .append("</td></tr>\n");
        }
        return html.toString();
    }
}