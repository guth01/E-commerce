package com.example.miniproject.dto;

import com.example.miniproject.models.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private Role role; // ADMIN, VENDOR, or CUSTOMER
}