package app.controllers;

import app.dto.CreateParcelDTO;
import app.dto.CreateDeliveryEventDTO;
import app.dto.DeliveryEventDTO;
import app.dto.ParcelDTO;
import app.exceptions.ApiException;
import app.security.AuthenticateUser;
import app.services.ParcelService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.List;

import static app.security.SecurityHandler.principal;

public class ParcelController {

    private final ParcelService parcelService;

    public ParcelController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    public void create(Context ctx) {
        CreateParcelDTO request = ctx.bodyAsClass(CreateParcelDTO.class);
        AuthenticateUser authenticatedUser = principal(ctx);
        ParcelDTO response = parcelService.create(request, authenticatedUser);

        ctx.status(HttpStatus.CREATED);
        ctx.json(response);
    }

    public void findAll(Context ctx) {
        AuthenticateUser authenticatedUser = principal(ctx);
        List<ParcelDTO> response = parcelService.findAll(authenticatedUser);
        ctx.json(response);
    }

    public void findByTrackingNumber(Context ctx) {
        String trackingNumber = ctx.pathParam("trackingNumber");
        AuthenticateUser authenticatedUser = principal(ctx);
        ParcelDTO response = parcelService.findByTrackingNumber(trackingNumber, authenticatedUser);
        ctx.json(response);
    }

    public void findEvents(Context ctx) {
        int id = parcelId(ctx);
        AuthenticateUser authenticatedUser = principal(ctx);
        List<DeliveryEventDTO> response = parcelService.findEvents(id, authenticatedUser);
        ctx.json(response);
    }

    public void addEvent(Context ctx) {
        CreateDeliveryEventDTO request = ctx.bodyAsClass(CreateDeliveryEventDTO.class);
        int id = parcelId(ctx);
        DeliveryEventDTO response = parcelService.addEvent(id, request);

        ctx.status(HttpStatus.CREATED);
        ctx.json(response);
    }

    public void delete(Context ctx) {
        int id = parcelId(ctx);
        AuthenticateUser authenticatedUser = principal(ctx);
        parcelService.delete(id, authenticatedUser);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    private int parcelId(Context ctx) {
        String idValue = ctx.pathParam("id");
        int id;

        try {
            id = Integer.parseInt(idValue);
        } catch (NumberFormatException exception) {
            throw new ApiException(400, "Parcel id must be a number");
        }

        if (id <= 0) {
            throw new ApiException(400, "Parcel id must be positive");
        }

        return id;
    }
}
