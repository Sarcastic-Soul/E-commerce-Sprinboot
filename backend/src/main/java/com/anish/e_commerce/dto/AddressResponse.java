package com.anish.e_commerce.dto;

import com.anish.e_commerce.model.Address;

public record AddressResponse(
    Long id,
    String fullName,
    String phone,
    String line1,
    String line2,
    String city,
    String state,
    String postalCode,
    String country,
    boolean isDefault
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(
            a.getId(),
            a.getFullName(),
            a.getPhone(),
            a.getLine1(),
            a.getLine2(),
            a.getCity(),
            a.getState(),
            a.getPostalCode(),
            a.getCountry(),
            a.isDefaultAddress()
        );
    }
}
