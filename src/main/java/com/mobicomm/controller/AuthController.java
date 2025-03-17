package com.mobicomm.controller;

import com.mobicomm.dto.JwtResponse;
import com.mobicomm.dto.LoginRequest;
import com.mobicomm.dto.ErrorResponse;
import com.mobicomm.entity.User;
import com.mobicomm.repository.UserRepository;
import com.mobicomm.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.util.List;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository; // Add this field

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = jwtTokenUtil.generateJwtToken(authentication);

        // Get user details
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Get roles
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Return JWT in response
        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getUsername(),
                roles
        ));
    }

    /**
     * Validate mobile number without full authentication
     */
    @PostMapping("/validate-mobile")
    public ResponseEntity<?> validateMobileNumber(@RequestBody LoginRequest loginRequest) {
        String mobileNumber = loginRequest.getUsername();

        // Validate mobile number format
        if (!isValidMobileNumber(mobileNumber)) {
            return ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse("Invalid mobile number format", HttpStatus.BAD_REQUEST.value()));
        }

        // Check if the mobile number exists in the database
        Optional<User> userOptional = userRepository.findByMobileNumber(mobileNumber);

        if (userOptional.isPresent()) {
            // Generate token for the user
            String jwt = jwtTokenUtil.generateTokenFromUsername(mobileNumber);

            return ResponseEntity.ok(new JwtResponse(
                    jwt,
                    mobileNumber,
                    List.of("ROLE_USER")
            ));
        } else {
            // Mobile number not found
            return ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse("Mobile number not registered", HttpStatus.NOT_FOUND.value()));
        }
    }

    // Helper method to validate mobile number
    private boolean isValidMobileNumber(String mobileNumber) {
        // Regex for Indian mobile numbers (starts with 6-9, exactly 10 digits)
        return mobileNumber != null && mobileNumber.matches("^[6-9]\\d{9}$");
    }


    @GetMapping("/user")
    public ResponseEntity<?> getUserInfo() {
        // Get the authenticated user from the Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
                null, // No need to send token again
                userDetails.getUsername(),
                roles
        ));
    }
}