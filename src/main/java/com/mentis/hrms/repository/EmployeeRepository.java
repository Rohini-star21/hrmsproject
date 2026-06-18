package com.mentis.hrms.repository;
import com.mentis.hrms.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.mentis.hrms.model.enums.UserRole;
import java.util.List;
import org.springframework.data.domain.Sort; // Add this import
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByPersonalEmail(String personalEmail);


    // Existing methods
    List<Employee> findByDocumentDeadlineIsNotNull();
    List<Employee> findByDocumentDeadlineIsNotNullAndOnboardingStatusIn(List<String> statuses);

    // NEW: Find all employees sorted by createdDate descending (newest first)
    List<Employee> findAllByOrderByCreatedDateDesc();
}