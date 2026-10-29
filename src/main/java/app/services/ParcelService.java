package app.services;

import app.daos.ParcelDAO;
import app.daos.UserDAO;
import app.dto.CreateParcelDTO;
import app.dto.CreateDeliveryEventDTO;
import app.dto.DeliveryEventDTO;
import app.dto.ParcelDTO;
import app.dto.ValidatedAddress;
import app.entities.DeliveryEvent;
import app.entities.Parcel;
import app.entities.User;
import app.enums.PackageStatus;
import app.enums.Role;
import app.exceptions.ApiException;
import app.security.AuthPrincipal;

import java.util.List;
import java.util.UUID;

public class ParcelService {

    private final ParcelDAO parcelDAO;
    private final UserDAO userDAO;
    private final AddressService addressService;

    public ParcelService(
            ParcelDAO parcelDAO,
            UserDAO userDAO,
            AddressService addressService
    ) {
        this.parcelDAO = parcelDAO;
        this.userDAO = userDAO;
        this.addressService = addressService;
    }

    public ParcelDTO create(CreateParcelDTO request, AuthPrincipal principal) {
        validateCreateRequest(request);

        User owner = userDAO.findById(principal.getUserId())
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

    public List<ParcelDTO> findAll(AuthPrincipal principal) {
        List<Parcel> parcels = principal.getRole() == Role.USER
                ? parcelDAO.findAllByOwnerId(principal.getUserId())
                : parcelDAO.findAll();

        return parcels.stream()
                .map(ParcelDTO::new)
                .toList();
    }

    public ParcelDTO findByTrackingNumber(String trackingNumber, AuthPrincipal principal) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new ApiException(400, "Tracking number is required");
        }
        Parcel parcel = parcelDAO.findByTrackingNumber(trackingNumber.trim())
                .orElseThrow(() -> new ApiException(404, "Parcel not found"));
        requireOwnerOrStaff(parcel, principal);
        return new ParcelDTO(parcel);
    }

    public List<DeliveryEventDTO> findEvents(int parcelId, AuthPrincipal principal) {
        requireOwnerOrStaff(findParcel(parcelId), principal);
        return parcelDAO.findEventsByParcelId(parcelId).stream()
                .map(DeliveryEventDTO::new)
                .toList();
    }

    public DeliveryEventDTO addEvent(int parcelId, CreateDeliveryEventDTO request) {
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

        return new DeliveryEventDTO(event);
    }

    public void delete(int parcelId, AuthPrincipal principal) {
        Parcel parcel = findParcel(parcelId);
        boolean owner = parcel.getOwner().getId() == principal.getUserId();
        boolean admin = principal.getRole() == Role.ADMIN;
        if (!owner && !admin) {
            throw new ApiException(403, "Access denied");
        }
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
        String value = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
        return "PKG-" + value;
    }

    private boolean isTransitionAllowed(PackageStatus current, PackageStatus next) {
        return switch (current) {
            case CREATED -> next == PackageStatus.PICKED_UP || next == PackageStatus.CANCELLED;
            case PICKED_UP -> next == PackageStatus.IN_TRANSIT || next == PackageStatus.CANCELLED;
            case IN_TRANSIT, AT_SORTING_CENTER ->
                    next == PackageStatus.IN_TRANSIT
                            || next == PackageStatus.AT_SORTING_CENTER
                            || next == PackageStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> next == PackageStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireOwnerOrStaff(Parcel parcel, AuthPrincipal principal) {
        boolean owner = parcel.getOwner().getId() == principal.getUserId();
        boolean staff = principal.getRole() == Role.EMPLOYEE || principal.getRole() == Role.ADMIN;
        if (!owner && !staff) {
            throw new ApiException(403, "Access denied");
        }
    }
}
