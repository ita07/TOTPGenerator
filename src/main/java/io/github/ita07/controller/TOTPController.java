package io.github.ita07.controller;

import io.github.ita07.model.ValidationResult;
import io.github.ita07.service.TOTPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@Controller
@Tag(name = "TOTP Generator", description = "Time-based One-Time Password generation and management")
public class TOTPController {

    private static final Logger logger = LoggerFactory.getLogger(TOTPController.class);
    private final TOTPService totpService;

    public TOTPController(TOTPService totpService) {
        this.totpService = totpService;
    }

    @GetMapping("/totp-data")
    @ResponseBody
    @Operation(summary = "Get TOTP Data", description = "Returns current TOTP code and timing information as JSON")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated TOTP data",
                    content = @Content(mediaType = "application/json",
                              examples = @ExampleObject(value = "{\"code\":\"123456\",\"remainingTime\":25,\"progressPercent\":83.33}"))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters provided")
    })
    public ResponseEntity<?> totpData(@Parameter(description = "Base32 encoded secret key", example = "JBSWY3DPEHPK3PXP")
                                      @RequestParam(defaultValue = "JBSWY3DPEHPK3PXP") String secret,
                                      @Parameter(description = "Number of digits in the TOTP code (4-10)", example = "6")
                                      @RequestParam(defaultValue = "6") int digits,
                                      @Parameter(description = "Time period in seconds for each TOTP code (15-300)", example = "30")
                                      @RequestParam(defaultValue = "30") int period) {

        // Validate input parameters
        ValidationResult validation = totpService.validateParams(secret, digits, period);
        if (validation.hasError()) {
            return ResponseEntity.badRequest().body(Map.of("error", validation.getErrorMessage()));
        }

        try {
            Map<String, Object> response = totpService.generateTotpData(secret, digits, period);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to generate TOTP code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate TOTP code"));
        }
    }

    @GetMapping("/health")
    @ResponseBody
    @Operation(summary = "Health Check", description = "Returns application health status")
    @ApiResponse(responseCode = "200", description = "Application is healthy",
                content = @Content(mediaType = "application/json",
                          examples = @ExampleObject(value = "{\"status\":\"UP\"}")))
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping(value = "/totp-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    @Operation(summary = "Stream TOTP Data", description = "Returns real-time TOTP code updates via Server-Sent Events")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully streaming TOTP data",
                    content = @Content(mediaType = "text/event-stream")),
        @ApiResponse(responseCode = "400", description = "Invalid parameters provided")
    })
    public Flux<String> totpStream(@Parameter(description = "Base32 encoded secret key", example = "JBSWY3DPEHPK3PXP")
                                   @RequestParam(defaultValue = "JBSWY3DPEHPK3PXP") String secret,
                                   @Parameter(description = "Number of digits in the TOTP code (4-10)", example = "6")
                                   @RequestParam(defaultValue = "6") int digits,
                                   @Parameter(description = "Time period in seconds for each TOTP code (15-300)", example = "30")
                                   @RequestParam(defaultValue = "30") int period) {

        // Validate parameters for SSE stream
        ValidationResult validation = totpService.validateParams(secret, digits, period);
        if (validation.hasError()) {
            logger.warn("SSE stream validation failed for parameters: digits={}, period={}", digits, period);
            String errorMessage = String.format("{\"error\":\"%s\"}",
                validation.getErrorMessage().replace("\"", "\\\""));
            return Flux.just(errorMessage);
        }

        return totpService.createTotpStream(secret, digits, period);
    }
}