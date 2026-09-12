package com.examforge.common.controller;

import com.examforge.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight system endpoint used to verify the Phase 1 foundation
 * (routing, response envelope, Swagger docs) is wired correctly end to end.
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "System/foundation diagnostics")
public class SystemController {

    @GetMapping("/info")
    @Operation(summary = "Get basic system info", description = "Confirms the API, response envelope, and docs pipeline are working")
    public ApiResponse<Map<String, String>> info() {
        return ApiResponse.success(Map.of(
                "application", "ExamForge",
                "phase", "Phase 1 - Foundation",
                "status", "UP"
        ));
    }
}
