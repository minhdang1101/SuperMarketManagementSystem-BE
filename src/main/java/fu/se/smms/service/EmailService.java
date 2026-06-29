package fu.se.smms.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetLink);
}
