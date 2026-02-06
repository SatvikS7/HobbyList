package HobbyList.example.HobbyList.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;

import com.resend.Resend;

@Configuration
@Profile("!test")
public class ResendConfig {
    @Bean
    public Resend resend(@Value("${resend.api.key}") String apiKey) {
        return new Resend(apiKey);
    }
}