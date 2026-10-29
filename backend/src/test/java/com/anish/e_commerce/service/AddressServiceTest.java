package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.dto.AddressRequest;
import com.anish.e_commerce.dto.AddressResponse;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.Address;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.AddressRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepo addressRepo;

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private AddressService addressService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("bob");
    }

    private static AddressRequest req(Boolean isDefault) {
        AddressRequest r = new AddressRequest();
        r.setFullName("Bob");
        r.setPhone("+919876543210");
        r.setLine1("1 Road");
        r.setLine2("  ");
        r.setCity("Pune");
        r.setState("MH");
        r.setPostalCode("411001");
        r.setIsDefault(isDefault);
        return r;
    }

    private Address address(long id, boolean isDefault) {
        Address a = new Address();
        a.setId(id);
        a.setUser(user);
        a.setFullName("Bob");
        a.setDefaultAddress(isDefault);
        a.setCreatedAt(LocalDateTime.now());
        return a;
    }

    @Test
    void firstAddressBecomesDefaultAndCountryDefaultsToIndia() {
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(user));
        when(addressRepo.countByUser_Username("bob")).thenReturn(0L);
        when(addressRepo.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

        AddressResponse r = addressService.create("bob", req(false));

        assertTrue(r.isDefault());
        assertEquals("India", r.country());
        assertNull(r.line2()); // blank optional line stored as null
        verify(addressRepo).clearDefault(1L);
    }

    @Test
    void laterAddressIsNotDefaultUnlessAsked() {
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(user));
        when(addressRepo.countByUser_Username("bob")).thenReturn(2L);
        when(addressRepo.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

        assertFalse(addressService.create("bob", req(null)).isDefault());
        verify(addressRepo, never()).clearDefault(anyLong());

        assertTrue(addressService.create("bob", req(true)).isDefault());
        verify(addressRepo).clearDefault(1L);
    }

    @Test
    void setDefaultClearsTheOldDefault() {
        Address a = address(7L, false);
        when(addressRepo.findByIdAndUser_Username(7L, "bob")).thenReturn(Optional.of(a));
        when(addressRepo.save(a)).thenReturn(a);

        assertTrue(addressService.setDefault(7L, "bob").isDefault());
        verify(addressRepo).clearDefault(1L);
    }

    @Test
    void deletingTheDefaultPromotesTheNewestRemaining() {
        Address gone = address(7L, true);
        Address newest = address(8L, false);
        when(addressRepo.findByIdAndUser_Username(7L, "bob")).thenReturn(Optional.of(gone));
        when(addressRepo.findByUser_UsernameOrderByDefaultAddressDescCreatedAtDescIdDesc("bob"))
            .thenReturn(List.of(newest, address(6L, false)));

        addressService.delete(7L, "bob");

        verify(addressRepo).delete(gone);
        assertTrue(newest.isDefaultAddress());
        verify(addressRepo).save(newest);
    }

    @Test
    void deletingANonDefaultLeavesTheDefaultAlone() {
        Address gone = address(7L, false);
        when(addressRepo.findByIdAndUser_Username(7L, "bob")).thenReturn(Optional.of(gone));

        addressService.delete(7L, "bob");

        verify(addressRepo).delete(gone);
        verify(addressRepo, never()).save(any());
    }

    @Test
    void someoneElsesAddressIs404() {
        when(addressRepo.findByIdAndUser_Username(7L, "mallory")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.update(7L, "mallory", req(null)));
        assertThrows(ResourceNotFoundException.class, () -> addressService.delete(7L, "mallory"));
        verify(addressRepo, never()).save(any());
        verify(addressRepo, never()).delete(any());
    }
}
