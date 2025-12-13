package HobbyList.example.HobbyList.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import HobbyList.example.HobbyList.repository.FollowRequestRepository;
import HobbyList.example.HobbyList.repository.MilestoneRepository;
import HobbyList.example.HobbyList.repository.PhotoRepository;
import HobbyList.example.HobbyList.repository.TokenRepository;
import HobbyList.example.HobbyList.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MilestoneRepository milestoneRepository;

    @Autowired
    protected PhotoRepository photoRepository;

    @Autowired
    protected FollowRequestRepository followRequestRepository;

    @Autowired
    protected TokenRepository tokenRepository;
}
