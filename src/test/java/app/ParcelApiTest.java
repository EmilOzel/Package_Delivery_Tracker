package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.controllers.ParcelController;
import app.daos.ParcelDAO;
import app.daos.UserDAO;
import app.dto.ValidatedAddress;
import app.entities.User;
import app.enums.Role;
import app.routes.ParcelRoutes;
import app.routes.Routes;
import app.services.AddressService;
import app.services.ParcelService;
import app.websocket.TrackingSocket;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcelApiTest {

    private static EntityManagerFactory emf;
    private static Javalin app;
    private static int ownerId;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void startApplication() {
        emf = HibernateConfig.createTestEntityManagerFactory();
        UserDAO userDAO = new UserDAO(emf);
        ParcelDAO parcelDAO = new ParcelDAO(emf);

        User owner = userDAO.create(new User(
                "Test User",
                "test@example.com",
                "test-only",
                Role.USER
        ));
        ownerId = owner.getId();

        AddressService fakeAddressService = address -> new ValidatedAddress(
                "Nørrebrogade 10, 2200 København N, Danmark",
                55.694696,
                12.547292
        );
        TrackingSocket trackingSocket = new TrackingSocket();
        ParcelService service = new ParcelService(
                parcelDAO,
                userDAO,
                fakeAddressService,
                trackingSocket
        );
        ParcelController controller = new ParcelController(service);
        Routes routes = new Routes(new ParcelRoutes(controller, trackingSocket));
        app = new ApplicationConfig(routes).startServer(0);
    }

    @AfterAll
    static void stopApplication() {
        if (app != null) {
            app.stop();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void createParcelAndReadItsInitialEvent() throws Exception {
        String body = """
                {
                  "ownerId": %d,
                  "senderName": "Emil",
                  "recipientName": "Anna",
                  "destinationAddress": "Nørrebrogade 10, 2200 København N"
                }
                """.formatted(ownerId);

        HttpResponse<String> createResponse = HTTP.send(
                HttpRequest.newBuilder(baseUri("/parcels"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(201, createResponse.statusCode());
        Map<String, Object> parcel = JSON.readValue(
                createResponse.body(),
                new TypeReference<>() {}
        );
        assertEquals("CREATED", parcel.get("currentStatus"));
        assertTrue(parcel.get("trackingNumber").toString().startsWith("PKG-"));

        int parcelId = ((Number) parcel.get("id")).intValue();
        HttpResponse<String> eventsResponse = HTTP.send(
                HttpRequest.newBuilder(baseUri("/parcels/" + parcelId + "/events"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, eventsResponse.statusCode());
        List<Map<String, Object>> events = JSON.readValue(
                eventsResponse.body(),
                new TypeReference<>() {}
        );
        assertEquals(1, events.size());
        assertEquals("CREATED", events.getFirst().get("status"));
    }

    private static URI baseUri(String path) {
        return URI.create("http://localhost:" + app.port() + "/api" + path);
    }
}
