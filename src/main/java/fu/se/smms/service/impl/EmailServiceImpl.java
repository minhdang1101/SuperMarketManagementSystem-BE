package fu.se.smms.service.impl;

import fu.se.smms.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public boolean sendPasswordResetEmail(String to, String resetLink) {
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("MAIL_USERNAME is not configured; password reset email was not sent to {}", to);
            return false;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Đặt lại mật khẩu");

            String htmlMsg = "<p>Hello,</p>"
                    + "<p>You have requested to reset your password.</p>"
                    + "<p>Click the link below to change your password:</p>"
                    + "<p><a href=\"" + resetLink + "\">Change my password</a></p>"
                    + "<br>"
                    + "<p>Ignore this email if you do remember your password, "
                    + "or you have not made the request.</p>";

            helper.setText(htmlMsg, true);
            javaMailSender.send(message);
            log.info("Password reset email sent to {}", to);
            return true;
        } catch (MessagingException | RuntimeException e) {
            log.error("Failed to send email to {}", to, e);
            return false;
        }
    }
}
