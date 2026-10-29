package app.routes;

import app.controllers.ParcelController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;
import static app.enums.Role.ADMIN;
import static app.enums.Role.EMPLOYEE;
import static app.enums.Role.USER;

public class ParcelRoutes {

    private final ParcelController controller;

    public ParcelRoutes(ParcelController controller) {
        this.controller = controller;
    }

    public EndpointGroup getRoutes() {
        return () -> {
            post("/", controller::create, USER, EMPLOYEE, ADMIN);
            get("/", controller::findAll, USER, EMPLOYEE, ADMIN);
            get("/tracking/{trackingNumber}", controller::findByTrackingNumber, USER, EMPLOYEE, ADMIN);
            get("/{id}/events", controller::findEvents, USER, EMPLOYEE, ADMIN);
            post("/{id}/events", controller::addEvent, EMPLOYEE, ADMIN);
            delete("/{id}", controller::delete, USER, ADMIN);
        };
    }
}
