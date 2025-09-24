package io.github.ita07.service;

import io.github.ita07.model.ValidationResult;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Service interface for TOTP operations
 */
public interface TOTPService {

    /**
     * Validates TOTP parameters
     */
    ValidationResult validateParams(String secret, int digits, int period);

    /**
     * Generates TOTP data for REST endpoint
     */
    Map<String, Object> generateTotpData(String secret, int digits, int period);

    /**
     * Creates SSE stream for real-time TOTP updates
     */
    Flux<String> createTotpStream(String secret, int digits, int period);
}