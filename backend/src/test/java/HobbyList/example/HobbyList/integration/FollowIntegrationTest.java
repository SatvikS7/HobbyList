package HobbyList.example.HobbyList.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.service.JwtService;

public class FollowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private User follower;
    private User publicTarget;
    private User privateTarget;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        followRequestRepository.deleteAll();
        userRepository.deleteAll();

        follower = new User();
        follower.setUsername("follower");
        follower.setPassword(passwordEncoder.encode("pw"));
        follower.setEmail("f@ex.com");
        follower.setDisplayName("Follower");
        follower.setRole("ROLE_USER");
        follower = userRepository.save(follower);

        token = jwtService.generateToken(follower);

        publicTarget = new User();
        publicTarget.setUsername("public");
        publicTarget.setPassword(passwordEncoder.encode("pw"));
        publicTarget.setEmail("pub@ex.com");
        publicTarget.setDisplayName("Public User");
        publicTarget.setPrivate(false);
        publicTarget.setRole("ROLE_USER");
        publicTarget = userRepository.save(publicTarget);

        privateTarget = new User();
        privateTarget.setUsername("private");
        privateTarget.setPassword(passwordEncoder.encode("pw"));
        privateTarget.setEmail("priv@ex.com");
        privateTarget.setDisplayName("Private User");
        privateTarget.setPrivate(true);
        privateTarget.setRole("ROLE_USER");
        privateTarget = userRepository.save(privateTarget);
    }

    @Test
    void shouldFollowPublicUserImmediately() throws Exception {
        mockMvc.perform(post("/api/users/" + publicTarget.getId() + "/follow")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        // Assert state in DB or response if possible
    }

    @Test
    void shouldCreateRequestForPrivateUser() throws Exception {
        mockMvc.perform(post("/api/users/" + privateTarget.getId() + "/follow")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        // Should verify that a request was created, not immediate follow
    }
}
