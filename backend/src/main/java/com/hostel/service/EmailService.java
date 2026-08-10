package com.hostel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@hostel.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("[EMAIL DISABLED] To: {}, Subject: {}, Body: {}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {} - {}", to, e.getMessage());
        }
    }

    public void sendLeaveApproval(String to, String studentName, String status, String fromDate, String toDate, String remarks) {
        String subject = "Leave Request " + status;
        String body = String.format("""
                Dear %s,
                
                Your leave request from %s to %s has been %s.
                
                Remarks: %s
                
                Best regards,
                Hostel Management
                """, studentName, fromDate, toDate, status, remarks != null ? remarks : "N/A");
        sendEmail(to, subject, body);
    }

    public void sendComplaintUpdate(String to, String studentName, String complaintTitle, String status) {
        String subject = "Complaint Status Update";
        String body = String.format("""
                Dear %s,
                
                Your complaint "%s" status has been updated to: %s.
                
                Best regards,
                Hostel Management
                """, studentName, complaintTitle, status);
        sendEmail(to, subject, body);
    }

    public void sendNewNotice(String to, String title, String content) {
        String subject = "New Notice: " + title;
        String body = String.format("""
                A new notice has been posted:
                
                Title: %s
                Content: %s
                
                Best regards,
                Hostel Management
                """, title, content);
        sendEmail(to, subject, body);
    }

    public void sendRoomAllocation(String to, String studentName, String roomNo, String blockName) {
        String subject = "Room Allocation Update";
        String body = String.format("""
                Dear %s,
                
                Your room has been allocated as follows:
                Room: %s
                Block: %s
                
                Best regards,
                Hostel Management
                """, studentName, roomNo, blockName);
        sendEmail(to, subject, body);
    }
}
