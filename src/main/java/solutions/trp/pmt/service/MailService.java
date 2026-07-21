package solutions.trp.pmt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public MailService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.username:}") String from,
            @Value("${pmt.mail.enabled:false}") boolean enabled
    ) {
        this.mailSender = mailSender.getIfAvailable();
        this.from = from;
        this.enabled = enabled;
    }

    public boolean sendPasswordReset(String to, String resetLink) {
        if(!enabled || mailSender == null || to == null || to.isBlank()) {
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            if(from != null && !from.isBlank()) {
                message.setFrom(from);
            }
            message.setSubject("Password reset");
            message.setText("Use this link to reset your password. It expires in 30 minutes:\n\n" + resetLink);

            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            System.err.println("Password reset email failed: " + ex.getMessage());
            return false;
        }
    }
}
