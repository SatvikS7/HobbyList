package HobbyList.example.HobbyList.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import HobbyList.example.HobbyList.model.User;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    private String SECRET_KEY = "myVeryLongAndSecureSecretKeyThatIsAtLeast256BitsLongForHS256AlgorithmSecurity";
    private long EXPIRATION_TIME = 86400000;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");

        jwtService = new JwtService(
                SECRET_KEY,
                EXPIRATION_TIME);
    }

    /////////////////////////
    // generateToken Tests //
    /////////////////////////

    // ----------------------------------------------
    // 1. user exists -> generate token
    // ----------------------------------------------
    @Test
    void generateToken_ShouldGenerateToken_WhenUserIsNotNull() {
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    // ----------------------------------------------
    // 2. user exists -> generate token with correct subject
    // ----------------------------------------------
    @Test
    void generateToken_ShouldContainCorrectSubject() {
        String token = jwtService.generateToken(user);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("testuser@example.com", claims.getSubject());
    }

    // ----------------------------------------------
    // 3. user exists -> generate token with correct issuedAt and expiration
    // ----------------------------------------------
    @Test
    void generateToken_ShouldSetIssuedAtAndExpirationCorrectly() {
        long beforeCreation = System.currentTimeMillis();
        String token = jwtService.generateToken(user);
        long afterCreation = System.currentTimeMillis();

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        // issuedAt is between before and after creation
        assertTrue(claims.getIssuedAt().getTime() / 1000 >= beforeCreation / 1000);
        assertTrue(claims.getIssuedAt().getTime() / 1000 <= afterCreation / 1000);

        // expiration = issuedAt + EXPIRATION_TIME
        long expectedExpiration = claims.getIssuedAt().getTime() + EXPIRATION_TIME;
        assertEquals(expectedExpiration, claims.getExpiration().getTime(), 1000); // allow 1s drift
    }

    // ----------------------------------------------
    // 4. user exists -> generate token with correct signature
    // ----------------------------------------------
    @Test
    void generateToken_ShouldCreateAParsableJwt() {
        String token = jwtService.generateToken(user);

        assertDoesNotThrow(() -> Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                .build()
                .parseClaimsJws(token));
    }

    // ----------------------------------------------
    // 5. user exists -> throws exception when secret key is invalid
    // ----------------------------------------------
    @Test
    void generateToken_ShouldThrowException_WhenSecretKeyInvalid() {
        JwtService brokenService = new JwtService("short-key", EXPIRATION_TIME);

        assertThrows(RuntimeException.class, () -> brokenService.generateToken(user));
    }

    // ----------------------------------------------
    // 6. user exists with invalid email -> throws exception
    // ----------------------------------------------
    @Test
    void generateToken_ShouldThrowException_WhenUserEmailIsNull() {
        User badUser = new User();
        badUser.setEmail(null);

        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken(badUser));
    }

    /////////////////////////
    // extractEmail Tests //
    /////////////////////////

    // ----------------------------------------------
    // 1. valid token -> returns correct email
    // ----------------------------------------------
    @Test
    void extractEmail_ShouldReturnCorrectEmail_WhenTokenIsValid() {
        String token = jwtService.generateToken(user);
        String extractedEmail = jwtService.extractEmail(token);
        assertEquals(user.getEmail(), extractedEmail);
    }

    // ----------------------------------------------
    // 2. invalid token -> throws exception
    // ----------------------------------------------
    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsInvalid() {
        String invalidToken = "invalidString";
        assertThrows(RuntimeException.class, () -> jwtService.extractEmail(invalidToken));
    }

    /////////////////////////////
    // extractExpiration Tests //
    /////////////////////////////

    // ----------------------------------------------
    // 1. valid token -> returns correct expiration
    // ----------------------------------------------
    @Test
    void extractExpiration_ShouldReturnCorrectExpiration_WhenTokenIsValid() {
        String token = jwtService.generateToken(user);
        java.util.Date expiration = jwtService.extractExpiration(token);
        assertNotNull(expiration);

        long expected = System.currentTimeMillis() + EXPIRATION_TIME;
        // Allow some buffer (e.g. 2 seconds) for execution time difference
        long diff = Math.abs(expiration.getTime() - expected);
        assertTrue(diff < 2000, "Expiration time should be close to expected");
    }

    ////////////////////////
    // extractClaim Tests //
    ////////////////////////

    // ----------------------------------------------
    // 1. extract arbitrary claim (subject)
    // ----------------------------------------------
    @Test
    void extractClaim_ShouldExtractSubject_WhenSelectorPassed() {
        String token = jwtService.generateToken(user);
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        assertEquals(user.getEmail(), subject);
    }

    //////////////////////////
    // isTokenExpired Tests //
    //////////////////////////

    // ----------------------------------------------
    // 1. valid token (not expired) -> returns false
    // ----------------------------------------------
    @Test
    void isTokenExpired_ShouldReturnFalse_WhenTokenIsNotExpired() {
        String token = jwtService.generateToken(user);
        assertFalse(jwtService.isTokenExpired(token));
    }

    // ----------------------------------------------
    // 2. expired token -> returns true
    // ----------------------------------------------
    @Test
    void isTokenExpired_ShouldReturnTrue_WhenTokenIsExpired() {
        // Create service that produces expired tokens immediately
        JwtService expiredService = new JwtService(SECRET_KEY, -10000);
        String expiredToken = expiredService.generateToken(user);

        assertTrue(jwtService.isTokenExpired(expiredToken));
    }

    // ----------------------------------------------
    // 3. invalid token -> returns true (handled exception)
    // ----------------------------------------------
    @Test
    void isTokenExpired_ShouldReturnTrue_WhenTokenIsInvalid() {
        assertTrue(jwtService.isTokenExpired("invalidToken"));
    }

    /////////////////////////
    // validateToken Tests //
    /////////////////////////

    // ----------------------------------------------
    // 1. valid token + matching email -> true
    // ----------------------------------------------
    @Test
    void validateToken_ShouldReturnTrue_WhenTokenIsValidAndEmailMatches() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.validateToken(token, user.getEmail()));
    }

    // ----------------------------------------------
    // 2. valid token + mismatching email -> false
    // ----------------------------------------------
    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsValidButEmailMismatch() {
        String token = jwtService.generateToken(user);
        assertFalse(jwtService.validateToken(token, "other@example.com"));
    }

    // ----------------------------------------------
    // 3. expired token -> false
    // ----------------------------------------------
    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsExpired() {
        JwtService expiredService = new JwtService(SECRET_KEY, -10000);
        String expiredToken = expiredService.generateToken(user);
        assertFalse(jwtService.validateToken(expiredToken, user.getEmail()));
    }

    // ----------------------------------------------
    // 4. invalid token -> false
    // ----------------------------------------------
    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() {
        assertFalse(jwtService.validateToken("invalidToken", user.getEmail()));
    }

    ////////////////////////
    // isTokenValid Tests //
    ////////////////////////

    // ----------------------------------------------
    // 1. valid token + matching user -> true
    // ----------------------------------------------
    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenIsValidAndUserMatches() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    // ----------------------------------------------
    // 2. valid token + mismatching user -> false
    // ----------------------------------------------
    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenIsValidButUserMismatch() {
        String token = jwtService.generateToken(user);
        User otherUser = new User();
        otherUser.setEmail("other@example.com");

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    // ----------------------------------------------
    // 3. expired token -> throws RuntimeException (as per current impl)
    // ----------------------------------------------
    @Test
    void isTokenValid_ShouldThrowException_WhenTokenIsExpired() {
        JwtService expiredService = new JwtService(SECRET_KEY, -10000);
        String expiredToken = expiredService.generateToken(user);

        assertThrows(RuntimeException.class, () -> jwtService.isTokenValid(expiredToken, user));
    }

}