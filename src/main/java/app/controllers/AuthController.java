package app.controllers;

import app.dto.LoginRequestDTO;
import app.dto.RegisterRequestDTO;
import app.dto.TokenResponseDTO;
import app.dto.UserDTO;
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
        UserDTO response = authService.register(request);

        ctx.status(HttpStatus.CREATED);
        ctx.json(response);
    }

    public void login(Context ctx) {
        LoginRequestDTO request = ctx.bodyAsClass(LoginRequestDTO.class);
        TokenResponseDTO response = authService.login(request);
        ctx.json(response);
    }

}
