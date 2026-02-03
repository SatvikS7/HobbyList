package HobbyList.example.HobbyList.controller;

import HobbyList.example.HobbyList.dto.LoginRequest;
import HobbyList.example.HobbyList.dto.SignupRequest;
import HobbyList.example.HobbyList.dto.VerificationEmailEvent;
import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.model.VerificationToken;
import HobbyList.example.HobbyList.repository.TokenRepository;
import HobbyList.example.HobbyList.repository.UserRepository;
import HobbyList.example.HobbyList.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.event.ApplicationEvents;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@RecordApplicationEvents
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenRepository tokenRepository;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEvents applicationEvents;

    private User testUser;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole("ROLE_USER");
        testUser.setActive(true);

        signupRequest = new SignupRequest("test@example.com", "password123");
        loginRequest = new LoginRequest("test@example.com", "password123");
    }

    @Test
    void testSignup_Success() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(userRepository).save(any(User.class));
        assertThat(applicationEvents.stream(VerificationEmailEvent.class).count())
                .isEqualTo(1);
    }

    @Test
    void testSignup_EmailInUse_Enabled() throws Exception {
        testUser.setActive(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email in use"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testSignup_EmailInUse_Disabled_ResendEmail() throws Exception {
        User disabledUser = spy(new User());
        disabledUser.setEmail("test@example.com");
        disabledUser.setActive(false);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(disabledUser));
        when(disabledUser.isActive()).thenReturn(false);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification email resent"));

        assertThat(applicationEvents.stream(VerificationEmailEvent.class).count())
                .isEqualTo(1);
    }

    @Test
    void testLogin_Success() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void testLogin_AccountNotActivated() throws Exception {
        testUser.setActive(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Account not activated"));
    }

    @Test
    void testVerifyUser_Success() throws Exception {
        String token = "valid-token";
        VerificationToken verificationToken = new VerificationToken(token, testUser, "EMAIL_VERIFICATION");

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(get("/api/auth/verify")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account verified successfully"));

        verify(tokenRepository).delete(verificationToken);
        verify(userRepository).save(testUser);
    }

    @Test
    void testVerifyUser_InvalidToken() throws Exception {
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/verify")
                .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid verification token"));
    }
}
