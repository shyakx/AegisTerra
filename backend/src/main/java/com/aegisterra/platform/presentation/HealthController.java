package com.aegisterra.platform.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health")
public class HealthController {

    private final String appVersion;

    public HealthController(@Value("${aegisterra.version:1.0.0}") String appVersion) {
        this.appVersion = appVersion;
    }

    @GetMapping("/health")
    @Operation(summary = "Application health probe")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "aegisterra-platform",
            "version", appVersion
        ));
    }
}
