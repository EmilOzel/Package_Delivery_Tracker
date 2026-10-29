package app.dto;

import app.entities.Parcel;
import app.enums.PackageStatus;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ParcelDTO {
    private int id;
    private final String trackingNumber;
    private final String senderName;
    private final String recipientName;
    private final String destinationAddress;
    private final Double destinationLatitude;
    private final Double destinationLongitude;
    private final PackageStatus currentStatus;
    private final Instant createdAt;
    private final int ownerId;

    public ParcelDTO(Parcel parcel) {
        this.id = parcel.getId();
        this.trackingNumber = parcel.getTrackingNumber();
        this.senderName = parcel.getSenderName();
        this.recipientName = parcel.getRecipientName();
        this.destinationAddress = parcel.getDestinationAddress();
        this.destinationLatitude = parcel.getDestinationLatitude();
        this.destinationLongitude = parcel.getDestinationLongitude();
        this.currentStatus = parcel.getCurrentStatus();
        this.createdAt = parcel.getCreatedAt();
        this.ownerId = parcel.getOwner().getId();
    }
}
