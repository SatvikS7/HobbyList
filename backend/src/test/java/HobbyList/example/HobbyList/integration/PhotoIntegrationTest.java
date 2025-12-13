package HobbyList.example.HobbyList.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import HobbyList.example.HobbyList.dto.PhotoDto;
import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.service.JwtService;
import HobbyList.example.HobbyList.service.S3Service;
import java.time.LocalDateTime;

public class PhotoIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private S3Service s3Service;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() {
        photoRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("photoUser");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setEmail("p@example.com");
        testUser.setDisplayName("Photographer");
        testUser.setRole("ROLE_USER");
        testUser = userRepository.save(testUser);

        token = jwtService.generateToken(testUser);

        // Mock S3
        when(s3Service.generateUploadUrl(anyString(), anyString(), anyString())).thenReturn("https://s3.fake/upload");
        when(s3Service.generateDownloadUrl(anyString(), any())).thenReturn("https://s3.fake/download");
    }

    @Test
    void shouldSavePhotoMetadata() throws Exception {
        PhotoDto photoDto = new PhotoDto(1L, "test-photo.jpg", "", "A test photo", LocalDateTime.now());

        // The save-url endpoint likely takes a PhotoDto or similar
        mockMvc.perform(post("/api/photos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(photoDto)))
                .andExpect(status().isOk());
    }
}
