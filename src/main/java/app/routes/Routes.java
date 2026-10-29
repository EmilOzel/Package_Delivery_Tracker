package app.routes;

import io.javalin.apibuilder.EndpointGroup;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class Routes {

    private final ParcelRoutes parcelRoutes;
    private final AuthRoutes authRoutes;

    public Routes(ParcelRoutes parcelRoutes, AuthRoutes authRoutes) {
        this.parcelRoutes = parcelRoutes;
        this.authRoutes = authRoutes;
    }

    public EndpointGroup getRoutes() {
        return () -> {
            get("/", ctx -> ctx.json(Map.of("name", "Package Tracker API")));
            get("/health", ctx -> ctx.json(Map.of("status", "UP")));
            path("/auth", authRoutes.getRoutes());
            path("/parcels", parcelRoutes.getRoutes());
        };
    }
}
