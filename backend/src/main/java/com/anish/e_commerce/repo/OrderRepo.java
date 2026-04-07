package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    List<Order> findByUser_UsernameOrderByCreatedAtDesc(String username);
    java.util.Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
}
