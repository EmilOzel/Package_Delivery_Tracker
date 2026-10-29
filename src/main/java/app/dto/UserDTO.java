package app.dto;

import app.entities.User;
import app.enums.Role;
import lombok.Getter;

@Getter
public class UserDTO {
    private final int id;
    private final String name;
    private final String email;
    private final Role role;

    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }
}
