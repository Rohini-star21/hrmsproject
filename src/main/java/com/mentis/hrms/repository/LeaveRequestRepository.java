package com.mentis.hrms.repository;

import com.mentis.hrms.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, String> {

    Optional<LeaveRequest> findById(String id);

    @Query("SELECT l FROM LeaveRequest l WHERE l.employeeId = :employeeId " +
            "AND UPPER(l.status) = 'APPROVED' " +
            "AND l.startDate <= :endDate " +
            "AND l.endDate >= :startDate")
    List<LeaveRequest> findApprovedLeavesForEmployeeInRange(
            @Param("employeeId") String employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<LeaveRequest> findByStatus(String status);
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);

    @Query("SELECT l FROM LeaveRequest l WHERE l.employeeId = :employeeId " +
            "AND l.startDate <= :date AND l.endDate >= :date " +
            "AND l.status IN ('PENDING', 'APPROVED')")
    List<LeaveRequest> findExistingLeavesForDate(
            @Param("employeeId") String employeeId,
            @Param("date") LocalDate date);
}