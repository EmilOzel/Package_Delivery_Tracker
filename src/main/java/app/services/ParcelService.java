package app.services;

import app.daos.ParcelDAO;
import app.daos.UserDAO;
import app.dto.CreateParcelDTO;
import app.dto.DeliveryEventDTO;
import app.dto.ParcelDTO;
import app.dto.ValidatedAddress;
import app.entities.DeliveryEvent;
import app.entities.Parcel;
import app.entities.User;
import app.enums.PackageStatus;
import app.exceptions.ApiException;
import app.websocket.TrackingSocket;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ParcelService {

    private static final Map<PackageStatus, Set<PackageStatus>> ALLOWED_TRANSITIONS =
            createAllowedTransitions();

    private final ParcelDAO parcelDAO;
    private final UserDAO userDAO;
    private final AddressService addressService;
    private final TrackingSocket trackingSocket;

    public ParcelService(
            ParcelDAO parcelDAO,
            UserDAO userDAO,
            AddressService addressService,
            TrackingSocket trackingSocket
    ) {
        this.parcelDAO = parcelDAO;
        this.userDAO = userDAO;
        this.addressService = addressService;
        this.trackingSocket = trackingSocket;
    }

    public ParcelDTO create(CreateParcelDTO request) {
        validateCreateRequest(request);

        User owner = userDAO.findById(request.getOwnerId())
                .orElseThrow(() -> new ApiException(404, "Owner not found"));

        ValidatedAddress address = addressService.validate(request.getDestinationAddress());
        Parcel parcel = new Parcel(
                generateTrackingNumber(),
                request.getSenderName().trim(),
                request.getRecipientName().trim(),
                address.getFormattedAddress(),
                address.getLatitude(),
                address.getLongitude(),
                owner
        );

        return new ParcelDTO(parcelDAO.createWithInitialEvent(parcel));
    }

    public List<ParcelDTO> findAll() {
        return parcelDAO.findAll().stream()
                .map(ParcelDTO::new)
                .toList();
    }

    public List<ParcelDTO> findAllByOwnerId(int ownerId) {
        if (userDAO.findById(ownerId).isEmpty()) {
            throw new ApiException(404, "Owner not found");
        }
        return parcelDAO.findAllByOwnerId(ownerId).stream()
                .map(ParcelDTO::new)
                .toList();
    }

    public ParcelDTO findByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new ApiException(400, "Tracking number is required");
        }
        return new ParcelDTO(parcelDAO.findByTrackingNumber(trackingNumber.trim())
                .orElseThrow(() -> new ApiException(404, "Parcel not found")));
    }

    public List<DeliveryEventDTO> findEvents(int parcelId) {
        findParcel(parcelId);
        return parcelDAO.findEventsByParcelId(parcelId).stream()
                .map(DeliveryEventDTO::new)
                .toList();
    }

    public DeliveryEventDTO addEvent(int parcelId, DeliveryEventDTO request) {
        if (request == null || request.getStatus() == null) {
            throw new ApiException(400, "Status is required");
        }

        Parcel parcel = findParcel(parcelId);
        if (!isTransitionAllowed(parcel.getCurrentStatus(), request.getStatus())) {
            throw new ApiException(
                    409,
                    "Status cannot change from " + parcel.getCurrentStatus()
                            + " to " + request.getStatus()
            );
        }

        DeliveryEvent event = parcelDAO.addEventAndUpdateStatus(
                parcelId,
                request.getStatus(),
                trimToNull(request.getLocation()),
                trimToNull(request.getMessage())
        );
        if (event == null) {
            throw new ApiException(404, "Parcel not found");
        }

        DeliveryEventDTO result = new DeliveryEventDTO(event);
        trackingSocket.broadcast(parcel.getTrackingNumber(), result);
        return result;
    }

    public void delete(int parcelId) {
        Parcel parcel = findParcel(parcelId);
        if (parcel.getCurrentStatus() != PackageStatus.CREATED) {
            throw new ApiException(409, "Parcel cannot be deleted after pickup");
        }
        if (!parcelDAO.deleteById(parcelId)) {
            throw new ApiException(404, "Parcel not found");
        }
    }

    private Parcel findParcel(int parcelId) {
        if (parcelId <= 0) {
            throw new ApiException(400, "Parcel id must be positive");
        }
        return parcelDAO.findById(parcelId)
                .orElseThrow(() -> new ApiException(404, "Parcel not found"));
    }

    private void validateCreateRequest(CreateParcelDTO request) {
        if (request == null) {
            throw new ApiException(400, "Request body is required");
        }
        if (request.getOwnerId() <= 0) {
            throw new ApiException(400, "Owner id must be positive");
        }
        requireText(request.getSenderName(), "Sender name is required");
        requireText(request.getRecipientName(), "Recipient name is required");
        requireText(request.getDestinationAddress(), "Destination address is required");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(400, message);
        }
    }

    private String generateTrackingNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String value = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();
            String trackingNumber = "PKG-" + value;
            if (!parcelDAO.trackingNumberExists(trackingNumber)) {
                return trackingNumber;
            }
        }
        throw new ApiException(500, "Could not generate a unique tracking number");
    }

    private boolean isTransitionAllowed(PackageStatus current, PackageStatus next) {
        return ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(next);
    }

    private static Map<PackageStatus, Set<PackageStatus>> createAllowedTransitions() {
        Map<PackageStatus, Set<PackageStatus>> transitions = new EnumMap<>(PackageStatus.class);
        transitions.put(PackageStatus.CREATED, Set.of(PackageStatus.PICKED_UP, PackageStatus.CANCELLED));
        transitions.put(PackageStatus.PICKED_UP, Set.of(PackageStatus.IN_TRANSIT, PackageStatus.CANCELLED));
        transitions.put(PackageStatus.IN_TRANSIT, Set.of(
                PackageStatus.AT_SORTING_CENTER,
                PackageStatus.OUT_FOR_DELIVERY
        ));
        transitions.put(PackageStatus.AT_SORTING_CENTER, Set.of(
                PackageStatus.IN_TRANSIT,
                PackageStatus.OUT_FOR_DELIVERY
        ));
        transitions.put(PackageStatus.OUT_FOR_DELIVERY, Set.of(PackageStatus.DELIVERED));
        transitions.put(PackageStatus.DELIVERED, Set.of());
        transitions.put(PackageStatus.CANCELLED, Set.of());
        return Map.copyOf(transitions);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
