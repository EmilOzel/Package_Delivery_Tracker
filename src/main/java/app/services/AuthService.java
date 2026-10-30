package app.services;

import app.daos.UserDAO;
import app.dto.LoginRequestDTO;
import app.dto.RegisterRequestDTO;
import app.dto.TokenResponseDTO;
import app.dto.UserDTO;
import app.entities.User;
import app.enums.Role;
import app.exceptions.ApiException;
import app.security.JwtService;
import app.security.PasswordService;

import java.util.Locale;
import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    public AuthService(UserDAO userDAO, PasswordService passwordService, JwtService jwtService) {
        this.userDAO = userDAO;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    public UserDTO register(RegisterRequestDTO request) {
        validateRegistration(request);

        String email = normalizeEmail(request.getEmail());
        if (userDAO.findByEmail(email).isPresent()) {
            throw new ApiException(409, "Email is already registered");
        }

        User user = new User(
                request.getName().trim(),
                email,
                passwordService.hash(request.getPassword()),
                Role.USER
        );
        return new UserDTO(userDAO.create(user));
    }

    public TokenResponseDTO login(LoginRequestDTO request) {
        if (request == null) {
            throw new ApiException(400, "Email and password are required");
        }

        if (request.getEmail() == null) {
            throw new ApiException(400, "Email and password are required");
        }

        if (request.getPassword() == null) {
            throw new ApiException(400, "Email and password are required");
        }

        String email = normalizeEmail(request.getEmail());
        Optional<User> foundUser = userDAO.findByEmail(email);
        if (foundUser.isEmpty()) {
            throw invalidCredentials();
        }

        User user = foundUser.get();
        if (!passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        return new TokenResponseDTO(
                jwtService.createToken(user),
                "Bearer",
                jwtService.getExpiresInSeconds()
        );
    }

    private void validateRegistration(RegisterRequestDTO request) {
        if (request == null) {
            throw new ApiException(400, "Request body is required");
        }

        if (request.getName() == null) {
            throw new ApiException(400, "Name is required");
        }

        if (request.getName().isBlank()) {
            throw new ApiException(400, "Name is required");
        }

        if (request.getEmail() == null) {
            throw new ApiException(400, "A valid email is required");
        }

        if (!request.getEmail().contains("@")) {
            throw new ApiException(400, "A valid email is required");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(401, "Invalid email or password");
    }
}
