package com.mentis.hrms.service;

import com.mentis.hrms.model.Location;
import com.mentis.hrms.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service  // Make sure this annotation is present
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    // Get all locations
    @Transactional(readOnly = true)
    public List<Location> getAllLocations() {
        return locationRepository.findAllByOrderByNameAsc();
    }

    // Get location by ID
    @Transactional(readOnly = true)
    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }

    // Save location (create or update)
    @Transactional
    public Location saveLocation(Location location) {
        return locationRepository.save(location);
    }

    // Delete location by ID
    @Transactional
    public void deleteLocation(Long id) {
        locationRepository.deleteById(id);
    }

    // Check if location exists by name
    @Transactional(readOnly = true)
    public boolean locationExists(String name) {
        return locationRepository.existsByName(name);
    }

    // Get locations by country
    @Transactional(readOnly = true)
    public List<Location> getLocationsByCountry(String country) {
        return locationRepository.findByCountryOrderByNameAsc(country);
    }

    // Get locations by city
    @Transactional(readOnly = true)
    public List<Location> getLocationsByCity(String city) {
        return locationRepository.findByCityOrderByNameAsc(city);
    }

    // Get location count
    @Transactional(readOnly = true)
    public long getLocationCount() {
        return locationRepository.count();
    }

    // Update employee count for a location
    @Transactional
    public void updateEmployeeCount(Long locationId, int newCount) {
        locationRepository.findById(locationId).ifPresent(location -> {
            location.setEmployees(newCount);
            locationRepository.save(location);
        });
    }
}