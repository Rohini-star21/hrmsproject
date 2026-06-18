package com.mentis.hrms.repository;

import com.mentis.hrms.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    // Find all holidays on a specific date
    List<Holiday> findByHolidayDate(LocalDate date);

    // Find holidays between two dates
    List<Holiday> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);

    // Find holidays for a specific year and month
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.holidayDate) = :year AND MONTH(h.holidayDate) = :month ORDER BY h.holidayDate ASC")
    List<Holiday> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    // Find upcoming holidays from today
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate >= :today ORDER BY h.holidayDate ASC")
    List<Holiday> findUpcomingHolidays(@Param("today") LocalDate today);

    // Find holidays by type
    List<Holiday> findByHolidayType(String holidayType);

    // Check if holiday exists on date with same name (optional)
    boolean existsByHolidayDateAndHolidayName(LocalDate date, String holidayName);
}