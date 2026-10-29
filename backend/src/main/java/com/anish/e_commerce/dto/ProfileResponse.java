package com.anish.e_commerce.dto;

import java.util.Set;

public record ProfileResponse(
    String username,
    Set<String> roles,
    long orderCount,
    long reviewCount,
    long addressCount
) {}
