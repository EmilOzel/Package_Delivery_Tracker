package app.controllers;

import app.dto.ErrorResponse;
import app.exceptions.ApiException;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionController.class);

    public void handleApiException(ApiException exception, Context ctx) {
        if (exception.getCode() >= 500) {
            LOGGER.error("API error {} on {}: {}", exception.getCode(), ctx.path(), exception.getMessage());
        } else {
            LOGGER.warn("Request rejected with {} on {}: {}", exception.getCode(), ctx.path(), exception.getMessage());
        }
        ctx.status(exception.getCode());
        ctx.json(new ErrorResponse(exception.getCode(), exception.getMessage(), ctx.path()));
    }

    public void handleUnexpectedException(Exception exception, Context ctx) {
        LOGGER.error("Unexpected error on {}", ctx.path(), exception);
        ctx.status(500);
        ctx.json(new ErrorResponse(500, "An internal server error occurred", ctx.path()));
    }
}
