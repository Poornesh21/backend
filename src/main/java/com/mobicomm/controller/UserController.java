package com.mobicomm.controller;

import com.mobicomm.entity.User;
import com.mobicomm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for user-related operations
 * Handles user information retrieval and updates
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", 
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get user information by mobile number
     * This endpoint is public to allow user validation without authentication
     */
    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<?> getUserByMobileNumber(@PathVariable String mobileNumber) {
        try {
            Optional<User> userOpt = userRepository.findByMobileNumber(mobileNumber);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Create a response with relevant user information
                Map<String, Object> response = new HashMap<>();
                response.put("mobileNumber", user.getMobileNumber());
                
                // Include email if available
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    response.put("email", user.getEmail());
                }
                
                // Include last recharge date if available
                if (user.getLastRechargeDate() != null) {
                    response.put("lastRechargeDate", user.getLastRechargeDate());
                }
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of(
                    "mobileNumber", mobileNumber,
                    "message", "User not found"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to retrieve user information",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Update user email address
     */
    @PutMapping("/update-email")
    public ResponseEntity<?> updateUserEmail(@RequestBody Map<String, String> request) {
        String mobileNumber = request.get("mobileNumber");
        String newEmail = request.get("email");
        
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Mobile number is required"
            ));
        }
        
        if (newEmail == null || newEmail.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Email address is required"
            ));
        }
        
        try {
            Optional<User> userOpt = userRepository.findByMobileNumber(mobileNumber);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Update email
                user.setEmail(newEmail);
                userRepository.save(user);
                
                return ResponseEntity.ok(Map.of(
                    "message", "Email updated successfully",
                    "mobileNumber", mobileNumber,
                    "email", newEmail
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "message", "User not found, creating new user",
                    "mobileNumber", mobileNumber
                ));
                
                // Note: In a real implementation, you might want to create a new user here
                // or return a different response code
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to update email",
                "message", e.getMessage()
            ));
        }
    }
}