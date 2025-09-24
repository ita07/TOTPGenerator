package io.github.ita07.serviceImpl;

import io.github.ita07.model.ValidationResult;
import io.github.ita07.service.TOTPService;
import io.github.ita07.util.TOTPGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of TOTP service
 */
@Service
public class TOTPServiceImpl implements TOTPService {

    private static final Logger logger = LoggerFactory.getLogger(TOTPServiceImpl.class);

    // Cache for TOTP generator instances to avoid recreating them
    private final Map<String, TOTPGenerator> generatorCache = new ConcurrentHashMap<>();

    /**
     * Gets or creates a cached TOTP generator instance
     */
    private TOTPGenerator getOrCreateGenerator(String secret, int digits, int period) {
        String cacheKey = secret + ":" + digits + ":" + period;
        return generatorCache.computeIfAbsent(cacheKey, key -> {
            logger.debug("Creating new TOTP generator for cache key: {}", cacheKey);
            return new TOTPGenerator(secret, digits, period);
        });
    }

    @Override
    public ValidationResult validateParams(String secret, int digits, int period) {
        if (secret == null || secret.trim().isEmpty()) {
            logger.warn("TOTP validation failed: Empty secret key");
            return ValidationResult.error("Secret key cannot be empty");
        }

        if (secret.trim().length() < 8) {
            logger.warn("TOTP validation failed: Secret key too short (length: {})", secret.trim().length());
            return ValidationResult.error("Secret key must be at least 8 characters long");
        }

        if (!secret.matches("^[A-Z2-7=\\s]*$")) {
            logger.warn("TOTP validation failed: Invalid Base32 characters in secret key");
            return ValidationResult.error("Secret key contains invalid Base32 characters. Only A-Z, 2-7, and = are allowed");
        }

        if (digits < TOTPGenerator.MIN_DIGITS || digits > TOTPGenerator.MAX_DIGITS) {
            logger.warn("TOTP validation failed: Invalid digits value: {}", digits);
            return ValidationResult.error(String.format("Digits must be between %d and %d",
                TOTPGenerator.MIN_DIGITS, TOTPGenerator.MAX_DIGITS));
        }

        if (period < TOTPGenerator.MIN_TIME_STEP || period > TOTPGenerator.MAX_TIME_STEP) {
            logger.warn("TOTP validation failed: Invalid period value: {}", period);
            return ValidationResult.error(String.format("Period must be between %d and %d seconds",
                TOTPGenerator.MIN_TIME_STEP, TOTPGenerator.MAX_TIME_STEP));
        }

        logger.debug("TOTP parameters validated successfully: digits={}, period={}", digits, period);
        return ValidationResult.success();
    }

    @Override
    public Map<String, Object> generateTotpData(String secret, int digits, int period) {
        logger.info("Generating TOTP code for REST endpoint with digits={}, period={}", digits, period);

        TOTPGenerator generator = getOrCreateGenerator(secret, digits, period);
        String code = generator.generateTOTP();
        long remainingTime = generator.getRemainingTime();
        double progressPercent = ((double) remainingTime / period) * 100;

        Map<String, Object> response = Map.of(
            "code", code,
            "remainingTime", remainingTime,
            "progressPercent", Double.parseDouble(String.format(Locale.US, "%.2f", progressPercent))
        );

        logger.debug("Successfully generated TOTP code, remaining time: {}s", remainingTime);
        return response;
    }

    @Override
    public Flux<String> createTotpStream(String secret, int digits, int period) {
        logger.info("Starting TOTP SSE stream with digits={}, period={}", digits, period);

        TOTPGenerator generator = getOrCreateGenerator(secret, digits, period);

        // Send initial code immediately
        String initialCode = generator.generateTOTP();
        long initialRemainingTime = generator.getRemainingTime();
        double initialProgressPercent = ((double) initialRemainingTime / period) * 100;
        String initialData = String.format(Locale.US, "{\"code\":\"%s\",\"remainingTime\":%d,\"progressPercent\":%.2f}",
                                         initialCode, initialRemainingTime, initialProgressPercent);

        return Flux.concat(
            // Send initial data immediately
            Flux.just(initialData),
            // Then send updates only when code changes
            Flux.interval(Duration.ofSeconds(1))
                .map(tick -> {
                    TOTPGenerator gen = getOrCreateGenerator(secret, digits, period);
                    return gen.generateTOTP();
                })
                .distinctUntilChanged() // Only emit when TOTP code changes
                .map(code -> {
                    TOTPGenerator gen = getOrCreateGenerator(secret, digits, period);
                    long remainingTime = gen.getRemainingTime();
                    double progressPercent = ((double) remainingTime / period) * 100;

                    return String.format(Locale.US, "{\"code\":\"%s\",\"remainingTime\":%d,\"progressPercent\":%.2f}",
                                       code, remainingTime, progressPercent);
                })
        );
    }
}