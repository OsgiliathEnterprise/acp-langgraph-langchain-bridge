package com.example.erp.service;

import org.springframework.transaction.annotation.Transactional;

/**
 * Business service layer class demonstrating service pattern.
 * This class handles business logic and transaction management.
 *
 * Key characteristics:
 * - Service Layer pattern
 * - Transaction management with Spring
 * - Dependency Injection ready
 *
 * Design patterns used:
 * - Service Layer pattern
 * - Transaction Script pattern
 * - Repository pattern (via dependencies)
 */
@Service
@Transactional
public class Service {

    private final Repository repository;

    /**
     * Constructor injection for dependencies.
     * Design pattern: Dependency Injection
     *
     * @param repository the data repository
     */
    public Service(Repository repository) {
        this.repository = repository;
    }

    /**
     * Processes a business operation.
     *
     * @param data the data to process
     * @return processing result
     */
    public Result processOperation(String data) {
        // Validate input
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        // Business logic
        String processed = transformData(data);

        // Persist changes
        repository.save(processed);

        return new Result(true, "Operation completed successfully");
    }

    private String transformData(String data) {
        // Business transformation logic
        return data.toUpperCase();
    }

    /**
     * Mock repository interface.
     */
    interface Repository {
        void save(String data);
    }

    /**
     * Result value object.
     */
    public static class Result {
        private final boolean success;
        private final String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}

