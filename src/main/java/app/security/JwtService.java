package app.security;

import app.entities.User;
import app.enums.Role;
import app.exceptions.ApiException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

import java.time.Duration;
import java.time.Instant;

public class JwtService {

    private static final String ISSUER = "package-tracker";
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(String secret) {
        if (secret == null) {
            throw new IllegalStateException("JWT_SECRET is required");
        }

        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
        }

        algorithm = Algorithm.HMAC256(secret);

        Verification verification = JWT.require(algorithm);
        verification.withIssuer(ISSUER);
        verifier = verification.build();
    }

    public String createToken(User user) {
        Instant now = Instant.now();

        JWTCreator.Builder tokenBuilder = JWT.create();
        tokenBuilder.withIssuer(ISSUER);
        tokenBuilder.withSubject(String.valueOf(user.getId()));
        tokenBuilder.withClaim("role", user.getRole().name());
        tokenBuilder.withIssuedAt(now);
        tokenBuilder.withExpiresAt(now.plus(TOKEN_LIFETIME));

        return tokenBuilder.sign(algorithm);
    }

    public AuthenticateUser verify(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            int userId = Integer.parseInt(jwt.getSubject());
            String roleName = jwt.getClaim("role").asString();
            Role role = Role.valueOf(roleName);

            return new AuthenticateUser(userId, role);
        } catch (RuntimeException exception) {
            throw new ApiException(401, "Invalid or expired token");
        }
    }

    public long getExpiresInSeconds() {
        return TOKEN_LIFETIME.toSeconds();
    }
}
