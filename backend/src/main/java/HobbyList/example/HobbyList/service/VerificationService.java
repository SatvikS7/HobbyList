package HobbyList.example.HobbyList.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;

import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.model.VerificationToken;
import HobbyList.example.HobbyList.repository.TokenRepository;

@Service
public class VerificationService {
    private final TokenRepository tokenRepository;
    private final Resend resend;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    public VerificationService(TokenRepository tokenRepository, Resend resend) {
        this.tokenRepository = tokenRepository;
        this.resend = resend;
    }

    public void sendVerificationEmail(User user, String type) {
        // Generate verification token
        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken(token, user, type);
        tokenRepository.save(verificationToken);
        String compositeUrl = "";

        if (type.equals("EMAIL_VERIFICATION")) {
            compositeUrl = frontendUrl + "verification?token=" + token;
        } else if (type.equals("PASSWORD_RESET")) {
            compositeUrl = frontendUrl + "reset-password?token=" + token;
        }

        String subject = "";
        String htmlContent = "";

        if (type.equals("EMAIL_VERIFICATION")) {
            subject = "Verify your email";
            htmlContent = "<p>Click the link to verify your account: <a href=\"" + compositeUrl + "\">" + compositeUrl
                    + "</a></p>";
        } else if (type.equals("PASSWORD_RESET")) {
            subject = "Reset your password";
            htmlContent = "<p>Click the link to reset your password: <a href=\"" + compositeUrl + "\">" + compositeUrl
                    + "</a></p>";
        }

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("HobbyList <onboarding@resend.dev>")
                .to("hobbylistproj@gmail.com" /* user.getEmail() */)
                .subject(subject)
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
