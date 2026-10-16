package com.oskarro.oskarito.user;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    public boolean configured() {
        return !value("MAIL_HOST").isBlank()
                && !value("MAIL_USERNAME").isBlank()
                && !value("MAIL_PASSWORD").isBlank();
    }

    public boolean send(String to, String subject, String text) {
        if (!configured() || to == null || to.isBlank()) {
            return false;
        }
        String from = value("MAIL_FROM");
        if (from.isBlank()) {
            from = value("MAIL_USERNAME");
        }
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(value("MAIL_HOST"));
            sender.setPort(port());
            sender.setUsername(value("MAIL_USERNAME"));
            sender.setPassword(value("MAIL_PASSWORD"));
            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");
            if (port() == 465) {
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
            }
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(text, false);
            sender.send(message);
            return true;
        } catch (Exception ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            log.warn("SMTP did not accept the message ({})", cause.getClass().getSimpleName());
            return false;
        }
    }

    private int port() {
        String raw = value("MAIL_PORT");
        if (raw.isBlank()) {
            return 587;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 587;
        }
    }

    private String value(String name) {
        String raw = System.getenv(name);
        return raw == null ? "" : raw.trim();
    }
}
