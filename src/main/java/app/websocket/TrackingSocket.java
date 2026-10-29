package app.websocket;

import app.dto.DeliveryEventDTO;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TrackingSocket {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackingSocket.class);

    private final Map<String, Set<WsContext>> subscribers = new ConcurrentHashMap<>();

    public void configure(WsConfig socket) {
        socket.onConnect(ctx -> {
            String trackingNumber = ctx.pathParam("trackingNumber");
            subscribers
                    .computeIfAbsent(trackingNumber, ignored -> ConcurrentHashMap.newKeySet())
                    .add(ctx);
            LOGGER.info("WebSocket connected for tracking number {}", trackingNumber);
        });

        socket.onMessage(ctx -> ctx.send("Tracking connection is active"));

        socket.onClose(ctx -> {
            remove(ctx.pathParam("trackingNumber"), ctx);
            LOGGER.info("WebSocket disconnected for tracking number {}", ctx.pathParam("trackingNumber"));
        });

        socket.onError(ctx -> {
            remove(ctx.pathParam("trackingNumber"), ctx);
            LOGGER.warn("WebSocket error for tracking number {}", ctx.pathParam("trackingNumber"), ctx.error());
        });
    }

    public void broadcast(String trackingNumber, DeliveryEventDTO event) {
        Set<WsContext> connections = subscribers.get(trackingNumber);
        if (connections == null) {
            return;
        }

        connections.removeIf(connection -> {
            try {
                connection.sendAsClass(event, DeliveryEventDTO.class);
                return false;
            } catch (RuntimeException exception) {
                LOGGER.warn("Could not send WebSocket update for {}", trackingNumber, exception);
                return true;
            }
        });

        if (connections.isEmpty()) {
            subscribers.remove(trackingNumber, connections);
        }
    }

    private void remove(String trackingNumber, WsContext context) {
        Set<WsContext> connections = subscribers.get(trackingNumber);
        if (connections == null) {
            return;
        }
        connections.remove(context);
        if (connections.isEmpty()) {
            subscribers.remove(trackingNumber, connections);
        }
    }
}
