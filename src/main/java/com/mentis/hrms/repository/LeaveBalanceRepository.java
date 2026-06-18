package com.mentis.hrms.repository;

import com.mentis.hrms.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    // Find all leave balances for an employee in current year
    List<LeaveBalance> findByEmployeeIdAndYear(String employeeId, Integer year);

    // Find specific leave type balance for an employee
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeAndYear(
            String employeeId, String leaveType, Integer year);

    // Check if employee has sufficient balance - UPDATED: Earned leaves always return true
    @Query("SELECT CASE WHEN :leaveType = 'earned' THEN true " +
            "ELSE CASE WHEN (lb.usedDays + :requestedDays) <= lb.totalDays THEN true ELSE false END END " +
            "FROM LeaveBalance lb WHERE lb.employeeId = :employeeId " +
            "AND lb.leaveType = :leaveType AND lb.year = :year")
    Boolean hasSufficientBalance(
            @Param("employeeId") String employeeId,
            @Param("leaveType") String leaveType,
            @Param("year") Integer year,
            @Param("requestedDays") Double requestedDays);

    // Update used days when leave is approved - UPDATED: Always allow earned leaves to increase
    @Modifying
    @Transactional
    @Query("UPDATE LeaveBalance lb SET lb.usedDays = lb.usedDays + :days " +
            "WHERE lb.employeeId = :employeeId AND lb.leaveType = :leaveType AND lb.year = :year")
    int addUsedDays(
            @Param("employeeId") String employeeId,
            @Param("leaveType") String leaveType,
            @Param("year") Integer year,
            @Param("days") Double days);

    // Decrease used days when leave is cancelled/rejected
    @Modifying
    @Transactional
    @Query("UPDATE LeaveBalance lb SET lb.usedDays = lb.usedDays - :days " +
            "WHERE lb.employeeId = :employeeId AND lb.leaveType = :leaveType AND lb.year = :year " +
            "AND lb.usedDays >= :days")
    int subtractUsedDays(
            @Param("employeeId") String employeeId,
            @Param("leaveType") String leaveType,
            @Param("year") Integer year,
            @Param("days") Double days);

    // Initialize default balances for new employee - UPDATED: Set earned leaves to a large number (999)
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO leave_balance (employee_id, leave_type, total_days, used_days, year) " +
            "VALUES " +
            "(:employeeId, 'earned', 999, 0, :year), " +
            "(:employeeId, 'sick', 12, 0, :year), " +
            "(:employeeId, 'casual', 7, 0, :year)", nativeQuery = true)
    void initializeEmployeeBalances(@Param("employeeId") String employeeId, @Param("year") Integer year);
}