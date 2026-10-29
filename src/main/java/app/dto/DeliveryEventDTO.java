package app.dto;

import app.entities.DeliveryEvent;
import app.enums.PackageStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class DeliveryEventDTO {
    private int id;
    private int parcelId;
    private PackageStatus status;
    private String location;
    private Instant timestamp;
    private String message;

    public DeliveryEventDTO(DeliveryEvent event){
        this.id = event.getId();
        this.parcelId = event.getParcel().getId();
        this.status = event.getStatus();
        this.location = event.getLocation();
        this.timestamp = event.getTimestamp();
        this.message = event.getMessage();
    }

}
