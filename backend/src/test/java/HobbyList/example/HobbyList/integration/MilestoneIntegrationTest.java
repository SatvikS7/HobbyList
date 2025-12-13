package HobbyList.example.HobbyList.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import HobbyList.example.HobbyList.dto.MilestoneDto;
import HobbyList.example.HobbyList.model.Milestone;
import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.service.JwtService;

import java.time.LocalDateTime;
import java.util.Collections;

public class MilestoneIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        milestoneRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user
        testUser = new User();
        testUser.setUsername("milestoneUser");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setEmail("m@example.com");
        testUser.setDisplayName("Milester");
        testUser.setRole("ROLE_USER");
        testUser = userRepository.save(testUser);

        token = jwtService.generateToken(testUser);
    }

    @Test
    void shouldCreateMilestone() throws Exception {
        MilestoneDto milestoneDto = new MilestoneDto("My First Milestone");

        mockMvc.perform(post("/api/milestones")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(milestoneDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.task").value("My First Milestone"));
    }

    @Test
    void shouldUpdateMilestoneStatus() throws Exception {
        // Create milestone first
        MilestoneDto mDto = new MilestoneDto("To Complete");
        MvcResult res = mockMvc.perform(post("/api/milestones")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String json = res.getResponse().getContentAsString();
        MilestoneDto created = objectMapper.readValue(json, MilestoneDto.class);

        // Update
        MilestoneDto mDto_updated = new MilestoneDto("To Complete", true);

        mockMvc.perform(patch("/api/milestones/" + created.id())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mDto_updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }
}
