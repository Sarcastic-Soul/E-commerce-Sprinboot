package com.anish.e_commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name is too long")
    private String fullName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone must be 7-15 digits, optionally starting with +")
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 200, message = "Address line 1 is too long")
    private String line1;

    @Size(max = 200, message = "Address line 2 is too long")
    private String line2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City is too long")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State is too long")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(min = 3, max = 10, message = "Postal code must be 3-10 characters")
    private String postalCode;

    @Size(max = 100, message = "Country is too long")
    private String country;

    private Boolean isDefault;
}
