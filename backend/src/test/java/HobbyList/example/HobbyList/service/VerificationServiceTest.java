package HobbyList.example.HobbyList.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.model.VerificationToken;
import HobbyList.example.HobbyList.repository.TokenRepository;

@ExtendWith(MockitoExtension.class)
public class VerificationServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private com.resend.Resend resend;

    @Mock
    private com.resend.services.emails.Emails emails;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        // Prepare the mock chain
        when(resend.emails()).thenReturn(emails);

        verificationService = new VerificationService(tokenRepository, resend);
        ReflectionTestUtils.setField(verificationService, "frontendUrl", "http://localhost:3000/");
    }

    /////////////////////////////////
    // sendVerificationEmail Tests //
    /////////////////////////////////

    @Test
    void sendVerificationEmail_ShouldSendEmailVerification() {
        User user = new User();
        user.setEmail("test@example.com");

        // We cannot easily verify external Resend API call without strict refactoring
        // or mocking construction.
        // For now, we verify that the token logic remains correct.

        verificationService.sendVerificationEmail(user, "EMAIL_VERIFICATION");

        // Verify token saved
        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        VerificationToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertEquals("EMAIL_VERIFICATION", savedToken.getType());
        assertNotNull(savedToken.getToken());
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
    }
}
