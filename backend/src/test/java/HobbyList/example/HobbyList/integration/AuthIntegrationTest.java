package HobbyList.example.HobbyList.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import HobbyList.example.HobbyList.dto.LoginRequest;
import HobbyList.example.HobbyList.dto.SignupRequest;

public class AuthIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldSignUpAndLogin() throws Exception {

        // 1. sign up using SignupRequest DTO
        SignupRequest signup = new SignupRequest(
                "test@example.com",
                "password123");

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk());

        // manually activate user so login can succeed
        var user = userRepository.findByEmail("test@example.com").get();
        user.setActive(true);
        userRepository.save(user);

        // 2. login using LoginRequest DTO
        LoginRequest login = new LoginRequest(
                "test@example.com",
                "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void shouldFailLoginWithWrongPassword() throws Exception {

        // create user directly
        SignupRequest signup = new SignupRequest(
                "wrongtest@example.com",
                "password123");
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk());

        // activate user
        var user = userRepository.findByEmail("wrongtest@example.com").get();
        user.setActive(true);
        userRepository.save(user);

        // send wrong password
        LoginRequest badLogin = new LoginRequest(
                "wrongtest@example.com",
                "incorrectPass");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }
}
