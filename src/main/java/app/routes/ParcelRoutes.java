package app.routes;

import app.controllers.ParcelController;
import app.websocket.TrackingSocket;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.ws;

public class ParcelRoutes {

    private final ParcelController controller;
    private final TrackingSocket trackingSocket;

    public ParcelRoutes(ParcelController controller, TrackingSocket trackingSocket) {
        this.controller = controller;
        this.trackingSocket = trackingSocket;
    }

    public EndpointGroup getRoutes() {
        return () -> {
            post("/", controller::create);
            get("/", controller::findAll);
            get("/tracking/{trackingNumber}", controller::findByTrackingNumber);
            get("/{id}/events", controller::findEvents);
            post("/{id}/events", controller::addEvent);
            delete("/{id}", controller::delete);
            ws("/{trackingNumber}/live", trackingSocket::configure);
        };
    }
}
