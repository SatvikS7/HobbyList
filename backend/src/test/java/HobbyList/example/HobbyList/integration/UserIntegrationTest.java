package HobbyList.example.HobbyList.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.service.JwtService;

public class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        me = new User();
        me.setUsername("me");
        me.setPassword(passwordEncoder.encode("pw"));
        me.setEmail("me@ex.com");
        me.setDisplayName("Me Myself");
        me.setRole("ROLE_USER");
        me = userRepository.save(me);

        token = jwtService.generateToken(me);

        other = new User();
        other.setUsername("other");
        other.setPassword(passwordEncoder.encode("pw"));
        other.setEmail("other@ex.com");
        other.setDisplayName("Other Person");
        other.setRole("ROLE_USER");
        other = userRepository.save(other);
    }

    @Test
    void shouldGetOwnProfile() throws Exception {
        mockMvc.perform(get("/api/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Me Myself"));
    }

    @Test
    void shouldSearchUsers() throws Exception {
        mockMvc.perform(get("/api/profile/" + other.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Other Person"));
    }
}
