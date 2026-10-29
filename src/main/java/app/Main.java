package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.controllers.ParcelController;
import app.daos.ParcelDAO;
import app.daos.UserDAO;
import app.routes.ParcelRoutes;
import app.routes.Routes;
import app.services.AddressService;
import app.services.OpenRouteService;
import app.services.ParcelService;
import app.utils.Utils;
import app.websocket.TrackingSocket;
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
        TrackingSocket trackingSocket = new TrackingSocket();

        ParcelService parcelService = new ParcelService(
                parcelDAO,
                userDAO,
                addressService,
                trackingSocket
        );
        ParcelController parcelController = new ParcelController(parcelService);
        ParcelRoutes parcelRoutes = new ParcelRoutes(parcelController, trackingSocket);

        Routes routes = new Routes(parcelRoutes);

        ApplicationConfig applicationConfig = new ApplicationConfig(routes);

        Javalin app =
                applicationConfig.startServer(7070);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            applicationConfig.stopServer(app);
            if (emf.isOpen()) {
                emf.close();
            }
        }));
    }
}
