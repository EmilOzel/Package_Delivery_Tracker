package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.controllers.AuthController;
import app.controllers.ParcelController;
import app.daos.ParcelDAO;
import app.daos.UserDAO;
import app.dto.ValidatedAddress;
import app.entities.User;
import app.enums.Role;
import app.routes.AuthRoutes;
import app.routes.ParcelRoutes;
import app.routes.Routes;
import app.security.JwtService;
import app.security.PasswordService;
import app.security.SecurityHandler;
import app.services.AddressService;
import app.services.AuthService;
import app.services.ParcelService;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

class ParcelApiTest {

    private static final String PASSWORD = "Test-password-2026";
    private static final String JWT_SECRET = "test-jwt-secret-with-at-least-thirty-two-characters";

    private static EntityManagerFactory emf;
    private static Javalin app;

    @BeforeAll
    static void startApplication() {
        emf = HibernateConfig.createTestEntityManagerFactory();
        UserDAO userDAO = new UserDAO(emf);
        ParcelDAO parcelDAO = new ParcelDAO(emf);
        PasswordService passwordService = new PasswordService();
        JwtService jwtService = new JwtService(JWT_SECRET);

        userDAO.create(new User(
                "Test Employee",
                "employee@example.com",
                passwordService.hash(PASSWORD),
                Role.EMPLOYEE
        ));

        AddressService fakeAddressService = address -> new ValidatedAddress(
                "Amperestien 10, 2605 Brøndby",
                55.694696,
                12.547292
        );

        ParcelService parcelService = new ParcelService(parcelDAO, userDAO, fakeAddressService);
        AuthService authService = new AuthService(userDAO, passwordService, jwtService);

        Routes routes = new Routes(
                new ParcelRoutes(new ParcelController(parcelService)),
                new AuthRoutes(new AuthController(authService))
        );
        SecurityHandler securityHandler = new SecurityHandler(jwtService);
        app = new ApplicationConfig(routes, securityHandler).startServer(0);

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = app.port();
        RestAssured.basePath = "/api";
    }

    @AfterAll
    static void stopApplication() {
        RestAssured.reset();
        if (app != null) app.stop();
        if (emf != null) {
            if (emf.isOpen()) {
                emf.close();
            }
        }
    }

    @Test
    void protectedEndpointToken() {
        given()
                .when().get("/parcels")
                .then().statusCode(401)
                .body("message", equalTo("Authentication is required"));
    }

    @Test
    void registrationLoginAndParcelFlow() {
        String userToken = registerAndLogin("owner@example.com");
        String employeeToken = login("employee@example.com");

        int parcelId = given()
                .auth().oauth2(userToken)
                .contentType("application/json")
                .body(Map.of(
                        "senderName", "Emil",
                        "recipientName", "Bo Jensen",
                        "destinationAddress", "Amperestien 10, 2605 Brøndby"
                ))
                .when().post("/parcels")
                .then().statusCode(201)
                .body("currentStatus", equalTo("CREATED"))
                .body("trackingNumber", startsWith("PKG-"))
                .extract().path("id");

        given()
                .auth().oauth2(userToken)
                .contentType("application/json")
                .body(Map.of("status", "PICKED_UP"))
                .when().post("/parcels/{id}/events", parcelId)
                .then().statusCode(403);

        given()
                .auth().oauth2(employeeToken)
                .contentType("application/json")
                .body(Map.of(
                        "status", "PICKED_UP",
                        "location", "København",
                        "message", "Pakken er afhentet"
                ))
                .when().post("/parcels/{id}/events", parcelId)
                .then().statusCode(201)
                .body("status", equalTo("PICKED_UP"));

        given()
                .auth().oauth2(userToken)
                .when().get("/parcels/{id}/events", parcelId)
                .then().statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    void ownerCanDeleteNewParcel() {
        String token = registerAndLogin("delete-owner@example.com");
        var parcelResponse = given()
                .auth().oauth2(token)
                .contentType("application/json")
                .body(Map.of(
                        "senderName", "Emil",
                        "recipientName", "Bo Jensen",
                        "destinationAddress", "Amperestien 10, 2605 Brøndby"
                ))
                .when().post("/parcels")
                .then().statusCode(201)
                .extract().response();

        int parcelId = parcelResponse.path("id");
        String trackingNumber = parcelResponse.path("trackingNumber");

        given()
                .auth().oauth2(token)
                .when().delete("/parcels/{id}", parcelId)
                .then().statusCode(204);

        given()
                .auth().oauth2(token)
                .when().get("/parcels/tracking/{trackingNumber}", trackingNumber)
                .then().statusCode(404);
    }

    private static String registerAndLogin(String email) {
        given()
                .contentType("application/json")
                .body(Map.of("name", "Test Owner", "email", email, "password", PASSWORD))
                .when().post("/auth/register")
                .then().statusCode(201)
                .body("role", equalTo("USER"));
        return login(email);
    }

    private static String login(String email) {
        return given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", PASSWORD))
                .when().post("/auth/login")
                .then().statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .extract().path("token");
    }

}
