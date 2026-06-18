package com.mentis.hrms.repository;

import com.mentis.hrms.model.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    List<Designation> findByDepartmentName(String departmentName);

    @Query("SELECT d FROM Designation d WHERE d.department.id = :departmentId")
    List<Designation> findByDepartmentId(@Param("departmentId") Long departmentId);

    boolean existsByNameAndDepartmentId(String name, Long departmentId);
}