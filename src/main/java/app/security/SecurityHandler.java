package app.security;

import app.exceptions.ApiException;
import io.javalin.http.Context;

public class SecurityHandler {

    public static final String PRINCIPAL_ATTRIBUTE = "authenticatedUser";

    private final JwtService jwtService;

    public SecurityHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void authorizeHttp(Context ctx) {
        if (ctx.routeRoles().isEmpty()) {
            return;
        }

        AuthenticateUser principal = authenticateBearer(ctx.header("Authorization"));
        requireRouteRole(ctx, principal);
        ctx.attribute(PRINCIPAL_ATTRIBUTE, principal);
    }

    public static AuthenticateUser principal(Context ctx) {
        AuthenticateUser principal = ctx.attribute(PRINCIPAL_ATTRIBUTE);
        if (principal == null) {
            throw new ApiException(401, "Authentication is required");
        }
        return principal;
    }

    private AuthenticateUser authenticateBearer(String header) {
        if (header == null) {
            throw new ApiException(401, "Authentication is required");
        }

        if (!header.startsWith("Bearer ")) {
            throw new ApiException(401, "Authentication is required");
        }

        String token = header.substring(7);
        return jwtService.verify(token);
    }

    private void requireRouteRole(Context ctx, AuthenticateUser principal) {
        if (!ctx.routeRoles().contains(principal.getRole())) {
            throw new ApiException(403, "Access denied");
        }
    }

}
