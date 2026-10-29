package app.controllers;

import app.dto.LoginRequestDTO;
import app.dto.RegisterRequestDTO;
import app.services.AuthService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;


public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void register(Context ctx) {
        RegisterRequestDTO request = ctx.bodyAsClass(RegisterRequestDTO.class);
        ctx.status(HttpStatus.CREATED).json(authService.register(request));
    }

    public void login(Context ctx) {
        LoginRequestDTO request = ctx.bodyAsClass(LoginRequestDTO.class);
        ctx.json(authService.login(request));
    }

}
