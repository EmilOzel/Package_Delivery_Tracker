package app.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void hashesAndVerifiesPassword() {
        String hash = passwordService.hash("Secure-password-2026");
        assertTrue(passwordService.matches("Secure-password-2026", hash));
        assertFalse(passwordService.matches("Wrong-password-2026", hash));
    }

    @Test
    void samePasswordGetsDifferentSalt() {
        String first = passwordService.hash("Secure-password-2026");
        String second = passwordService.hash("Secure-password-2026");
        assertNotEquals(first, second);
    }
}
