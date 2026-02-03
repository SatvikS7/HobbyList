package HobbyList.example.HobbyList.controller;

import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.model.VerificationToken;
import HobbyList.example.HobbyList.dto.LoginRequest;
import HobbyList.example.HobbyList.dto.SignupRequest;
import HobbyList.example.HobbyList.dto.VerificationEmailEvent;
import HobbyList.example.HobbyList.repository.TokenRepository;
import HobbyList.example.HobbyList.repository.UserRepository;
import HobbyList.example.HobbyList.service.JwtService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AuthController(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenRepository tokenRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        // Check if email already exists
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user != null) {
            if (user.isActive()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email in use"));
            } else {
                // Resend Email
                eventPublisher.publishEvent(new VerificationEmailEvent(user, "EMAIL_VERIFICATION"));
                return ResponseEntity.ok(Map.of("message", "Verification email resent"));
            }
        }

        // Encode password and save user
        User newUser = new User();
        newUser.setEmail(request.email());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setRole("ROLE_USER");
        userRepository.save(newUser);
        eventPublisher.publishEvent(new VerificationEmailEvent(newUser, "EMAIL_VERIFICATION"));
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.email());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }
        User user = optionalUser.get();
        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Account not activated"));
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }
        boolean newAcct = user.isNewAccount();
        user.setOld();
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(Map.of("token", token, "newAccount", newAcct));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestParam("token") String token) {
        Optional<VerificationToken> verificationToken = tokenRepository.findByToken(token);

        if (verificationToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification token"));
        }

        User user = verificationToken.get().getUser();
        // User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        user.setActive(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken.get());
        return ResponseEntity.ok(Map.of("message", "Account verified successfully"));
    }
}