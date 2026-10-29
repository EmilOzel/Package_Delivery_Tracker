package app.config;

import app.controllers.ExceptionController;
import app.dto.ErrorResponse;
import app.exceptions.ApiException;
import app.routes.Routes;
import app.security.SecurityHandler;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import io.javalin.validation.ValidationException;

public class ApplicationConfig {

    private final Routes routes;
    private final SecurityHandler securityHandler;
    private final ExceptionController exceptionController = new ExceptionController();

    public ApplicationConfig(Routes routes, SecurityHandler securityHandler) {
        this.routes = routes;
        this.securityHandler = securityHandler;
    }

    public void configuration(JavalinConfig config){
        config.bundledPlugins.enableRouteOverview("/routes");
        config.router.contextPath = "/api"; // base path for all endpoints
        config.http.defaultContentType = "application/json";
        config.routes.apiBuilder(routes.getRoutes());
        config.routes.beforeMatched(securityHandler::authorizeHttp);
        config.routes.exception(ApiException.class, exceptionController::handleApiException);
        config.routes.exception(ValidationException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse(
                    HttpStatus.BAD_REQUEST.getCode(),
                    e.getMessage(),
                    ctx.path()
            ));
        });
        config.routes.exception(Exception.class, exceptionController::handleUnexpectedException);
    }

    public Javalin startServer(int port) {
        var app = Javalin.create(this::configuration);
        app.start(port);
        return app;
    }

    public void stopServer(Javalin app) {
        app.stop();
    }
}
