package com.examforge.admin.controller;

import com.examforge.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal endpoint proving RBAC is enforced end to end. Real admin
 * functionality (user management, content moderation, etc.) arrives
 * with the modules that need it (Phase 9 onward).
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin-only diagnostics")
@SecurityRequirement(name = "bearerAuth")
public class AdminPingController {

    @GetMapping("/ping")
    @Operation(summary = "Verify ADMIN/SUPER_ADMIN access")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<String> ping() {
        return ApiResponse.success("pong");
    }
}
