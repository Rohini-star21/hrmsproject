package com.mentis.hrms.repository;

import com.mentis.hrms.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /* --- duplicate-check (case-insensitive) --- */
    @Query("SELECT COUNT(a) > 0 FROM JobApplication a " +
            "WHERE LOWER(a.email) = LOWER(:email) AND a.job.id = :jobId")
    boolean existsByEmailIgnoreCaseAndJobId(@Param("email") String email,
                                            @Param("jobId") Long jobId);

    /* --- top-5 recent --- */
    List<JobApplication> findTop5ByOrderByApplicationDateDesc();

    /* --- counter by status --- */
    long countByStatus(String status);

    /* --- Find by ID --- */
    Optional<JobApplication> findById(Long id);

    /* --- Find applications by status ordered by application date --- */
    List<JobApplication> findByStatusOrderByApplicationDateDesc(String status);

    /* --- Find top 5 applications by status ordered by application date --- */
    List<JobApplication> findTop5ByStatusOrderByApplicationDateDesc(String status);
    /* --- Get all applications ordered by date descending --- */
    List<JobApplication> findAllByOrderByApplicationDateDesc();
    /* --- NEW: Find applications with "Hired" status --- */
    @Query("SELECT ja FROM JobApplication ja WHERE UPPER(ja.status) = UPPER('Hired') ORDER BY ja.applicationDate DESC")
    List<JobApplication> findHiredCandidates();

    /* --- NEW: Find all applications with their jobs loaded --- */
    @Query("SELECT DISTINCT ja FROM JobApplication ja LEFT JOIN FETCH ja.job ORDER BY ja.applicationDate DESC")
    List<JobApplication> findAllWithJob();

    /* --- NEW: Find applications by status (case-insensitive) --- */
    @Query("SELECT ja FROM JobApplication ja WHERE UPPER(ja.status) = UPPER(:status) ORDER BY ja.applicationDate DESC")
    List<JobApplication> findByStatusIgnoreCase(@Param("status") String status);
}