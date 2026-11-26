package com.beanstack.notifyx.identity.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanstack.notifyx.shared.response.ApiResponse;
import com.beanstack.notifyx.identity.dto.request.UpdateProfileRequest;
import com.beanstack.notifyx.identity.dto.response.UserResponse;
import com.beanstack.notifyx.identity.mapper.UserMapper;
import com.beanstack.notifyx.identity.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Read and update the authenticated user's profile.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    @GetMapping("/profile")
    @Operation(summary = "Get profile", description = "Returns the authenticated user's profile access information.")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> profile() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Profile accessed successfully",
                        Map.of("access", "USER")
                )
        );
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile", description = "Updates the authenticated user's display name.")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setFullName(request.fullName().trim());
        return ResponseEntity.ok(ApiResponse.success(
            "Profile updated successfully",
            userMapper.toUserResponse(userRepository.save(user))
        ));
    }
}
