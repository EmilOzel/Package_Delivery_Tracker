package app.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ValidatedAddress {
    private String formattedAddress;
    private double latitude;
    private double longitude;
}
