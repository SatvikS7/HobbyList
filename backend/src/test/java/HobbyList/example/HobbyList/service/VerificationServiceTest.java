package HobbyList.example.HobbyList.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.model.VerificationToken;
import HobbyList.example.HobbyList.repository.TokenRepository;

@ExtendWith(MockitoExtension.class)
public class VerificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verificationService, "frontendUrl", "http://localhost:3000/");
    }

    /////////////////////////////////
    // sendVerificationEmail Tests //
    /////////////////////////////////

    @Test
    void sendVerificationEmail_ShouldSendEmailVerification() {
        User user = new User();
        user.setEmail("test@example.com");

        verificationService.sendVerificationEmail(user, "EMAIL_VERIFICATION");

        // Verify token saved
        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        VerificationToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertEquals("EMAIL_VERIFICATION", savedToken.getType());
        assertNotNull(savedToken.getToken());

        // Verify email sent
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        SimpleMailMessage sentMail = mailCaptor.getValue();
        assertEquals("test@example.com", sentMail.getTo()[0]);
        assertEquals("Verify your email", sentMail.getSubject());
        assertTrue(sentMail.getText().contains("verification?token=" + savedToken.getToken()));
        assertTrue(sentMail.getText().contains("http://localhost:3000/"));
    }

    @Test
    void sendVerificationEmail_ShouldSendPasswordReset() {
        User user = new User();
        user.setEmail("test@example.com");

        verificationService.sendVerificationEmail(user, "PASSWORD_RESET");

        // Verify token saved
        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        VerificationToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertEquals("PASSWORD_RESET", savedToken.getType());

        // Verify email sent
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        SimpleMailMessage sentMail = mailCaptor.getValue();
        assertEquals("Reset your password", sentMail.getSubject());
        assertTrue(sentMail.getText().contains("reset-password?token=" + savedToken.getToken()));
        assertTrue(sentMail.getText().contains("http://localhost:3000/"));
    }
}
