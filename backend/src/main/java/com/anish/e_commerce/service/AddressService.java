package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.AddressRequest;
import com.anish.e_commerce.dto.AddressResponse;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.Address;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.AddressRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final String DEFAULT_COUNTRY = "India";

    private final AddressRepo addressRepo;
    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public List<AddressResponse> list(String username) {
        return addressRepo
            .findByUser_UsernameOrderByDefaultAddressDescCreatedAtDescIdDesc(username)
            .stream()
            .map(AddressResponse::from)
            .toList();
    }

    /** Owner-scoped lookup: someone else's address id is a 404, not a 403. */
    @Transactional(readOnly = true)
    public Address getOwned(Long id, String username) {
        return addressRepo
            .findByIdAndUser_Username(id, username)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
    }

    @Transactional
    public AddressResponse create(String username, AddressRequest request) {
        User user = userRepo
            .findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Address address = new Address();
        address.setUser(user);
        address.setCreatedAt(LocalDateTime.now());
        copy(request, address);

        // A user's first address is their default, whatever the request says.
        boolean first = addressRepo.countByUser_Username(username) == 0;
        if (first || Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepo.clearDefault(user.getId());
            address.setDefaultAddress(true);
        }
        return AddressResponse.from(addressRepo.save(address));
    }

    @Transactional
    public AddressResponse update(Long id, String username, AddressRequest request) {
        Address address = getOwned(id, username);
        copy(request, address);
        // Only promotes; un-defaulting would leave the book without a default.
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefaultAddress()) {
            addressRepo.clearDefault(address.getUser().getId());
            address.setDefaultAddress(true);
        }
        return AddressResponse.from(addressRepo.save(address));
    }

    @Transactional
    public AddressResponse setDefault(Long id, String username) {
        Address address = getOwned(id, username);
        if (!address.isDefaultAddress()) {
            addressRepo.clearDefault(address.getUser().getId());
            address.setDefaultAddress(true);
            address = addressRepo.save(address);
        }
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long id, String username) {
        Address address = getOwned(id, username);
        boolean wasDefault = address.isDefaultAddress();
        addressRepo.delete(address);
        addressRepo.flush();

        if (wasDefault) {
            // The newest remaining address takes over as default.
            addressRepo
                .findByUser_UsernameOrderByDefaultAddressDescCreatedAtDescIdDesc(username)
                .stream()
                .findFirst()
                .ifPresent(next -> {
                    next.setDefaultAddress(true);
                    addressRepo.save(next);
                });
        }
    }

    private static void copy(AddressRequest r, Address a) {
        a.setFullName(r.getFullName().trim());
        a.setPhone(r.getPhone().trim());
        a.setLine1(r.getLine1().trim());
        a.setLine2(r.getLine2() == null || r.getLine2().isBlank() ? null : r.getLine2().trim());
        a.setCity(r.getCity().trim());
        a.setState(r.getState().trim());
        a.setPostalCode(r.getPostalCode().trim());
        a.setCountry(
            r.getCountry() == null || r.getCountry().isBlank()
                ? DEFAULT_COUNTRY
                : r.getCountry().trim()
        );
    }
}
