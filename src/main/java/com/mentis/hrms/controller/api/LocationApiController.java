package com.mentis.hrms.controller.api;

import com.mentis.hrms.model.Location;
import com.mentis.hrms.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")
public class LocationApiController {

    @Autowired
    private LocationService locationService;

    /**
     * GET ALL LOCATIONS - Full details
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllLocations() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Location> locations = locationService.getAllLocations();
            response.put("success", true);
            response.put("locations", locations);
            response.put("count", locations.size());
            response.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to fetch locations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * GET LOCATIONS LIST - Simplified for dropdown (with debug logging)
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getLocationsList() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Location> locations = locationService.getAllLocations();

            // Debug: Log the locations being returned
            System.out.println("📍 Locations API - Found " + locations.size() + " locations:");
            locations.forEach(loc -> System.out.println("   - " + loc.getId() + ": " + loc.getName() + " (" + loc.getCity() + ", " + loc.getCountry() + ")"));

            List<Map<String, Object>> locationList = locations.stream()
                    .map(loc -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", loc.getId());

                        String displayName = loc.getName();
                        if (loc.getCity() != null || loc.getCountry() != null) {
                            displayName += " (";
                            if (loc.getCity() != null) displayName += loc.getCity();
                            if (loc.getCity() != null && loc.getCountry() != null) displayName += ", ";
                            if (loc.getCountry() != null) displayName += loc.getCountry();
                            displayName += ")";
                        }
                        map.put("displayName", displayName);
                        map.put("name", loc.getName());
                        map.put("city", loc.getCity());
                        map.put("country", loc.getCountry());
                        return map;
                    })
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("locations", locationList);
            response.put("count", locationList.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); // Print full stack trace for debugging
            response.put("success", false);
            response.put("error", "Failed to fetch locations list: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * GET LOCATION BY ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLocationById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Location> locationOpt = locationService.getLocationById(id);

            if (locationOpt.isPresent()) {
                response.put("success", true);
                response.put("location", locationOpt.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Location not found with id: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to fetch location: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * CREATE NEW LOCATION
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createLocation(@RequestBody Location location) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Validate required fields
            if (location.getName() == null || location.getName().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Location name is required");
                return ResponseEntity.badRequest().body(response);
            }
            if (location.getCountry() == null || location.getCountry().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Country is required");
                return ResponseEntity.badRequest().body(response);
            }
            if (location.getCity() == null || location.getCity().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "City is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if location name already exists
            if (locationService.locationExists(location.getName())) {
                response.put("success", false);
                response.put("error", "Location with name '" + location.getName() + "' already exists");
                return ResponseEntity.badRequest().body(response);
            }

            // Set timestamps
            LocalDateTime now = LocalDateTime.now();
            location.setCreatedAt(now);
            location.setUpdatedAt(now);

            // Save to database
            Location savedLocation = locationService.saveLocation(location);

            response.put("success", true);
            response.put("message", "Location created successfully");
            response.put("location", savedLocation);
            response.put("id", savedLocation.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to create location: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * UPDATE LOCATION
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateLocation(@PathVariable Long id, @RequestBody Location location) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Check if location exists
            Optional<Location> existingOpt = locationService.getLocationById(id);
            if (!existingOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Location not found with id: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Location existing = existingOpt.get();

            // Check name uniqueness (if name changed)
            if (!existing.getName().equals(location.getName()) &&
                    locationService.locationExists(location.getName())) {
                response.put("success", false);
                response.put("error", "Location with name '" + location.getName() + "' already exists");
                return ResponseEntity.badRequest().body(response);
            }

            // Update fields
            existing.setName(location.getName());
            existing.setLocationType(location.getLocationType());
            existing.setCountry(location.getCountry());
            existing.setState(location.getState());
            existing.setCity(location.getCity());
            existing.setZipCode(location.getZipCode());
            existing.setAddress(location.getAddress());
            existing.setPhone(location.getPhone());
            existing.setEmail(location.getEmail());
            existing.setEmployees(location.getEmployees() != null ? location.getEmployees() : 0);
            existing.setUpdatedAt(LocalDateTime.now());

            // Save to database
            Location updatedLocation = locationService.saveLocation(existing);

            response.put("success", true);
            response.put("message", "Location updated successfully");
            response.put("location", updatedLocation);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to update location: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * DELETE LOCATION
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteLocation(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Check if location exists
            Optional<Location> existingOpt = locationService.getLocationById(id);
            if (!existingOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Location not found with id: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Delete from database
            locationService.deleteLocation(id);

            response.put("success", true);
            response.put("message", "Location deleted successfully");
            response.put("id", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to delete location: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    /**
     * TEST ENDPOINT - Remove after debugging
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("message", "Location API is working");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("locationsCount", locationService.getLocationCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    /**
     * CHECK IF LOCATION NAME EXISTS
     */
    @GetMapping("/check-name")
    public ResponseEntity<Map<String, Object>> checkLocationName(@RequestParam String name) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean exists = locationService.locationExists(name);

            response.put("success", true);
            response.put("exists", exists);
            response.put("name", name);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to check location name: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}