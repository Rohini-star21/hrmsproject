package com.mentis.hrms.repository;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.OnboardingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnboardingDocumentRepository extends JpaRepository<OnboardingDocument, Long> {
    List<OnboardingDocument> findByEmployee(Employee employee);
    Optional<OnboardingDocument> findByEmployeeAndDocumentType(Employee employee, String documentType);
    Long countByEmployeeAndStatus(Employee employee, String status);
    List<OnboardingDocument> findByStatus(String status);
    Long countByEmployee(Employee employee);

    // ADD THESE MISSING METHODS
    List<OnboardingDocument> findByEmployeeAndStatus(Employee employee, String status);

    @Query("SELECT od FROM OnboardingDocument od WHERE od.employee.employeeId = :employeeId")
    List<OnboardingDocument> findByEmployeeId(@Param("employeeId") String employeeId);

    @Query("SELECT od FROM OnboardingDocument od WHERE od.employee.employeeId = :employeeId AND od.status = :status")
    List<OnboardingDocument> findByEmployeeIdAndStatus(@Param("employeeId") String employeeId, @Param("status") String status);



}