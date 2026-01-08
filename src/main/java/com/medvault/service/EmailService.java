package com.medvault.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendSetPasswordEmail(String toEmail, String doctorName, String token) {
        try {
            log.info("==========================================");
            log.info("📧 Preparing to send password setup email");
            log.info("To: {}", toEmail);
            log.info("From: MedVault <{}>", fromEmail);
            log.info("Doctor Name: {}", doctorName);
            log.info("Token: {}", token);
            log.info("==========================================");

            String setPasswordLink = "http://localhost:3000/set-password?token=" + token;
            log.debug("Password reset link: {}", setPasswordLink);

            // Try to load HTML template, fallback to simple HTML if fails
            String htmlContent;
            try {
                ClassPathResource resource = new ClassPathResource("templates/email/set-password.html");
                String htmlTemplate = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

                htmlContent = htmlTemplate
                        .replace("{{doctorName}}", doctorName)
                        .replace("{{setPasswordLink}}", setPasswordLink)
                        .replace("{{doctorEmail}}", toEmail);

                log.debug("✓ HTML template loaded successfully");
            } catch (Exception e) {
                log.warn("⚠ Could not load HTML template, using fallback: {}", e.getMessage());
                htmlContent = getFallbackSetPasswordEmail(doctorName, setPasswordLink, toEmail);
            }

            sendHtmlEmail(toEmail, "Set Your MedVault Password - Action Required", htmlContent);

            log.info("==========================================");
            log.info("✅ Set password email sent successfully!");
            log.info("Recipient: {}", toEmail);
            log.info("==========================================");

        } catch (Exception e) {
            log.error("==========================================");
            log.error("❌ FAILED to send set password email");
            log.error("Recipient: {}", toEmail);
            log.error("Error Type: {}", e.getClass().getSimpleName());
            log.error("Error Message: {}", e.getMessage());
            log.error("==========================================", e);
        }
    }

    @Async
    public void sendAppointmentConfirmationEmail(String toEmail, String patientName,
            String doctorName, String appointmentDateTime) {
        try {
            log.info("📧 Sending appointment confirmation email to: {}", toEmail);

            String dashboardLink = "http://localhost:3000/patient/dashboard";

            String htmlContent;
            try {
                ClassPathResource resource = new ClassPathResource("templates/email/appointment-confirmation.html");
                String htmlTemplate = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

                htmlContent = htmlTemplate
                        .replace("{{patientName}}", patientName)
                        .replace("{{doctorName}}", doctorName)
                        .replace("{{appointmentDateTime}}", appointmentDateTime)
                        .replace("{{specialization}}", "General Medicine")
                        .replace("{{dashboardLink}}", dashboardLink);
            } catch (Exception e) {
                log.warn("⚠ Using fallback email template for appointment confirmation");
                htmlContent = getFallbackAppointmentEmail(patientName, doctorName, appointmentDateTime, dashboardLink);
            }

            sendHtmlEmail(toEmail, "Appointment Confirmation - MedVault", htmlContent);
            log.info("✅ Appointment confirmation email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send appointment confirmation email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendAppointmentStatusEmail(String toEmail, String patientName,
            String status, String doctorName,
            String appointmentDateTime) {
        try {
            log.info("📧 Sending appointment status email to: {}", toEmail);

            String subject = "Appointment " + status + " - MedVault";
            String statusEmoji = status.equals("APPROVED") ? "✅" : "❌";
            String statusColor = status.equals("APPROVED") ? "#43e97b" : "#ff6b6b";

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f7fa; padding: 20px; margin: 0; }
                                    .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                                    .header { background: linear-gradient(135deg, %s 0%%, %s 100%%); color: white; padding: 40px 30px; text-align: center; }
                                    .content { padding: 40px 30px; }
                                    .status-badge { background-color: %s; color: white; padding: 10px 20px; border-radius: 20px; display: inline-block; font-weight: 600; }
                                    .details { background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }
                                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef; font-size: 14px; color: #6c757d; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h1>%s Appointment %s</h1>
                                    </div>
                                    <div class="content">
                                        <h2>Hello %s,</h2>
                                        <p>Your appointment status has been updated.</p>
                                        <div style="text-align: center; margin: 30px 0;">
                                            <span class="status-badge">%s %s</span>
                                        </div>
                                        <div class="details">
                                            <p><strong>👨‍⚕️ Doctor:</strong> %s</p>
                                            <p><strong>📅 Date & Time:</strong> %s</p>
                                        </div>
                                        <p>Thank you for using MedVault. </p>
                                    </div>
                                    <div class="footer">
                                        <p><strong>MedVault</strong> - Your Health, Our Priority</p>
                                        <p>© 2025 MedVault. All rights reserved. </p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    statusColor, statusColor, statusColor,
                    statusEmoji, status,
                    patientName,
                    statusEmoji, status,
                    doctorName, appointmentDateTime);

            sendHtmlEmail(toEmail, subject, htmlContent);
            log.info("✅ Appointment status email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ Failed to send appointment status email to: {}", toEmail, e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        try {
            log.debug("Creating MimeMessage.. .");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set sender with custom name "MedVault"
            helper.setFrom(new InternetAddress(fromEmail, "MedVault"));

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.debug("Sending email via SMTP...");
            mailSender.send(message);
            log.debug("✓ Email sent via SMTP successfully");

        } catch (MessagingException e) {
            log.error("❌ MessagingException while sending email");
            log.error("Error details: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error while sending email");
            log.error("Error details: {}", e.getMessage());
            throw new MessagingException("Failed to send email", e);
        }
    }

    // Fallback email templates with enhanced button styling
    private String getFallbackSetPasswordEmail(String doctorName, String setPasswordLink, String doctorEmail) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; padding: 0; margin: 0; background-color: #f5f7fa; }
                                .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                                .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 40px 30px; text-align: center; }
                                .header h1 { margin: 0; font-size: 28px; font-weight: 700; }
                                .content { padding: 40px 30px; }
                                .button { display: inline-block; padding: 18px 48px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white ! important; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 18px; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4); transition: all 0.3s ease; }
                                .button:hover { box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6); transform: translateY(-2px); }
                                .info-box { background-color: #e7f3ff; border-left: 4px solid #2196F3; padding: 20px; margin: 20px 0; border-radius: 4px; }
                                .warning { background-color: #fff3cd; border-left-color: #ffc107; color: #856404; }
                                .footer { background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef; font-size: 14px; color: #6c757d; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>🏥 Welcome to MedVault</h1>
                                    <p style="margin: 10px 0 0 0; opacity: 0.9;">Your Health, Our Priority</p>
                                </div>
                                <div class="content">
                                    <h2 style="color: #333; margin-bottom: 20px;">Hello Dr. %s,</h2>
                                    <p>Welcome to MedVault! Your account has been successfully created by our administrator.</p>
                                    <p>To get started, please click the button below to set up your password and activate your account:</p>
                                    <div style="text-align: center; margin: 30px 0;">
                                        <a href="%s" class="button">Set Your Password</a>
                                    </div>
                                    <div class="info-box">
                                        <p style="margin: 8px 0;"><strong>📧 Your Account Email:</strong> %s</p>
                                        <p style="margin: 8px 0;"><strong>⏰ Link Validity:</strong> 24 hours from now</p>
                                    </div>
                                    <div class="info-box warning">
                                        <p style="margin: 8px 0;"><strong>⚠️ Important:</strong></p>
                                        <ul style="margin: 10px 0; padding-left: 20px;">
                                            <li>This link will expire in 24 hours for security reasons</li>
                                            <li>If the link expires, please contact your administrator</li>
                                            <li>Never share your password with anyone</li>
                                        </ul>
                                    </div>
                                </div>
                                <div class="footer">
                                    <p><strong>MedVault</strong> - Personal Electronic Health Record System</p>
                                    <p>© 2025 MedVault. All rights reserved. </p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                doctorName, setPasswordLink, doctorEmail);
    }

    private String getFallbackAppointmentEmail(String patientName, String doctorName,
            String appointmentDateTime, String dashboardLink) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; padding: 0; margin: 0; background-color: #f5f7fa; }
                                .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                                .header { background: linear-gradient(135deg, #4facfe 0%%, #00f2fe 100%%); color: white; padding: 40px 30px; text-align: center; }
                                .content { padding: 40px 30px; }
                                .details { background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }
                                .button { display: inline-block; padding: 18px 48px; background: linear-gradient(135deg, #4facfe 0%%, #00f2fe 100%%); color: white !important; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 18px; box-shadow: 0 4px 15px rgba(79, 172, 254, 0.4); }
                                .footer { background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef; font-size: 14px; color: #6c757d; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1 style="margin: 0;">📅 Appointment Confirmed! </h1>
                                </div>
                                <div class="content">
                                    <h2>Hello %s,</h2>
                                    <p>Great news! Your appointment has been successfully booked.</p>
                                    <div class="details">
                                        <p><strong>👨‍⚕️ Doctor:</strong> %s</p>
                                        <p><strong>📅 Date & Time:</strong> %s</p>
                                        <p><strong>📝 Status:</strong> <span style="color: #ffc107; font-weight: 600;">⏳ Pending Approval</span></p>
                                    </div>
                                    <div style="text-align: center;">
                                        <a href="%s" class="button">View Dashboard</a>
                                    </div>
                                </div>
                                <div class="footer">
                                    <p><strong>MedVault</strong> - Your Health, Our Priority</p>
                                    <p>© 2025 MedVault.  All rights reserved.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                patientName, doctorName, appointmentDateTime, dashboardLink);
    }

    @Async
    public void sendPaymentConfirmationEmail(String toEmail, String patientName, String doctorName,
            Double amount, String appointmentDateTime) {
        try {
            log.info("📧 Sending payment confirmation email to: {}", toEmail);

            String dashboardLink = "http://localhost:3000/patient/dashboard";

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f7fa; padding: 20px; margin: 0; }
                                    .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                                    .header { background: linear-gradient(135deg, #43e97b 0%%, #38f9d7 100%%); color: white; padding: 40px 30px; text-align: center; }
                                    .content { padding: 40px 30px; }
                                    .button { display: inline-block; padding: 18px 48px; background: linear-gradient(135deg, #43e97b 0%%, #38f9d7 100%%); color: white !important; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 18px; box-shadow: 0 4px 15px rgba(67, 233, 123, 0.4); }
                                    .details { background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }
                                    .amount-box { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; }
                                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef; font-size: 14px; color: #6c757d; }
                                    .icon { font-size: 48px; margin: 20px 0; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <div class="icon">✅</div>
                                        <h1>Payment Successful!</h1>
                                    </div>
                                    <div class="content">
                                        <h2>Hello %s,</h2>
                                        <p>Your payment for the consultation has been successfully processed!</p>
                                        <div class="amount-box">
                                            <h3 style="margin: 0; font-size: 24px;">Amount Paid</h3>
                                            <h1 style="margin: 10px 0; font-size: 48px;">₹%.2f</h1>
                                        </div>
                                        <div class="details">
                                            <p><strong>👨‍⚕️ Doctor:</strong> %s</p>
                                            <p><strong>📅 Appointment Date:</strong> %s</p>
                                            <p><strong>✅ Payment Status:</strong> <span style="color: #43e97b; font-weight: 600;">Completed</span></p>
                                        </div>
                                        <p>Your appointment is confirmed and pending doctor approval. You will receive a notification once the doctor reviews your booking.</p>
                                        <div style="text-align: center;">
                                            <a href="%s" class="button">View Appointment</a>
                                        </div>
                                        <p style="margin-top: 30px; font-size: 14px; color: #6c757d;">
                                            Thank you for choosing MedVault. If you have any questions, please contact our support team.
                                        </p>
                                    </div>
                                    <div class="footer">
                                        <p><strong>MedVault</strong> - Your Health, Our Priority</p>
                                        <p>© 2025 MedVault. All rights reserved.</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    patientName, amount, doctorName, appointmentDateTime, dashboardLink);

            sendHtmlEmail(toEmail, "💰 Payment Successful - MedVault", htmlContent);
            log.info("✅ Payment confirmation email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send payment confirmation email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendDoctorNewBookingEmail(String toEmail, String doctorName, String patientName,
            String appointmentDateTime, String reasonForVisit) {
        try {
            log.info("📧 Sending new booking notification email to doctor: {}", toEmail);

            String appointmentsLink = "http://localhost:3000/doctor/appointments";

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f7fa; padding: 20px; margin: 0; }
                                    .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 40px 30px; text-align: center; }
                                    .content { padding: 40px 30px; }
                                    .button { display: inline-block; padding: 18px 48px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white !important; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 18px; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4); }
                                    .details { background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }
                                    .alert-box { background: linear-gradient(135deg, #feca57 0%%, #ff9ff3 100%%); padding: 15px; border-radius: 8px; margin: 20px 0; text-align: center; color: #333; font-weight: 600; }
                                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef; font-size: 14px; color: #6c757d; }
                                    .icon { font-size: 48px; margin: 20px 0; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <div class="icon">🔔</div>
                                        <h1>New Appointment Request</h1>
                                    </div>
                                    <div class="content">
                                        <h2>Hello Dr. %s,</h2>
                                        <p>You have received a new appointment booking from a patient.</p>
                                        <div class="alert-box">
                                            ⏳ Pending Your Approval
                                        </div>
                                        <div class="details">
                                            <p><strong>👤 Patient Name:</strong> %s</p>
                                            <p><strong>📅 Appointment Date:</strong> %s</p>
                                            <p><strong>📝 Reason for Visit:</strong> %s</p>
                                        </div>
                                        <p>Please review the appointment details and take appropriate action (Approve/Reject).</p>
                                        <div style="text-align: center;">
                                            <a href="%s" class="button">Review Appointment</a>
                                        </div>
                                        <p style="margin-top: 30px; font-size: 14px; color: #6c757d;">
                                            Timely response helps patients receive care faster. Thank you for your dedication.
                                        </p>
                                    </div>
                                    <div class="footer">
                                        <p><strong>MedVault</strong> - Your Health, Our Priority</p>
                                        <p>© 2025 MedVault. All rights reserved.</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    doctorName, patientName, appointmentDateTime, reasonForVisit, appointmentsLink);

            sendHtmlEmail(toEmail, "🔔 New Appointment Request - MedVault", htmlContent);
            log.info("✅ New booking notification email sent to doctor: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send new booking email to doctor: {}", toEmail, e);
        }
    }

    @Async
    public void sendFeedbackRequestEmail(String toEmail, String patientName, String doctorName, Long appointmentId) {
        try {
            log.info("📧 Sending feedback request email to: {}", toEmail);

            String feedbackLink = "http://localhost:3000/patient/feedback";

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f7fa; padding: 20px; margin: 0; }
                                    .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 40px 30px; text-align: center; }
                                    .content { padding: 40px 30px; }
                                    .button { display: inline-block; padding: 18px 48px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white !important; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 18px; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4); }
                                    .details { background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }
                                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef; font-size: 14px; color: #6c757d; }
                                    .icon { font-size: 48px; margin: 20px 0; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <div class="icon">⭐</div>
                                        <h1>Appointment Completed!</h1>
                                    </div>
                                    <div class="content">
                                        <h2>Hello %s,</h2>
                                        <p>Your appointment with <strong>%s</strong> has been successfully completed!</p>
                                        <p>We value your experience and would love to hear your feedback. Your review helps us maintain quality care and assists other patients in making informed decisions.</p>
                                        <div class="details">
                                            <p><strong>👨‍⚕️ Doctor:</strong> %s</p>
                                            <p><strong>✅ Status:</strong> <span style="color: #43e97b; font-weight: 600;">Completed</span></p>
                                        </div>
                                        <p style="font-weight: 600; color: #667eea; margin-top: 25px;">Your feedback matters!</p>
                                        <p>Please take a moment to share your experience and rate your consultation.</p>
                                        <div style="text-align: center;">
                                            <a href="%s" class="button">Leave Feedback</a>
                                        </div>
                                        <p style="margin-top: 30px; font-size: 14px; color: #6c757d;">
                                            Thank you for choosing MedVault for your healthcare needs. We appreciate your trust in our services.
                                        </p>
                                    </div>
                                    <div class="footer">
                                        <p><strong>MedVault</strong> - Your Health, Our Priority</p>
                                        <p>© 2025 MedVault. All rights reserved.</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    patientName, doctorName, doctorName, feedbackLink);

            sendHtmlEmail(toEmail, "🌟 Share Your Experience - Appointment Completed", htmlContent);
            log.info("✅ Feedback request email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send feedback request email to: {}", toEmail, e);
        }
    }
}