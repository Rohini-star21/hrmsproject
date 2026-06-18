package com.mentis.hrms.repository;
import com.mentis.hrms.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {


    // Find the MOST RECENT active session (not checked out)
    @Query("SELECT a FROM Attendance a WHERE a.employeeId = :employeeId " +
            "AND a.attendanceDate = :date AND a.checkOutTime IS NULL " +
            "ORDER BY a.checkInTime DESC")
    Optional<Attendance> findActiveAttendanceByEmployeeAndDate(
            @Param("employeeId") String employeeId,
            @Param("date") LocalDate date);

    // Find ALL attendances for an employee on a specific date
    List<Attendance> findByEmployeeIdAndAttendanceDate(String employeeId, LocalDate date);

    // Find the LATEST attendance for an employee on a date
    @Query("SELECT a FROM Attendance a WHERE a.employeeId = :employeeId " +
            "AND a.attendanceDate = :date ORDER BY a.checkInTime DESC")
    List<Attendance> findLatestByEmployeeIdAndAttendanceDate(
            @Param("employeeId") String employeeId,
            @Param("date") LocalDate date);

    // Find attendances between dates
    List<Attendance> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            String employeeId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(
            String employeeId, LocalDate startDate, LocalDate endDate);
    // Add this method
    List<Attendance> findByEmployeeIdAndAttendanceDateOrderByCheckInTimeDesc(String employeeId, LocalDate attendanceDate);
    // Find by date for HR dashboard
    List<Attendance> findByAttendanceDate(LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date ORDER BY a.checkInTime DESC")
    List<Attendance> findByAttendanceDateOrderByCheckInTimeDesc(@Param("date") LocalDate date);

    List<Attendance> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);

    long countByEmployeeIdAndAttendanceDate(String employeeId, LocalDate attendanceDate);

    List<Attendance> findByEmployeeIdOrderByAttendanceDateDesc(String employeeId);

    // CRITICAL: Delete old inactive sessions for cleanup
    @Query("DELETE FROM Attendance a WHERE a.employeeId = :employeeId " +
            "AND a.attendanceDate = :date AND a.checkOutTime IS NOT NULL " +
            "AND a.id NOT IN (SELECT MAX(a2.id) FROM Attendance a2 WHERE a2.employeeId = :employeeId AND a2.attendanceDate = :date)")
    void deleteDuplicateCheckouts(@Param("employeeId") String employeeId, @Param("date") LocalDate date);
}
