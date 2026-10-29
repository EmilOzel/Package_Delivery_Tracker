package app.dto;

import app.enums.PackageStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateDeliveryEventDTO {
    private PackageStatus status;
    private String location;
    private String message;
}
