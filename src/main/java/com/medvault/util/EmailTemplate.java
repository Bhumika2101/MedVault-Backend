package com.medvault.util;

public class EmailTemplate {
    
    public static String getSetPasswordTemplate(String doctorName, String setPasswordLink) {
        return """
                <! DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 20px; }
                        .button { background-color: #4CAF50; color: white; padding: 12px 24px; text-decoration: none; 
                                  border-radius: 4px; display: inline-block; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Welcome to MedVault</h1>
                        </div>
                        <div class="content">
                            <h2>Hello Dr. %s,</h2>
                            <p>Your account has been created successfully by the administrator. </p>
                            <p>Please click the button below to set your password and activate your account:</p>
                            <a href="%s" class="button">Set Password</a>
                            <p>This link will expire in 24 hours.</p>
                            <p>If you didn't request this, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 MedVault. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(doctorName, setPasswordLink);
    }
    
    public static String getAppointmentConfirmationTemplate(String patientName, String doctorName, 
                                                           String appointmentDateTime) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 20px; }
                        .details { background-color: white; padding: 15px; border-left: 4px solid #2196F3; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Appointment Confirmation</h1>
                        </div>
                        <div class="content">
                            <h2>Hello %s,</h2>
                            <p>Your appointment has been booked successfully! </p>
                            <div class="details">
                                <p><strong>Doctor:</strong> %s</p>
                                <p><strong>Date & Time:</strong> %s</p>
                                <p><strong>Status:</strong> Pending Approval</p>
                            </div>
                            <p>You will receive a notification once the doctor approves your appointment.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 MedVault. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(patientName, doctorName, appointmentDateTime);
    }
}