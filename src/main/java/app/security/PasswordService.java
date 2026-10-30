package app.security;

import app.exceptions.ApiException;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;

public class PasswordService {

    private static final int WORK_FACTOR = 12;

    public String hash(String password) {
        validate(password);
        return BCrypt.hashpw(password, BCrypt.gensalt(WORK_FACTOR));
    }

    public boolean matches(String password, String passwordHash) {
        if (password == null) {
            return false;
        }

        if (passwordHash == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void validate(String password) {
        if (password == null) {
            throw new ApiException(400, "Password is required");
        }

        if (password.isBlank()) {
            throw new ApiException(400, "Password is required");
        }
        if (password.length() < 10) {
            throw new ApiException(400, "Password must contain at least 10 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(400, "Password must not exceed 72 bytes");
        }
    }
}
