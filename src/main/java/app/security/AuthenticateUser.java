package app.security;

import app.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticateUser {
    private int userId;
    private Role role;
}
