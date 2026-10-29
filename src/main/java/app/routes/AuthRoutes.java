package app.routes;

import app.controllers.AuthController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.post;

public class AuthRoutes {

    private final AuthController controller;

    public AuthRoutes(AuthController controller) {
        this.controller = controller;
    }

    public EndpointGroup getRoutes() {
        return () -> {
            post("/register", controller::register);
            post("/login", controller::login);
        };
    }
}
