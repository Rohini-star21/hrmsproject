package com.mentis.hrms.repository;

import com.mentis.hrms.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository  // Make sure this annotation is present
public interface LocationRepository extends JpaRepository<Location, Long> {

    // Find all locations ordered by name
    List<Location> findAllByOrderByNameAsc();

    // Check if location exists by name
    boolean existsByName(String name);

    // Find locations by country
    List<Location> findByCountryOrderByNameAsc(String country);

    // Find locations by city
    List<Location> findByCityOrderByNameAsc(String city);

    // Custom query to get location with employee count
    @Query("SELECT l FROM Location l ORDER BY l.name ASC")
    List<Location> findAllWithDetails();

    // Find by name (case insensitive)
    Optional<Location> findByNameIgnoreCase(String name);
}