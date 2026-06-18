package com.example.miniproject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Lombok creates a constructor with the token argument
public class JwtResponse {
    private String token;
}