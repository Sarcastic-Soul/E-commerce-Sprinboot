package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.PasswordChangeRequest;
import com.anish.e_commerce.dto.ProfileResponse;
import com.anish.e_commerce.exception.BusinessException;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.AddressRepo;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ReviewRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepo userRepo;
    private final OrderRepo orderRepo;
    private final ReviewRepo reviewRepo;
    private final AddressRepo addressRepo;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public ProfileResponse profile(String username) {
        User user = find(username);
        return new ProfileResponse(
            user.getUsername(),
            Set.copyOf(user.getRoles()),
            orderRepo.countByUser_Username(username),
            reviewRepo.countByUser_Username(username),
            addressRepo.countByUser_Username(username)
        );
    }

    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        User user = find(username);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new BusinessException("New password must be at least 8 characters");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);

        // Sign out every other device: their refresh tokens can no longer mint
        // access tokens. This session lives on until its access cookie expires.
        refreshTokenService.deleteByUserId(user.getId());
    }

    private User find(String username) {
        return userRepo
            .findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
