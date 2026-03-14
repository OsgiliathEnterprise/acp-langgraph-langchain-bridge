package com.example.erp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller demonstrating MVC pattern and RESTful API design.
 * This class handles HTTP requests and responses.
 *
 * Key characteristics:
 * - Model-View-Controller (MVC) pattern
 * - RESTful API design
 * - HTTP status code handling
 * - Request/Response mapping
 *
 * Design patterns used:
 * - MVC pattern (Controller layer)
 * - Front Controller pattern
 * - Data Transfer Object (DTO) pattern
 * - Command pattern (request handlers)
 */
@RestController
@RequestMapping("/api/resources")
public class Controller {

    private final Service service;

    /**
     * Constructor injection for service dependency.
     * Design pattern: Dependency Injection
     *
     * @param service the business service
     */
    public Controller(Service service) {
        this.service = service;
    }

    /**
     * Handles GET requests to retrieve a resource.
     * HTTP Method: GET
     *
     * @param id the resource identifier
     * @return response with resource data
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResourceDTO> getResource(@PathVariable Long id) {
        ResourceDTO resource = service.findById(id);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(resource);
    }

    /**
     * Handles POST requests to create a new resource.
     * HTTP Method: POST
     *
     * @param request the resource creation request
     * @return response with created resource
     */
    @PostMapping
    public ResponseEntity<ResourceDTO> createResource(@RequestBody CreateResourceRequest request) {
        // Validate request
        if (request.getName() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Create resource via service
        ResourceDTO created = service.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Handles PUT requests to update an existing resource.
     * HTTP Method: PUT
     *
     * @param id the resource identifier
     * @param request the update request
     * @return response with updated resource
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResourceDTO> updateResource(
            @PathVariable Long id,
            @RequestBody UpdateResourceRequest request) {

        ResourceDTO updated = service.update(id, request);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(updated);
    }

    /**
     * Handles DELETE requests to remove a resource.
     * HTTP Method: DELETE
     *
     * @param id the resource identifier
     * @return response indicating success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        boolean deleted = service.delete(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Mock service interface.
     */
    interface Service {
        ResourceDTO findById(Long id);
        ResourceDTO create(CreateResourceRequest request);
        ResourceDTO update(Long id, UpdateResourceRequest request);
        boolean delete(Long id);
    }

    /**
     * Data Transfer Object for resource representation.
     */
    public static class ResourceDTO {
        private Long id;
        private String name;
        private String status;

        // Getters and setters omitted for brevity
    }

    /**
     * Request object for resource creation.
     */
    public static class CreateResourceRequest {
        private String name;

        public String getName() {
            return name;
        }
    }

    /**
     * Request object for resource updates.
     */
    public static class UpdateResourceRequest {
        private String name;
        private String status;
    }
}

