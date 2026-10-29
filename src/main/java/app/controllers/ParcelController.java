package app.controllers;

import app.dto.CreateParcelDTO;
import app.dto.CreateDeliveryEventDTO;
import app.services.ParcelService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import static app.security.SecurityHandler.principal;

public class ParcelController {

    private final ParcelService parcelService;

    public ParcelController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    public void create(Context ctx) {
        CreateParcelDTO request = ctx.bodyAsClass(CreateParcelDTO.class);
        ctx.status(HttpStatus.CREATED).json(parcelService.create(request, principal(ctx)));
    }

    public void findAll(Context ctx) {
        ctx.json(parcelService.findAll(principal(ctx)));
    }

    public void findByTrackingNumber(Context ctx) {
        ctx.json(parcelService.findByTrackingNumber(ctx.pathParam("trackingNumber"), principal(ctx)));
    }

    public void findEvents(Context ctx) {
        ctx.json(parcelService.findEvents(parcelId(ctx), principal(ctx)));
    }

    public void addEvent(Context ctx) {
        CreateDeliveryEventDTO request = ctx.bodyAsClass(CreateDeliveryEventDTO.class);
        ctx.status(HttpStatus.CREATED).json(parcelService.addEvent(parcelId(ctx), request));
    }

    public void delete(Context ctx) {
        parcelService.delete(parcelId(ctx), principal(ctx));
        ctx.status(HttpStatus.NO_CONTENT);
    }

    private int parcelId(Context ctx) {
        return ctx.pathParamAsClass("id", Integer.class)
                .required()
                .check(id -> id > 0, "Parcel id must be positive")
                .get();
    }
}
