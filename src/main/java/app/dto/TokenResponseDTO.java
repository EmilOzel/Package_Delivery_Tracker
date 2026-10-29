package app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponseDTO {
    private String token;
    private String tokenType;
    private long expiresIn;
}
