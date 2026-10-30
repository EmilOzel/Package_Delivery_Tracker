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
import java.util.Optional;
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

        Optional<User> foundOwner = userDAO.findById(principal.getUserId());
        if (foundOwner.isEmpty()) {
            throw new ApiException(404, "Owner not found");
        }

        User owner = foundOwner.get();

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
        if (trackingNumber == null) {
            throw new ApiException(400, "Tracking number is required");
        }

        if (trackingNumber.isBlank()) {
            throw new ApiException(400, "Tracking number is required");
        }

        Optional<Parcel> foundParcel = parcelDAO.findByTrackingNumber(trackingNumber.trim());
        if (foundParcel.isEmpty()) {
            throw new ApiException(404, "Parcel not found");
        }

        Parcel parcel = foundParcel.get();
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
        if (request == null) {
            throw new ApiException(400, "Status is required");
        }

        if (request.getStatus() == null) {
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
        if (!owner) {
            if (!admin) {
                throw new ApiException(403, "Access denied");
            }
        }
        if (parcel.getCurrentStatus() != PackageStatus.CREATED) {
            throw new ApiException(409, "Parcel cannot be deleted after pickup");
        }
        boolean deleted = parcelDAO.deleteById(parcelId);
        if (!deleted) {
            throw new ApiException(404, "Parcel not found");
        }
    }

    private Parcel findParcel(int parcelId) {
        if (parcelId <= 0) {
            throw new ApiException(400, "Parcel id must be positive");
        }
        Optional<Parcel> foundParcel = parcelDAO.findById(parcelId);
        if (foundParcel.isEmpty()) {
            throw new ApiException(404, "Parcel not found");
        }

        return foundParcel.get();
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
        if (value == null) {
            throw new ApiException(400, message);
        }

        if (value.isBlank()) {
            throw new ApiException(400, message);
        }
    }

    private String generateTrackingNumber() {
        String uuid = UUID.randomUUID().toString();
        String uuidWithoutHyphens = uuid.replace("-", "");
        String shortUuid = uuidWithoutHyphens.substring(0, 6);
        String uppercaseUuid = shortUuid.toUpperCase();

        return "PKG-" + uppercaseUuid;
    }

    private boolean isTransitionAllowed(PackageStatus current, PackageStatus next) {
        if (current == PackageStatus.CREATED) {
            if (next == PackageStatus.PICKED_UP) {
                return true;
            }

            if (next == PackageStatus.CANCELLED) {
                return true;
            }

            return false;
        }

        if (current == PackageStatus.PICKED_UP) {
            if (next == PackageStatus.IN_TRANSIT) {
                return true;
            }

            if (next == PackageStatus.CANCELLED) {
                return true;
            }

            return false;
        }

        if (current == PackageStatus.IN_TRANSIT) {
            return isTransportStatus(next);
        }

        if (current == PackageStatus.AT_SORTING_CENTER) {
            return isTransportStatus(next);
        }

        if (current == PackageStatus.OUT_FOR_DELIVERY) {
            if (next == PackageStatus.DELIVERED) {
                return true;
            }

            return false;
        }

        return false;
    }

    private boolean isTransportStatus(PackageStatus status) {
        if (status == PackageStatus.IN_TRANSIT) {
            return true;
        }

        if (status == PackageStatus.AT_SORTING_CENTER) {
            return true;
        }

        if (status == PackageStatus.OUT_FOR_DELIVERY) {
            return true;
        }

        return false;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        if (value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void requireOwnerOrStaff(Parcel parcel, AuthenticateUser principal) {
        boolean owner = parcel.getOwner().getId() == principal.getUserId();
        if (owner) {
            return;
        }

        if (principal.getRole() == Role.EMPLOYEE) {
            return;
        }

        if (principal.getRole() == Role.ADMIN) {
            return;
        }

        throw new ApiException(403, "Access denied");
    }
}
