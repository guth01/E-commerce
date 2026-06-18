package com.example.miniproject.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    // Anyone can access this (if you add /api/public/** to permitAll)
    // Or you can just rely on anyRequest().authenticated() if you didn't!
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("You have successfully passed the JWT Filter!");
    }

    // Only ADMIN tokens can access this
    @GetMapping("/admin/test")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Welcome, Admin! Your token works perfectly.");
    }
}