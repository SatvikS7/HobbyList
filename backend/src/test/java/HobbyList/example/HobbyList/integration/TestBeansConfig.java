package HobbyList.example.HobbyList.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

import com.resend.Resend;

@TestConfiguration
public class TestBeansConfig {
    @Bean
    @Primary
    public Resend resend() {
        return mock(Resend.class);
    }
}
