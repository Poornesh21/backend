package com.mobicomm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending email notifications and invoices
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${admin.email:admin@mobicomm.com}")
    private String adminEmail; // Admin email from properties, default if not set

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    /**
     * Send a reminder email for expiring plans
     * @param toEmail Recipient email
     * @param mobileNumber User's mobile number
     * @param planName Plan name
     * @param expiryDate Expiry date string
     * @return true if email was sent successfully
     */
    public boolean sendReminderEmail(String toEmail, String mobileNumber, String planName, String expiryDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Your MobiComm Plan is Expiring Soon");
            // CC admin on all reminder emails
            helper.setCc(adminEmail);

            String emailContent = generateReminderEmailContent(mobileNumber, planName, expiryDate);
            helper.setText(emailContent, true); // true indicates HTML content

            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send an invoice email for a completed transaction
     * @param toEmail Recipient email
     * @param mobileNumber User's mobile number
     * @param planName Plan name
     * @param amount Transaction amount
     * @param transactionId Transaction ID
     * @param paymentMethod Payment method
     * @param transactionDate Transaction date
     * @return true if email was sent successfully
     */
    public boolean sendInvoiceEmail(String toEmail, String mobileNumber, String planName, String amount,
                                    String transactionId, String paymentMethod, String transactionDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("MobiComm Recharge Invoice #" + transactionId);
            // CC admin on all invoices
            helper.setCc(adminEmail);

            String emailContent = generateInvoiceEmailContent(
                    mobileNumber, planName, amount, transactionId, paymentMethod, transactionDate
            );
            helper.setText(emailContent, true); // true indicates HTML content

            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send a payment confirmation email
     * @param toEmail Recipient email
     * @param mobileNumber User's mobile number
     * @param planName Plan name
     * @param amount Transaction amount
     * @param transactionId Transaction ID
     * @return true if email was sent successfully
     */
    public boolean sendPaymentConfirmationEmail(String toEmail, String mobileNumber, String planName,
                                                String amount, String transactionId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("MobiComm Recharge Confirmation");
            // CC admin on all payment confirmations
            helper.setCc(adminEmail);

            LocalDateTime now = LocalDateTime.now();
            String formattedDate = now.format(DATE_FORMATTER);

            String emailContent = generatePaymentConfirmationEmailContent(
                    mobileNumber, planName, amount, transactionId, formattedDate
            );
            helper.setText(emailContent, true); // true indicates HTML content

            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generate reminder email content
     */
    private String generateReminderEmailContent(String mobileNumber, String planName, String expiryDate) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
                "        .header { background: linear-gradient(135deg, #FF385C 0%, #FF6B89 100%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }\n" +
                "        .content { background-color: #f9f9f9; padding: 20px; border-radius: 0 0 10px 10px; }\n" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #888; }\n" +
                "        .btn { background: #FF385C; color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none; display: inline-block; }\n" +
                "        .details { background-color: white; padding: 15px; border-radius: 5px; margin: 20px 0; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='header'>\n" +
                "            <h1>MobiComm Plan Expiry Reminder</h1>\n" +
                "        </div>\n" +
                "        <div class='content'>\n" +
                "            <p>Dear Customer,</p>\n" +
                "            <p>Your mobile plan will expire soon. To ensure uninterrupted service, please renew your plan before the expiry date.</p>\n" +
                "            \n" +
                "            <div class='details'>\n" +
                "                <p><strong>Mobile Number:</strong> " + mobileNumber + "</p>\n" +
                "                <p><strong>Plan:</strong> " + planName + "</p>\n" +
                "                <p><strong>Expiry Date:</strong> " + expiryDate + "</p>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div style='text-align: center;'>\n" +
                "                <a href='https://mobicomm.com/renew' class='btn'>Renew Now</a>\n" +
                "            </div>\n" +
                "            \n" +
                "            <p>Thank you for choosing MobiComm for your mobile service needs.</p>\n" +
                "            <p>Best regards,<br>The MobiComm Team</p>\n" +
                "        </div>\n" +
                "        <div class='footer'>\n" +
                "            <p>© 2025 MobiComm. All rights reserved.</p>\n" +
                "            <p>If you have any questions, please contact our support team at support@mobicomm.com</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * Generate invoice email content
     */
    private String generateInvoiceEmailContent(String mobileNumber, String planName, String amount,
                                               String transactionId, String paymentMethod, String transactionDate) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
                "        .header { background: linear-gradient(135deg, #FF385C 0%, #FF6B89 100%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }\n" +
                "        .content { background-color: #f9f9f9; padding: 20px; border-radius: 0 0 10px 10px; }\n" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #888; }\n" +
                "        .invoice { background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0; }\n" +
                "        .invoice-header { border-bottom: 1px solid #eee; padding-bottom: 10px; margin-bottom: 20px; }\n" +
                "        .invoice-row { display: flex; justify-content: space-between; margin-bottom: 10px; }\n" +
                "        .invoice-total { border-top: 2px solid #eee; margin-top: 20px; padding-top: 10px; font-weight: bold; }\n" +
                "        .brand { font-size: 24px; font-weight: bold; color: white; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='header'>\n" +
                "            <div class='brand'>MobiComm</div>\n" +
                "            <h1>Payment Invoice</h1>\n" +
                "        </div>\n" +
                "        <div class='content'>\n" +
                "            <p>Dear Customer,</p>\n" +
                "            <p>Thank you for your recent recharge. Below are the details of your transaction:</p>\n" +
                "            \n" +
                "            <div class='invoice'>\n" +
                "                <div class='invoice-header'>\n" +
                "                    <h2>INVOICE #" + transactionId + "</h2>\n" +
                "                    <p><strong>Date:</strong> " + transactionDate + "</p>\n" +
                "                </div>\n" +
                "                \n" +
                "                <div class='invoice-row'>\n" +
                "                    <span><strong>Mobile Number:</strong></span>\n" +
                "                    <span>" + mobileNumber + "</span>\n" +
                "                </div>\n" +
                "                \n" +
                "                <div class='invoice-row'>\n" +
                "                    <span><strong>Plan:</strong></span>\n" +
                "                    <span>" + planName + "</span>\n" +
                "                </div>\n" +
                "                \n" +
                "                <div class='invoice-row'>\n" +
                "                    <span><strong>Payment Method:</strong></span>\n" +
                "                    <span>" + paymentMethod + "</span>\n" +
                "                </div>\n" +
                "                \n" +
                "                <div class='invoice-row'>\n" +
                "                    <span><strong>Transaction ID:</strong></span>\n" +
                "                    <span>" + transactionId + "</span>\n" +
                "                </div>\n" +
                "                \n" +
                "                <div class='invoice-total'>\n" +
                "                    <div class='invoice-row'>\n" +
                "                        <span>TOTAL AMOUNT:</span>\n" +
                "                        <span>₹" + amount + "</span>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            \n" +
                "            <p>Thank you for choosing MobiComm. If you have any questions about this invoice, please contact our customer support.</p>\n" +
                "            <p>Best regards,<br>The MobiComm Team</p>\n" +
                "        </div>\n" +
                "        <div class='footer'>\n" +
                "            <p>© 2025 MobiComm. All rights reserved.</p>\n" +
                "            <p>For any issues, please contact us at support@mobicomm.com or call our toll-free number 1800-123-4567</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * Generate payment confirmation email content
     */
    private String generatePaymentConfirmationEmailContent(String mobileNumber, String planName, String amount,
                                                           String transactionId, String formattedDate) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
                "        .header { background: linear-gradient(135deg, #FF385C 0%, #FF6B89 100%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }\n" +
                "        .content { background-color: #f9f9f9; padding: 20px; border-radius: 0 0 10px 10px; }\n" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #888; }\n" +
                "        .success-icon { font-size: 48px; color: #4CAF50; text-align: center; margin: 20px 0; }\n" +
                "        .details { background-color: white; padding: 15px; border-radius: 5px; margin: 20px 0; }\n" +
                "        .support-btn { background: #FF385C; color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none; display: inline-block; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='header'>\n" +
                "            <h1>Payment Confirmation</h1>\n" +
                "        </div>\n" +
                "        <div class='content'>\n" +
                "            <div class='success-icon'>✓</div>\n" +
                "            <h2 style='text-align: center;'>Payment Successful!</h2>\n" +
                "            <p>Dear Customer,</p>\n" +
                "            <p>Your payment has been successfully processed. Your mobile recharge is now complete.</p>\n" +
                "            \n" +
                "            <div class='details'>\n" +
                "                <p><strong>Mobile Number:</strong> " + mobileNumber + "</p>\n" +
                "                <p><strong>Plan:</strong> " + planName + "</p>\n" +
                "                <p><strong>Amount:</strong> ₹" + amount + "</p>\n" +
                "                <p><strong>Transaction ID:</strong> " + transactionId + "</p>\n" +
                "                <p><strong>Date & Time:</strong> " + formattedDate + "</p>\n" +
                "            </div>\n" +
                "            \n" +
                "            <p>A detailed invoice has been sent to you separately. Please keep this for your records.</p>\n" +
                "            <p>Thank you for choosing MobiComm for your mobile recharge!</p>\n" +
                "            \n" +
                "            <div style='text-align: center; margin-top: 20px;'>\n" +
                "                <a href='https://mobicomm.com/support' class='support-btn'>Contact Support</a>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class='footer'>\n" +
                "            <p>© 2025 MobiComm. All rights reserved.</p>\n" +
                "            <p>This is an automated email. Please do not reply to this message.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}