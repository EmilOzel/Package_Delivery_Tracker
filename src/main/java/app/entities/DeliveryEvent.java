package app.entities;

import app.enums.PackageStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "delivery_events")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryEvent {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private int id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "package_id",
            nullable = false
    )
    private Parcel parcel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageStatus status;

    private String location;
    private String message;

    @Column(
            nullable = false,
            updatable = false
    )
    private Instant timestamp;

    public DeliveryEvent(
            Parcel parcel,
            PackageStatus status,
            String location,
            String message
    ) {
        this.parcel = parcel;
        this.status = status;
        this.location = location;
        this.message = message;
        this.timestamp = Instant.now();
    }

    @PrePersist
    private void beforeInsert() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}