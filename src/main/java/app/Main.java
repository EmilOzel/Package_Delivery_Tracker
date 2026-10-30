package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.controllers.AuthController;
import app.controllers.ParcelController;
import app.daos.ParcelDAO;
import app.daos.UserDAO;
import app.routes.AuthRoutes;
import app.routes.ParcelRoutes;
import app.routes.Routes;
import app.security.JwtService;
import app.security.PasswordService;
import app.security.SecurityHandler;
import app.services.AddressService;
import app.services.AuthService;
import app.services.OpenRouteService;
import app.services.ParcelService;
import app.utils.Utils;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

        String apiKey = Utils.getConfigurationValue(
                "OPENROUTESERVICE_API_KEY",
                "config.properties"
        );
        String jwtSecret = Utils.getConfigurationValue(
                "JWT_SECRET",
                "config.properties"
        );

        UserDAO userDAO = new UserDAO(emf);
        ParcelDAO parcelDAO = new ParcelDAO(emf);

        AddressService addressService = new OpenRouteService(apiKey);
        PasswordService passwordService = new PasswordService();
        JwtService jwtService = new JwtService(jwtSecret);
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

        int port = serverPort();
        Javalin app = applicationConfig.startServer(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            if (emf.isOpen()) {
                emf.close();
            }
        }));
    }

    private static int serverPort() {
        String configuredPort = System.getenv("PORT");

        if (configuredPort == null) {
            return 7070;
        }

        if (configuredPort.isBlank()) {
            return 7070;
        }

        try {
            return Integer.parseInt(configuredPort);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("PORT must be a number", exception);
        }
    }
}
