package com.mentis.hrms.repository;

import com.mentis.hrms.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.designations")
    List<Department> findAllWithDesignations();

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.designations WHERE d.name = :name")
    Optional<Department> findByNameWithDesignations(@Param("name") String name);
}