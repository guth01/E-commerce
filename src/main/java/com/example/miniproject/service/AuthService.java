package com.example.miniproject.service;

import com.example.miniproject.dto.RegisterRequest;
import com.example.miniproject.repository.UserRepository;
import com.example.miniproject.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication; // CORRECT Spring Security Import
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtUtils jwtUtils;
    public String register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        com.example.miniproject.models.User user = new com.example.miniproject.models.User();
        user.setUsername(req.getUsername());
        // ALWAYS hash passwords before saving them!
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(req.getRole());

        userRepository.save(user);
        return "User registered successfully!";
    }
    public String login(String username, String password) {
        // Authenticate the user against the database
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        // Set the authentication in the Security Context
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Generate and return the JWT
        return jwtUtils.generateToken(auth);
    }
}