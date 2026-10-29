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

        AuthPrincipal principal = authenticateBearer(ctx.header("Authorization"));
        requireRouteRole(ctx, principal);
        ctx.attribute(PRINCIPAL_ATTRIBUTE, principal);
    }

    public static AuthPrincipal principal(Context ctx) {
        AuthPrincipal principal = ctx.attribute(PRINCIPAL_ATTRIBUTE);
        if (principal == null) {
            throw new ApiException(401, "Authentication is required");
        }
        return principal;
    }

    private AuthPrincipal authenticateBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Authentication is required");
        }
        return jwtService.verify(header.substring(7));
    }

    private void requireRouteRole(Context ctx, AuthPrincipal principal) {
        if (!ctx.routeRoles().contains(principal.getRole())) {
            throw new ApiException(403, "Access denied");
        }
    }

}
