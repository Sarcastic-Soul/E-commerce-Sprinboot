package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepo extends JpaRepository<Address, Long> {
    // Default first, then newest — the order the address book and checkout show.
    List<Address> findByUser_UsernameOrderByDefaultAddressDescCreatedAtDescIdDesc(
        String username
    );

    // Scoped by owner so another user's id simply reads as "not found".
    Optional<Address> findByIdAndUser_Username(Long id, String username);

    long countByUser_Username(String username);

    @Modifying
    @Query(
        "UPDATE Address a SET a.defaultAddress = false " +
            "WHERE a.user.id = :userId AND a.defaultAddress = true"
    )
    int clearDefault(@Param("userId") Long userId);
}
