package com.anish.e_commerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A copy of the address taken when the order is placed. It is a snapshot on
 * purpose: editing or deleting an address-book entry later must not rewrite
 * where an already placed order was shipped.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress implements Serializable {

    @Column(name = "ship_full_name")
    private String fullName;

    @Column(name = "ship_phone")
    private String phone;

    @Column(name = "ship_line1")
    private String line1;

    @Column(name = "ship_line2")
    private String line2;

    @Column(name = "ship_city")
    private String city;

    @Column(name = "ship_state")
    private String state;

    @Column(name = "ship_postal_code")
    private String postalCode;

    @Column(name = "ship_country")
    private String country;

    public static ShippingAddress from(Address address) {
        return new ShippingAddress(
            address.getFullName(),
            address.getPhone(),
            address.getLine1(),
            address.getLine2(),
            address.getCity(),
            address.getState(),
            address.getPostalCode(),
            address.getCountry()
        );
    }
}
