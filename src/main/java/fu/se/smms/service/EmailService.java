package fu.se.smms.service;

public interface EmailService {
    boolean sendPasswordResetEmail(String to, String resetLink);
}
