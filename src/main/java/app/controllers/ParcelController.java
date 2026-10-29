package app.controllers;

import app.dto.CreateParcelDTO;
import app.dto.DeliveryEventDTO;
import app.services.ParcelService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class ParcelController {

    private final ParcelService parcelService;

    public ParcelController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    public void create(Context ctx) {
        CreateParcelDTO request = ctx.bodyAsClass(CreateParcelDTO.class);
        ctx.status(HttpStatus.CREATED).json(parcelService.create(request));
    }

    public void findAll(Context ctx) {
        String ownerId = ctx.queryParam("ownerId");
        if (ownerId == null) {
            ctx.json(parcelService.findAll());
            return;
        }

        int parsedOwnerId = ctx.queryParamAsClass("ownerId", Integer.class)
                .required()
                .get();
        ctx.json(parcelService.findAllByOwnerId(parsedOwnerId));
    }

    public void findByTrackingNumber(Context ctx) {
        ctx.json(parcelService.findByTrackingNumber(ctx.pathParam("trackingNumber")));
    }

    public void findEvents(Context ctx) {
        ctx.json(parcelService.findEvents(parcelId(ctx)));
    }

    public void addEvent(Context ctx) {
        DeliveryEventDTO request = ctx.bodyAsClass(DeliveryEventDTO.class);
        ctx.status(HttpStatus.CREATED).json(parcelService.addEvent(parcelId(ctx), request));
    }

    public void delete(Context ctx) {
        parcelService.delete(parcelId(ctx));
        ctx.status(HttpStatus.NO_CONTENT);
    }

    private int parcelId(Context ctx) {
        return ctx.pathParamAsClass("id", Integer.class)
                .required()
                .check(id -> id > 0, "Parcel id must be positive")
                .get();
    }
}
