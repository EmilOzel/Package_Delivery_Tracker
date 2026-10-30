package app.security;

import app.entities.User;
import app.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-jwt-secret-with-at-least-thirty-two-characters";

    @Test
    void createsAndVerifiesToken() {
        JwtService jwtService = new JwtService(SECRET);
        User user = new User("Emil", "emil@example.com", "hash", Role.USER);
        user.setId(42);

        AuthenticateUser principal = jwtService.verify(jwtService.createToken(user));

        assertEquals(42, principal.getUserId());
        assertEquals(Role.USER, principal.getRole());
    }

    @Test
    void rejectsToken() {
        JwtService issuer = new JwtService(SECRET);
        JwtService verifier = new JwtService("another-test-secret-with-at-least-thirty-two-characters");
        User user = new User("Emil", "emil@example.com", "hash", Role.USER);
        user.setId(42);

        String token = issuer.createToken(user);

        assertThrows(RuntimeException.class, () -> verifier.verify(token));
    }
}
