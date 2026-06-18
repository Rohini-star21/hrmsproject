package com.mentis.hrms.repository;

import com.mentis.hrms.model.OfferLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferLetterRepository extends JpaRepository<OfferLetter, Long> {

    List<OfferLetter> findByApplicationId(Long applicationId);

    // ADD THIS: Find offers by employee ID
    List<OfferLetter> findByEmployeeId(String employeeId);

    Optional<OfferLetter> findByIdAndApplicationId(Long id, Long applicationId);

    @Query("SELECT ol FROM OfferLetter ol WHERE ol.application.id = :applicationId ORDER BY ol.createdDate DESC")
    List<OfferLetter> findLatestByApplicationId(@Param("applicationId") Long applicationId);

    List<OfferLetter> findByStatus(String status);

    @Query("SELECT COUNT(ol) FROM OfferLetter ol WHERE ol.status = :status")
    long countByStatus(@Param("status") String status);

    // ADD THESE METHODS:

    /**
     * Find all offer letters ordered by creation date (newest first)
     */
    List<OfferLetter> findAllByOrderByCreatedDateDesc();
}