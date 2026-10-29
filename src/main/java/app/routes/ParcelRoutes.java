package app.routes;

import app.controllers.ParcelController;
import app.websocket.TrackingSocket;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;
import static io.javalin.apibuilder.ApiBuilder.ws;
import static app.enums.Role.ADMIN;
import static app.enums.Role.EMPLOYEE;
import static app.enums.Role.USER;

public class ParcelRoutes {

    private final ParcelController controller;
    private final TrackingSocket trackingSocket;

    public ParcelRoutes(ParcelController controller, TrackingSocket trackingSocket) {
        this.controller = controller;
        this.trackingSocket = trackingSocket;
    }

    public EndpointGroup getRoutes() {
        return () -> {
            post("/", controller::create, USER, EMPLOYEE, ADMIN);
            get("/", controller::findAll, USER, EMPLOYEE, ADMIN);
            get("/tracking/{trackingNumber}", controller::findByTrackingNumber, USER, EMPLOYEE, ADMIN);
            get("/{id}/events", controller::findEvents, USER, EMPLOYEE, ADMIN);
            post("/{id}/events", controller::addEvent, EMPLOYEE, ADMIN);
            put("/{id}/status", controller::updateStatus, EMPLOYEE, ADMIN);
            delete("/{id}", controller::delete, USER, ADMIN);
            ws("/{trackingNumber}/live", trackingSocket::configure);
        };
    }
}
