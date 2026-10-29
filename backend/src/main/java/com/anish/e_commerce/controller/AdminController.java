package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.AdminStatsResponse;
import com.anish.e_commerce.dto.OrderResponse;
import com.anish.e_commerce.dto.OrderStatusUpdateRequest;
import com.anish.e_commerce.model.OrderStatus;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.service.AdminService;
import com.anish.e_commerce.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final AdminService adminService;
    private final OrderService orderService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponse>> listOrders(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(orderService.listOrders(status, page, size));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
        @PathVariable Long id,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getStatus()));
    }

    @GetMapping("/products/low-stock")
    public ResponseEntity<List<Product>> lowStock(
        @RequestParam(defaultValue = "5") @Min(0) @Max(10000) int threshold
    ) {
        return ResponseEntity.ok(adminService.lowStock(threshold));
    }

    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders() {
        return csv("orders.csv", adminService.exportOrdersCsv());
    }

    @GetMapping("/products/export")
    public ResponseEntity<byte[]> exportProducts() {
        return csv("products.csv", adminService.exportProductsCsv());
    }

    private static ResponseEntity<byte[]> csv(String filename, String body) {
        return ResponseEntity.ok()
            .contentType(TEXT_CSV)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename).build().toString()
            )
            .body(body.getBytes(StandardCharsets.UTF_8));
    }
}
