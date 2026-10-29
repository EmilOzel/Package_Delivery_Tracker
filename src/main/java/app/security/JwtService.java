package app.security;

import app.entities.User;
import app.enums.Role;
import app.exceptions.ApiException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Duration;
import java.time.Instant;

public class JwtService {

    private static final String ISSUER = "package-tracker";
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
        }

        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(user.getId()))
                .withClaim("role", user.getRole().name())
                .withIssuedAt(now)
                .withExpiresAt(now.plus(TOKEN_LIFETIME))
                .sign(algorithm);
    }

    public AuthPrincipal verify(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return new AuthPrincipal(
                    Integer.parseInt(jwt.getSubject()),
                    Role.valueOf(jwt.getClaim("role").asString())
            );
        } catch (RuntimeException exception) {
            throw new ApiException(401, "Invalid or expired token");
        }
    }

    public long getExpiresInSeconds() {
        return TOKEN_LIFETIME.toSeconds();
    }
}
