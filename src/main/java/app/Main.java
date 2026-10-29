package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.controllers.ParcelController;
import app.controllers.AuthController;
import app.daos.ParcelDAO;
import app.daos.UserDAO;
import app.routes.ParcelRoutes;
import app.routes.AuthRoutes;
import app.routes.Routes;
import app.services.AddressService;
import app.services.OpenRouteService;
import app.services.ParcelService;
import app.services.AuthService;
import app.utils.Utils;
import app.security.JwtService;
import app.security.PasswordService;
import app.security.SecurityHandler;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

        UserDAO userDAO = new UserDAO(emf);
        ParcelDAO parcelDAO = new ParcelDAO(emf);
        AddressService addressService = new OpenRouteService(
                Utils.getConfigurationValue("OPENROUTESERVICE_API_KEY", "config.properties")
        );
        PasswordService passwordService = new PasswordService();
        JwtService jwtService = new JwtService(
                Utils.getConfigurationValue("JWT_SECRET", "config.properties")
        );
        SecurityHandler securityHandler = new SecurityHandler(jwtService);

        AuthService authService = new AuthService(userDAO, passwordService, jwtService);
        AuthController authController = new AuthController(authService);
        AuthRoutes authRoutes = new AuthRoutes(authController);

        ParcelService parcelService = new ParcelService(
                parcelDAO,
                userDAO,
                addressService
        );
        ParcelController parcelController = new ParcelController(parcelService);
        ParcelRoutes parcelRoutes = new ParcelRoutes(parcelController);

        Routes routes = new Routes(parcelRoutes, authRoutes);

        ApplicationConfig applicationConfig = new ApplicationConfig(routes, securityHandler);

        Javalin app =
                applicationConfig.startServer(serverPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            if (emf.isOpen()) {
                emf.close();
            }
        }));
    }

    private static int serverPort() {
        String configuredPort = System.getenv("PORT");
        if (configuredPort == null || configuredPort.isBlank()) {
            return 7070;
        }
        try {
            return Integer.parseInt(configuredPort);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("PORT must be a number", exception);
        }
    }
}
