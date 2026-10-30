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
import app.security.AuthenticateUser;

import java.util.ArrayList;
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

    public ParcelDTO create(CreateParcelDTO request, AuthenticateUser principal) {
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

    public List<ParcelDTO> findAll(AuthenticateUser principal) {
        List<Parcel> parcels;

        if (principal.getRole() == Role.USER) {
            parcels = parcelDAO.findAllByOwnerId(principal.getUserId());
        } else {
            parcels = parcelDAO.findAll();
        }

        List<ParcelDTO> parcelDTOs = new ArrayList<>();
        for (Parcel parcel : parcels) {
            parcelDTOs.add(new ParcelDTO(parcel));
        }
        return parcelDTOs;
    }

    public ParcelDTO findByTrackingNumber(String trackingNumber, AuthenticateUser principal) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new ApiException(400, "Tracking number is required");
        }
        Parcel parcel = parcelDAO.findByTrackingNumber(trackingNumber.trim())
                .orElseThrow(() -> new ApiException(404, "Parcel not found"));
        requireOwnerOrStaff(parcel, principal);
        return new ParcelDTO(parcel);
    }

    public List<DeliveryEventDTO> findEvents(int parcelId, AuthenticateUser principal) {
        requireOwnerOrStaff(findParcel(parcelId), principal);

        List<DeliveryEvent> events = parcelDAO.findEventsByParcelId(parcelId);
        List<DeliveryEventDTO> eventDTOs = new ArrayList<>();

        for (DeliveryEvent event : events) {
            eventDTOs.add(new DeliveryEventDTO(event));
        }
        return eventDTOs;
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

    public void delete(int parcelId, AuthenticateUser principal) {
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
        if (current == PackageStatus.CREATED) {
            return next == PackageStatus.PICKED_UP
                    || next == PackageStatus.CANCELLED;
        }

        if (current == PackageStatus.PICKED_UP) {
            return next == PackageStatus.IN_TRANSIT
                    || next == PackageStatus.CANCELLED;
        }

        if (current == PackageStatus.IN_TRANSIT
                || current == PackageStatus.AT_SORTING_CENTER) {
            return next == PackageStatus.IN_TRANSIT
                    || next == PackageStatus.AT_SORTING_CENTER
                    || next == PackageStatus.OUT_FOR_DELIVERY;
        }

        if (current == PackageStatus.OUT_FOR_DELIVERY) {
            return next == PackageStatus.DELIVERED;
        }

        return false;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void requireOwnerOrStaff(Parcel parcel, AuthenticateUser principal) {
        boolean owner = parcel.getOwner().getId() == principal.getUserId();
        boolean staff = principal.getRole() == Role.EMPLOYEE || principal.getRole() == Role.ADMIN;
        if (!owner && !staff) {
            throw new ApiException(403, "Access denied");
        }
    }
}
