package app.entities;

import app.enums.PackageStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "packages")
@NamedQuery(
        name = "Parcel.findByTrackingNumber",
        query = "SELECT p FROM Parcel p JOIN FETCH p.owner WHERE p.trackingNumber = :trackingNumber"
)
@Getter
@Setter
@NoArgsConstructor
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String destinationAddress;

    private Double destinationLatitude;

    private Double destinationLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageStatus currentStatus;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(
            mappedBy = "parcel",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("timestamp ASC")
    private List<DeliveryEvent> events = new ArrayList<>();

    public Parcel(
            String trackingNumber,
            String senderName,
            String recipientName,
            String destinationAddress,
            Double destinationLatitude,
            Double destinationLongitude,
            User owner
    ) {
        this.trackingNumber = trackingNumber;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.destinationAddress = destinationAddress;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.owner = owner;
        this.currentStatus = PackageStatus.CREATED;
        this.createdAt = Instant.now();
    }

    @PrePersist
    private void beforeInsert() {
        if (currentStatus == null) {
            currentStatus = PackageStatus.CREATED;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
