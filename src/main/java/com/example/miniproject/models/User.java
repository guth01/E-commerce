package com.example.miniproject.models;

import jakarta.persistence.*;
import lombok.Data;

@Data // This Lombok annotation generates getters and setters automatically!
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role; // ADMIN, VENDOR, CUSTOMER
}