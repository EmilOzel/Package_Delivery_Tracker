package app.services;

import app.dto.ValidatedAddress;

public interface AddressService {

    ValidatedAddress validate(String address);
}